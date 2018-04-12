/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.network.api.model.Message;
import com.thanksmister.bitcoin.localtrader.network.api.model.Method;
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.network.services.SyncProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

public class DbManager {

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

        // let's not clear these on reset as they require a lot of work to fetch
        //db.delete(CurrencyItem.TABLE, null);
        //db.delete(ExchangeItem.TABLE, null);
        //db.delete(MethodItem.TABLE, null);
    }

    public void clearTable(String tableName) {
        db.delete(tableName, null);
    }


    public List<String> insertContacts(List<Contact> items) {
        HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact item : items) {
            entryMap.put(item.contact_id, item);
        }

        // Get list of all items
        Cursor cursor = contentResolver.query(SyncProvider.CONTACT_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0)
            while (cursor.moveToNext()) {
                String contactId = Db.getString(cursor, ContactItem.CONTACT_ID);
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

    /**
     * Updates or inserts new contact with message count and unseen messages flag.
     *
     * @param contact           Contact
     * @param messageCount
     * @param hasUnseenMessages
     */
    public long updateContact(@NonNull Contact contact, int messageCount, boolean hasUnseenMessages) {
        if (!TextUtils.isEmpty(contact.contact_id)) {
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
                    if (uri != null) {
                        return Long.valueOf(uri.getLastPathSegment());
                    }

                }
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("updateContact Id is null: " + contact.contact_id));
            }
        }
        return -1;
    }

    public void deleteContact(@NonNull String contactId, ContentResolverAsyncHandler.AsyncQueryListener listener) {
        if (!TextUtils.isEmpty(contactId)) {
            ContentResolverAsyncHandler contentResolverAsyncHandler = new ContentResolverAsyncHandler(contentResolver, listener);
            contentResolverAsyncHandler.startDelete(1, null, SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{contactId});
            contentResolverAsyncHandler.startDelete(2, null, SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_ID + " = ?", new String[]{contactId});
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("deleteContact Id is null: " + contactId));
            }
        }
    }

    private void deleteContact(@NonNull String contactId) {
        if (!TextUtils.isEmpty(contactId)) {
            synchronized (this) {
                contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)});
                contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)});
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("deleteContact Id is null: " + contactId));
            }
        }
    }

    public Observable<List<ContactItem>> contactsQuery() {
        return briteContentResolver.createQuery(SyncProvider.CONTACT_TABLE_URI, null, null, null, ContactItem.CREATED_AT + " ASC", false)
                .map(ContactItem.MAP);
    }

    public Observable<List<NotificationItem>> notificationsQuery() {
        return briteContentResolver.createQuery(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, NotificationItem.CREATED_AT + " DESC", false)
                .map(NotificationItem.MAP);
    }

    public void markNotificationRead(@NonNull String notificationId) {
        if (!TextUtils.isEmpty(notificationId)) {
            synchronized (this) {
                Cursor cursor = contentResolver.query(SyncProvider.NOTIFICATION_TABLE_URI, null, NotificationItem.NOTIFICATION_ID + " = ?", new String[]{String.valueOf(notificationId)}, null);
                if (cursor != null && cursor.getCount() > 0) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(NotificationItem.READ, true);
                    contentResolver.update(SyncProvider.NOTIFICATION_TABLE_URI, contentValues, NotificationItem.NOTIFICATION_ID + " = ?", new String[]{String.valueOf(notificationId)});
                    cursor.close();
                }
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("markNotificationRead Id is null: " + notificationId));
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
                final long id = Db.getLong(cursor, TransactionItem.ID);
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
     * @param messages  List of Message items
     */
    public void updateMessages(@NonNull String contactId, List<Message> messages) {
        if (!TextUtils.isEmpty(contactId)) {
            for (Message message : messages) {
                message.contact_id = contactId;
                updateMessage(message);
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("updateMessages Id is null: " + contactId));
            }
        }
    }

    private void updateMessage(Message message) {
        if (!TextUtils.isEmpty(message.contact_id) && !TextUtils.isEmpty(message.created_at)) {
            synchronized (this) {
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
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("updateMessage Id is null: " + message.contact_id + " | createdAt " + message.created_at));
            }
        }
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

    public Observable<ExchangeRateItem> exchangeQuery() {
        return db.createQuery(ExchangeRateItem.TABLE, ExchangeRateItem.QUERY)
                .map(ExchangeRateItem.MAP_SINGLE);
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

    public Observable<AdvertisementItem> advertisementItemQuery(@NonNull String adId) {
        if (!TextUtils.isEmpty(adId)) {
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
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("advertisementItemQuery Id is null: " + adId));
            }
        }

        return Observable.just(null);
    }

    public Observable<WalletItem> walletQuery() {
        return db.createQuery(WalletItem.TABLE, WalletItem.QUERY)
                .map(WalletItem.MAP);
    }

    public void updateWallet(final Wallet wallet) {
        Cursor cursor = contentResolver.query(SyncProvider.WALLET_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, WalletItem.ID);
                contentResolver.update(SyncProvider.WALLET_TABLE_URI, WalletItem.createBuilder(wallet).build(), WalletItem.ID + " = ?", new String[]{String.valueOf(id)});
            }
        } else {
            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, WalletItem.createBuilder(wallet).build());
        }
    }

    public void updateExchange(final ExchangeRate exchange) {
        Cursor cursor = contentResolver.query(SyncProvider.EXCHANGE_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, ExchangeRateItem.ID);
                contentResolver.update(SyncProvider.EXCHANGE_TABLE_URI, ExchangeRateItem.createBuilder(exchange).build(), ExchangeRateItem.ID + " = ?", new String[]{String.valueOf(id)});
            }
        } else {
            contentResolver.insert(SyncProvider.EXCHANGE_TABLE_URI, ExchangeRateItem.createBuilder(exchange).build());
        }
    }

    /**
     * Insert localbitcoins currencies
     *
     * @param currencies
     */
    public void insertCurrencies(final List<ExchangeCurrency> currencies) {
        contentResolver.delete(SyncProvider.CURRENCY_TABLE_URI, null, null);
        for (ExchangeCurrency item : currencies) {
            CurrencyItem.Builder builder = new CurrencyItem.Builder().currency(item.getCurrency());
            contentResolver.insert(SyncProvider.CURRENCY_TABLE_URI, builder.build());
        }
    }

    /**
     * Bulk inserting methods, removing any that don't exist and only inserting
     * those that do not exist.
     *
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
    private MethodItem getMethod(@NonNull String key) {
        MethodItem model = null;
        if (!TextUtils.isEmpty(key)) {
            Cursor cursor = contentResolver.query(SyncProvider.METHOD_TABLE_URI, null, MethodItem.KEY + " = ?", new String[]{key}, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    model = MethodItem.getModel(cursor);
                    cursor.close();
                }
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("getMethod Key is null: " + key));
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
        if (cursor != null && cursor.getCount() > 0)
            while (cursor.moveToNext()) {
                String adId = Db.getString(cursor, AdvertisementItem.AD_ID);
                Advertisement match = entryMap.get(adId);
                if (match == null && !TextUtils.isEmpty(adId)) {
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

    public void deleteAdvertisement(@NonNull String adId) {
        if (!TextUtils.isEmpty(adId)) {
            synchronized (this) {
                contentResolver.delete(SyncProvider.ADVERTISEMENT_TABLE_URI, AdvertisementItem.AD_ID + " = ?", new String[]{String.valueOf(adId)});
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("deleteAdvertisement Id is null: " + adId));
            }
        }
    }

    public void updateAdvertisement(Advertisement item) {
        if (!TextUtils.isEmpty(item.ad_id)) {
            synchronized (this) {
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
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("updateAdvertisement Id is null: " + item.ad_id));
            }
        }
    }

    public void updateAdvertisementVisibility(@NonNull String adId, final boolean visible) {
        if (!TextUtils.isEmpty(adId)) {
            synchronized (this) {
                contentResolver.update(SyncProvider.ADVERTISEMENT_TABLE_URI, new AdvertisementItem.Builder().visible(visible).build(), AdvertisementItem.AD_ID + " = ?", new String[]{String.valueOf(adId)});
            }
        } else {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(new Throwable("updateAdvertisementVisibility Id is null: " + adId));
            }
        }
    }
}