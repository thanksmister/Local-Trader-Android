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

import com.crashlytics.android.Crashlytics;
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
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    public static String UPDATES = "updates";
    public static String ADDITIONS = "additions";
    public static String DELETIONS = "deletions";

    private CompositeSubscription subscriptions = new CompositeSubscription();

    SQLiteOpenHelper dbOpenHelper;
    private LocalBitcoins localBitcoins;
    private DbManager dbManager;
  
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        
        localBitcoins = initLocalBitcoins();
        dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        SqlBrite db = SqlBrite.create(dbOpenHelper);
        dbManager = new DbManager(db);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        if(dbManager.isLoggedIn()) {

            Observable<Wallet> walletBalanceObservable = getWalletBalance()
                    .onErrorResumeNext(refreshTokenAndRetry(getWalletBalance()))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());

            Observable<List<Contact>> contactsObservable = getContactsObservable()
                    .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable()))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());

            subscriptions.add(contactsObservable.subscribe(new Action1<List<Contact>>()
            {
                @Override
                public void call(List<Contact> items)
                {
                    saveContactsAndNotify(items);
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    handleError(throwable);
                }
            }));

            subscriptions.add(walletBalanceObservable.subscribe(new Action1<Wallet>()
            {
                @Override
                public void call(Wallet wallet)
                {
                    updateWalletBalance(wallet);
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    handleError(throwable);
                }
            }));
        }
    }

    protected void handleError(Throwable throwable)
    {
        if(DataServiceUtils.isNetworkError(throwable)) {
            Timber.e(getContext().getString(R.string.error_no_internet) + ", Code 503");
        } else if(DataServiceUtils.isHttp403Error(throwable)) {
            Timber.e(getContext().getString(R.string.error_authentication) + ", Code 403");
        } else if(DataServiceUtils.isHttp401Error(throwable)) {
            Timber.e(getContext().getString(R.string.error_no_internet) + ", Code 401");
        } else if(DataServiceUtils.isHttp500Error(throwable)) {
            Timber.e(getContext().getString(R.string.error_service_error) + ", Code 500");
        } else if(DataServiceUtils.isHttp404Error(throwable)) {
            Timber.e(getContext().getString(R.string.error_service_error) + ", Code 404");
        } else if(DataServiceUtils.isHttp400GrantError(throwable)) {
            Timber.e(getContext().getString(R.string.error_authentication) + ", Code 400 Grant Invalid");
        } else if(DataServiceUtils.isHttp400Error(throwable)) {
            Timber.e(getContext().getString(R.string.error_service_error) + ", Code 400");
        } else {
            Timber.e(getContext().getString(R.string.error_unknown_error));
        }

        if(throwable != null && throwable.getLocalizedMessage() != null)
            Timber.e("Data Error: " + throwable.getLocalizedMessage());
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
                        double oldBalance = Doubles.convertToDouble(walletItem.balance());
                        double newBalance = Doubles.convertToDouble(wallet.total.balance);
                        String address = walletItem.address();
                        String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);

                        if (oldBalance < newBalance) {
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
                        Crashlytics.setString("UpdateWallet", throwable.getLocalizedMessage());
                        Crashlytics.logException(throwable);
                    }
                });
    }
    
    private void updateWallet(Wallet wallet)
    {
        Observable<Boolean> observable = dbManager.updateWallet(wallet);
        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>()
                {
                    @Override
                    public void call(Boolean aBoolean)
                    {
                        Timber.d("Updated wallet successfully: " + aBoolean);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        Crashlytics.setString("UpdateWallet", throwable.getLocalizedMessage());
                        Crashlytics.logException(throwable);
                    }
                });
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
                        return Observable.just(Observable.from(contacts)
                                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>()
                                {
                                    @Override
                                    public Observable<? extends List<Contact>> call(final Contact contact)
                                    {
                                        return localBitcoins.getContact(contact.contact_id, sessionItem.access_token())
                                                .map(new ResponseToContact())
                                                .map(new Func1<Contact, List<Contact>>() {
                                                    @Override
                                                    public List<Contact> call(Contact contactResult)
                                                    {
                                                        if(TradeUtils.isCanceledTrade(contactResult) || TradeUtils.isReleased(contactResult)) {
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
                        Timber.d("Access Token: " + sessionItem.access_token());

                        return localBitcoins.getDashboard(sessionItem.access_token())
                                .map(new ResponseToContacts())
                                .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                                {
                                    @Override
                                    public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                    {
                                        Timber.d("Contacts: " + contacts);
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

    private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed)
    {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                // Here check if the error thrown really is a 401
                if (DataServiceUtils.isHttp403Error(throwable)) {
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
    }

    private Observable<String> refreshTokens()
    {
        Timber.d("refreshTokens");

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<String>>() {
                    @Override
                    public Observable<String> call(SessionItem sessionItem)
                    {
                        Timber.d("Refresh Token: " + sessionItem.refresh_token());
                        
                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize())
                                .flatMap(new Func1<Authorization, Observable<? extends String>>() {
                                    @Override
                                    public Observable<? extends String> call(Authorization authorization)
                                    {
                                        Timber.d("New Access tokens: " + authorization.access_token);
                                        updateTokens(authorization);
                                        return Observable.just(authorization.access_token);
                                    }
                                });
                    }
                });
    }

    private void saveContactsAndNotify(final List<Contact> contacts)
    {
        Observable<TreeMap<String, ArrayList<Contact>>> updatedContactList = dbManager.updateContacts(contacts);
        updatedContactList
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<TreeMap<String, ArrayList<Contact>>>()
                {
                    @Override
                    public void call(TreeMap<String, ArrayList<Contact>> stringArrayListTreeMap)
                    {
                        ArrayList<Contact> updatedContacts = stringArrayListTreeMap.get(UPDATES);
                        Timber.d("updated contacts: " + updatedContacts.size());

                        ArrayList<Contact> addedContacts = stringArrayListTreeMap.get(ADDITIONS);
                        Timber.d("added contacts: " + addedContacts.size());

                        ArrayList<Contact> deletedContacts = stringArrayListTreeMap.get(DELETIONS);
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
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        Timber.e(throwable.getMessage());
                    }
                });
    }

    public void updateMessages(List<Contact> contacts)
    {
        Observable<List<Message>> observable = dbManager.updateMessagesFromContacts(contacts);
        observable.subscribe(new Action1<List<Message>>()
        {
            @Override
            public void call(List<Message> messages)
            {
                if (messages.size() > 1) { // if just single arrived
                    NotificationUtils.createMessageNotification(getContext(), "New Messages", "You have new messages!", "You have " + messages.size() + " new trade messages.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                } else {
                    if (messages.size() > 0) {
                        Message message = messages.get(0);
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
