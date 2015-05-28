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

package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class DbManager 
{
    public static final String PREFS_USER = "pref_user";

    public static String UPDATES = "updates";
    public static String ADDITIONS = "additions";
    public static String DELETIONS = "deletions";
    
    private SqlBrite db;
    
    @Inject
    public DbManager(SqlBrite db)
    {
        this.db = db;
    }

    /**
     * Resets Db manager and clear all preferences
     */
    public void clearDbManager()
    {
        db.delete(WalletItem.TABLE, null);
        db.delete(SessionItem.TABLE, null);
        db.delete(ContactItem.TABLE, null);
        db.delete(MessageItem.TABLE, null);
        db.delete(AdvertisementItem.TABLE, null);
    }

    public Boolean isLoggedIn()
    {
        Cursor cursor = db.query(SessionItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String access_token = Db.getString(cursor, SessionItem.ACCESS_TOKEN);
                return (access_token != null && !Strings.isBlank(access_token));
            }
            return false;
        } finally {
            cursor.close();
        }

        /*return getTokens()
            .flatMap(new Func1<SessionItem, Observable<? extends Boolean>>()
            {
                @Override
                public Observable<? extends Boolean> call(SessionItem sessionItem)
                {
                    return Observable.just(sessionItem != null && !Strings.isBlank(sessionItem.access_token()));
                }
            });*/
    }
    
    public Observable<SessionItem> getTokens()
    {
        return db.createQuery(SessionItem.TABLE, SessionItem.QUERY)
                .map(SessionItem.MAP);
    }

    public void updateTokens(Authorization authorization)
    {
        Timber.d("UpdateTokens");
        
        SessionItem.Builder builder = new SessionItem.Builder()
                .access_token(authorization.access_token)
                .refresh_token(authorization.refresh_token);

        //db.beginTransaction();
        
        Cursor cursor = db.query(SessionItem.QUERY);
        
        try {
            if(cursor.getCount() > 0) {
               
                cursor.moveToFirst();
                long id = Db.getLong(cursor, SessionItem.ID);
                db.update(SessionItem.TABLE, builder.build(), SessionItem.ID + " = ?", String.valueOf(id));
                
            } else {
                db.insert(SessionItem.TABLE, builder.build());
            }
    
            //db.setTransactionSuccessful();
    
        } finally {
            //db.endTransaction();
            cursor.close();
        }
    }
    
    public Observable<ContactItem> contactQuery(String contactId)
    {
        return db.createQuery(ContactItem.TABLE, ContactItem.QUERY_ITEM_WITH_MESSAGES, String.valueOf(contactId))
                .map(ContactItem.MAP)
                .flatMap(new Func1<List<ContactItem>, Observable<ContactItem>>()
                {
                    @Override
                    public Observable<ContactItem> call(List<ContactItem> contactItems)
                    {
                        if (contactItems.size() > 0) {
                            return Observable.just(contactItems.get(0));
                        }

                        return null;
                    }
                });
    }
    

    public TreeMap<String, ArrayList<Contact>> updateContacts(List<Contact> contacts)
    {
        TreeMap<String, ArrayList<Contact>> updateMap = new TreeMap<String, ArrayList<Contact>>();
        ArrayList<Contact> newContacts = new ArrayList<Contact>();
        ArrayList<Contact> deletedContacts = new ArrayList<Contact>();
        ArrayList<Contact> updatedContacts = new ArrayList<Contact>();

        HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact item : contacts) {
            entryMap.put(item.contact_id, item);
        }

        db.beginTransaction();
        
        Cursor cursor = db.query(ContactItem.QUERY);

        try {
            while (cursor.moveToNext()) {
                
                long id = Db.getLong(cursor, ContactItem.ID);
                String contact_id = Db.getString(cursor, ContactItem.CONTACT_ID);
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
              
                Contact match = entryMap.get(contact_id);
                
                if (match != null) {

                    entryMap.remove(contact_id);

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

                    if ((match.payment_completed_at != null && !match.payment_completed_at.equals(payment_complete_at))
                            || (match.closed_at != null && !match.closed_at.equals(closed_at))
                            || (match.disputed_at != null && !match.disputed_at.equals(disputed_at))
                            || (match.escrowed_at != null && !match.escrowed_at.equals(escrowed_at))
                            || (match.funded_at != null && !match.funded_at.equals(funded_at))
                            || (match.released_at != null && !match.released_at.equals(released_at))
                            || (match.canceled_at != null && !match.canceled_at.equals(canceled_at))
                            || (match.actions.fund_url != null && !match.actions.fund_url.equals(fundUrl))
                            || (match.is_funded != isFunded)) {

                        int updateInt = db.update(ContactItem.TABLE, builder.build(), ContactItem.ID + " = ?", String.valueOf(id));

                        if (updateInt > 0) {
                            
                            Timber.d("Update contact in database: " + updateInt);
                            updatedContacts.add(match);
                        }
                        
                    } else if ((match.seller.last_online != null && !match.seller.last_online.equals(sellerLastSeen)) // update without notification
                            || (match.buyer.last_online != null && !match.buyer.last_online.equals(buyerLastSeen))) {

                        int updateInt = db.update(ContactItem.TABLE, builder.build(), ContactItem.ID + " = ?", String.valueOf(id));
                        
                        Timber.d("Update contact in database but not signal update status: " + updateInt);

                    }
                } else {
                    
                    Timber.d("Delete the fucking contact from database: " + contact_id);
                    
                    // Entry doesn't exist. Remove it from the database and its messages
                    int deleteInt = db.delete(ContactItem.TABLE, ContactItem.ID + " = ?", String.valueOf(id));

                    Timber.d("Deleted contact: " + deleteInt);

                    int messagesInt = db.delete(MessageItem.TABLE, MessageItem.CONTACT_LIST_ID + " = ?", String.valueOf(id));

                    Timber.d("Deleted messages: " + messagesInt);
                    
                    deletedContacts.add(match);
                }
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
                
                Timber.d("Insert contact from database: " + item.contact_id);
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            cursor.close();
        }
        
        updateMap.put(UPDATES, updatedContacts);
        updateMap.put(ADDITIONS, newContacts);
        updateMap.put(DELETIONS, deletedContacts);

        return updateMap;
    }

    public void updateContact(Contact contact)
    {
        ContactItem.Builder builder = new ContactItem.Builder()
                .contact_id(contact.contact_id)
                .created_at(contact.created_at)
                .amount(contact.amount)
                .amount_btc(contact.amount_btc)
                .currency(contact.currency)
                .reference_code(contact.reference_code)

                .payment_completed_at(contact.payment_completed_at)
                .contact_id(contact.contact_id)
                .disputed_at(contact.disputed_at)
                .funded_at(contact.funded_at)
                .escrowed_at(contact.escrowed_at)
                .released_at(contact.released_at)
                .canceled_at(contact.canceled_at)
                .closed_at(contact.closed_at)
                .disputed_at(contact.disputed_at)
                .is_funded(contact.is_funded)

                .fund_url(contact.actions.fund_url)
                .release_url(contact.actions.release_url)
                .advertisement_public_view(contact.actions.advertisement_public_view)
                .message_url(contact.actions.messages_url)
                .message_post_url(contact.actions.message_post_url)
                .mark_as_paid_url(contact.actions.mark_as_paid_url)
                .dispute_url(contact.actions.dispute_url)
                .cancel_url(contact.actions.cancel_url)

                .buyer_name(contact.buyer.name)
                .buyer_username(contact.buyer.username)
                .buyer_trade_count(contact.buyer.trade_count)
                .buyer_feedback_score(contact.buyer.feedback_score)
                .buyer_last_online(contact.buyer.last_online)

                .seller_name(contact.seller.name)
                .seller_username(contact.seller.username)
                .seller_trade_count(contact.seller.trade_count)
                .seller_feedback_score(contact.seller.feedback_score)
                .seller_last_online(contact.seller.last_online)

                .account_receiver_email(contact.account_details.email)
                .account_receiver_name(contact.account_details.receiver_name)
                .account_iban(contact.account_details.iban)
                .account_swift_bic(contact.account_details.swift_bic)
                .account_reference(contact.account_details.reference)

                .advertisement_id(contact.advertisement.id)
                .advertisement_trade_type(contact.advertisement.trade_type.name())
                .advertisement_payment_method(contact.advertisement.payment_method)

                .advertiser_name(contact.advertisement.advertiser.name)
                .advertiser_username(contact.advertisement.advertiser.username)
                .advertiser_trade_count(contact.advertisement.advertiser.trade_count)
                .advertiser_feedback_score(contact.advertisement.advertiser.feedback_score)
                .advertiser_last_online(contact.advertisement.advertiser.last_online);

        db.beginTransaction();

        Cursor cursor = db.query(ContactItem.QUERY_ITEM, contact.contact_id);

        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, ContactItem.ID);
                db.update(ContactItem.TABLE, builder.build(), ContactItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(ContactItem.TABLE, builder.build());
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            cursor.close();
        }
    }

    public void deleteContact(String contactId)
    {
        try {
            db.beginTransaction();
            db.delete(ContactItem.TABLE, ContactItem.ID + " = ?", contactId);
            db.delete(MessageItem.TABLE, MessageItem.CONTACT_LIST_ID + " = ?", contactId);
        } finally {
            db.endTransaction();
        }
    }
    
    public Observable<List<ContactItem>> contactsQuery()
    {
        return db.createQuery(ContactItem.TABLE, ContactItem.QUERY)
                .map(ContactItem.MAP);
    }
    
    public Observable<List<MessageItem>> messagesQuery(String contactId)
    {
        return db.createQuery(MessageItem.TABLE, MessageItem.QUERY, contactId)
                .map(MessageItem.MAP);
    }

    public Observable<Integer> messageCountQuery(long contactId)
    {
        return db.createQuery(MessageItem.TABLE, MessageItem.COUNT_QUERY, String.valueOf(contactId))
                .map(new Func1<SqlBrite.Query, Integer>()
                {
                    @Override
                    public Integer call(SqlBrite.Query query)
                    {
                        Cursor cursor = query.run();
                        try {
                            if (!cursor.moveToNext()) {
                                throw new AssertionError("No messages");
                            }
                            return cursor.getInt(0);
                        } finally {
                            cursor.close();
                        }
                    }
                });
    }
    
    // updates messages from list of contacts
    public Observable<List<Message>> updateMessagesFromContacts(List<Contact> contacts)
    {
        final List<Message> newMessages = new ArrayList<Message>();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<? extends List<Message>>>()
                {
                    @Override
                    public Observable<? extends List<Message>> call(final Contact contact)
                    {
                        if(contact.messages.isEmpty()) {
                            return Observable.just(new ArrayList<Message>());
                        } else {
                            List<Message> messages = updateMessages(contact.messages, contact.contact_id);
                            newMessages.addAll(messages);
                            return Observable.just(newMessages);
                        }
                        
                    }
                }).toBlocking().lastOrDefault(newMessages));
    }

    public List<Message> updateMessages(List<Message> messages, String contactId)
    {
        ArrayList<Message> newMessages = new ArrayList<Message>();
        HashMap<String, Message> entryMap = new HashMap<String, Message>();
        
        for (Message message : messages) {
            message.contact_id = contactId;
            entryMap.put(message.created_at, message);
        }

        db.beginTransaction();
        
        Cursor cursor = db.query(MessageItem.SELECT_QUERY, String.valueOf(contactId));
  
        try {
            
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, MessageItem.ID);
                String createdAt = Db.getString(cursor, MessageItem.CREATED_AT);
                boolean seen = Db.getBoolean(cursor, MessageItem.SEEN);
                Message match = entryMap.get(createdAt);

                if (match != null) {
                    // Entry exists. Do not update message
                    entryMap.remove(createdAt);

                    if(seen != match.seen) {
                        
                        MessageItem.Builder builder = new MessageItem.Builder()
                                .seen(true);

                        db.update(MessageItem.TABLE, builder.build(), MessageItem.ID + " = ?", String.valueOf(id));
                    }
  
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(MessageItem.TABLE, MessageItem.ID + " = ?", String.valueOf(id));
                }
            }

            // Add new items
            for (Message item : entryMap.values()) {

                MessageItem.Builder builder = new MessageItem.Builder()
                        .contact_list_id(Long.parseLong(item.contact_id))
                        .message(item.msg)
                        .seen(true)
                        .created_at(item.created_at)
                        .sender_id(item.sender.id)
                        .sender_name(item.sender.name)
                        .sender_username(item.sender.username)
                        .sender_trade_count(item.sender.trade_count)
                        .sender_last_online(item.sender.last_seen_on)
                        .is_admin(item.is_admin)
                        .attachment_name(item.attachment_name)
                        .attachment_type(item.attachment_type)
                        .attachment_url(item.attachment_url);

                db.insert(MessageItem.TABLE, builder.build());

                newMessages.add(item);
            }

            db.setTransactionSuccessful();
            
        } finally {
            
            db.endTransaction();
            cursor.close();
        }
        
        return newMessages;
    }
    
    public Observable<List<MethodItem>> methodQuery()
    {
       return db.createQuery(MethodItem.TABLE, MethodItem.QUERY)
                .map(MethodItem.MAP);
    }

    public Observable<ExchangeItem> exchangeQuery()
    {
        return  db.createQuery(ExchangeItem.TABLE, ExchangeItem.QUERY)
                .map(ExchangeItem.MAP);
    }
    
    public Observable<List<AdvertisementItem>> advertisementQuery()
    {
        return  db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY)
                .map(AdvertisementItem.MAP);
    }

    public Observable<AdvertisementItem> advertisementItemQuery(String adId)
    {
        return db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY_ITEM, adId)
                .map(AdvertisementItem.MAP)
                .flatMap(new Func1<List<AdvertisementItem>, Observable<AdvertisementItem>>()
                {
                    @Override
                    public Observable<AdvertisementItem> call(List<AdvertisementItem> advertisementItems)
                    {
                        if (advertisementItems.size() > 0) {
                            return Observable.just(advertisementItems.get(0));
                        }

                        return null;
                    }
                });
    }

    public Observable<WalletItem> walletQuery()
    {
        return db.createQuery(WalletItem.TABLE, WalletItem.QUERY).map(WalletItem.MAP);
    }

    public void updateWallet(Wallet wallet)
    {
        assert wallet.qrImage != null;
        
        Timber.d("Wallet Image: " + wallet.qrImage);

        // TODO This has to be in some type of async
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        
        WalletItem.Builder builder = new WalletItem.Builder()
                .message(wallet.message)
                .balance(wallet.total.balance)
                .sendable(wallet.total.sendable)
                .address(wallet.address.address)
                .receivable(wallet.address.received)
                .qrcode(baos.toByteArray());
        
        db.beginTransaction();
        
        Cursor cursor = db.query(WalletItem.QUERY);
        
        try {
            if (cursor.getCount() > 0) {
                
                cursor.moveToFirst();

                long id = Db.getLong(cursor, WalletItem.ID);
                String address = Db.getString(cursor, WalletItem.ADDRESS);
                String balance = Db.getString(cursor, WalletItem.BALANCE);
                String received = Db.getString(cursor, WalletItem.RECEIVABLE);
                
                if(!address.equals(wallet.address.address) 
                        || !balance.equals(wallet.total.balance)
                        || !received.equals(wallet.address.received)
                        || !balance.equals(wallet.total.sendable )) {
                    
                    db.update(WalletItem.TABLE, builder.build(), WalletItem.ID + " = ?", String.valueOf(id)); 
                }
 
            } else {
                db.insert(WalletItem.TABLE, builder.build());
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            cursor.close();
        }
    }
    
    public void updateExchange(final Exchange exchange)
    {
        ExchangeItem.Builder builder = new ExchangeItem.Builder()
                .ask(exchange.ask)
                .bid(exchange.bid)
                .last(exchange.last)
                .exchange(exchange.name);
        
        db.beginTransaction();
        Cursor cursor = db.query(ExchangeItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, ExchangeItem.ID);
                db.update(ExchangeItem.TABLE, builder.build(), ExchangeItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(ExchangeItem.TABLE, builder.build());
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            cursor.close();
        }
    }
    
    // Experimental code for updating database without causing back pressure exception
    // rx.exceptions.MissingBackpressureException
    public void updateMethods(final List<Method> methods)
    {
        HashMap<String, Method> entryMap = new HashMap<String, Method>();
        
        for (Method item : methods) {
            entryMap.put(item.key, item);
        }

        db.beginTransaction();
        
        // Get list of all items
        Cursor cursor = db.query(MethodItem.QUERY);
    
        try {
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, MethodItem.ID);
                String key = Db.getString(cursor, MethodItem.KEY);

                Method match = entryMap.get(key);

                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later. Do not update
                    entryMap.remove(key);
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(MethodItem.TABLE, MethodItem.ID + " = ?", String.valueOf(id));
                }
            }

            // Add new items
            for (Method item : entryMap.values()) {
                MethodItem.Builder builder = new MethodItem.Builder()
                        .key(item.key)
                        .name(item.name)
                        .code(item.code)
                        .countryCode(item.countryCode)
                        .countryName(item.countryName);


                db.insert(MethodItem.TABLE, builder.build());
            }

            db.setTransactionSuccessful();
            
        } finally {
            db.endTransaction();
            cursor.close();
        }
    }

    public void updateAdvertisements(final List<Advertisement> advertisements)
    {
        // Build hash table of incoming entries
        HashMap<String, Advertisement> entryMap = new HashMap<String, Advertisement>();
        for (Advertisement item : advertisements) {
            entryMap.put(item.ad_id, item);
        }

        db.beginTransaction();
        
        // Get list of all items
        Cursor cursor = db.query(AdvertisementItem.QUERY);
        
        try {
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, AdvertisementItem.ID);
                String adId = Db.getString(cursor, AdvertisementItem.AD_ID);
                boolean visible = Db.getBoolean(cursor, AdvertisementItem.VISIBLE);

                String location = Db.getString(cursor, AdvertisementItem.LOCATION_STRING);
                String city = Db.getString(cursor, AdvertisementItem.CITY);
                String country_code = Db.getString(cursor, AdvertisementItem.COUNTRY_CODE);
                String trade_type = Db.getString(cursor, AdvertisementItem.TRADE_TYPE);
                String online_provider = Db.getString(cursor, AdvertisementItem.ONLINE_PROVIDER);
                String price_equation = Db.getString(cursor, AdvertisementItem.PRICE_EQUATION);
                String currency = Db.getString(cursor, AdvertisementItem.CURRENCY);
                String account_info = Db.getString(cursor, AdvertisementItem.ACCOUNT_INFO);

                double lat = Db.getDouble(cursor, AdvertisementItem.LAT);
                double lon = Db.getDouble(cursor, AdvertisementItem.LON);

                String min_amount = Db.getString(cursor, AdvertisementItem.MIN_AMOUNT);
                String max_amount = Db.getString(cursor, AdvertisementItem.MAX_AMOUNT);
                String temp_price = Db.getString(cursor, AdvertisementItem.TEMP_PRICE);
                String bank_name = Db.getString(cursor, AdvertisementItem.BANK_NAME);
                String message = Db.getString(cursor, AdvertisementItem.MESSAGE);

                boolean smsRequired = Db.getBoolean(cursor, AdvertisementItem.SMS_VERIFICATION_REQUIRED);
                boolean trustRequired = Db.getBoolean(cursor, AdvertisementItem.TRUSTED_REQUIRED);
                boolean trackMaxAmount = Db.getBoolean(cursor, AdvertisementItem.TRACK_MAX_AMOUNT);

                Advertisement match = entryMap.get(adId);

                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later.
                    entryMap.remove(adId);

                    if ((match.price_equation != null && !match.price_equation.equals(price_equation)) ||
                            (match.temp_price != null && !match.temp_price.equals(temp_price)) ||
                            (match.city != null && !match.city.equals(city)) ||
                            (match.country_code != null && !match.country_code.equals(country_code)) ||
                            (match.bank_name != null && !match.bank_name.equals(bank_name)) ||
                            (match.currency != null && !match.currency.equals(currency)) ||
                            (match.lat != lat) || (match.lon != lon) ||
                            (match.location != null && !match.location.equals(location)) ||
                            (match.max_amount != null && !match.max_amount.equals(max_amount)) ||
                            (match.min_amount != null && !match.min_amount.equals(min_amount)) ||
                            (match.online_provider != null && !match.online_provider.equals(online_provider)) ||
                            (match.account_info != null && !match.account_info.equals(account_info)) ||
                            (match.trade_type.name() != null && !match.trade_type.name().equals(trade_type)) ||
                            (match.sms_verification_required != smsRequired) ||
                            (match.visible != visible ||
                                    (match.trusted_required != trustRequired) ||
                                    (match.track_max_amount != trackMaxAmount) ||
                                    (match.message != null && !match.message.equals(message)))) {


                        AdvertisementItem.Builder builder = new AdvertisementItem.Builder()
                                .city(match.city)
                                .country_code(match.country_code)
                                .currency(match.currency)
                                .lat(match.lat)
                                .lon(match.lon)
                                .location_string(match.location)
                                .max_amount(match.max_amount)
                                .min_amount(match.min_amount)
                                .online_provider(match.online_provider)
                                .temp_price(match.temp_price)
                                .price_equation(match.price_equation)
                                .action_public_view(match.actions.public_view)
                                .trade_type(match.trade_type.name())
                                .visible(match.visible)
                                .account_info(match.account_info)
                                .profile_last_online(match.profile.last_online)
                                .profile_name(match.profile.name)
                                .profile_username(match.profile.username)
                                .bank_name(match.bank_name)
                                .trusted_required(match.trusted_required)
                                .message(match.message)
                                .track_max_amount(match.track_max_amount);

                        db.update(AdvertisementItem.TABLE, builder.build(), AdvertisementItem.ID + " = ?", String.valueOf(id));

                    }
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(AdvertisementItem.TABLE, AdvertisementItem.ID + " = ?", String.valueOf(id));
                }
            }

            for (Advertisement item : entryMap.values()) {

                AdvertisementItem.Builder builder = new AdvertisementItem.Builder()
                        .created_at(item.created_at)
                        .ad_id(item.ad_id)
                        .city(item.city)
                        .country_code(item.country_code)
                        .currency(item.currency)
                        .email(item.email)
                        .lat(item.lat)
                        .lon(item.lon)
                        .location_string(item.location)
                        .max_amount(item.max_amount)
                        .min_amount(item.min_amount)
                        .max_amount_available(item.max_amount_available)
                        .online_provider(item.online_provider)
                        .require_trade_volume(item.require_trade_volume)
                        .require_feedback_score(item.require_feedback_score)
                        .atm_model(item.atm_model)
                        .temp_price(item.temp_price)
                        .temp_price_usd(item.temp_price_usd)
                        .price_equation(item.price_equation)
                        .reference_type(item.reference_type)
                        .action_public_view(item.actions.public_view)
                        .sms_verification_required(item.sms_verification_required)
                        .trade_type(item.trade_type.name())
                        .visible(item.visible)
                        .account_info(item.account_info)
                        .profile_last_online(item.profile.last_online)
                        .profile_name(item.profile.name)
                        .profile_username(item.profile.username)
                        .profile_feedback_score(item.profile.feedback_score)
                        .profile_trade_count(item.profile.trade_count)
                        .bank_name(item.bank_name)
                        .message(item.message)
                        .track_max_amount(item.track_max_amount)
                        .trusted_required(item.trusted_required);

                db.insert(AdvertisementItem.TABLE, builder.build());
            }
            
            db.setTransactionSuccessful();
            
        } finally {
            db.endTransaction();
            cursor.close();
        }
    }

    public Boolean updateAdvertisement(Advertisement advertisement)
    {
        AdvertisementItem.Builder builder = new AdvertisementItem.Builder()
                .created_at(advertisement.created_at)
                .ad_id(advertisement.ad_id)
                .city(advertisement.city)
                .country_code(advertisement.country_code)
                .currency(advertisement.currency)
                .email(advertisement.email)
                .lat(advertisement.lat)
                .lon(advertisement.lon)
                .location_string(advertisement.location)
                .max_amount(advertisement.max_amount)
                .min_amount(advertisement.min_amount)
                .max_amount_available(advertisement.max_amount_available)
                .online_provider(advertisement.online_provider)
                .require_trade_volume(advertisement.require_trade_volume)
                .require_feedback_score(advertisement.require_feedback_score)
                .atm_model(advertisement.atm_model)
                .temp_price(advertisement.temp_price)
                .temp_price_usd(advertisement.temp_price_usd)
                .price_equation(advertisement.price_equation)
                .reference_type(advertisement.reference_type)
                .action_public_view(advertisement.actions.public_view)
                .sms_verification_required(advertisement.sms_verification_required)
                .trade_type(advertisement.trade_type.name())
                .visible(advertisement.visible)
                .account_info(advertisement.account_info)
                .profile_last_online(advertisement.profile.last_online)
                .profile_name(advertisement.profile.name)
                .profile_username(advertisement.profile.username)
                .profile_feedback_score(advertisement.profile.feedback_score)
                .profile_trade_count(advertisement.profile.trade_count)
                .bank_name(advertisement.bank_name)
                .message(advertisement.message)
                .track_max_amount(advertisement.track_max_amount)
                .trusted_required(advertisement.trusted_required);
        
        db.beginTransaction();
        Cursor cursor = db.query(AdvertisementItem.QUERY_ITEM, advertisement.ad_id);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, AdvertisementItem.ID);
                db.update(AdvertisementItem.TABLE, builder.build(), AdvertisementItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(AdvertisementItem.TABLE, builder.build());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            cursor.close();
        }

        return true;
    }

    private class QrCodeTask extends AsyncTask<Object, Void, Object[]>
    {
        private Context context;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        protected Object[] doInBackground(Object... params)
        {
            Wallet wallet = (Wallet) params[0];
            context = (Context) params[1];
            Bitmap qrCode = null;

            wallet.qrImage = WalletUtils.encodeAsBitmap(wallet.address.address, context.getApplicationContext());

            return new Object[]{wallet};
        }

        protected void onPostExecute(Object[] result)
        {
            Wallet wallet = (Wallet) result[0];
            //updateWalletQrCode(wallet.id, wallet.qrImage, context);
        }
    }
}
