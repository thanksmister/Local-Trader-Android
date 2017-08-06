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
import android.net.Uri;

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

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

public class DbManager {
    public static final String PREFS_USER = "pref_user";

    public static String UPDATES = "updates";
    public static String ADDITIONS = "additions";
    public static String DELETIONS = "deletions";

    private BriteDatabase db;
    private BriteContentResolver briteContentResolver;
    private ContentResolver contentResolver;

    @Inject
    public DbManager(BriteDatabase db, BriteContentResolver briteContentResolver, ContentResolver contentResolver) {
        this.db = db;
        this.briteContentResolver = briteContentResolver;
        this.contentResolver = contentResolver;
    }

    /**
     * Resets Db manager and clear all preferences
     */
    public void clearDbManager() {
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

    public void clearTable(String tableName) {
        db.delete(tableName, null);
    }

    /*public Observable<ContactItem> contactQuery(String contactId) {
        return db.createQuery(ContactItem.TABLE, ContactItem.QUERY, String.valueOf(contactId))
                .map(ContactItem.MAP)
                .flatMap(new Func1<List<ContactItem>, Observable<ContactItem>>() {
                    @Override
                    public Observable<ContactItem> call(List<ContactItem> contactItems) {
                        if (contactItems.size() > 0) {
                            return Observable.just(contactItems.get(0));
                        }
                        return Observable.just(null);
                    }
                });
    }*/

    public List<String> insertContacts(List<Contact> items) {

        Timber.d("insertContacts");

        HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact item : items) {
            entryMap.put(item.contact_id, item);
        }

        // Get list of all items
        Cursor cursor = contentResolver.query(SyncProvider.CONTACT_TABLE_URI, null, null, null, null);
        if(cursor != null && cursor.getCount() > 0)
            while (cursor.moveToNext()) {
                String contactId = Db.getString(cursor, ContactItem.CONTACT_ID);
                Timber.d("current contact: " + contactId);
                Contact match = entryMap.get(contactId);
                if (match == null) {
                    // delete advertisements that no longer exist in the list
                    deleteContact(contactId);
                }
            }
        items = new ArrayList<Contact>(entryMap.values());
        
        // update or insert new contact
        List<String> updatedContactIds = new ArrayList<>();
        for (Contact item : items) {
            long updatedId = updateContact(item, item.messageCount, item.hasUnseenMessages);
            if (updatedId >= 0) {
                updatedContactIds.add(item.contact_id);
            }
        }
        
        return updatedContactIds;
    }
    
    /*public void updateContacts(List<Contact> contacts) {
        HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact item : contacts) {
            entryMap.put(item.contact_id, item);
        }
        //db.beginTransaction();
        Cursor cursor = db.query(ContactItem.QUERY);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, ContactItem._ID);
                String contact_id = Db.getString(cursor, ContactItem.CONTACT_ID);
                Contact match = entryMap.get(contact_id);
                if (match != null) {
                    entryMap.remove(contact_id);
                    db.update(ContactItem.TABLE, ContactItem.createBuilder(match, match.messageCount, match.hasUnseenMessages).build(), ContactItem._ID + " = ?", String.valueOf(id));
                } else {
                    // Entry doesn't exist. Remove it from the database and its messages
                    db.delete(ContactItem.TABLE, ContactItem._ID + " = ?", String.valueOf(id));
                    db.delete(MessageItem.TABLE, MessageItem.CONTACT_ID + " = ?", String.valueOf(id));
                }
            }
        }

        for (Contact item : entryMap) {
            int messageCount = item.messages.size();
            contentResolver.insert(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(item, messageCount, true).build());
        }
    }*/

