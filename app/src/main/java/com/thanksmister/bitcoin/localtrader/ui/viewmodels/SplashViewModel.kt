/*
 * Copyright (c) 2019 ThanksMister LLC
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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants.ADVANCED_AD_EDITING
import com.thanksmister.bitcoin.localtrader.constants.Constants.DEAD_MAN_SWITCH
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.*
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.*
import javax.inject.Inject

class SplashViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val userDao: UserDao,
            private val methodsDao: MethodsDao,
            private val currenciesDao: CurrenciesDao,
            private val advertisementsDao: AdvertisementsDao,
            private val contactsDao: ContactsDao,
            private val notificationsDao: NotificationsDao,
            private val preferences: Preferences,
            private val sharedPreferences: SharedPreferences,
            private val remoteConfig: FirebaseRemoteConfig) : BaseViewModel(application) {

    private var syncMap = HashMap<String, Boolean>()
    private val syncing = MutableLiveData<String>()

    private val fetcher: LocalBitcoinsFetcher by lazy {
        val api = LocalBitcoinsApi(okHttpClient, preferences.getServiceEndpoint())
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getSyncing(): LiveData<String> {
        return syncing
    }

    private fun setSyncing(value: String) {
        this.syncing.value = value
    }

    /*@OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        setSyncing(SYNC_IDLE)
    }*/

    init {
        setSyncing(SYNC_IDLE)
    }

    fun startSync() {
        Timber.d("startSync")
        resetSyncing()
        fetchRemoteConfigValues()
    }

    private fun getUser(): Single<List<User>> {
        return userDao.getItems()
    }

    private fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    private fun getCurrencies(): Flowable<List<Currency>> {
        return currenciesDao.getItems()
    }

    private fun fetchRemoteConfigValues() {
        Timber.e("fetchRemoteConfigValues")
        updateSyncMap(SYNC_REMOTE_CONFIG, true)
        remoteConfig.setDefaults(R.xml.remoteconfig)
        remoteConfig.fetch()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.e("Firebase remote config fetch success!")
                        remoteConfig.activate()
                        updateSyncMap(SYNC_REMOTE_CONFIG, false)
                        Timber.w("advanced editing: ${remoteConfig.getBoolean(ADVANCED_AD_EDITING)}")
                        val deadMan = remoteConfig.getBoolean(DEAD_MAN_SWITCH)
                        Timber.w("dead man walking: $deadMan")
                        if(!deadMan) {
                            fetchUser()
                            fetchMethods()
                            fetchCurrencies()
                        } else {
                            showAlertMessage("Dead man switch activated, this app has stopped functioning indefinitely.")
                        }
                    } else {
                        Timber.e("Firebase remote config fetch vales failed.")
                        updateSyncMap(SYNC_REMOTE_CONFIG, false)
                        updateSyncMap(SYNC_ERROR, true)
                    }
                }
    }

    private fun fetchMethods() {
        disposable += (getMethods()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ results ->
                    Timber.d("Methods results ${results.size}")
                    if (results == null || results.isEmpty() || needToRefreshMethods()) {
                        Timber.d("fetching methods")
                        updateSyncMap(SYNC_METHODS, true)
                        fetcher.methods()
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
                                    updateSyncMap(SYNC_METHODS, false)
                                    setSyncing(SYNC_ERROR)
                                })
                    } else {
                        updateSyncMap(SYNC_METHODS, false)
                    }
                }, { error ->
                    Timber.e("Error getting methods ${error.message}")
                    updateSyncMap(SYNC_METHODS, false)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun insertMethods(methods: List<Method>) {
        disposable += (Completable.fromAction {
            methodsDao.replaceItem(methods)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Methods insert error ${error.message}") }))
    }

    private fun fetchCurrencies() {
        disposable += (getCurrencies()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ results ->
                    Timber.d("Currency results ${results.size}")
                    if (results == null || results.isEmpty() || needToRefreshCurrency()) {
                        Timber.d("fetching currencies")
                        updateSyncMap(SYNC_CURRENCIES, true)
                        fetcher.currencies()
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
                                    updateSyncMap(SYNC_CURRENCIES, false)
                                    setSyncing(SYNC_ERROR)
                                })
                    } else {
                        updateSyncMap(SYNC_CURRENCIES, false)
                    }
                }, { error ->
                    Timber.e("Error getting methods ${error.message}")
                    updateSyncMap(SYNC_CURRENCIES, false)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun insertCurrencies(currencies: List<Currency>) {
        disposable += (Completable.fromAction {
            currenciesDao.replaceItem(currencies)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Currencies insert error ${error.message}") }))
    }

    private fun fetchUser() {
        disposable += (getUser()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ results ->
                    Timber.d("user $results")
                    if (results.isEmpty() || needToRefreshUser()) {
                        updateSyncMap(SYNC_MYSELF, true)
                        fetcher.myself()
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    insertUser(it)
                                    updateSyncMap(SYNC_MYSELF, false)
                                    setUserExpireTime()
                                }, {
                                    error ->
                                    when (error) {
                                        is HttpException -> {
                                            val errorHandler = RetrofitErrorHandler(getApplication())
                                            val networkException = errorHandler.create(error)
                                            handleNetworkException(networkException)
                                        }
                                        is NetworkException -> handleNetworkException(error)
                                        else -> {
                                            showAlertMessage(error.message)
                                        }
                                    }
                                    updateSyncMap(SYNC_MYSELF, false)
                                    setSyncing(SYNC_ERROR)
                                })
                    } else {
                        updateSyncMap(SYNC_MYSELF, false)
                    }
                }, { error ->
                    Timber.e("Error getting user ${error.message}")
                    updateSyncMap(SYNC_MYSELF, false)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun fetchContacts() {
        Timber.d("fetchContacts")
        updateSyncMap(DashboardViewModel.SYNC_CONTACTS, true)
        disposable += (fetcher.contacts()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    updateSyncMap(DashboardViewModel.SYNC_CONTACTS, false)
                    insertContacts(it)
                }, { error ->
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> {}
                        else -> {
                            showAlertMessage(error.message)
                        }
                    }
                    updateSyncMap(DashboardViewModel.SYNC_CONTACTS, false)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun fetchAdvertisements() {
        updateSyncMap(DashboardViewModel.SYNC_ADVERTISEMENTS, true)
        disposable += (fetcher.advertisements()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertAdvertisements(it)
                    updateSyncMap(DashboardViewModel.SYNC_ADVERTISEMENTS, false)
                }, { error -> Timber.e("Error fetching advertisement ${error.message}")
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> {}
                        else -> {
                            showAlertMessage(error.message)
                        }
                    }
                    updateSyncMap(DashboardViewModel.SYNC_ADVERTISEMENTS, false)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun fetchNotifications() {
        updateSyncMap(DashboardViewModel.SYNC_NOTIFICATIONS, true)
        disposable += (fetcher.notifications()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    if(it != null) {
                        replaceNotifications(it)
                        updateSyncMap(DashboardViewModel.SYNC_NOTIFICATIONS, false)
                    }
                }, { error -> Timber.e("Error fetching notification ${error.message}")
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> {}
                        else -> {
                            showAlertMessage(error.message)
                        }
                    }
                    updateSyncMap(DashboardViewModel.SYNC_NOTIFICATIONS, false)
                    setSyncing(SYNC_ERROR)
                }))
    }

    private fun insertContacts(items: List<Contact>) {
        disposable += (Completable.fromAction {
            contactsDao.insertItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Contacts insert error" + error.message)}))
    }

    private fun insertAdvertisements(items: List<Advertisement>) {
        disposable += (Completable.fromAction {
            advertisementsDao.insertItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Advertisement insert error" + error.message)}))
    }

    private fun replaceNotifications(items: List<Notification>) {
        disposable += (Completable.fromAction {
            notificationsDao.replaceItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification insert error" + error.message)}))
    }

    private fun insertUser(user: User) {
        disposable += (Completable.fromAction {
            userDao.updateItem(user)
        }
                .applySchedulers()
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

    private fun needToRefreshUser(): Boolean {
        return System.currentTimeMillis() > sharedPreferences.getLong(PREFS_USER_EXPIRE_TIME, -1);
    }

    private fun setUserExpireTime() {
        val expire = System.currentTimeMillis() + CHECK_USER_DATA; // 1 hours
        sharedPreferences.edit().putLong(PREFS_USER_EXPIRE_TIME, expire).apply()
    }

    private fun resetCurrenciesExpireTime() {
        sharedPreferences.edit().remove(PREFS_CURRENCY_EXPIRE_TIME).apply()
    }

    companion object {
        const val SYNC_MYSELF = "SYNC_MYSELF"
        const val SYNC_CURRENCIES = "SYNC_CURRENCIES"
        const val SYNC_METHODS = "SYNC_METHODS"
        const val SYNC_REMOTE_CONFIG = "SYNC_REMOTE_CONFIG"
        const val SYNC_CONTACTS = "SYNC_CONTACTS"
        const val SYNC_NOTIFICATIONS = "SYNC_NOTIFICATIONS"
        const val SYNC_ADVERTISEMENTS = "SYNC_ADVERTISEMENTS"
        const val SYNC_IDLE = "SYNC_IDLE"
        const val SYNC_STARTED = "SYNC_STARTED"
        const val SYNC_COMPLETE = "SYNC_COMPLETE"
        const val SYNC_ERROR = "SYNC_ERROR"
        const val CHECK_CURRENCY_DATA = 604800000 // 1 week 604800000
        const val CHECK_METHODS_DATA = 604800000 // 1 week 604800000
        const val CHECK_USER_DATA = 21600000 // 6 hours
        const val PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire_time";
        const val PREFS_CURRENCY_EXPIRE_TIME = "pref_currency_expire_time";
        const val PREFS_USER_EXPIRE_TIME = "pref_currency_expire_time";
    }
}