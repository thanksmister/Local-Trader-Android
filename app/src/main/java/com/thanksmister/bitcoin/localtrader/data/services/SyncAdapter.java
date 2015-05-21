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
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
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
    private Subscription walletSubscription;
    private Subscription contactsSubscription;
    private Subscription tokensSubscription;

    SQLiteOpenHelper dbOpenHelper;
    private LocalBitcoins localBitcoins;
    private DbManager dbManager;
    private NotificationService notificationService;
    private SharedPreferences sharedPreferences;
   
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);

        sharedPreferences = getContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        notificationService = new NotificationService(context, sharedPreferences);
        localBitcoins = initLocalBitcoins();
        dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        SqlBrite db = SqlBrite.create(dbOpenHelper);
        dbManager = new DbManager(db);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        Timber.d("onPerformSync");
        
        if(dbManager.isLoggedIn()) {
            
            updateContacts();
        }
    }

    @Override
    public void onSyncCanceled()
    {
        super.onSyncCanceled();

        if(walletSubscription != null)
            walletSubscription.unsubscribe();
        
        if(contactsSubscription != null)
            contactsSubscription.unsubscribe();

        if(tokensSubscription != null)
            tokensSubscription.unsubscribe();
    }

    private void updateContacts()
    {
        if(contactsSubscription != null)
            return;
        
        Timber.d("UpdateContacts");
        
        contactsSubscription = getContactsObservable()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> items)
                    {
                        contactsSubscription = null;

                        setContactsExpireTime(); // tell system we synced contacts

                        saveContactsAndNotify(items); // notify of any new contacts or messages

                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        contactsSubscription = null;

                        handleError(throwable);
                    }
                });
    }
    
    private void updateWalletBalance()
    {
        if(walletSubscription != null)
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
                        walletSubscription = null;
                        
                        updateWalletBalance(wallet);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        walletSubscription = null;
                        
                        handleError(throwable);
                    }
                });
    }
    
    private void refreshAccessTokens()
    {
        if(tokensSubscription != null)
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
                        tokensSubscription = null;
                        updateTokens(authorization);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        tokensSubscription = null;
                        
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

    private void updateWalletBalance(final Wallet wallet)
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
        Timber.e("deleted contacts: " + contacts.size());
        
        getContactInfo(contacts)
                .onErrorResumeNext(getContactInfo(contacts))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) 
                    {
                        Timber.e("deleted contacts info: " + contacts.size());
                        notificationService.contactDeleteNotification(contacts);
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
                    public Observable<Wallet> call(final SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;

                        Timber.d("Access Token: " + sessionItem.access_token());

                        return localBitcoins.getWalletBalance(sessionItem.access_token())
                                .map(new ResponseToWalletBalance())
                                .flatMap(new Func1<Wallet, Observable<Wallet>>()
                                {
                                    @Override
                                    public Observable<Wallet> call(final Wallet wallet)
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

    // TODO move this to util as its used in other locations
    private Observable<Bitmap> generateBitmap(final String address)
    {
        return Observable.create(new Observable.OnSubscribe<Bitmap>()
        {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber)
            {
                try {
                    subscriber.onNext(WalletUtils.encodeAsBitmap(address, getContext()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
    
    private Observable<List<Contact>> getContactInfo(final List<Contact> contacts)
    {
        final List<Contact> contactList = Collections.emptyList();
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(final SessionItem sessionItem)
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
                    public Observable<List<Contact>> call(final SessionItem sessionItem)
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
        updateMessages(contacts)
        .subscribe(new Action1<List<Message>>()
        {
            @Override
            public void call(List<Message> messages)
            {
                notificationService.messageNotifications(messages);

                updateContacts(contacts);
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
    
    private void updateContacts(List<Contact> contacts)
    {
        TreeMap<String, ArrayList<Contact>> updatedContactList = dbManager.updateContacts(contacts);
        ArrayList<Contact> updatedContacts = updatedContactList.get(DbManager.UPDATES);
        Timber.d("updated contacts: " + updatedContacts.size());

        ArrayList<Contact> addedContacts = updatedContactList.get(DbManager.ADDITIONS);
        Timber.d("added contacts: " + addedContacts.size());

        ArrayList<Contact> deletedContacts = updatedContactList.get(DbManager.DELETIONS);

        notificationService.contactNewNotification(addedContacts);
        notificationService.contactUpdateNotification(updatedContacts);

        // look up deleted trades and find the reason
        if (deletedContacts.size() > 0) {
            getDeletedContactsInfo(deletedContacts);
        }

        updateWalletBalance(); // get wallet balance
    }

    private void setContactsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, DataService.PREFS_CONTACTS_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + DataService.CHECK_CONTACTS_DATA; // 1 hours
            preference.set(expire);
        }
    }

    public Observable<List<Message>> updateMessages(List<Contact> contacts)
    {
        return dbManager.updateMessagesFromContacts(contacts);
    }
}
