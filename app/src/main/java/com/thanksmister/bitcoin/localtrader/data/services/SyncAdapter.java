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
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

import com.squareup.okhttp.OkHttpClient;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactSync;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContactSyncs;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.database.CupboardProvider;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.BooleanPreference;
import com.thanksmister.bitcoin.localtrader.data.prefs.IntPreference;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

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

import static android.content.Context.MODE_PRIVATE;
import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{  
    private static String WALLET_KEY = "wallet_balance";
    
    PublishSubject<List<ContactSync>> contactsPublishSubject;
    PublishSubject<List<Contact>> contactsInfoPublishSubject;
    PublishSubject<Wallet> walletPublishSubject;
    private LocalBitcoins localBitcoins;
    private SharedPreferences sharedPreferences;
  
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        localBitcoins = initLocalBitcoins();
        sharedPreferences = getContext().getApplicationContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        getBalance();
        //getContacts();
    }

    private void getBalance()
    {
        Timber.d("Get Balance");
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
            public void onError(Throwable e) {
                if(e != null)
                    Timber.e("Sync Wallet Balance Error: " + e);
            }

            @Override
            public void onNext(Wallet wallet) {
                Timber.d("Wallet Balance: " + wallet.total.balance);
                
                StringPreference stringPreference = new StringPreference(sharedPreferences, WALLET_KEY, "");
                String balance = stringPreference.get();
              
                // TODO reset wallet balance on logout
                // TODO open wallet screen
                if(balance.equals("")) {
                    NotificationUtils.createMessageNotification(getContext(), "Wallet Balance", "Wallet balance", "Your current wallet balance is " + wallet.total.balance + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                } else if(!wallet.total.balance.equals(balance)) {
                    double oldBalance = Doubles.convertToDouble(balance);
                    double newBalance = Doubles.convertToDouble(wallet.total.balance);
                    if(oldBalance < newBalance) {
                        String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                        NotificationUtils.createMessageNotification(getContext(), "Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                    }  
                }

                stringPreference.set(wallet.total.balance);
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
        contactsPublishSubject.subscribe(new Observer<List<ContactSync>>() {
            @Override
            public void onCompleted() {
                contactsPublishSubject = null;
            }

            @Override
            public void onError(Throwable e) {
                if(e != null)
                    Timber.e("Sync Contacts Error: " + e.getMessage());
            }

            @Override
            public void onNext(List<ContactSync> contacts) {
                if(!contacts.isEmpty())
                    saveContactsAndNotify(contacts);
            }
        });

        getContactsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactsPublishSubject);
    }
    
    private void getDeletedContactsInfo(List<ContactSync> contacts)
    {
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
            public void onError(Throwable e) {
                if(e != null)
                    Timber.e("Contacts Dashboard error: " + e);
            }

            @Override
            public void onNext(List<Contact> results) {
                if(results.size() > 1) {
                    NotificationUtils.createNotification(getContext(), "Trades canceled or released", "Trades canceled or released..", "Two or more of your trades have been canceled or released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                    for (Contact contact : results) {
                        deleteContact(contact); // delete contact list
                    }
                } else {
                    Contact contact = results.get(0);
                    String contactName = TradeUtils.getContactName(contact);
                    String saleType = (contact.is_selling)? " with buyer ":" with seller ";
                    if(TradeUtils.isCanceledTrade(contact)) {
                        NotificationUtils.createNotification(getContext(), "Trade Canceled", ("Trade with" + contactName + " canceled."), ("Trade #" + contact.contact_id + saleType + contactName + " has been canceled."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
                    } else if (TradeUtils.isReleased(contact)) {
                        NotificationUtils.createNotification(getContext(), "Trade Released", ("Trade with" + contactName + " released."), ("Trade #" + contact.contact_id + saleType + contactName + " has been released."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
                    }

                    deleteContact(contact); // remove from database
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

    private Observable<List<Contact>> getContactInfo(List<ContactSync> contacts)
    {
        String access_token = getAccessToken();
        List<Contact> contactList = Collections.emptyList();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<ContactSync, Observable<? extends List<Contact>>>() {
                    @Override
                    public Observable<? extends List<Contact>> call(final ContactSync contact) {
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

    private Observable<List<ContactSync>> getContactsObservable()
    {
        String access_token = getAccessToken();
        return localBitcoins.getDashboard(access_token)
                    .map(new ResponseToContactSyncs())
                    .flatMap(new Func1<List<ContactSync>, Observable<? extends List<ContactSync>>>() {
                        @Override
                        public Observable<? extends List<ContactSync>> call(final List<ContactSync> contacts) {
                            if (contacts.isEmpty()) {
                                return Observable.just(contacts);
                            }
                            
                            return getContactsMessageObservable(contacts);
                        }
                    });
    }

    private Observable<List<ContactSync>> getContactsMessageObservable(final List<ContactSync> contacts)
    {
        String access_token = getAccessToken();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<ContactSync, Observable<? extends List<ContactSync>>>() {
                    @Override
                    public Observable<? extends List<ContactSync>> call(final ContactSync contact) {
                        return localBitcoins.contactMessages(contact.contact_id, access_token)
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, List<ContactSync>>() {
                                    @Override
                                    public List<ContactSync> call(List<Message> messages) {
                                        contact.messageCount = messages.size();
                                        contact.lastMessageText = messages.get(0).msg;
                                        contact.lastMessageSender = messages.get(0).sender.name;
                                        return contacts;
                                    }
                                });
                    }
                }).toBlockingObservable().last());
    }

    private String getAccessToken()
    {
        Authorization authorization = getAuthorization();
        if(authorization == null)
            return null;
            
        return authorization.access_token;
    }

    private Authorization getAuthorization()
    {
        List<Authorization> list = cupboard().withContext(getContext()).query(CupboardProvider.TOKEN_URI, Authorization.class).list();
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
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
        Authorization authorization = getAuthorization();
        Timber.e("Get Refresh token: " + authorization.refresh_token);
        return localBitcoins.refreshToken("refresh_token", authorization.refresh_token, Constants.CLIENT_ID, Constants.CLIENT_SECRET)
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

    private void saveAuthorization(Authorization authorization)
    {
        Timber.e("Save Authorization: " + authorization.access_token);

        // remove old tokens
        Authorization oldToken = getAuthorization();
        if(oldToken != null) {
            cupboard().withContext(getContext()).delete(CupboardProvider.TOKEN_URI, oldToken);
        }

        // save to cupboard
        cupboard().withContext(getContext()).put(CupboardProvider.TOKEN_URI, authorization);
    }

    private void saveContactsAndNotify(List<ContactSync> contacts)
    {
        TreeMap<String, ContactSync> deleteMap = new TreeMap<String, ContactSync>();
        TreeMap<String, ContactSync> updateMap = new TreeMap<String, ContactSync>();
        TreeMap<String, ContactSync> entryMap = new TreeMap<String, ContactSync>();
        for (ContactSync syncNew : contacts) {
            entryMap.put(syncNew.contact_id, syncNew);
        }
        
        List<ContactSync> contactList = cupboard().withContext(getContext()).query(CupboardProvider.TOKEN_URI, ContactSync.class).list();
        for (ContactSync sync : contactList) {
            String id = sync.contact_id;
            ContactSync match = entryMap.get(id);
            if (match != null) {
                entryMap.remove(id); //remove from entry map to prevent insert later.
                if (sync.messageCount > match.messageCount || sync.is_funded != match.is_funded) {
                    updateMap.put(id, match); // update item
                }
            } else {
                deleteMap.put(id, sync);
            }
        }

        List <ContactSync> updateList = Collections.emptyList();
        for (ContactSync updated : updateMap.values()) {
            updateContact(updated);
            updateList.add(updated);
        }

        List <ContactSync> addList = Collections.emptyList();
        for (ContactSync added : entryMap.values()) {
            saveContact(added);
            addList.add(added);
        }

        List <ContactSync> infoList = Collections.emptyList();
        for (ContactSync deleted : deleteMap.values()) {
            infoList.add(deleted);
        }

        // notify user of any new trades
        if (updateList.size() > 1){
            NotificationUtils.createNotification(getContext(), "Trade Updates", "Trade status updates..", "Two or more of your trades have been updated.", NotificationUtils.NOTIFICATION_TYPE_CONTACT, null);
        } else {
            ContactSync contact = updateList.get(0);
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling)? " with buyer ":" with seller ";
            NotificationUtils.createNotification(getContext(), "Trade Updated", ("The trade with" + contactName + " updated."), ("Trade #" + contact.contact_id + saleType + contactName + " has been updated."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }

        // notify user of any new trades
        if (addList.size() > 1){
            NotificationUtils.createNotification(getContext(), "New Trades", "You have new trades to buy or sell bitcoin!", "You have " + entryMap.values() + " new trades to buy or sell bitcoins.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else {
            ContactSync contact = addList.get(0);
            String username = TradeUtils.getContactName(contact);
            String type = (contact.is_buying)? "sell":"buy";
            String location = (TradeUtils.isLocalTrade(contact))? "local":"online";
            NotificationUtils.createNotification(getContext(), "New trade with " + username, "New " + location + " trade with " + username, "Trade to " + type + " " + contact.amount + " " + contact.currency + " (" + getContext().getString(R.string.btc_symbol) + contact.amount_btc + ")", NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }

        // look up deleted trades and find the reason
        if(infoList.size() > 0) {
            getDeletedContactsInfo(infoList); // 
        }
    }

    private void setHasMessages(String contact_id)
    {
        BooleanPreference booleanPreference = new BooleanPreference(sharedPreferences, contact_id, true);
        booleanPreference.set(true);
    }

    private void clearHasMessages(String contact_id)
    {
        BooleanPreference booleanPreference = new BooleanPreference(sharedPreferences, contact_id);
        booleanPreference.delete();
    }
    
    private int getMessageCount(String contact_id)
    {
        IntPreference intPreference = new IntPreference(sharedPreferences, contact_id, 0);
        return intPreference.get();
    }

    private void updateMessageCount(String contact_id, int count)
    {
        IntPreference intPreference = new IntPreference(sharedPreferences, contact_id);
        intPreference.set(count);
    }

    private void clearMessageCount(String contact_id)
    {
        IntPreference intPreference = new IntPreference(sharedPreferences, contact_id);
        intPreference.delete();
    }
   
    private void deleteContact(Contact contact)
    {
        cupboard().withContext(getContext()).delete(CupboardProvider.CONTACT_URI, contact);
    }

    private void updateContact(ContactSync contact)
    {
        cupboard().withContext(getContext()).put(CupboardProvider.CONTACT_URI, contact);
    }

    private void saveContact(ContactSync contact)
    {
        cupboard().withContext(getContext()).put(CupboardProvider.CONTACT_URI, contact);
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
