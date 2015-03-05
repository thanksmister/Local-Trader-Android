/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.data.services;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.zxing.WriterException;
import com.squareup.okhttp.OkHttpClient;
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
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseManager;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{  
    PublishSubject<List<Contact>> contactsPublishSubject;
    PublishSubject<List<Contact>> contactsInfoPublishSubject;
    PublishSubject<Wallet> walletPublishSubject;
    PublishSubject<List<Message>> messagePublishSubject;
    private LocalBitcoins localBitcoins;
    private DatabaseManager databaseManager;
  
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        localBitcoins = initLocalBitcoins();
        databaseManager = new DatabaseManager();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        if(isLoggedIn()) {
            getBalance();
            getContacts();
        }
    }

    private void getBalance()
    {
        Timber.d("GET Wallet Balance");
        
        if(walletPublishSubject != null) {
            return;
        }

        walletPublishSubject = PublishSubject.create();
        walletPublishSubject.subscribe(new Observer<Wallet>() {
            @Override
            public void onCompleted(){
                walletPublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) {
                /*RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                Timber.e("Sync Wallet Error Message: " + retroError.getMessage());
                Timber.e("Sync Wallet Error Code: " + retroError.getCode());*/
                walletPublishSubject = null;
            }

            @Override
            public void onNext(Wallet wallet) {
  
                Timber.d("Wallet Balance: " + wallet.total.balance);

                Wallet current = databaseManager.getWallet(getContext());

                Timber.d("Current Wallet: " + current);
                
                if(current != null) {

                    Timber.d("Current Wallet Balance: " + current.total.balance);
                    
                    double oldBalance = Doubles.convertToDouble(current.total.balance);
                    double newBalance = Doubles.convertToDouble(wallet.total.balance);
                    String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
           
                    Timber.d("Current Wallet diff: " + diff);
                    
                    // notify user of balance change
                    if(oldBalance < newBalance){
                        databaseManager.updateWallet(current.id, wallet, getContext());
                        NotificationUtils.createMessageNotification(getContext(), "Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                    } else if(!current.address.address.equals(wallet.address.address)) {
                        databaseManager.updateWallet(current.id, wallet, getContext());
                    }

                } else {
                    boolean inserted = databaseManager.insertWallet(wallet, getContext());
                    Timber.d(inserted?"Wallet inserted!":"Wallet insertion failed!");
                    NotificationUtils.createMessageNotification(getContext(), "Wallet Balance", "Wallet balance", "Your current wallet balance is " + wallet.total.balance + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                }
            }
        });

        getWalletBalance()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletBalance()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(walletPublishSubject);
    }
    
    private void getContacts() 
    {
        if(contactsPublishSubject != null) {
            return;
        }

        contactsPublishSubject = PublishSubject.create();
        contactsPublishSubject.subscribe(new Observer<List<Contact>>() {
            @Override
            public void onCompleted() {
                contactsPublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) 
            {
                contactsPublishSubject = null;
            }

            @Override
            public void onNext(List<Contact> contacts) {

                Timber.e("Sync Contacts: " + contacts.size());
                saveContactsAndNotify(contacts);
            }
        });

        getContactsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactsPublishSubject);
    }
    
    private void getDeletedContactsInfo(List<Contact> contacts)
    {
        Timber.d("getDeletedContactsInfo");
        if(contactsInfoPublishSubject != null) {
            return;
        }

        contactsInfoPublishSubject = PublishSubject.create();
        contactsInfoPublishSubject.subscribe(new Observer<List<Contact>>() {
            @Override
            public void onCompleted(){
                contactsInfoPublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) {
                /*RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                Timber.e("Sync Dashboard Error Message: " + retroError.getMessage());
                Timber.e("Sync Dashboard Error Code: " + retroError.getCode());*/
                contactsInfoPublishSubject = null;
            }

            @Override
            public void onNext(List<Contact> results) {
                if(results.size() > 1) {
                    NotificationUtils.createNotification(getContext(), "Trades canceled or released", "Trades canceled or released..", "Two or more of your trades have been canceled or released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                } else {
                    Contact contact = results.get(0);
                    String contactName = TradeUtils.getContactName(contact);
                    String saleType = (contact.is_selling)? " with buyer ":" with seller ";
                    if(TradeUtils.isCanceledTrade(contact)) {
                        NotificationUtils.createNotification(getContext(), "Trade Canceled", ("Trade with" + contactName + " canceled."), ("Trade #" + contact.contact_id + saleType + contactName + " has been canceled."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
                    } else if (TradeUtils.isReleased(contact)) {
                        NotificationUtils.createNotification(getContext(), "Trade Released", ("Trade with" + contactName + " released."), ("Trade #" + contact.contact_id + saleType + contactName + " has been released."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
                    }
                }
            }
        });

        getContactInfo(contacts)
                .onErrorResumeNext(getContactInfo(contacts))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactsInfoPublishSubject);
    }

    private Observable<Wallet> getWalletBalance()
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseWalletBalance(MockData.WALLET_BALANCE));
        }

        String access_token = getAccessToken();
        return localBitcoins.getWalletBalance(access_token)
                .map(new ResponseToWalletBalance());
    }

    private Observable<List<Contact>> getContactInfo(List<Contact> contacts)
    {
        String access_token = getAccessToken();
        List<Contact> contactList = Collections.emptyList();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>() {
                    @Override
                    public Observable<? extends List<Contact>> call(final Contact contact) {
                        return localBitcoins.getContact(contact.contact_id, access_token)
                                .map(new ResponseToContact())
                                .map(new Func1<Contact, List<Contact>>() {
                                    @Override
                                    public List<Contact> call(Contact result) {
                                        contactList.add(result);
                                        return contactList;
                                    }
                                });
                    }
                }).toBlockingObservable().last());
    }

    private Observable<List<Contact>> getContactsObservable()
    {
        String access_token = getAccessToken();
        return localBitcoins.getDashboard(access_token)
            .map(new ResponseToContacts())
            .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
            {
                @Override
                public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                {
                    if (contacts.isEmpty()) {
                        return Observable.just(contacts);
                    }
                    return getContactsMessageObservable(contacts);
                }
            });
    }

    private Observable<List<Contact>> getContactsMessageObservable(final List<Contact> contacts)
    {
        String access_token = getAccessToken();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>() {
                    @Override
                    public Observable<? extends List<Contact>> call(final Contact contact) {
                        return localBitcoins.contactMessages(contact.contact_id, access_token)
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, List<Contact>>() {
                                    @Override
                                    public List<Contact> call(List<Message> messages) {
                                        contact.messages = messages;
                                        return contacts;
                                    }
                                });
                    }
                }).toBlockingObservable().last());
    }

    public boolean isLoggedIn()
    {
        final String token = getAccessToken();
        return (!Strings.isBlank(token));
    }

    private String getAccessToken()
    {
        return databaseManager.getAccessToken(getContext());
    }

    private String getRefreshToken()
    {
        return databaseManager.getRefreshToken(getContext());
    }

    private void saveAuthorization(Authorization authorization)
    {
        databaseManager.updateTokens(getContext(), authorization.access_token, authorization.refresh_token);
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
        String refresh_token = getRefreshToken();

        return localBitcoins.refreshToken("refresh_token", refresh_token, Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                .map(new ResponseToAuthorize())
                .flatMap(new Func1<Authorization, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Authorization authorization) {
                        Timber.d("New Access tokens: " + authorization.access_token);
                        saveAuthorization(authorization);
                        return Observable.just(authorization.access_token);
                    }
                });
    }

    private void saveContactsAndNotify(List<Contact> contacts)
    {
        TreeMap<String, ArrayList<Contact>> updatedContactList = databaseManager.updateContacts(contacts, getContext());
        
        // notify user of any new trades
        
        ArrayList<Contact> updatedContacts = updatedContactList.get(DatabaseManager.UPDATES);
        Timber.d("updated contacts: " + updatedContacts.size());
        ArrayList<Contact> addedContacts = updatedContactList.get(DatabaseManager.ADDITIONS);
        Timber.d("added contacts: " + addedContacts.size());
        ArrayList<Contact> deletedContacts = updatedContactList.get(DatabaseManager.DELETIONS);
        Timber.d("deleted contacts: " + deletedContacts.size());
        
        if (updatedContacts.size() > 1){
            NotificationUtils.createNotification(getContext(), "Trade Updates", "Trade status updates..", "Two or more of your trades have been updated.", NotificationUtils.NOTIFICATION_TYPE_CONTACT, null);
        } else if (updatedContactList.size() == 1) {
            Contact contact = updatedContacts.get(0);
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling)? " with buyer ":" with seller ";
            NotificationUtils.createNotification(getContext(), "Trade Updated", ("The trade with" + contactName + " updated."), ("Trade #" + contact.contact_id + saleType + contactName + " has been updated."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }

        // notify user of any new trades
        if (addedContacts.size() > 1){
            NotificationUtils.createNotification(getContext(), "New Trades", "You have new trades to buy or sell bitcoin!", "You have " + addedContacts.size() + " new trades to buy or sell bitcoins.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else if (addedContacts.size() == 1) {
            Contact contact = addedContacts.get(0);
            String username = TradeUtils.getContactName(contact);
            String type = (contact.is_buying)? "sell":"buy";
            String location = (TradeUtils.isLocalTrade(contact))? "local":"online";
            NotificationUtils.createNotification(getContext(), "New trade with " + username, "New " + location + " trade with " + username, "Trade to " + type + " " + contact.amount + " " + contact.currency + " (" + getContext().getString(R.string.btc_symbol) + contact.amount_btc + ")", NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }

        // look up deleted trades and find the reason
        if(deletedContacts.size() > 0) {
            getDeletedContactsInfo(deletedContacts); // 
        }

        updateMessages(updatedContacts);
    }

    private void updateMessages(final List<Contact> contacts)
    {
        if(messagePublishSubject != null) {
            return;
        }

        messagePublishSubject = PublishSubject.create();
        messagePublishSubject.subscribe(new Observer<List<Message>>() {
            @Override
            public void onCompleted(){
                messagePublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) {
                messagePublishSubject = null;
            }

            @Override
            public void onNext(List<Message> messages) {
                if (messages.size() > 1) { // if just single arrived
                    NotificationUtils.createMessageNotification(getContext(), "New Messages", "You have new messages!", "You have " + messages.size() + " new trade messages.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                } else {

                    if(messages.size() > 0) {
                        Message message = messages.get(0);
                        String username = message.sender.username;
                        NotificationUtils.createMessageNotification(getContext(), "New message from " + username, "New message from " + username, message.msg, NotificationUtils.NOTIFICATION_TYPE_MESSAGE, message.contact_id);
                    }
                }
            }
        });

        updateMessagesObservable(contacts, getContext())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messagePublishSubject);
    }

    private Observable<List<Message>> updateMessagesObservable(final List<Contact> contacts, final Context context)
    {
        final ArrayList<Message> messages = new ArrayList<Message>();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<List<Message>>>() {
                    @Override
                    public Observable<List<Message>> call(final Contact contact) {
                        return Observable.just(databaseManager.updateMessages(contact.contact_id, contact.messages, context))
                               .flatMap(new Func1<ArrayList<Message>, Observable<List<Message>>>()
                               {
                                   @Override
                                   public Observable<List<Message>> call(ArrayList<Message> results)
                                   {
                                       messages.addAll(results);
                                       return Observable.just(messages);
                                   }
                               });
                               
                    }
                }).toBlockingObservable().last());
    }

   
    /*
    if (messages.size() > 1) { // if just single arrived
                NotificationUtils.createMessageNotification(getContext(), "New Messages", "You have new messages!", "You have " + messages.size() + " new trade messages.", null);
            } else {
                
                if(messages.size() > 0) {
                    Message message = messages.get(0);
                    String username = message.getSender_username();
                    Contact contact = ContactManager.getInstance().getContactByContactId(message.getContact_id(), getContext());
                    NotificationUtils.createMessageNotification(getContext(), "New message from " + username, "New message from " + username, message.getMessage(), contact);
                }
            }
            
            
       if (newContacts.size() > 1){
            NotificationUtils.createNotification(context, "New Trades", "You have new trades to buy or sell bitcoin!", "You have " + newContacts.size() + " new trades to buy or sell bitcoins.", null);
        } else {
            if(newContacts.size() > 0) {
                Contact contact = newContacts.get(0);
                String username = (contact.youAreSelling())? contact.getBuyer_username():contact.getSeller_username();
                
                String type = (contact.youAreBuying())? "sell":"buy";
                String location = (contact.isLocalTrade())? "local":"online";
                NotificationUtils.createNotification(context, "New trade with " + username, "New " + location + " trade with " + username, "Trade to " + type + " " + contact.getAmount() + " " + contact.getCurrency() + " (" + context.getString(R.string.btc_symbol) + contact.getAmount_btc() + ")", contact);
            }
        }
     */
}
