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

package com.thanksmister.bitcoin.localtrader.network.services;

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

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.network.api.model.Method;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.persistence.Preferences;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import dpreference.DPreference;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

@Singleton
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String ACTION_SYNC = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC";
    public static final String ACTION_TYPE_START = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_START";
    public static final String ACTION_TYPE_COMPLETE = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_COMPLETE";
    public static final String ACTION_TYPE_REFRESH = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_TYPE_REFRESH";
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

    private NotificationService notificationService;
    private SharedPreferences sharedPreferences;
    private Preferences preferences;
    private ContentResolver contentResolver;

    // store all ongoing syncs
    private HashMap<String, Boolean> syncMap;
    private final AtomicBoolean canceled = new AtomicBoolean(false);

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        syncMap = new HashMap<>(); // init sync map

        /*preference = new DPreference(getContext().getApplicationContext(), "LocalTraderPref");
        sharedPreferences = getContext().getApplicationContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        notificationService = new NotificationService(context);
        DbOpenHelper dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        SqlBrite sqlBrite = SqlBrite.create();
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(dbOpenHelper);
        contentResolver = context.getContentResolver();
        BriteContentResolver briteContentResolver = sqlBrite.wrapContentProvider(contentResolver);

        dbManager = new DbManager(db, briteContentResolver, contentResolver);
        LocalBitcoinsService localBitcoins = initLocalBitcoins();
        if(localBitcoins != null) {
            dataService = new DataService((BaseApplication) context.getApplicationContext(), preference, sharedPreferences, localBitcoins);
        }*/
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        boolean hasCredentials = preferences.hasCredentials();
        Timber.d("onPerformSync hasCredentials: " + hasCredentials);
        Timber.d("onPerformSync isSyncing: " + isSyncing());

        this.canceled.set(false);

        if (!isSyncing() && hasCredentials && !isCanceled()) {
            getCurrencies();
            getMethods();
            getNotifications();
            getWalletBalance();
            if(preferences.isFirstTime()) {
                getContacts();
                getAdvertisements();
            }
            if (!isSyncing() && !isCanceled()) {
                resetSyncing();
                onSyncComplete();
            } else if (isCanceled()) {
                resetSyncing();
                onSyncCanceled();
            }
        }
    }

    /**
     * Keep a map of all syncing calls to update sync status and
     * broadcast when no more syncs running
     *
     * @param key
     * @param value
     */
    private void updateSyncMap(String key, boolean value) {
        Timber.d("updateSyncMap: " + key + " value: " + value);
        syncMap.put(key, value);
        if (isSyncing()) {
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
     *
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
     *
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

    private void onSyncRefresh() {
        Timber.d("onSyncRefresh");
        Intent intent = new Intent(ACTION_SYNC);
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_REFRESH);
        getContext().sendBroadcast(intent);
    }

    private void onSyncFailed(Throwable cause) {
        int code = SYNC_ERROR_CODE;
      /*  if(cause instanceof NetworkException) {
            NetworkException networkException = (NetworkException) cause;
            code = (networkException.getCode());
            if(code == 3) {
                // allow refresh
                return;
            } else {
                code = networkException.getStatus();
            }
        }
*/
        Intent intent = new Intent(ACTION_SYNC);
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, cause.getCause());
        intent.putExtra(EXTRA_ERROR_CODE, code);
        getContext().sendBroadcast(intent);

        if (cause.getMessage() != null) {
            Timber.e("Sync Data Error: " + cause.getMessage());
            cause.printStackTrace();
        }
    }

    private void getCurrencies() {
        Timber.d("getCurrencies");
        /*dbManager.currencyQuery()
                .subscribe(new Action1<List<CurrencyItem>>() {
                    @Override
                    public void call(List<CurrencyItem> currencyItems) {
                        if (currencyItems == null || currencyItems.isEmpty()) {
                            fetchCurrencies();
                        } else {
                            if (dataService.needToRefreshCurrency()) {
                                fetchCurrencies();
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());
                        updateSyncMap(SYNC_CURRENCIES, false);
                    }
                });*/
    }

    private void fetchCurrencies() {
        updateSyncMap(SYNC_CURRENCIES, true);
        /*dataService.getCurrencies()
                .subscribe(new Action1<List<ExchangeCurrency>>() {
                    @Override
                    public void call(List<ExchangeCurrency> currencies) {
                        if (currencies != null) {
                            dbManager.insertCurrencies(currencies);
                            dataService.setCurrencyExpireTime();
                        }
                        updateSyncMap(SYNC_CURRENCIES, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cancelSync();
                        onSyncFailed(throwable);
                        updateSyncMap(SYNC_CURRENCIES, false);
                    }
                });*/
    }

    private void getMethods() {
        Timber.d("getMethods");
       /* dbManager.methodQuery().subscribe(new Action1<List<MethodItem>>() {
            @Override
            public void call(List<MethodItem> methodItems) {
                if (methodItems == null || methodItems.isEmpty()) {
                    fetchMethods();
                } else {
                    if (dataService.needToRefreshMethods()) {
                        fetchMethods();
                    }
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable.getMessage());
                updateSyncMap(SYNC_METHODS, false);
            }
        });*/
    }

    private void fetchMethods() {
        Timber.d("getMethods");
        updateSyncMap(SYNC_METHODS, true);
        /*dataService.getMethods()
                .subscribe(new Action1<List<Method>>() {
                    @Override
                    public void call(List<Method> methods) {
                        if (methods != null) {
                            dbManager.updateMethods(methods);
                            dataService.setMethodsExpireTime();
                        }
                        updateSyncMap(SYNC_METHODS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cancelSync();
                        onSyncFailed(throwable);
                        updateSyncMap(SYNC_METHODS, false);
                    }
                });*/
    }

    private void getAdvertisements() {

        Timber.d("getAdvertisements");

        updateSyncMap(SYNC_ADVERTISEMENTS, true);

        boolean force = false;
        Timber.d("getAdvertisements force: " + force);

        /*dataService.getAdvertisements(force)
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
                        if (throwable instanceof InterruptedIOException) {
                            Timber.d("Advertisements Error: " + throwable.getMessage());
                        } else {
                            Timber.e("Advertisements Error: " + throwable.getMessage());
                        }
                        cancelSync();
                        onSyncFailed(throwable);
                        updateSyncMap(SYNC_ADVERTISEMENTS, false);
                    }
                });*/
    }

    private void getContacts() {
        Timber.d("getContacts");
        updateSyncMap(SYNC_CONTACTS, true);
        /*dataService.getContacts(DashboardType.ACTIVE)
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) {
                        if (contacts != null) {
                            dbManager.insertContacts(contacts);
                        }
                        updateSyncMap(SYNC_CONTACTS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cancelSync();
                        onSyncFailed(throwable);
                        updateSyncMap(SYNC_CONTACTS, false);
                    }
                });*/
    }

    private void getNotifications() {
        Timber.d("getNotifications");
        updateSyncMap(SYNC_NOTIFICATIONS, true);
        /*dataService.getNotifications()
                .subscribe(new Action1<List<Notification>>() {
                    @Override
                    public void call(List<Notification> notifications) {
                        if (notifications != null) {
                            updateNotifications(notifications);
                        }
                        updateSyncMap(SYNC_NOTIFICATIONS, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cancelSync();
                        onSyncFailed(throwable);
                        updateSyncMap(SYNC_NOTIFICATIONS, false);
                    }
                });*/
    }

    private void getWalletBalance() {
        Timber.d("getWalletBalance");
        updateSyncMap(SYNC_WALLET, true);
        /*dataService.getWalletBalance()
                .subscribe(new Action1<Wallet>() {
                    @Override
                    public void call(Wallet wallet) {
                        if (wallet != null) {
                            updateWalletBalance(wallet);
                        }
                        updateSyncMap(SYNC_WALLET, false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        cancelSync();
                        onSyncFailed(throwable);
                        updateSyncMap(SYNC_WALLET, false);
                    }
                });*/
    }

    /**
     * Updates the notifications list by adding only the newest
     * notifications, updating the current notifications status
     */
    private void updateNotifications(final List<Notification> notifications) {
        Timber.d("updateNotifications : " + notifications.size());

        /*final HashMap<String, Notification> entryMap = new HashMap<>();
        for (Notification notification : notifications) {
            entryMap.put(notification.getNotification_id(), notification);
        }

        Cursor cursor = contentResolver.query(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                final long id = Db.getLong(cursor, NotificationItem.ID);
                String notificationId = Db.getString(cursor, NotificationItem.NOTIFICATION_ID);
                boolean notificationRead = Db.getBoolean(cursor, NotificationItem.READ);
                String url = Db.getString(cursor, NotificationItem.URL);
                Notification match = entryMap.get(notificationId);
                if (match != null) {
                    entryMap.remove(notificationId);
                    if (match.getRead() != notificationRead || !match.getUrl().equals(url)) {
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
            onSyncRefresh(); // we assume we have new contacts and need to refresh
        }*/
    }

    private void updateWalletBalance(final Wallet wallet) {

        Timber.d("updateWalletBalance");

        /*Cursor cursor = contentResolver.query(SyncProvider.WALLET_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            final long id = Db.getLong(cursor, WalletItem.ID);
            String address = Db.getString(cursor, WalletItem.ADDRESS);
            String balance = Db.getString(cursor, WalletItem.BALANCE);
            if (!address.equals(wallet.getAddress()) || !balance.equals(wallet.getBalance())) {
                WalletItem.Builder builder = WalletItem.createBuilder(wallet);
                contentResolver.update(SyncProvider.WALLET_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(id)});
                try {
                    double newBalance = Doubles.convertToDouble(wallet.getBalance());
                    double oldBalance = Doubles.convertToDouble(balance);
                    String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                    Timber.d("updateWalletBalance newBalance: " + newBalance);
                    Timber.d("updateWalletBalance oldBalance: " + oldBalance);
                    if (newBalance > oldBalance) {
                        notificationService.balanceUpdateNotification("Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC");
                    }
                } catch (Exception e) {
                    Timber.e(e.getMessage());
                }
            }
            cursor.close();
        } else {
            WalletItem.Builder builder = WalletItem.createBuilder(wallet);
            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, builder.build());
            Timber.d("updateWalletBalance Init Balance: " + wallet.getBalance());
            notificationService.balanceUpdateNotification("Bitcoin Balance", "Bitcoin balance...", "You have " + wallet.getBalance() + " BTC");
        }*/
    }

    // TODO save this to local disk to access later for faster render time
    /*private Observable<Bitmap> generateBitmap(final String address) {
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
    }*/

    /*private LocalBitcoinsService initLocalBitcoins() {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            OkClient client = new OkClient(okHttpClient);
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setClient(client)
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .setEndpoint(BASE_URL)
                    .build();
            return restAdapter.create(LocalBitcoinsService.class);
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
        return null;
    }*/
}