    /**
     * Updates or inserts new contact with message count and unseen messages flag.
     * @param contact Contact
     * @param messageCount
     * @param hasUnseenMessages
     */
    public long updateContact(Contact contact, int messageCount, boolean hasUnseenMessages) {
        synchronized (this) {
            Cursor cursor = contentResolver.query(SyncProvider.CONTACT_TABLE_URI, null, ContactItem.CONTACT_ID + " = ?", new String[]{contact.contact_id}, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long id = Db.getLong(cursor, ContactItem._ID);
                    contentResolver.update(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(contact, messageCount, hasUnseenMessages).build(), ContactItem._ID + " = ?", new String[]{String.valueOf(id)});
                }
                cursor.close();
            } else {
                Uri uri = contentResolver.insert(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(contact, messageCount, hasUnseenMessages).build());
                if(uri != null) {
                    return Long.valueOf(uri.getLastPathSegment()); 
                }
                
            }
        }
        return -1;
    }
    
    public void deleteContact(String contactId, ContentResolverAsyncHandler.AsyncQueryListener listener) {
        ContentResolverAsyncHandler contentResolverAsyncHandler = new ContentResolverAsyncHandler(contentResolver, listener);
        contentResolverAsyncHandler.startDelete(1, null, SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{contactId});
        contentResolverAsyncHandler.startDelete(2, null, SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_ID + " = ?", new String[]{contactId});
    }

    public void deleteContact(String contactId) {
        synchronized( this ) {
            Timber.d("deleteContact: " + contactId);
            contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)});
            contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_ID  + " = ?", new String[]{String.valueOf(contactId)});
        }
    }

    public Observable<List<ContactItem>> contactsQuery() {
        return briteContentResolver.createQuery(SyncProvider.CONTACT_TABLE_URI, null, null, null, ContactItem.CREATED_AT + " ASC", false)
                .map(ContactItem.MAP);
    }

    public Observable<ContactItem> contactQuery(String contactId) {
        return briteContentResolver.createQuery(SyncProvider.CONTACT_TABLE_URI, null, ContactItem.CONTACT_ID + " = ?", new String[]{contactId}, null, false)
                .map(ContactItem.MAP_SINGLE);
    }

    public Observable<List<MessageItem>> messagesQuery(String contactId) {
        return briteContentResolver.createQuery(SyncProvider.MESSAGE_TABLE_URI, null, MessageItem.CONTACT_ID + " = ?", new String[]{contactId}, MessageItem.CREATED_AT + " DESC", false)
                .map(MessageItem.MAP);
    }

    public Observable<List<RecentMessageItem>> recentMessagesQuery() {
        return briteContentResolver.createQuery(SyncProvider.RECENT_MESSAGE_TABLE_URI, null, null, null, RecentMessageItem.CREATED_AT + " DESC", false)
                .map(RecentMessageItem.MAP);
    }

    public void markRecentMessagesSeen(String contactId) {
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

    public Observable<List<NotificationItem>> notificationsQuery() {
        return briteContentResolver.createQuery(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, NotificationItem.CREATED_AT + " DESC", false)
                .map(NotificationItem.MAP);
    }

    public void markNotificationRead(String notificationId) {
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

    public Observable<List<TransactionItem>> transactionsQuery() {
        return db.createQuery(TransactionItem.TABLE, TransactionItem.QUERY)
                .map(TransactionItem.MAP);
    }

    public void updateTransactions(final List<Transaction> transactions) {
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

    /**
     * Bulk inserts messages into the database after adding the contact id to each.
     * There is no need to delete previous messages because they are not deleted, so 
     * we only need to insert them. 
     * 
     * @param contactId Contact Id associated with the message.
     * @param messages List of Message items
     */
    public void updateMessages(final String contactId, List<Message> messages) {
        for (Message message : messages) {
            message.contact_id = contactId;
            updateMessage(message);
        }
    }
    
    private void updateMessage(Message message)  {
        synchronized( this ) {
            Cursor cursor = contentResolver.query(SyncProvider.MESSAGE_TABLE_URI, null, MessageItem.CONTACT_ID + " = ? AND " + MessageItem.CREATED_AT + " = ? ", new String[]{message.contact_id, message.created_at}, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long id = Db.getLong(cursor, MessageItem.ID);
                    contentResolver.update(SyncProvider.MESSAGE_TABLE_URI, MessageItem.createBuilder(message).build(), MessageItem.ID + " = ?", new String[]{String.valueOf(id)});
                }
            } else {
                contentResolver.insert(SyncProvider.MESSAGE_TABLE_URI, MessageItem.createBuilder(message).build());
            }
        }
    }

    /**
     * Bulk insert messages.
     * @param items List of Message items
     * @return
     */
    private int bulkInsertMessages(List<Message> items) {
        ContentValues[] contentValuesList = new ContentValues[items.size()];
        for (int i = 0; i < items.size(); i++) {
            Message item = items.get(i);
            ContentValues contentValues = MessageItem.createBuilder(item).build();
            MessageItem model = (getMessage(item.contact_id, item.created_at));
            if (model != null) {
                contentValues = MessageItem.getContentValues(item, model.id());
            }
            Timber.d("MessageItem: " + model);
            Timber.d("ContentValues: " + contentValues.toString());
            contentValuesList[i] = contentValues;
        }
        if (contentValuesList.length > 0) {
            return contentResolver.bulkInsert(SyncProvider.MESSAGE_TABLE_URI, contentValuesList);
        }
        return 0;
    }

    /**
     * Returns a single message item based on contact Id and created date.
     * @param contentId Contact Id associated with the message.
     * @param createdAt The date the message was created.
     * @return MessageItem
     */
    private MessageItem getMessage(String contentId, String createdAt) {
        MessageItem model = null;
        Cursor cursor = contentResolver.query(SyncProvider.MESSAGE_TABLE_URI, null, MessageItem.CONTACT_ID + " = ? AND " + MessageItem.CREATED_AT + " = ? ", new String[]{contentId, createdAt}, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                model = MessageItem.getModel(cursor);
                cursor.close();
            }
        }
        return model;
    }

    @Deprecated // see bulk insert
    public void updateMessages(final String contactId, List<Message> messages, final ContentResolverAsyncHandler.AsyncQueryListener listener) {
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
                .subscribe(new Action1<List<MessageItem>>() {
                    @Override
                    public void call(List<MessageItem> messageItems) {
                        for (MessageItem messageItem : messageItems) {
                            String id = messageItem.contact_id() + "_" + messageItem.create_at();
                            Message match = entryMap.get(id);

                            if (match != null) {
                                entryMap.remove(id);
                            }
                        }

                        ContentResolverAsyncHandler contentResolverAsyncHandler = new ContentResolverAsyncHandler(contentResolver);
                        contentResolverAsyncHandler.setQueryListener(listener);
                        if (entryMap.isEmpty()) {
                            contentResolverAsyncHandler.onQueryComplete();
                        } else {
                            int token = 100;
                            for (Message message : entryMap.values()) {
                                contentResolverAsyncHandler.startInsert(token, null, SyncProvider.MESSAGE_TABLE_URI, MessageItem.createBuilder(message).build());
                                token++;
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
    }

    public Observable<List<MethodItem>> methodQuery() {
        return db.createQuery(MethodItem.TABLE, MethodItem.QUERY)
                .distinct()
                .map(MethodItem.MAP);
    }

    // We get only the methods that ar va
    public Observable<List<MethodItem>> methodSubSetQuery() {
        return db.createQuery(MethodItem.TABLE, MethodItem.QUERY)
                .map(MethodItem.MAP_SUBSET);
    }

    public Observable<List<ExchangeRateItem>> exchangeQuery() {
        return db.createQuery(ExchangeRateItem.TABLE, ExchangeRateItem.QUERY)
                .map(ExchangeRateItem.MAP);
    }

    /**
     * Returns the LocalBitcoins service currencies
     *
     * @return
     */
    public Observable<List<CurrencyItem>> currencyQuery() {
        return db.createQuery(CurrencyItem.TABLE, CurrencyItem.QUERY)
                .distinct()
                .map(CurrencyItem.MAP);
    }

    public Observable<AdvertisementItem> advertisementItemQuery(String adId) {
        return db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY_ITEM, adId)
                .map(AdvertisementItem.MAP)
                .flatMap(new Func1<List<AdvertisementItem>, Observable<AdvertisementItem>>() {
                    @Override
                    public Observable<AdvertisementItem> call(List<AdvertisementItem> advertisementItems) {
                        if (advertisementItems.size() > 0) {
                            return Observable.just(advertisementItems.get(0));
                        }

                        return null;
                    }
                });
    }

    public Observable<WalletItem> walletQuery() {
        return briteContentResolver.createQuery(SyncProvider.WALLET_TABLE_URI, null, null, null, null, false)
                .map(WalletItem.MAP);
    }

    public void updateWallet(final Wallet wallet) {
        Timber.d("updateWallet: " + wallet);

        Subscription subscription = briteContentResolver.createQuery(SyncProvider.WALLET_TABLE_URI, null, null, null, null, false)
                .map(WalletItem.MAP)
                .subscribe(new Action1<WalletItem>() {
                    @Override
                    public void call(WalletItem walletItem) {
                        if (walletItem != null) {

                            if (!walletItem.address().equals(wallet.address)
                                    || !walletItem.balance().equals(wallet.balance)
                                    || !walletItem.sendable().equals(wallet.sendable)) {


                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

                                WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                                //contentResolverAsyncHandler.startUpdate(0, null, SyncProvider.MESSAGE_TABLE_URI, builder.build(), WalletItem._ID + " = ?", new String[]{String.valueOf(walletItem.id())});
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
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
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
     *
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
     *
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

    /**
     * Bulk inserting methods, removing any that don't exist and only inserting
     * those that do not exist. 
     * @param methods
     */
    public void updateMethods(final List<Method> methods) {
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
                    db.delete(MethodItem.TABLE, MethodItem.KEY + " = ?", String.valueOf(key));
                }
            }
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
            MethodItem model = getMethod(item.key);
            if (model != null) {
                contentValues = MethodItem.getContentValues(item, model.id());
            }
            contentValuesList[i] = contentValues;
        }
        if (contentValuesList.length > 0) {
            return contentResolver.bulkInsert(SyncProvider.METHOD_TABLE_URI, contentValuesList);
        }
        return 0;
    }

    /**
     * Get a single <code>NotificationModel</code>
     *
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
    
    public void insertAdvertisements(List<Advertisement> items) {
        
        Timber.d("insertAdvertisements");
        
        HashMap<String, Advertisement> entryMap = new HashMap<String, Advertisement>();
        for (Advertisement item : items) {
            entryMap.put(item.ad_id, item);
        }
        
        // Get list of all items
        Cursor cursor = contentResolver.query(SyncProvider.ADVERTISEMENT_TABLE_URI, null, null, null, null);
        if(cursor != null && cursor.getCount() > 0)
            while (cursor.moveToNext()) {
                String adId = Db.getString(cursor, AdvertisementItem.AD_ID);
                Timber.d("current editAdvertisement: " + adId);
                Advertisement match = entryMap.get(adId);
                if (match == null) {
                    // delete advertisements that no longer exist in the list
                    deleteAdvertisement(adId);
                }
            }
        items = new ArrayList<Advertisement>(entryMap.values());
        // update or insert new advertisements
        for (Advertisement item : items) {
            updateAdvertisement(item);
        }
    }
    
    public void deleteAdvertisement(String adId) {
        synchronized( this ) {
            Timber.d("deleteAdvertisement: " + adId);
            contentResolver.delete(SyncProvider.ADVERTISEMENT_TABLE_URI, AdvertisementItem.AD_ID + " = ?", new String[]{String.valueOf(adId)});
        }
    }
    
    public void updateAdvertisement(Advertisement item) {
        synchronized( this ) {
            Cursor cursor = contentResolver.query(SyncProvider.ADVERTISEMENT_TABLE_URI, null, AdvertisementItem.AD_ID + " = ? ", new String[]{item.ad_id}, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Timber.d("update editAdvertisement: " + item.ad_id);
                    long id = Db.getLong(cursor, AdvertisementItem.ID);
                    contentResolver.update(SyncProvider.ADVERTISEMENT_TABLE_URI, AdvertisementItem.createBuilder(item).build(), AdvertisementItem.ID + " = ?", new String[]{String.valueOf(id)});
                }
            } else {
                Timber.d("insert editAdvertisement: " + item.ad_id);
                contentResolver.insert(SyncProvider.ADVERTISEMENT_TABLE_URI, AdvertisementItem.createBuilder(item).build());
            }
        }
    }

    private int bulkInsertAdvertisements(List<Advertisement> items) {
        ContentValues[] contentValuesList = new ContentValues[items.size()];
        for (int i = 0; i < items.size(); i++) {
            Advertisement item = items.get(i);
            ContentValues contentValues = AdvertisementItem.createBuilder(item).build();
            AdvertisementItem model = getAdvertisement(item.ad_id);
            if (model != null) {
                contentValues = AdvertisementItem.getContentValues(item, model.id());
            }
            contentValuesList[i] = contentValues;
        }
        if (contentValuesList.length > 0) {
            int result = contentResolver.bulkInsert(SyncProvider.ADVERTISEMENT_TABLE_URI, contentValuesList);
            return result;
        }
        return 0;
    }

    private AdvertisementItem getAdvertisement(String adId) {
        AdvertisementItem model = null;
        Cursor cursor = contentResolver.query(SyncProvider.ADVERTISEMENT_TABLE_URI, null, AdvertisementItem.AD_ID + " = ? ", new String[]{adId}, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            model = AdvertisementItem.getModel(cursor);
        }
        return model;
    }
    
    public void updateAdvertisementVisibility(final String adId, final boolean visible) {
        synchronized( this ) {
            contentResolver.update(SyncProvider.ADVERTISEMENT_TABLE_URI, new AdvertisementItem.Builder().visible(visible).build(), AdvertisementItem.AD_ID + " = ?", new String[]{String.valueOf(adId)});
        }
    }

    protected void reportError(Throwable throwable) {
        if (throwable != null && throwable.getLocalizedMessage() != null) {
            Timber.e("Database Manager Error: " + throwable.getLocalizedMessage());
        }
    }
}