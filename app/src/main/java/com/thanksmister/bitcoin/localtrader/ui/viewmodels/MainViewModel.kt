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
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import com.thanksmister.bitcoin.localtrader.architecture.AlertMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.User
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.UserDao
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.HashMap
import javax.inject.Inject

class MainViewModel @Inject
constructor(application: Application, private val userDao: UserDao,
            private val preferences: Preferences, private val sharedPreferences: SharedPreferences) : AndroidViewModel(application) {

    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val disposable = CompositeDisposable()
    private var syncMap = HashMap<String, Boolean>()
    private val syncing = MutableLiveData<Boolean>()
    private var fetcher: LocalBitcoinsFetcher? = null

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getSyncing(): LiveData<Boolean> {
        return syncing
    }

    private fun setSyncing(value: Boolean) {
        this.syncing.value = value
    }

    init {
        val api = LocalBitcoinsApi(getApplication(), preferences.getServiceEndpoint())
        fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        setSyncing(true)
    }

    public override fun onCleared() {
        //prevents memory leaks by disposing pending observable objects
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
    }

    private fun showAlertMessage(message: String?) {
        alertText.value = message
    }

    private fun showToastMessage(message: String?) {
        toastText.value = message
    }

    fun startSync() {
        getUserData()
        getMethods()
        getCurrencies()
    }

    private fun getUserData() {
        updateSyncMap(SYNC_MYSELF, true)
        disposable.add(fetcher!!.myself
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    updateSyncMap(SYNC_MYSELF, false)
                    insertUser(it)
                }, {
                    error -> Timber.e("Error getting user" + error.message)
                    updateSyncMap(SYNC_MYSELF, false)
                    showAlertMessage(error.message)
                }))
    }

    private fun insertUser(user: User) {
        disposable.add(Completable.fromAction {
            userDao.updateItem(user) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("User insert error" + error.message)}))
    }

    private fun getMethods() {
        if(needToRefreshMethods()) {
            updateSyncMap(SYNC_METHODS, true)
            disposable.add(fetcher!!.methods
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        updateSyncMap(SYNC_METHODS, false)
                        setMethodsExpireTime()
                    }, { error ->
                        Timber.e("Error getting methods" + error.message)
                        updateSyncMap(SYNC_METHODS, false)
                        showAlertMessage(error.message)
                    }))
        }
    }

    private fun getCurrencies() {
        if(needToRefreshCurrency()) {
            updateSyncMap(SYNC_CURRENCIES, true)
            disposable.add(fetcher!!.myself
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        updateSyncMap(SYNC_CURRENCIES, false)
                        setCurrencyExpireTime()
                    }, { error ->
                        Timber.e("Error getting currencies" + error.message)
                        updateSyncMap(SYNC_CURRENCIES, false)
                        showAlertMessage(error.message)
                    }))
        }
    }

    // TODO we need to setup our workers for background loading data

    /**
     * Keep a map of all syncing calls to update sync status and
     * broadcast when no more syncs running
     */
    private fun updateSyncMap(key: String, value: Boolean) {
        Timber.d("updateSyncMap: $key value: $value")
        syncMap[key] = value
        if (!isSyncing()) {
            resetSyncing()
            setSyncing(false)
        } else {
            setSyncing(true)
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
        const val CHECK_CURRENCY_DATA = 604800000;// // 1 week 604800000
        const val CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
        const val PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire_time";
        const val PREFS_CURRENCY_EXPIRE_TIME = "pref_currency_expire_time";
    }
}