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
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.User
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.persistence.CurrenciesDao
import com.thanksmister.bitcoin.localtrader.persistence.MethodsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.UserDao
import com.thanksmister.bitcoin.localtrader.workers.WalletBalanceScheduler
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class SplashViewModel @Inject
constructor(application: Application, private val userDao: UserDao, private val methodsDao: MethodsDao, private val currenciesDao: CurrenciesDao,
            private val preferences: Preferences, private val sharedPreferences: SharedPreferences) : BaseViewModel(application) {

    private var syncMap = HashMap<String, Boolean>()
    private val syncing = MutableLiveData<String>()
    private var fetcher: LocalBitcoinsFetcher? = null

    fun getSyncing(): LiveData<String> {
        return syncing
    }

    private fun setSyncing(value: String) {
        this.syncing.value = value
    }

    init {
        val api = LocalBitcoinsApi(getApplication(), preferences.getServiceEndpoint())
        fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        setSyncing(SYNC_IDLE)
    }

    fun startSync() {
        resetSyncing()
        fetchUser()
        fetchMethods()
        fetchCurrencies()
    }

    fun setupPeriodicWork() {
        WalletBalanceScheduler.refreshWalletBalanceWorkPeriodically()
    }

    private fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    private fun getCurrencies(): Flowable<List<Currency>> {
        return currenciesDao.getItems()
    }

    private fun fetchMethods() {
        //if(needToRefreshMethods()) {
        /*disposable.add(Observable.concatArray(getMethods().toObservable(),
                    fetcher!!.methods)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    insertMethods(it)
                    updateSyncMap(SYNC_METHODS, false)
                    setMethodsExpireTime()
                }, { error ->
                    Timber.e("Error getting methods ${error.message}")
                    if(error is NetworkException) {
                        showNetworkMessage(error.message, error.code)
                    }
                    showNetworkMessage(error.message, ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE)
                    setSyncing(SYNC_ERROR)
                }))*/
        disposable.add(getMethods()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ results ->
                    Timber.d("Methods results ${results.size}")
                    if (results == null || results.isEmpty() || needToRefreshMethods()) {
                        Timber.d("fetching methods")
                        updateSyncMap(SYNC_METHODS, true)
                        fetcher!!.methods
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    insertMethods(it)
                                    updateSyncMap(SYNC_METHODS, false)
                                    setMethodsExpireTime()
                                }, { error ->
                                    Timber.e("Error getting methods ${error.message}")
                                    if (error is NetworkException) {
                                        showNetworkMessage(error.message, error.code)
                                    } else {
                                        showAlertMessage(error.message)
                                    }
                                    setSyncing(SYNC_ERROR)
                                })
                    }
                }, { error ->
                    Timber.e("Error getting methods ${error.message}")
                }))
    }

    private fun insertMethods(methods: List<Method>) {
        disposable.add(Completable.fromAction {
            methodsDao.replaceItem(methods)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Methods insert error ${error.message}") }))
    }

    private fun fetchCurrencies() {
        disposable.add(getCurrencies()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ results ->
                    Timber.d("Currency results ${results.size}")
                    if (results == null || results.isEmpty() || needToRefreshCurrency()) {
                        Timber.d("fetching currencies")
                        updateSyncMap(SYNC_CURRENCIES, true)
                        fetcher!!.currencies
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    insertCurrencies(it)
                                    updateSyncMap(SYNC_CURRENCIES, false)
                                    setCurrencyExpireTime()
                                }, { error ->
                                    Timber.e("Error getting currencies ${error.message}")
                                    if (error is NetworkException) {
                                        showNetworkMessage(error.message, error.code)
                                    } else {
                                        showAlertMessage(error.message)
                                    }
                                    setSyncing(SYNC_ERROR)
                                })
                    }
                }, { error ->
                    Timber.e("Error getting methods ${error.message}")
                }))
    }

    private fun insertCurrencies(currencies: List<Currency>) {
        disposable.add(Completable.fromAction {
            currenciesDao.replaceItem(currencies)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Currencies insert error ${error.message}") }))
    }

    private fun fetchUser() {
        updateSyncMap(SYNC_MYSELF, true)
        disposable.add(fetcher!!.myself
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    insertUser(it)
                    updateSyncMap(SYNC_MYSELF, false)
                }, { error ->
                    Timber.e("Error getting user ${error.message}")
                    if (error is NetworkException) {
                        showNetworkMessage(error.message, error.code)
                    }
                    showNetworkMessage(error.message, ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun insertUser(user: User) {
        disposable.add(Completable.fromAction {
            userDao.updateItem(user)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("User insert error ${error.message}") }))
    }



    /**
     * Keep a map of all syncing calls to update sync status and
     * broadcast when no more syncs running
     */
    private fun updateSyncMap(key: String, value: Boolean) {
        Timber.d("updateSyncMap: $key value: $value")
        syncMap[key] = value
        if (!isSyncing()) {
            resetSyncing()
            setSyncing(SYNC_COMPLETE)
        } else {
            setSyncing(SYNC_STARTED)
        }
    }

    /**
     * Prints the sync map for debugging
     */
    private fun printSyncMap() {
        for (o in syncMap.entries) {
            val pair = o as Map.Entry<*, *>
            Timber.d("Sync Map>>>>>> " + pair.key + " = " + pair.value)
        }
    }

    /**
     * Checks if any active syncs are going one
     */
    private fun isSyncing(): Boolean {
        printSyncMap()
        Timber.d("isSyncing: " + syncMap.containsValue(true))
        return syncMap.containsValue(true)
    }

    /**
     * Resets the syncing map
     */
    private fun resetSyncing() {
        syncMap = HashMap()
    }

    private fun needToRefreshMethods(): Boolean {
        return System.currentTimeMillis() > sharedPreferences.getLong(PREFS_METHODS_EXPIRE_TIME, -1);
    }

    private fun resetMethodsExpireTime() {
        sharedPreferences.edit().remove(PREFS_METHODS_EXPIRE_TIME).apply()
    }

    private fun setMethodsExpireTime() {
        val expire = System.currentTimeMillis() + CHECK_METHODS_DATA; // 1 hours
        sharedPreferences.edit().putLong(PREFS_METHODS_EXPIRE_TIME, expire).apply()
    }

    private fun needToRefreshCurrency(): Boolean {
        return System.currentTimeMillis() > sharedPreferences.getLong(PREFS_CURRENCY_EXPIRE_TIME, -1);
    }

    private fun setCurrencyExpireTime() {
        val expire = System.currentTimeMillis() + CHECK_CURRENCY_DATA; // 1 hours
        sharedPreferences.edit().putLong(PREFS_CURRENCY_EXPIRE_TIME, expire).apply()
    }

    private fun resetCurrenciesExpireTime() {
        sharedPreferences.edit().remove(PREFS_CURRENCY_EXPIRE_TIME).apply()
    }

    companion object {
        const val SYNC_MYSELF = "SYNC_MYSELF"
        const val SYNC_CURRENCIES = "SYNC_CURRENCIES"
        const val SYNC_METHODS = "SYNC_METHODS"

        const val SYNC_IDLE = "SYNC_IDLE"
        const val SYNC_STARTED = "SYNC_STARTED"
        const val SYNC_COMPLETE = "SYNC_COMPLETE"
        const val SYNC_ERROR = "SYNC_ERROR"

        const val CHECK_CURRENCY_DATA = 604800000;// // 1 week 604800000
        const val CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
        const val PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire_time";
        const val PREFS_CURRENCY_EXPIRE_TIME = "pref_currency_expire_time";
    }
}