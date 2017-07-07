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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.services.SyncProvider;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

public class DbManager 
{
    public static final String PREFS_USER = "pref_user";

    public static String UPDATES = "updates";
    public static String ADDITIONS = "additions";
    public static String DELETIONS = "deletions";
    
    private BriteDatabase db;
    private BriteContentResolver briteContentResolver;
    private ContentResolver contentResolver;
    
    @Inject
    public DbManager(BriteDatabase db, BriteContentResolver briteContentResolver, ContentResolver contentResolver)
    {
        this.db = db;
        this.briteContentResolver = briteContentResolver;
        this.contentResolver = contentResolver;
    }

    /**
     * Resets Db manager and clear all preferences
     */
    public void clearDbManager()
    {
        db.delete(WalletItem.TABLE, null);
        db.delete(ContactItem.TABLE, null);
        db.delete(MessageItem.TABLE, null);
        db.delete(AdvertisementItem.TABLE, null);
        db.delete(TransactionItem.TABLE, null);
        db.delete(RecentMessageItem.TABLE, null);
        db.delete(NotificationItem.TABLE, null);
        db.delete(ExchangeCurrencyItem.TABLE, null);
        db.delete(AdvertisementItem.TABLE, null);
    }
    
    public Observable<ContactItem> contactQuery(String contactId) {
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

        //db.beginTransaction();
        
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
                
                Timber.d("Insert contact from database: " + item.contact_id);
            }
            
        } finally {
            cursor.close();
        }
        
        updateMap.put(UPDATES, updatedContacts);
        updateMap.put(ADDITIONS, newContacts);
        updateMap.put(DELETIONS, deletedContacts);

        return updateMap;
    }
    
    public void updateContact(Contact contact, int messageCount, boolean hasUnseenMessages, ContentResolverAsyncHandler.AsyncQueryListener listener)
    {
        // TODO check that update is needed before runnging update query
        ContentResolverAsyncHandler contentResolverAsyncHandler = new ContentResolverAsyncHandler(contentResolver, listener);
        contentResolverAsyncHandler.startUpdate(0, null, SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(contact, messageCount, hasUnseenMessages).build(), ContactItem.CONTACT_ID + " = ?", new String[]{contact.contact_id});
        //return contentResolver.update(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(contact, messageCount, hasUnseenMessages).build(), ContactItem.CONTACT_ID + " = ?", new String[]{contact.contact_id});
    }
    
    public void deleteContact(String contactId, ContentResolverAsyncHandler.AsyncQueryListener listener)
    {
        ContentResolverAsyncHandler contentResolverAsyncHandler = new ContentResolverAsyncHandler(contentResolver, listener);
        contentResolverAsyncHandler.startDelete(1, null, SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{contactId});
        contentResolverAsyncHandler.startDelete(2, null, SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{contactId});
      
        //contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{contactId});
        //contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{contactId});
    }
    
    public Observable<List<ContactItem>> contactsQuery()
    {
        return briteContentResolver.createQuery(SyncProvider.CONTACT_TABLE_URI, null, null, null, ContactItem.CREATED_AT  + " ASC", false)
                .map(ContactItem.MAP);
    }
    
    public Observable<List<MessageItem>> messagesQuery(String contactId)
    {
        return briteContentResolver.createQuery(SyncProvider.MESSAGE_TABLE_URI, null, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{contactId}, MessageItem.CREATED_AT + " DESC", false)
                .map(MessageItem.MAP);
    }
    
    public Observable<List<RecentMessageItem>> recentMessagesQuery()
    {
        return briteContentResolver.createQuery(SyncProvider.RECENT_MESSAGE_TABLE_URI, null, null, null, RecentMessageItem.CREATED_AT + " DESC", false)
                .map(RecentMessageItem.MAP);
    }
    
    public void markRecentMessagesSeen()
    {
        Timber.d("markRecentMessagesSeen");
        synchronized (this) {
            Cursor cursor = contentResolver.query(SyncProvider.RECENT_MESSAGE_TABLE_URI, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(RecentMessageItem.SEEN, true);
                contentResolver.update(SyncProvider.RECENT_MESSAGE_TABLE_URI, contentValues, null, null);
                cursor.close();
            }
        }
    }

    public void markRecentMessagesSeen(String contactId)
    {
        Timber.d("markRecentMessagesSeen contactId: " + contactId);
        synchronized (this) {
            Cursor cursor = contentResolver.query(SyncProvider.RECENT_MESSAGE_TABLE_URI, null, RecentMessageItem.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)}, null);
            if (cursor != null && cursor.getCount() > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(RecentMessageItem.SEEN, true);
                contentResolver.update(SyncProvider.RECENT_MESSAGE_TABLE_URI, contentValues, RecentMessageItem.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)});
                cursor.close();
            }
        }
    }

    public Observable<List<NotificationItem>> notificationsQuery()
    {
        return briteContentResolver.createQuery(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, NotificationItem.CREATED_AT + " DESC", false)
                .map(NotificationItem.MAP);
    }

    public void markNotificationRead(String notificationId)
    {
        Timber.d("markNotificationSeen notificationId: " + notificationId);
        synchronized (this) {
            Cursor cursor = contentResolver.query(SyncProvider.NOTIFICATION_TABLE_URI, null, NotificationItem.NOTIFICATION_ID + " = ?", new String[]{String.valueOf(notificationId)}, null);
            if (cursor != null && cursor.getCount() > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(NotificationItem.READ, true);
                contentResolver.update(SyncProvider.NOTIFICATION_TABLE_URI, contentValues, NotificationItem.NOTIFICATION_ID + " = ?", new String[]{String.valueOf(notificationId)});
                cursor.close();
            }
        }
    }

    public Observable<List<TransactionItem>> transactionsQuery()
    {
        return db.createQuery(TransactionItem.TABLE, TransactionItem.QUERY)
                .map(TransactionItem.MAP);
    }
    
    public void updateTransactions(final List<Transaction> transactions)
    {
        HashMap<String, Transaction> entryMap = new HashMap<String, Transaction>();

        for (Transaction item : transactions) {
            entryMap.put(item.txid, item);
        }
        
        // Get list of all items
        Cursor cursor = db.query(TransactionItem.QUERY);

        try {
            while (cursor.moveToNext()) {
                
                long id = Db.getLong(cursor, TransactionItem.ID);
                String tx_id = Db.getString(cursor, TransactionItem.TRANSACTION_ID);
                
                Transaction match = entryMap.get(tx_id);
                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later. Do not update
                    entryMap.remove(tx_id);
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(TransactionItem.TABLE, TransactionItem.ID + " = ?", String.valueOf(id));
                }
            }

            // Add new items
            for (Transaction item : entryMap.values()) {
                db.insert(TransactionItem.TABLE, TransactionItem.createBuilder(item).build());
            }
            
        } finally {
          
            cursor.close();
        }
    }

    public void updateMessages(final String contactId, List<Message> messages, final ContentResolverAsyncHandler.AsyncQueryListener listener)
    {
        final HashMap<String, Message> entryMap = new HashMap<String, Message>();
        
        for (Message message : messages) {
            message.id = contactId + "_" + message.created_at;
            message.contact_id = contactId;
            entryMap.put(message.id, message);
        }
        
        // get all the current messages
        Subscription subscription = briteContentResolver.createQuery(SyncProvider.MESSAGE_TABLE_URI,
                null, null, null, null, false)
                .map(MessageItem.MAP)
                .subscribe(new Action1<List<MessageItem>>()
                {
                    @Override
                    public void call(List<MessageItem> messageItems)
                    {
                        for (MessageItem messageItem : messageItems) {
                            String id = messageItem.contact_id() + "_" + messageItem.create_at();
                            Message match = entryMap.get(id);
                            
                            if (match != null) {
                                entryMap.remove(id);
                            }
                        }

                        ContentResolverAsyncHandler contentResolverAsyncHandler = new ContentResolverAsyncHandler(contentResolver);
                        contentResolverAsyncHandler.setQueryListener(listener);
                        if(entryMap.isEmpty()) {
                            contentResolverAsyncHandler.onQueryComplete();
                        } else {
                            int token = 100;
                            for (Message message : entryMap.values()) {
                                contentResolverAsyncHandler.startInsert(token, null, SyncProvider.MESSAGE_TABLE_URI, MessageItem.createBuilder(message).build());
                                token++;
                            } 
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
    }
    
    public Observable<List<MethodItem>> methodQuery()
    {
       return db.createQuery(MethodItem.TABLE, MethodItem.QUERY)
                .map(MethodItem.MAP);
    }

    // We get only the methods that ar va
    public Observable<List<MethodItem>> methodSubSetQuery()
    {
        return db.createQuery(MethodItem.TABLE, MethodItem.QUERY)
                .map(MethodItem.MAP_SUBSET);
    }

    public Observable<List<ExchangeRateItem>> exchangeQuery()
    {
        return db.createQuery(ExchangeRateItem.TABLE, ExchangeRateItem.QUERY)
                .map(ExchangeRateItem.MAP);
    }

    /**
     * Returns the LocalBitcoins service currencies
     * @return
     */
    public Observable<List<CurrencyItem>> currencyQuery() {
        return db.createQuery(CurrencyItem.TABLE, CurrencyItem.QUERY)
                .map(CurrencyItem.MAP);
    }

    /**
     * Returns the exchange currencies
     * @return
     */
    public Observable<List<ExchangeCurrencyItem>> exchangeCurrencyQuery()
    {
        return db.createQuery(ExchangeCurrencyItem.TABLE, ExchangeCurrencyItem.QUERY)
                .map(ExchangeCurrencyItem.MAP);
    }
    
    public Observable<List<AdvertisementItem>> advertisementsQuery()
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
        return briteContentResolver.createQuery(SyncProvider.WALLET_TABLE_URI, null, null, null, null, false)
                .map(WalletItem.MAP);
    }

    public void updateWallet(final Wallet wallet)
    {
        Timber.d("updateWallet: " + wallet);
     
        Subscription subscription = briteContentResolver.createQuery(SyncProvider.WALLET_TABLE_URI, null, null, null, null, false)
                .map(WalletItem.MAP)
                .subscribe(new Action1<WalletItem>()
                {
                    @Override
                    public void call(WalletItem walletItem)
                    {
                        if (walletItem != null) {
                            
                            if (!walletItem.address().equals(wallet.address)
                                    || !walletItem.balance().equals(wallet.balance)
                                    || !walletItem.sendable().equals(wallet.sendable)) {

                                
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

                                WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                                //contentResolverAsyncHandler.startUpdate(0, null, SyncProvider.MESSAGE_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(walletItem.id())});
                                contentResolver.update(SyncProvider.WALLET_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(walletItem.id())});
                            }

                        } else {

                            Timber.d("updateWallet insert");

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

                            WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                           // contentResolverAsyncHandler.startInsert(0, null, SyncProvider.WALLET_TABLE_URI, builder.build());
                            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, builder.build());
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
    }
    
    public void updateExchange(final ExchangeRate exchange) {
        Cursor cursor = db.query(ExchangeRateItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, ExchangeRateItem.ID);
                db.update(ExchangeRateItem.TABLE, ExchangeRateItem.createBuilder(exchange).build(), ExchangeRateItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(ExchangeRateItem.TABLE, ExchangeRateItem.createBuilder(exchange).build());
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Insert localbitcoins currencies
     * @param currencies
     */
    public void insertCurrencies(final List<ExchangeCurrency> currencies) {
        db.delete(CurrencyItem.TABLE, null);
        for (ExchangeCurrency item : currencies) {
            CurrencyItem.Builder builder = new CurrencyItem.Builder()
                    .currency(item.getCurrency());
            db.insert(CurrencyItem.TABLE, builder.build());
        }
    }

    /**
     * Insert exchange currencies
     * @param currencies
     */
    public void insertExchangeCurrencies(final List<ExchangeCurrency> currencies) {
        db.delete(ExchangeCurrencyItem.TABLE, null);
        for (ExchangeCurrency item : currencies) {
            ExchangeCurrencyItem.Builder builder = new ExchangeCurrencyItem.Builder()
                    .currency(item.getCurrency());
            db.insert(ExchangeCurrencyItem.TABLE, builder.build());
        }
    }
    
    /*
    private int bulkInsertFlows(List<Flow> items) {
        // remove duplicates
        Set<Flow> hs = new HashSet<>();
        hs.addAll(items);
        items.clear();
        items.addAll(hs);
        
        ContentValues[] contentValuesList = new ContentValues[items.size()];
        for (int i = 0; i < items.size(); i++) {
            Flow item = items.get(i);
            ContentValues contentValues = FlowModel.createBuilder(item).build();
            FlowModel model = getFlow(item.getFlowId());
            if(model != null) {
                contentValues = FlowModel.getContentValues(item, model.id());
            }
            contentValuesList[i] = contentValues;
        }
        if(contentValuesList.length > 0) {
            return mContentResolver.bulkInsert(SyncProvider.FLOW_TABLE_URI, contentValuesList);
        }
        return 0;
    }
     */

    // Experimental code for updating database without causing back pressure exception
    // rx.exceptions.MissingBackpressureException
    public void updateMethods(final List<Method> methods)
    {
        HashMap<String, Method> entryMap = new HashMap<String, Method>();
        for (Method item : methods) {
            entryMap.put(item.key, item);
        }
        
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
            /*for (Method item : entryMap.values()) {
                MethodItem.Builder builder = new MethodItem.Builder()
                        .key(item.key)
                        .name(item.name)
                        .code(item.code)
                        .countryCode(item.countryCode)
                        .countryName(item.countryName);

                // TODO handle large updates
                db.insert(MethodItem.TABLE, builder.build());
            }*/
            bulkInsertMethods(new ArrayList<Method>(entryMap.values()));
        } finally {
            cursor.close();
        }
    }

    private int bulkInsertMethods(List<Method> items) {
        // remove duplicates
        Set<Method> hs = new HashSet<>();
        hs.addAll(items);
        items.clear();
        items.addAll(hs);
        ContentValues[] contentValuesList = new ContentValues[items.size()];
        for (int i = 0; i < items.size(); i++) {
            Method item = items.get(i);
            ContentValues contentValues = MethodItem.createBuilder(item).build();
            MethodItem model = (getMethod(item.key));
            if(model != null) {
                contentValues = MethodItem.getContentValues(item, model.id());
            }
            contentValuesList[i] = contentValues;
        }
        if(contentValuesList.length > 0) {
            return contentResolver.bulkInsert(SyncProvider.METHOD_TABLE_URI, contentValuesList);
        }
        return 0;
    }

    /**
     * Get a single <code>NotificationModel</code>
     * @param key
     * @return
     */
    private MethodItem getMethod(String key) {
        MethodItem model = null;
        Cursor cursor = contentResolver.query(SyncProvider.METHOD_TABLE_URI, null, MethodItem.KEY + " = ?", new String[]{key}, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                model = MethodItem.getModel(cursor);
                cursor.close();
            }
        }
        return model;
    }

    public void updateAdvertisements(final List<Advertisement> advertisements)
    {
        // Build hash table of incoming entries
        HashMap<String, Advertisement> entryMap = new HashMap<String, Advertisement>();
        for (Advertisement item : advertisements) {
            entryMap.put(item.ad_id, item);
        }

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
            
        } finally {
            cursor.close();
        }
    }

    public void updateAdvertisementVisibility(final String adId, final boolean visible)
    {
        db.update(AdvertisementItem.TABLE, new AdvertisementItem.Builder().visible(visible).build(), AdvertisementItem.AD_ID + " = ?", String.valueOf(adId));
    }
    
    public int updateAdvertisement(final Advertisement advertisement)
    {
       return db.update(AdvertisementItem.TABLE, AdvertisementItem.createBuilder(advertisement).build(), AdvertisementItem.AD_ID + " = ?", advertisement.ad_id);
    }

    protected void reportError(Throwable throwable)
    {
        if(throwable != null && throwable.getLocalizedMessage() != null)
            Timber.e("Database Manager Error: " + throwable.getLocalizedMessage());
    }
}
