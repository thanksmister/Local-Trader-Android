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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.Db;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.DbOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.NotificationItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.io.ByteArrayOutputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import dpreference.DPreference;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

@Singleton
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    
    public static final String ACTION_SYNC = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC";
    public static final String ACTION_TYPE_START = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_START";
    public static final String ACTION_TYPE_COMPLETE = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_COMPLETE";
    public static final String ACTION_TYPE_CANCELED = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_CANCELED";
    public static final String ACTION_TYPE_ERROR = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_ERROR";
    
    public static final String EXTRA_ACTION_TYPE = "com.thanksmister.bitcoin.localtrader.extra.EXTRA_ACTION";
    public static final String EXTRA_ERROR_CODE = "com.thanksmister.bitcoin.localtrader.extra.EXTRA_ERROR_CODE";
    public static final String EXTRA_ERROR_MESSAGE = "com.thanksmister.bitcoin.localtrader.extra.EXTRA_ERROR_MESSAGE";
    
    private static final String SYNC_CURRENCIES = "com.thanksmister.bitcoin.localtrader.sync.SYNC_CURRENCIES";
    private static final String SYNC_WALLET = "com.thanksmister.bitcoin.localtrader.sync.SYNC_WALLET";
    private static final String SYNC_ADVERTISEMENTS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_ADVERTISEMENTS";
    private static final String SYNC_METHODS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_METHODS";
    private static final String SYNC_CONTACTS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_CONTACTS";
    private static final String SYNC_NOTIFICATIONS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_NOTIFICATIONS";
    
    public static final int SYNC_ERROR_CODE = 9;
    
    private static final String BASE_URL = "https://localbitcoins.com/";

    private DataService dataService;
    private DbManager dbManager;
    private NotificationService notificationService;
    private SharedPreferences sharedPreferences;
    private DPreference preference;
    private ContentResolver contentResolver;
    private BriteContentResolver briteContentResolver;
    
    // store all ongoing syncs
    private HashMap<String, Boolean> syncMap;
    private final AtomicBoolean canceled = new AtomicBoolean(false);

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        syncMap = new HashMap<>(); // init sync map

        preference = new DPreference(getContext().getApplicationContext(), "LocalTraderPref");
        sharedPreferences = getContext().getApplicationContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        notificationService = new NotificationService(context, preference, sharedPreferences);
        DbOpenHelper dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        SqlBrite sqlBrite = SqlBrite.create();
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(dbOpenHelper);
        contentResolver = context.getContentResolver();
        briteContentResolver = sqlBrite.wrapContentProvider(contentResolver);

        dbManager = new DbManager(db, briteContentResolver, contentResolver);
        LocalBitcoins localBitcoins = initLocalBitcoins();
        dataService = new DataService((BaseApplication) context.getApplicationContext(), preference, sharedPreferences, localBitcoins);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        boolean hasCredentials = AuthUtils.hasCredentials(preference, sharedPreferences);
        Timber.d("onPerformSync hasCredentials: " + hasCredentials);
        Timber.d("onPerformSync isSyncing: " + isSyncing());

        this.canceled.set(false);

        if(!isSyncing() && hasCredentials && !isCanceled()) {
            getCurrencies();
            getMethods();
            getContacts();
            getAdvertisements();
            getNotifications();
            getWalletBalance();
            if(!isSyncing() && !isCanceled()) {
                onSyncComplete();
            } else if (isCanceled()) {
                onSyncCanceled();
            }
        }
    }

    /**
     * Keep a map of all syncing calls to update sync status and
     * broadcast when no more syncs running
     * @param key
     * @param value
     */
    private void updateSyncMap(String key, boolean value) {
        Timber.d("updateSyncMap: " + key + " value: " + value);
        syncMap.put(key, value);
        if(isSyncing()) {
            onSyncStart();
        } else {
            resetSyncing();
            onSyncComplete();
        }
    }

    /**
     * Prints the sync map for debugging
     */
    private void printSyncMap() {
        for (Object o : syncMap.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            Timber.d("Sync Map>>>>>> " + pair.getKey() + " = " + pair.getValue());
        }
    }

    /**
     * Checks if any active syncs are going one
     * @return
     */
    private boolean isSyncing() {
        printSyncMap();
        Timber.d("isSyncing: " + syncMap.containsValue(true));
        return syncMap.containsValue(true);
    }

    /**
     * Resets the syncing map
     */
    private void resetSyncing() {
        syncMap = new HashMap<>();
    }

    /**
     * Check if the sync has been canceled due to error or network
     * @return
     */
    private boolean isCanceled() {
        return canceled.get();
    }
    
    private void cancelSync() {
        this.canceled.set(true);
    }
    
    @Override
    public void onSyncCanceled() {
        Timber.d("onSyncComplete");
        super.onSyncCanceled();
        Intent intent = new Intent(ACTION_SYNC);
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_CANCELED);
        getContext().sendBroadcast(intent);
    }

    private void onSyncStart() {
        Timber.d("onSyncStart");
        Intent intent = new Intent(ACTION_SYNC);
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_START);
        getContext().sendBroadcast(intent);
    }

    private void onSyncComplete() {
        Timber.d("onSyncComplete");
        Intent intent = new Intent(ACTION_SYNC);
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_COMPLETE);
        getContext().sendBroadcast(intent);
    }

    private void onSyncFailed(String message, int code) {
        Intent intent = new Intent(ACTION_SYNC);
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, message);
        intent.putExtra(EXTRA_ERROR_CODE, code);
        getContext().sendBroadcast(intent);
    }

    private void getCurrencies() {
        Timber.d("getCurrencies");
        updateSyncMap(SYNC_CURRENCIES, true);
        dataService.getCurrencies()
                .subscribe(new Action1<List<ExchangeCurrency>>() {
                    @Override
                    public void call(List<ExchangeCurrency> currencies) {
                        if(currencies != null) {
                            dbManager.insertCurrencies(currencies);
                        }
                        updateSyncMap(SYNC_CURRENCIES, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);
                        cancelSync();
                        onSyncFailed(throwable.getMessage(), SYNC_ERROR_CODE);
                        updateSyncMap(SYNC_CURRENCIES, false);
                    }
                });
    }
    
    private void getMethods() {
        Timber.d("getMethods");
        updateSyncMap(SYNC_METHODS, true);
        dataService.getMethods()
                .subscribe(new Action1<List<Method>>() {
                    @Override
                    public void call(List<Method> methods) {
                        if(methods != null) {
                            dbManager.updateMethods(methods);
                        }
                        updateSyncMap(SYNC_METHODS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);
                        cancelSync();
                        onSyncFailed(throwable.getMessage(), SYNC_ERROR_CODE);
                        updateSyncMap(SYNC_METHODS, false);
                    }
                });
    }
    
    private void getAdvertisements() {
        
        Timber.d("getAdvertisements");
        
        updateSyncMap(SYNC_ADVERTISEMENTS, true);
        
        boolean force = AuthUtils.getForceUpdate(preference);
        Timber.d("getAdvertisements force: " + force);
        
        dataService.getAdvertisements(force)
                .subscribe(new Action1<List<Advertisement>>() {
                    @Override
                    public void call(List<Advertisement> advertisements) {
                        if (advertisements != null && !advertisements.isEmpty()) {
                            dbManager.insertAdvertisements(advertisements);
                        }
                        AuthUtils.setForceUpdate(preference, false);
                        updateSyncMap(SYNC_ADVERTISEMENTS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if(throwable instanceof InterruptedIOException) {
                            Timber.d("Advertisements Error: " + throwable.getMessage());
                        } else {
                            Timber.e("Advertisements Error: " + throwable.getMessage());
                            handleError(throwable);
                        }
                        cancelSync();
                        AuthUtils.setForceUpdate(preference, false);
                        onSyncFailed(throwable.getMessage(), SYNC_ERROR_CODE);
                        updateSyncMap(SYNC_ADVERTISEMENTS, false);
                    }
                });
    }

    private void getContacts() {
        Timber.d("getContacts");
        updateSyncMap(SYNC_CONTACTS, true);
        dataService.getContacts(DashboardType.ACTIVE)
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) {
                        if(contacts != null) {
                            dbManager.updateContacts(contacts);
                            updateContacts(contacts); 
                        }
                        updateSyncMap(SYNC_CONTACTS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);
                        cancelSync();
                        onSyncFailed(throwable.getMessage(), SYNC_ERROR_CODE);
                        updateSyncMap(SYNC_CONTACTS, false);
                    }
                });
    }

    private void getNotifications() {
        Timber.d("getNotifications");
        updateSyncMap(SYNC_NOTIFICATIONS, true);
        dataService.getNotifications()
                .subscribe(new Action1<List<Notification>>() {
                    @Override
                    public void call(List<Notification> notifications) {
                        if(notifications != null) {
                            updateNotifications(notifications);
                        }
                        updateSyncMap(SYNC_NOTIFICATIONS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);
                        isCanceled();
                        onSyncFailed(throwable.getMessage(), SYNC_ERROR_CODE);
                        updateSyncMap(SYNC_NOTIFICATIONS, false);
                    }
                });
    }

    private void getWalletBalance() {
        Timber.d("getWalletBalance");
        updateSyncMap(SYNC_WALLET, true);
        dataService.getWalletBalance()
                .flatMap(new Func1<Wallet, Observable<Wallet>>() {
                    @Override
                    public Observable<Wallet> call(final Wallet wallet) {
                        if(wallet == null || TextUtils.isEmpty(wallet.address)) {
                            return Observable.just(wallet);
                        }
                        
                        return generateBitmap(wallet.address)
                                .map(new Func1<Bitmap, Wallet>() {
                                    @Override
                                    public Wallet call(Bitmap bitmap) {
                                        wallet.qrImage = bitmap;
                                        return wallet;
                                    }
                                }).onErrorReturn(new Func1<Throwable, Wallet>() {
                                    @Override
                                    public Wallet call(Throwable throwable) {
                                        reportError(throwable);
                                        return wallet;
                                    }
                                });
                    }
                })
                .subscribe(new Action1<Wallet>() {
                    @Override
                    public void call(Wallet wallet) {
                        if(wallet != null) {
                            updateWalletBalance(wallet);
                        }
                        updateSyncMap(SYNC_WALLET, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);
                        isCanceled();
                        onSyncFailed(throwable.getMessage(), SYNC_ERROR_CODE);
                        updateSyncMap(SYNC_WALLET, false);
                    }
                });
    }

    protected void reportError(Throwable throwable) {
        if (throwable != null && throwable.getMessage() != null) {
            Timber.e("Sync Data Error: " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    protected void handleError(Throwable throwable) {
        reportError(throwable);
    }

    /**
     * Updates the notifications list by adding only the newest
     * notifications, updating the current notifications status
     */
    private void updateNotifications(final List<Notification> notifications) {
        Timber.d("updateNotifications : " + notifications.size());

        boolean hasCredentials = AuthUtils.hasCredentials(preference, sharedPreferences);
        if (!hasCredentials) {
            return;
        }

        final HashMap<String, Notification> entryMap = new HashMap<>();
        for (Notification notification : notifications) {
            entryMap.put(notification.notification_id, notification);
        }

        Cursor cursor = contentResolver.query(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, NotificationItem.ID);
                String notificationId = Db.getString(cursor, NotificationItem.NOTIFICATION_ID);
                boolean notificationRead = Db.getBoolean(cursor, NotificationItem.READ);
                String url = Db.getString(cursor, NotificationItem.URL);
                Notification match = entryMap.get(notificationId);
                if (match != null) {
                    entryMap.remove(notificationId);
                    if (match.read != notificationRead || !match.url.equals(url)) {
                        NotificationItem.Builder builder = NotificationItem.createBuilder(match);
                        contentResolver.update(SyncProvider.NOTIFICATION_TABLE_URI, builder.build(), NotificationItem.ID + " = ?", new String[]{String.valueOf(id)});
                    }
                } else {
                    contentResolver.delete(SyncProvider.NOTIFICATION_TABLE_URI, NotificationItem.ID + " = ?", new String[]{String.valueOf(id)});
                }
            }
            cursor.close();
        }

        List<Notification> newNotifications = new ArrayList<>();
        if (!entryMap.isEmpty()) {
            for (Notification notification : entryMap.values()) {
                newNotifications.add(notification);
                contentResolver.insert(SyncProvider.NOTIFICATION_TABLE_URI, NotificationItem.createBuilder(notification).build());
            }
        }

        Timber.d("updateNotifications newNotifications: " + newNotifications.size());
        if (!newNotifications.isEmpty()) {
            notificationService.createNotifications(newNotifications);
        }
    }

    private void updateWalletBalance(final Wallet wallet) {
        Timber.d("updateWalletBalance");

        boolean hasCredentials = AuthUtils.hasCredentials(preference, sharedPreferences);
        if (!hasCredentials) {
            return;
        }

        Cursor cursor = contentResolver.query(SyncProvider.WALLET_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            long id = Db.getLong(cursor, WalletItem.ID);
            String address = Db.getString(cursor, WalletItem.ADDRESS);
            String balance = Db.getString(cursor, WalletItem.BALANCE);
            if (!address.equals(wallet.address) || !balance.equals(wallet.balance)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
                WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                contentResolver.update(SyncProvider.WALLET_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(id)});
                try {
                    double newBalance = Doubles.convertToDouble(wallet.balance);
                    double oldBalance = Doubles.convertToDouble(balance);
                    String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                    Timber.d("updateWalletBalance newBalance: " + newBalance);
                    Timber.d("updateWalletBalance oldBalance: " + oldBalance);
                    if (newBalance > oldBalance) {
                        notificationService.balanceUpdateNotification("Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC");
                    }
                } catch (Exception e) {
                    reportError(e);
                }
            }
            cursor.close();
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
            WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, builder.build());
            Timber.d("updateWalletBalance Init Balance: " + wallet.balance);
            notificationService.balanceUpdateNotification("Bitcoin Balance", "Bitcoin balance...", "You have " + wallet.balance + " BTC");
        }
    }

    // TODO move this to util as its used in other locations
    private Observable<Bitmap> generateBitmap(final String address) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                try {
                    subscriber.onNext(WalletUtils.encodeAsBitmap(address, getContext()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
    
    private LocalBitcoins initLocalBitcoins() {
        OkHttpClient okHttpClient = new OkHttpClient();
        OkClient client = new OkClient(okHttpClient);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(BASE_URL)
                .build();
        return restAdapter.create(LocalBitcoins.class);
    }

    private void updateContacts(List<Contact> contacts) {
        Timber.d("Update Contacts Data Size: " + contacts.size());

        boolean hasCredentials = AuthUtils.hasCredentials(preference, sharedPreferences);
        if (!hasCredentials) {
            return;
        }

        final HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact contact : contacts) {
            entryMap.put(contact.contact_id, contact);
        }

        final ArrayList<Contact> newContacts = new ArrayList<Contact>();
        final ArrayList<String> deletedContacts = new ArrayList<String>();
        final ArrayList<Contact> updatedContacts = new ArrayList<Contact>();

        Subscription subscription = briteContentResolver.createQuery(SyncProvider.CONTACT_TABLE_URI, null, null, null, null, false)
                .map(ContactItem.MAP)
                .subscribe(new Action1<List<ContactItem>>() {
                    @Override
                    public void call(List<ContactItem> contactItems) {
                        Timber.d("Contact Items in Database: " + contactItems.size());
                        for (ContactItem contactItem : contactItems) {
                            Contact match = entryMap.get(contactItem.contact_id());
                            if (match != null) {
                                entryMap.remove(contactItem.contact_id());
                                if ((match.payment_completed_at != null && !match.payment_completed_at.equals(contactItem.payment_completed_at()))
                                        || (match.closed_at != null && !match.closed_at.equals(contactItem.closed_at()))
                                        || (match.disputed_at != null && !match.disputed_at.equals(contactItem.disputed_at()))
                                        || (match.escrowed_at != null && !match.escrowed_at.equals(contactItem.escrowed_at()))
                                        || (match.funded_at != null && !match.funded_at.equals(contactItem.funded_at()))
                                        || (match.released_at != null && !match.released_at.equals(contactItem.released_at()))
                                        || (match.canceled_at != null && !match.canceled_at.equals(contactItem.canceled_at()))
                                        || (match.actions.fund_url != null && !match.actions.fund_url.equals(contactItem.funded_at()))
                                        || (match.is_funded != contactItem.is_funded())) {
                                    updatedContacts.add(match);
                                } else if ((match.seller.last_online != null && !match.seller.last_online.equals(contactItem.seller_last_online()))
                                        || (match.hasUnseenMessages != contactItem.hasUnseenMessages())
                                        || (match.messageCount != contactItem.messageCount())
                                        || (match.buyer.last_online != null && !match.buyer.last_online.equals(contactItem.buyer_last_online()))) {
                                    updatedContacts.add(match);
                                }
                            } else {
                                deletedContacts.add(contactItem.contact_id());
                            }
                        }

                        for (Contact contact : entryMap.values()) {
                            newContacts.add(contact);
                        }

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
        
        for (Contact item : newContacts) {
            int messageCount = item.messages.size();
            contentResolver.insert(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(item, messageCount, true).build());
        }
        
        for (Contact item : updatedContacts) {
            int messageCount = item.messages.size();
            contentResolver.update(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(item, messageCount, item.hasUnseenMessages).build(), ContactItem.CONTACT_ID + " = ?", new String[]{item.contact_id});
        }

        // FIXME we are deleting contacts that we happen to want to see from our notifications, just keep the history
        for (String id : deletedContacts) {
            //contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{id});
            //contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_ID + " = ?", new String[]{id});
        }
    }
}
