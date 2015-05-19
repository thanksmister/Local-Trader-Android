/*
 * Copyright (c) 2015 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.data.services;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.DbOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription walletSubscription = Subscriptions.empty();
    private Subscription contactsSubscription = Subscriptions.empty();
    private Subscription tokensSubscription = Subscriptions.empty();

    SQLiteOpenHelper dbOpenHelper;
    private LocalBitcoins localBitcoins;
    private DbManager dbManager;
    private SharedPreferences sharedPreferences;
    private StringPreference stringPreference;
  
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);

        sharedPreferences = getContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        stringPreference = new StringPreference(sharedPreferences, DataService.PREFS_USER);

        localBitcoins = initLocalBitcoins();
        dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        SqlBrite db = SqlBrite.create(dbOpenHelper);
        dbManager = new DbManager(db);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        if(dbManager.isLoggedIn()) {
            updateContacts();
        } else {
            walletSubscription.unsubscribe();
            contactsSubscription.unsubscribe();
            tokensSubscription.unsubscribe();
        }
    }

    @Override
    public void onSyncCanceled()
    {
        super.onSyncCanceled();

        walletSubscription.unsubscribe();
        contactsSubscription.unsubscribe();
        tokensSubscription.unsubscribe();
    }

    private void updateContacts()
    {
        if(!contactsSubscription.isUnsubscribed())
            return;
        
        Timber.d("UpdateContacts");
        
        contactsSubscription = getContactsObservable()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> items)
                    {
                        contactsSubscription.unsubscribe();
                        
                        saveContactsAndNotify(items);

                        updateWalletBalance();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        contactsSubscription.unsubscribe();
                                
                        handleError(throwable);
                    }
                });
    }
    
    private void updateWalletBalance()
    {
        if(!walletSubscription.isUnsubscribed())
            return;
        
        Timber.d("UpdateWalletBalance");
        
        walletSubscription = getWalletBalance()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Wallet>()
                {
                    @Override
                    public void call(Wallet wallet)
                    {
                        walletSubscription.unsubscribe();
                        
                        updateWalletBalance(wallet);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        walletSubscription.unsubscribe();
                        
                        handleError(throwable);
                    }
                });
    }
    
    private void refreshAccessTokens()
    {
        if(!tokensSubscription.isUnsubscribed())
            return;

        Timber.d("RefreshAccessTokens");
        
        tokensSubscription = refreshTokens()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization>()
                {
                    @Override
                    public void call(Authorization authorization)
                    {
                        tokensSubscription.unsubscribe();
                        
                        updateTokens(authorization);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        tokensSubscription.unsubscribe();
                        
                        reportError(throwable);
                    }
                });
    }
    
    protected void reportError(Throwable throwable)
    {
        if(throwable != null && throwable.getLocalizedMessage() != null)
            Timber.e("Sync Data Error: " + throwable.getLocalizedMessage());
    }

    protected void handleError(Throwable throwable)
    {
        reportError(throwable);
        
        if(DataServiceUtils.isHttp403Error(throwable) || DataServiceUtils.isHttp400GrantError(throwable)) {
            refreshAccessTokens();
        }
    }

    private void updateWalletBalance(Wallet wallet)
    {
        Observable<WalletItem> walletObservable = dbManager.walletQuery();
        walletObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<WalletItem>()
                {
                    @Override
                    public void call(WalletItem walletItem)
                    {
                        if (walletItem == null) {
                            updateWallet(wallet);
                            NotificationUtils.createMessageNotification(getContext(), "Bitcoin Balance", "Bitcoin balance...", "You have " + wallet.total.balance + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                            return;
                        }

                        double newBalance = Doubles.convertToDouble(wallet.total.balance);
                        double oldBalance = Doubles.convertToDouble(walletItem.balance());

                        String address = walletItem.address();
                        String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);

                        if (newBalance > oldBalance) {
                            NotificationUtils.createMessageNotification(getContext(), "Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                            updateWallet(wallet);
                        } else if (!address.equals(wallet.address.address)) {
                            updateWallet(wallet);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        Timber.e(throwable.getMessage());
                    }
                });
    }
    
    private void updateWallet(Wallet wallet)
    {
        dbManager.updateWallet(wallet);
    }
    
    private void getDeletedContactsInfo(List<Contact> contacts)
    {
        getContactInfo(contacts)
                .onErrorResumeNext(getContactInfo(contacts))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) 
                    {
                        if (contacts.size() > 1) {
                            
                            List<Contact> canceled = new ArrayList<Contact>();
                            List<Contact> released = new ArrayList<Contact>();
                            for (Contact contact : contacts) {
                                if(TradeUtils.isCanceledTrade(contact)) {
                                    canceled.add(contact);
                                } else if (TradeUtils.isReleased(contact)){
                                    released.add(contact);
                                }
                            }
                            
                            if(canceled.size() > 1 && released.size() > 1) {
                                NotificationUtils.createNotification(getContext(), "Trades canceled or released", "Trades canceled or released...", "Two or more of your trades have been canceled or released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                            } else if (canceled.size() > 1) {
                                NotificationUtils.createNotification(getContext(), "Trades canceled", "Trades canceled...", "Two or more of your trades have been canceled.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                            } else if (canceled.size() > 1) {
                                NotificationUtils.createNotification(getContext(), "Trades released", "Trades released...", "Two or more of your trades have been released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                            }
                        } else {
                            Contact contact = contacts.get(0);
                            String contactName = TradeUtils.getContactName(contact);
                            String saleType = (contact.is_selling) ? " with buyer " : " with seller ";
                            if (TradeUtils.isCanceledTrade(contact)) {
                                NotificationUtils.createNotification(getContext(), "Trade Canceled", ("Trade with" + contactName + " canceled."), ("Trade #" + contact.contact_id + saleType + contactName + " has been canceled."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
                            } else if (TradeUtils.isReleased(contact)) {
                                NotificationUtils.createNotification(getContext(), "Trade Released", ("Trade with" + contactName + " released."), ("Trade #" + contact.contact_id + saleType + contactName + " has been released."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
                            }
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable);
                    }
                });
    }

    private Observable<Wallet> getWalletBalance()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;

                        Timber.d("Access Token: " + sessionItem.access_token());

                        return localBitcoins.getWalletBalance(sessionItem.access_token())
                                .map(new ResponseToWalletBalance())
                                .flatMap(new Func1<Wallet, Observable<Wallet>>()
                                {
                                    @Override
                                    public Observable<Wallet> call(Wallet wallet)
                                    {
                                        return generateBitmap(wallet.address.address)
                                                .map(new Func1<Bitmap, Wallet>()
                                                {
                                                    @Override
                                                    public Wallet call(Bitmap bitmap)
                                                    {
                                                        wallet.qrImage = bitmap;
                                                        return wallet;
                                                    }
                                                }).onErrorReturn(new Func1<Throwable, Wallet>()
                                                {
                                                    @Override
                                                    public Wallet call(Throwable throwable)
                                                    {
                                                        return wallet;
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private Observable<Bitmap> generateBitmap(final String address)
    {
        return Observable.create((Subscriber<? super Bitmap> subscriber) -> {
            try {
                subscriber.onNext(WalletUtils.encodeAsBitmap(address, getContext()));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
    
    private Observable<List<Contact>> getContactInfo(List<Contact> contacts)
    {
        List<Contact> contactList = Collections.emptyList();
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(SessionItem sessionItem)
                    {
                        if(sessionItem == null) return null;

                        Timber.d("Access Token: " + sessionItem.access_token());
                        
                        return Observable.just(Observable.from(contacts)
                                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>()
                                {
                                    @Override
                                    public Observable<? extends List<Contact>> call(final Contact contact)
                                    {
                                        return localBitcoins.getContact(contact.contact_id, sessionItem.access_token())
                                                .map(new ResponseToContact())
                                                .map(new Func1<Contact, List<Contact>>()
                                                {
                                                    @Override
                                                    public List<Contact> call(Contact contactResult)
                                                    {
                                                        if (TradeUtils.isCanceledTrade(contactResult) || TradeUtils.isReleased(contactResult)) {
                                                            contactList.add(contactResult);
                                                        }

                                                        return contactList;
                                                    }
                                                });
                                    }
                                }).toBlocking().lastOrDefault(contactList));
                    }
                });
    }

    private Observable<List<Contact>> getContactsObservable()
    {
        // TODO handle bad token
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;

                        Timber.d("Access Token: " + sessionItem.access_token());

                        return localBitcoins.getDashboard(sessionItem.access_token())
                                .map(new ResponseToContacts())
                                .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                                {
                                    @Override
                                    public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                    {
                                        if (contacts.isEmpty()) {
                                            return Observable.just(contacts);
                                        }
                                        return getContactsMessageObservable(contacts, sessionItem.access_token());
                                    }
                                });
                    }
                });
    }

    private Observable<List<Contact>> getContactsMessageObservable(final List<Contact> contacts, final String access_token)
    {
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>()
                {
                    @Override
                    public Observable<? extends List<Contact>> call(final Contact contact)
                    {
                        return localBitcoins.contactMessages(contact.contact_id, access_token)
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, List<Contact>>()
                                {
                                    @Override
                                    public List<Contact> call(List<Message> messages)
                                    {
                                        for (Message message : messages) {
                                            message.contact_id = contact.contact_id;
                                        }

                                        contact.messages = messages;
                                        return contacts;
                                    }
                                });
                    }
                }).toBlocking().last());
    }

    
    public Observable<SessionItem> getTokens()
    {
        return dbManager.getTokens();
    }
    
    public void updateTokens(Authorization authorization)
    {
        Timber.d("Access Token: " + authorization.access_token);
        Timber.d("Refresh Token : " + authorization.refresh_token);
        
        dbManager.updateTokens(authorization);
    }
    
    LocalBitcoins initLocalBitcoins()
    {
        OkHttpClient okHttpClient = new OkHttpClient();
        OkClient client = new OkClient(okHttpClient);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(Constants.BASE_URL)
                .build();
        return restAdapter.create(LocalBitcoins.class);
    }

    /*private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed)
    {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                // Here check if the error thrown really is a 401
                if (DataServiceUtils.isHttp403Error(throwable)) {
                    
                    Timber.e("isHttp403Error");
                    
                    return refreshTokens().flatMap(new Func1<String, Observable<? extends T>>() {
                        @Override
                        public Observable<? extends T> call(String token)
                        {
                            Timber.e("Sync Error refreshTokenAndRetry");
                            return toBeResumed;
                        }
                    });
                }
                // re-throw this error because it's not recoverable from here
                return Observable.error(throwable);
            }
        };
    }*/

    private Observable<Authorization> refreshTokens()
    {
        Timber.d("refreshTokens");

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Authorization>>() {
                    @Override
                    public Observable<Authorization> call(SessionItem sessionItem)
                    {
                        if(sessionItem == null) 
                            return null;
                        
                        Timber.d("Refresh Token: " + sessionItem.refresh_token());
                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize());
                    }
                });
    }

    private void saveContactsAndNotify(final List<Contact> contacts)
    {
        TreeMap<String, ArrayList<Contact>> updatedContactList = dbManager.updateContacts(contacts);
        ArrayList<Contact> updatedContacts = updatedContactList.get(DbManager.UPDATES);
        Timber.d("updated contacts: " + updatedContacts.size());

        ArrayList<Contact> addedContacts = updatedContactList.get(DbManager.ADDITIONS);
        Timber.d("added contacts: " + addedContacts.size());

        ArrayList<Contact> deletedContacts = updatedContactList.get(DbManager.DELETIONS);
        Timber.d("deleted contacts: " + deletedContacts.size());

        if (updatedContacts.size() > 1) {
            NotificationUtils.createNotification(getContext(), "Trade Updates", "Trade status updates..", "Two or more of your trades have been updated.", NotificationUtils.NOTIFICATION_TYPE_CONTACT, null);
        } else if (updatedContacts.size() == 1) {
            Contact contact = updatedContacts.get(0);
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling) ? " with buyer " : " with seller ";
            NotificationUtils.createNotification(getContext(), "Trade Updated", ("The trade with" + contactName + " updated."), ("Trade #" + contact.contact_id + saleType + contactName + " has been updated."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }

        // notify user of any new trades
        if (addedContacts.size() > 1) {
            NotificationUtils.createNotification(getContext(), "New Trades", "You have new trades to buy or sell bitcoin!", "You have " + addedContacts.size() + " new trades to buy or sell bitcoins.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else if (addedContacts.size() == 1) {
            Contact contact = addedContacts.get(0);
            String username = TradeUtils.getContactName(contact);
            String type = (contact.is_buying) ? "sell" : "buy";
            String location = (TradeUtils.isLocalTrade(contact)) ? "local" : "online";
            NotificationUtils.createNotification(getContext(), "New trade with " + username, "New " + location + " trade with " + username, "Trade to " + type + " " + contact.amount + " " + contact.currency + " (" + getContext().getString(R.string.btc_symbol) + contact.amount_btc + ")", NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }

        // look up deleted trades and find the reason
        if (deletedContacts.size() > 0) {
            getDeletedContactsInfo(deletedContacts);
        }

        updateMessages(contacts);
    }

    public void updateMessages(List<Contact> contacts)
    {
        Observable<List<Message>> observable = dbManager.updateMessagesFromContacts(contacts);
        observable.subscribe(new Action1<List<Message>>()
        {
            @Override
            public void call(List<Message> messages)
            {
                List<Message> newMessages = new ArrayList<Message>();
                for (Message message : messages) {
                    boolean isAccountUser = message.sender.username.toLowerCase().equals(stringPreference.get());
                    if(!isAccountUser) {
                        newMessages.add(message);
                    }
                }
                
                if (newMessages.size() > 1) { // if just single arrived
                    NotificationUtils.createMessageNotification(getContext(), "New Messages", "You have new messages!", "You have " + newMessages.size() + " new trade messages.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                } else {
                    if (newMessages.size() > 0) {
                        Message message = newMessages.get(0);
                        String username = message.sender.username;
                        NotificationUtils.createMessageNotification(getContext(), "New message from " + username, "New message from " + username, message.msg, NotificationUtils.NOTIFICATION_TYPE_MESSAGE, message.contact_id);
                    }
                }
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e(throwable.getMessage());
            }
        });
    }
}
