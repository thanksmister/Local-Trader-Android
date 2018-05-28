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

package com.thanksmister.bitcoin.localtrader.network.services

import android.accounts.Account
import android.arch.lifecycle.MutableLiveData
import android.content.*
import android.os.Bundle
import android.text.TextUtils
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.*
import com.thanksmister.bitcoin.localtrader.network.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.persistence.*
import com.thanksmister.bitcoin.localtrader.persistence.Currency
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Parser
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.Map

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.ArrayList

class SyncAdapter @Inject
constructor(context: Context, autoInitialize: Boolean, private val preferences: Preferences,
            private val notificationData: NotificationDao, private val walletData: WalletDao,
            private val currencyData: CurrencyDao, private val methodData: MethodDao) : AbstractThreadedSyncAdapter(context, autoInitialize) {

    private val notificationService: NotificationService

    private val disposable = CompositeDisposable()

    // store all ongoing syncs
    private var syncMap: HashMap<String, Boolean>? = null
    private val canceled = AtomicBoolean(false)

    /**
     * Checks if any active syncs are going one
     *
     * @return
     */
    private val isSyncing: Boolean
        get() {
            printSyncMap()
            Timber.d("isSyncing: " + syncMap!!.containsValue(true))
            return syncMap!!.containsValue(true)
        }

    /**
     * Check if the sync has been canceled due to error or network
     * @return
     */
    private val isCanceled: Boolean
        get() = canceled.get()

    init {
        syncMap = HashMap() // init sync map

        // TODO inject notification service
        notificationService = NotificationService(context)
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        val hasCredentials = preferences.hasCredentials()
        Timber.d("onPerformSync hasCredentials: $hasCredentials")
        Timber.d("onPerformSync isSyncing: $isSyncing")

        this.canceled.set(false)

        if (!isSyncing && hasCredentials && !isCanceled) {
            getCurrencies()
            getMethods()
            getNotifications()
            getWalletBalance()
            if (!isSyncing && !isCanceled) {
                resetSyncing()
                onSyncComplete()
            } else if (isCanceled) {
                resetSyncing()
                onSyncCanceled()
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
    private fun updateSyncMap(key: String, value: Boolean) {
        Timber.d("updateSyncMap: $key value: $value")
        syncMap!![key] = value
        if (isSyncing) {
            onSyncStart()
        } else {
            resetSyncing()
            onSyncComplete()
        }
    }

    /**
     * Prints the sync map for debugging
     */
    private fun printSyncMap() {
        for (o in syncMap!!.entries) {
            val pair = o as Map.Entry<*, *>
            Timber.d("Sync Map>>>>>> " + pair.key + " = " + pair.value)
        }
    }

    /**
     * Resets the syncing map
     */
    private fun resetSyncing() {
        syncMap = HashMap()
    }

    private fun cancelSync() {
        this.canceled.set(true)
    }

    override fun onSyncCanceled() {
        Timber.d("onSyncComplete")
        super.onSyncCanceled()
        val intent = Intent(ACTION_SYNC)
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_CANCELED)
        context.sendBroadcast(intent)
    }

    private fun onSyncStart() {
        Timber.d("onSyncStart")
        val intent = Intent(ACTION_SYNC)
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_START)
        context.sendBroadcast(intent)
    }

    private fun onSyncComplete() {
        Timber.d("onSyncComplete")
        val intent = Intent(ACTION_SYNC)
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_COMPLETE)
        context.sendBroadcast(intent)
    }

    private fun onSyncFailed(cause: Throwable) {
        var code = SYNC_ERROR_CODE
        if (cause is NetworkException) {
            code = cause.code
            if (code == 3) {
                // allow refresh
                return
            } else {
                code = cause.status
            }
        }

        val intent = Intent(ACTION_SYNC)
        intent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_ERROR)
        intent.putExtra(EXTRA_ERROR_MESSAGE, cause.cause)
        intent.putExtra(EXTRA_ERROR_CODE, code)
        context.sendBroadcast(intent)

        if (cause.message != null) {
            Timber.e("Sync Data Error: " + cause.message)
            cause.printStackTrace()
        }
    }

    private fun getCurrencies() {
        disposable.add(currencyData.getItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({items ->
                    if(items.isEmpty()) {
                        fetchCurrencies()
                    } else if (preferences.needToRefreshCurrency()) {
                        fetchCurrencies();
                    }
                }, { error -> Timber.e("Database error: " + error)}))
    }

    private fun fetchCurrencies() {
        updateSyncMap(SYNC_CURRENCIES, true);
        val endpoint = preferences.endPoint()!!
        val api = LocalBitcoinsApi(endpoint)
        val fetcher = LocalBitcoinsFetcher(context, api, preferences)
        disposable.add(fetcher.currencies
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map { currencies ->
                    Parser.parseCurrencies(currencies)
                }
                .subscribe({currencies ->
                    updateSyncMap(SYNC_CURRENCIES, false);
                    updateCurrencies(currencies)
                    preferences.setCurrencyExpireTime()
                }, { error ->
                    Timber.e("Error message: " + error.message)
                    cancelSync();
                    onSyncFailed(error);
                    updateSyncMap(SYNC_CURRENCIES, false);
                }))
    }

    private fun updateCurrencies(currencies: List<Currency>) {
        disposable.add(Completable.fromAction {
            currencyData.updateItem(currencies)}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { error -> Timber.e("Database error" + error.message)}))
    }

    private fun getMethods() {
        disposable.add(methodData.getItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({items ->
                    if(items.isEmpty()) {
                        fetchMethods()
                    } else if (preferences.needToRefreshMethods()) {
                        fetchMethods();
                    }
                }, { error -> Timber.e("Database error: " + error)}))
    }

    private fun fetchMethods() {
        updateSyncMap(SYNC_METHODS, true);
        val endpoint = preferences.endPoint()!!
        val api = LocalBitcoinsApi(endpoint)
        val fetcher = LocalBitcoinsFetcher(context, api, preferences)
        disposable.add(fetcher.methods
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map { methods ->
                    Parser.parseMethods(methods)
                }
                .subscribe({methods ->
                    updateSyncMap(SYNC_METHODS, false);
                    updateMethods(methods)
                    preferences.setMethodsExpireTime()
                }, { error ->
                    Timber.e("Error message: " + error.message)
                    cancelSync();
                    onSyncFailed(error);
                    updateSyncMap(SYNC_METHODS, false);
                }))
    }

    private fun updateMethods(methods: List<Method>) {
        disposable.add(Completable.fromAction {
            methodData.updateItems(methods)}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { error -> Timber.e("Database error" + error.message)}))
    }


    private fun getNotifications() {
        updateSyncMap(SYNC_NOTIFICATIONS, true);
        val endpoint = preferences.endPoint()!!
        val api = LocalBitcoinsApi(endpoint)
        val fetcher = LocalBitcoinsFetcher(context, api, preferences)
        disposable.add(fetcher.notifications
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({response ->
                    updateSyncMap(SYNC_NOTIFICATIONS, false);
                    updateNotifications(response)
                }, { error ->
                    Timber.e("Error message: " + error.message)
                    cancelSync();
                    onSyncFailed(error);
                    updateSyncMap(SYNC_NOTIFICATIONS, false);
                }))
    }

    private fun getWalletBalance() {
        if(preferences.needToRefreshWalletBalance()) {
            updateSyncMap(SYNC_WALLET, true);
            val endpoint = preferences.endPoint()!!
            val api = LocalBitcoinsApi(endpoint)
            val fetcher = LocalBitcoinsFetcher(context, api, preferences)
            disposable.add(fetcher.getWalletBalance()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ wallet ->
                        updateSyncMap(SYNC_WALLET, false);
                        Timber.d("wallet: " + wallet.total?.balance)
                        Timber.d("wallet: " + wallet.address)
                        updateWallet(wallet)
                        preferences.setWalletBalanceExpireTime()
                    }, { error ->
                        Timber.e("Error message: " + error.message)
                        cancelSync();
                        onSyncFailed(error);
                        updateSyncMap(SYNC_WALLET, false);
                    }))
        }
    }

    private fun updateWallet(wallet: Wallet) {
        Timber.d("updateWallet")
        disposable.add(walletData.getItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({items ->
                    if(items.isNotEmpty()) {
                        val walletItem = items.get(0)
                        updateWalletBalanceAndNotify(wallet, walletItem)
                    } else {
                        insertWallet(wallet)
                    }
                }, { error -> Timber.e("Unable to get message: " + error)}))
    }

    private fun insertWallet(wallet: Wallet) {
        disposable.add(Completable.fromAction {
            walletData.insertItem(wallet)}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { error -> Timber.e("Database error" + error.message)}))

       if(wallet.total != null) {
           notificationService.createNotification(context.getString(R.string.notifcation_bitcoin_amount_title), context.getString(R.string.notifcation_bitcoin_amount_description) + wallet.total?.balance + " BTC")
       }
    }

    private fun updateWalletBalanceAndNotify(newWallet: Wallet, oldWallet: Wallet) {
        try {
           val oldBalance = oldWallet.total?.balance?.toDoubleOrNull()
           val newBalance = newWallet.total?.balance?.toDoubleOrNull()
            if(oldBalance != null && newBalance != null) {
                val diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                if (newBalance > oldBalance) {
                    notificationService.createNotification(context.getString(R.string.notification_bitcoin_received), context.getString(R.string.notification_bitcoin_received_description, diff));
                }
                if(newBalance > oldBalance || newWallet.address != oldWallet.address) {
                    disposable.add(Completable.fromAction {
                        walletData.updateItem(newWallet)}
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}, { error -> Timber.e("Database error" + error.message)}))
                }
            }
        } catch (e: Exception) {
            Timber.d("Trouble doubling the balance: " + e.message)
        }
    }

    /**
     * Updates the notifications list by adding only the newest
     * notifications, updating the current notifications status
     */
    private fun updateNotifications(notifications: List<Notification>) {
        Timber.d("updateNotifications")
        disposable.add(notificationData.getItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({items ->
                    if(items.isNotEmpty()) {
                        reconcileNotificationsList(notifications, items)
                    } else {
                        reconcileNotificationsList(notifications, ArrayList<Notification>())
                    }
                }, { error -> Timber.e("Unable to get message: " + error)}))
    }

    private fun reconcileNotificationsList(newNotifications: List<Notification>, currentNotifications: List<Notification>) {
        val insertNotifications = ArrayList<Notification>()
        for(new in newNotifications) {
            var exists = false
            for (current in currentNotifications) {
                if(current.id == new.id) {
                    exists = true
                }
            }
            if(!exists) {
                insertNotifications.add(new)
            }
        }

        if (!insertNotifications.isEmpty()) {
            disposable.add(Completable.fromAction {
                notificationData.insertAll(insertNotifications) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({}, { error -> Timber.e("Database error" + error.message)}))

            notificationService.createNotifications(insertNotifications);
        }
    }

    companion object {
        const val ACTION_SYNC = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC"
        const val ACTION_TYPE_START = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_START"
        const val ACTION_TYPE_COMPLETE = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_COMPLETE"
        const val ACTION_TYPE_CANCELED = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_CANCELED"
        const val ACTION_TYPE_ERROR = "com.thanksmister.bitcoin.localtrader.data.services.ACTION_SYNC_ERROR"
        const val EXTRA_ACTION_TYPE = "com.thanksmister.bitcoin.localtrader.extra.EXTRA_ACTION"
        const val EXTRA_ERROR_CODE = "com.thanksmister.bitcoin.localtrader.extra.EXTRA_ERROR_CODE"
        const val EXTRA_ERROR_MESSAGE = "com.thanksmister.bitcoin.localtrader.extra.EXTRA_ERROR_MESSAGE"
        const val SYNC_CURRENCIES = "com.thanksmister.bitcoin.localtrader.sync.SYNC_CURRENCIES"
        const val SYNC_WALLET = "com.thanksmister.bitcoin.localtrader.sync.SYNC_WALLET"
        const val SYNC_METHODS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_METHODS"
        const val SYNC_NOTIFICATIONS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_NOTIFICATIONS"
        const val SYNC_ERROR_CODE = 9
    }
}