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
import android.database.Cursor;
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
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseManager;
import com.thanksmister.bitcoin.localtrader.data.database.Db;
import com.thanksmister.bitcoin.localtrader.data.database.DbOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.MessageItem;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private Observable<List<Contact>> contactsObservable;
    private Observable<Wallet> walletBalanceObservable;

    SQLiteOpenHelper dbOpenHelper;
    private LocalBitcoins localBitcoins;
    private SharedPreferences sharedPreferences;
    private StringPreference stringPreference;
    private SqlBrite db;
  
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        
        localBitcoins = initLocalBitcoins();
        sharedPreferences = getContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        stringPreference = new StringPreference(sharedPreferences, DataService.PREFS_USER);
        dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        db = SqlBrite.create(dbOpenHelper);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        if(isLoggedIn()) {

            walletBalanceObservable = getWalletBalance()
                    .onErrorResumeNext(refreshTokenAndRetry(getWalletBalance()))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());

            contactsObservable = getContactsObservable()
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
                    if(throwable.getLocalizedMessage() != null)
                        handleError(throwable);
                }
            }));

            subscriptions.add(walletBalanceObservable.subscribe(new Action1<Wallet>()
            {
                @Override
                public void call(Wallet wallet)
                {
                    updateWallet(wallet);
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
        Timber.e("handleError: " + throwable.getLocalizedMessage());

        if(DataServiceUtils.isHttp403Error(throwable)) {
            Timber.e("Wallet Error: " + getContext().getString(R.string.error_authentication)  + " Code 404");
        } else if(DataServiceUtils.isHttp401Error(throwable)) {
            Timber.e("Wallet Error: " + getContext().getString(R.string.error_no_internet)  + " Code 401");
        } else if(DataServiceUtils.isHttp400Error(throwable)) {
            Timber.e("Wallet Error: " + getContext().getString(R.string.error_service_error)  + " Code 400");
        } else if(DataServiceUtils.isHttp500Error(throwable)) {
            Timber.e("Wallet Error: " + getContext().getString(R.string.error_service_error)  + " Code 500");
        } else if(DataServiceUtils.isHttp404Error(throwable)) {
            Timber.e("Wallet Error: " + getContext().getString(R.string.error_service_error) + " Code 404");
        } else {
            Timber.e("Wallet Error: " + getContext().getString(R.string.error_generic_error));
        }
    }

    private void updateWallet(Wallet wallet)
    {
        WalletItem.Builder builder = new WalletItem.Builder()
                .address(wallet.address.address)
                .balance(wallet.total.balance)
                .message(wallet.message)
                .receivable(wallet.address.received)
                .sendable(wallet.total.sendable);

        if(wallet.qrImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
            builder.qrcode(baos.toByteArray());
        }
        
        Cursor cursor = db.query(WalletItem.QUERY);
        
        try{
            if(cursor.getCount() > 0) {
                
                cursor.moveToFirst();
                long id = Db.getLong(cursor, WalletItem.ID);
                String address = Db.getString(cursor, WalletItem.ADDRESS);
                String balance = Db.getString(cursor, WalletItem.BALANCE);
                byte[] qrCode = Db.getBlob(cursor, WalletItem.QRCODE);
                double oldBalance = Doubles.convertToDouble(balance);
                double newBalance = Doubles.convertToDouble(wallet.total.balance);
                String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);

                Timber.d("Old Wallet address: " + address);
                Timber.d("Current Wallet address: " + wallet.address.address);
                
                if(oldBalance < newBalance){
                    Timber.d("Current Wallet diff: " + diff);
                    db.update(WalletItem.TABLE, builder.build(), WalletItem.ID + " = ?", String.valueOf(id));
                    NotificationUtils.createMessageNotification(getContext(), "Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC", NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
                } else if(!address.equals(wallet.address.address)) {
                    Timber.d("Current Wallet address: " + address);
                    db.update(WalletItem.TABLE, builder.build(), WalletItem.ID + " = ?", String.valueOf(id));
                } else if(qrCode == null || qrCode.length == 0) {
                    Timber.d("Current Wallet address: " + address);
                    db.update(WalletItem.TABLE, builder.build(), WalletItem.ID + " = ?", String.valueOf(id));
                }
                
            } else {
                db.insert(WalletItem.TABLE, builder.build());
            }
        } finally {
            cursor.close();
        }
    }
    
    private void getDeletedContactsInfo(List<Contact> contacts)
    {
        getContactInfo(contacts)
                .onErrorResumeNext(getContactInfo(contacts))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Contact>>() {
                    @Override
                    public void onCompleted()
                    {
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                    }

                    @Override
                    public void onNext(List<Contact> contacts)
                    {
                        if(contacts.size() > 1) {
                            NotificationUtils.createNotification(getContext(), "Trades canceled or released", "Trades canceled or released..", "Two or more of your trades have been canceled or released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
                        } else {
                            Contact contact = contacts.get(0);
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
    }

    private Observable<Wallet> getWalletBalance()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(SessionItem sessionItem)
                    {
                        Timber.d("Token Wallet: " + sessionItem.access_token());
                        
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
                                                .map(new Func1<Contact, List<Contact>>()
                                                {
                                                    @Override
                                                    public List<Contact> call(Contact result)
                                                    {
                                                        contactList.add(result);
                                                        return contactList;
                                                    }
                                                });
                                    }
                                }).toBlocking().last());
                    }
                });
    }

    private Observable<List<Contact>> getContactsObservable()
    {
        // TODO handle bad token
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>() {
                    @Override
                    public Observable<List<Contact>> call(SessionItem sessionItem)
                    {
                        Timber.d("Token: " + sessionItem.access_token());
                        
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


    public boolean isLoggedIn()
    {
        Cursor cursor = db.query(SessionItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String access_token = Db.getString(cursor, SessionItem.ACCESS_TOKEN);
                return (!Strings.isBlank(access_token));
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    public Observable<SessionItem> getTokens()
    {
        return db.createQuery(SessionItem.TABLE, SessionItem.QUERY)
                .map(SessionItem.MAP);
    }

    private String getRefreshToken()
    {
        Cursor cursor = db.query(SessionItem.QUERY);
        if(cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                return Db.getString(cursor, SessionItem.REFRESH_TOKEN);
            } finally {
                cursor.close();
            }
        } else {
            return null;
        }
    }

    private void updateTokens(Authorization authorization)
    {
        SessionItem.Builder builder = new SessionItem.Builder()
                .access_token(authorization.access_token)
                .refresh_token(authorization.refresh_token);

        Cursor cursor = db.query(SessionItem.QUERY);
        if(cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, SessionItem.ID );
                db.update(SessionItem.TABLE, builder.build(), SessionItem.ID + " = ?", String.valueOf(id));
            } finally {
                cursor.close();
            }
        } else {
            db.insert(ContactItem.TABLE, builder.build());
        }
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
                .flatMap(new Func1<SessionItem, Observable<String>>()
                {
                    @Override
                    public Observable<String> call(SessionItem sessionItem)
                    {
                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize())
                                .flatMap(new Func1<Authorization, Observable<? extends String>>()
                                {
                                    @Override
                                    public Observable<? extends String> call(Authorization authorization)
                                    {
                                        Timber.d("New Access tokens: " + authorization.access_token);
                                        db.insert(SessionItem.TABLE, new SessionItem.Builder().access_token(authorization.access_token).refresh_token(authorization.refresh_token).build());
                                        return Observable.just(authorization.access_token);
                                    }
                                });
                    }
                });
    }

    private void saveContactsAndNotify(List<Contact> contacts)
    {
        TreeMap<String, ArrayList<Contact>> updatedContactList = updateContacts(contacts);
        
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
            getDeletedContactsInfo(deletedContacts);
        } 
        
        final ArrayList<Message> messages = new ArrayList<Message>();
        final ArrayList<Contact> contactsToCheck = new ArrayList<Contact>();
        
        contactsToCheck.addAll(addedContacts);
        contactsToCheck.addAll(updatedContacts);
        
        for (Contact contact : contactsToCheck) {
            final ArrayList<Message> contactMessages = updateMessages(contact.contact_id, contact.messages);
            messages.addAll(contactMessages);
        }
        
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

    public ArrayList<Message> updateMessages(final String contact_id, final List<Message> messages)
    {
        Timber.d("Update Messages: " + messages.size());
        
        ArrayList<Message> newMessages = new ArrayList<Message>();
        HashMap<String, Message> entryMap = new HashMap<String, Message>();
        for (Message message : messages) {
            entryMap.put(message.created_at, message);
        }
        
        Cursor cursor = db.query(MessageItem.QUERY, String.valueOf(contact_id));
        
        try {
            
            Timber.d("Update Messages Databases: " + cursor.getCount());
            
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, MessageItem.ID);
                String createdAt = Db.getString(cursor, MessageItem.CREATED_AT);
                
                Message match = entryMap.get(createdAt);
                
                if (match != null) {
                    // Entry exists. Do not update message
                    entryMap.remove(createdAt);
                    
                } else {
                    // Entry doesn't exist. Don't remove unless we remove the contact
                    db.delete(MessageItem.TABLE, String.valueOf(id));
                }
            } 
        } finally {
            cursor.close();
        }
        
        // Add new items
        for (Message item : entryMap.values()) {
            
            boolean isAccountUser = item.sender.username.toLowerCase().equals(stringPreference.get());
      
            MessageItem.Builder builder = new MessageItem.Builder()
                    .contact_list_id(Long.valueOf(item.contact_id))
                    .message(item.msg)
                    .seen(isAccountUser)
                    .created_at(item.created_at)
                    .sender_id(item.sender.id)
                    .sender_name(item.sender.name)
                    .sender_username(item.sender.username)
                    .sender_trade_count(item.sender.trade_count)
                    .sender_last_online(item.sender.last_seen_on)
                    .is_admin(item.is_admin);

            db.insert(MessageItem.TABLE, builder.build());

            // ignore messages by logged in user as being "new"
            if(!isAccountUser) {
                newMessages.add(item);
            }
        }

        return newMessages;
    }

    public TreeMap<String, ArrayList<Contact>> updateContacts(final List<Contact> items)
    {
        TreeMap<String, ArrayList<Contact>> updateMap = new TreeMap<String, ArrayList<Contact>>();
        ArrayList<Contact> newContacts = new ArrayList<Contact>();
        ArrayList<Contact> deletedContacts = new ArrayList<Contact>();
        ArrayList<Contact> updatedContacts = new ArrayList<Contact>();

        HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact item : items) {
            Timber.d("Item Created At: " + item.created_at);
            entryMap.put(item.contact_id, item);
        }

        // Get cursor of contacts
        Cursor cursor = db.query(ContactItem.QUERY);
        
        try {
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, ContactItem.ID);
                String contact_id = Db.getString(cursor, ContactItem.CONTACT_ID);
                String created_at = Db.getString(cursor, ContactItem.CREATED_AT);
                String payment_complete_at = Db.getString(cursor, ContactItem.PAYMENT_COMPLETED_AT);
                String closed_at = Db.getString(cursor, ContactItem.CLOSED_AT);
                String disputed_at = Db.getString(cursor, ContactItem.DISPUTED_AT);
                String escrowed_at = Db.getString(cursor, ContactItem.ESCROWED_AT);
                String funded_at = Db.getString(cursor, ContactItem.FUNDED_AT);
                String released_at = Db.getString(cursor, ContactItem.RELEASED_AT);
                String canceled_at = Db.getString(cursor, ContactItem.CANCELED_AT);
                String buyerLastSeen = Db.getString(cursor, ContactItem.BUYER_LAST_SEEN);
                String fundUrl = Db.getString(cursor, ContactItem.FUND_URL);
                boolean isFunded = Db.getBoolean(cursor, ContactItem.IS_FUNDED);
                String sellerLastSeen = Db.getString(cursor, ContactItem.SELLER_LAST_SEEN);
                
                //int messageCount = Db.getInt(cursor, ContactItem.MESSAGE_COUNT);
                
                Contact match = entryMap.get(contact_id);

                if (match != null) {

                    entryMap.remove(contact_id);

                    if (  (match.created_at != null && !match.created_at.equals(created_at))  
                            || (match.payment_completed_at != null && !match.payment_completed_at.equals(payment_complete_at))
                            || (match.closed_at != null && !match.closed_at.equals(closed_at))
                            || (match.disputed_at != null && !match.disputed_at.equals(disputed_at))
                            || (match.escrowed_at != null && !match.escrowed_at.equals(escrowed_at))
                            || (match.funded_at != null && !match.funded_at.equals(funded_at))
                            || (match.released_at != null && !match.released_at.equals(released_at))
                            || (match.canceled_at != null && !match.canceled_at.equals(canceled_at))
                            || (match.buyer.last_online != null && !match.buyer.last_online.equals(buyerLastSeen))
                            || (match.actions.fund_url != null && !match.actions.fund_url.equals(fundUrl))
                            || (match.is_funded != isFunded)
                            || (match.seller.last_online != null && !match.seller.last_online.equals(sellerLastSeen))) {

                        // Update existing record
                        ContactItem.Builder builder = new ContactItem.Builder()
                                .created_at(match.created_at)
                                .payment_completed_at(match.payment_completed_at)
                                .contact_id(match.contact_id)
                                .disputed_at(match.disputed_at)
                                .funded_at(match.funded_at)
                                .escrowed_at(match.escrowed_at)
                                .released_at(match.released_at)
                                .canceled_at(match.canceled_at)
                                .closed_at(match.closed_at)
                                .disputed_at(match.disputed_at)
                                .buyer_last_online(match.buyer.last_online)
                                .seller_last_online(match.seller.last_online)
                                .is_funded(match.is_funded)
                                .fund_url(match.actions.fund_url);

                        int updateInt = db.update(ContactItem.TABLE, builder.build(), ContactItem.ID + " = ?", String.valueOf(id));

                        if (updateInt > 0) {
                            updatedContacts.add(match);
                        }
                    }
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(ContactItem.TABLE, String.valueOf(id));
                    deletedContacts.add(match);
                }
            }
        } finally {
            cursor.close();
        }

        // Add new items
        for (Contact item : entryMap.values()) {

            newContacts.add(item);

            ContactItem.Builder builder = new ContactItem.Builder()
                    .contact_id(item.contact_id)
                    .created_at(item.created_at)
                    .amount(item.amount)
                    .amount_btc(item.amount_btc)
                    .currency(item.currency)
                    .reference_code(item.reference_code)
                    
                    .is_buying(item.is_buying)
                    .is_selling(item.is_selling)
                    
                    .payment_completed_at(item.payment_completed_at)
                    .contact_id(item.contact_id)
                    .disputed_at(item.disputed_at)
                    .funded_at(item.funded_at)
                    .escrowed_at(item.escrowed_at)
                    .released_at(item.released_at)
                    .canceled_at(item.canceled_at)
                    .closed_at(item.closed_at)
                    .disputed_at(item.disputed_at)
                    .is_funded(item.is_funded)

                    .fund_url(item.actions.fund_url)
                    .release_url(item.actions.release_url)
                    .advertisement_public_view(item.actions.advertisement_public_view)
                    
                    .message_url(item.actions.messages_url)
                    .message_post_url(item.actions.message_post_url)
                    
                    .mark_as_paid_url(item.actions.mark_as_paid_url)
                    .dispute_url(item.actions.dispute_url)
                    .cancel_url(item.actions.cancel_url)

                    .buyer_name(item.buyer.name)
                    .buyer_username(item.buyer.username)
                    .buyer_trade_count(item.buyer.trade_count)
                    .buyer_feedback_score(item.buyer.feedback_score)
                    .buyer_last_online(item.buyer.last_online)
                    
                    .seller_name(item.seller.name)
                    .seller_username(item.seller.username)
                    .seller_trade_count(item.seller.trade_count)
                    .seller_feedback_score(item.seller.feedback_score)
                    .seller_last_online(item.seller.last_online)
                    
                    .account_receiver_email(item.account_details.email)
                    .account_receiver_name(item.account_details.receiver_name)
                    .account_iban(item.account_details.iban)
                    .account_swift_bic(item.account_details.swift_bic)
                    .account_reference(item.account_details.reference)

                    .advertisement_id(item.advertisement.id)
                    .advertisement_trade_type(item.advertisement.trade_type.name())
                    .advertisement_payment_method(item.advertisement.payment_method)

                    .advertiser_name(item.advertisement.advertiser.name)
                    .advertiser_username(item.advertisement.advertiser.username)
                    .advertiser_trade_count(item.advertisement.advertiser.trade_count)
                    .advertiser_feedback_score(item.advertisement.advertiser.feedback_score)
                    .advertiser_last_online(item.advertisement.advertiser.last_online);

            db.insert(ContactItem.TABLE, builder.build());
        }
        
        updateMap.put(UPDATES, updatedContacts);
        updateMap.put(ADDITIONS, newContacts);
        updateMap.put(DELETIONS, deletedContacts);
        
        return updateMap;
    }
}
