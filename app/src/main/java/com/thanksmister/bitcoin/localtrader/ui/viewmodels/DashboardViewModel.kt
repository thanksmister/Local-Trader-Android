/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.databinding.ActivityMainBinding
import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.*
import com.thanksmister.bitcoin.localtrader.utils.Parser
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.applySchedulersIo
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.HashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DashboardViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val advertisementsDao: AdvertisementsDao,
            private val contactsDao: ContactsDao,
            private val notificationsDao: NotificationsDao,
            private val exchangeRateDao: ExchangeRateDao,
            private val userDao: UserDao,
            private val preferences: Preferences) : BaseViewModel(application) {

    private var syncMap = HashMap<String, Boolean>()

    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    private val exchangeFetcher: ExchangeFetcher by lazy {
        val api = ExchangeApi(okHttpClient, preferences)
        ExchangeFetcher(api, preferences)
    }

    private val syncing = MutableLiveData<String>()

    fun getSyncing(): LiveData<String> {
        return syncing
    }

    private fun setSyncing(value: String) {
        this.syncing.value = value
    }

    init {
        Timber.d("init")
        setSyncing(SYNC_IDLE)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        super.onCleared()
        resetSyncing()
    }

    fun getDashboardData() {
        Timber.d("getDashboardData")
        resetSyncing()
        updateSyncMap(SYNC_CONTACTS, true)
        updateSyncMap(SYNC_ADVERTISEMENTS, true)
        updateSyncMap(SYNC_NOTIFICATIONS, true)
        fetchContacts()
        fetchExchange()
    }

    fun getUser(): Maybe<User> {
        return userDao.getItems()
                .filter {items -> items.isNotEmpty()}
                .map { items -> items[0] }
    }

    fun getExchange(): Flowable<ExchangeRate> {
        return exchangeRateDao.getItems()
                .filter {items -> items.isNotEmpty()}
                .map { items -> items[0] }
    }

    private fun getUnreadNotifications():Flowable<List<Notification>> {
        return notificationsDao.getItemUnreadItems(false)
    }

    fun markNotificationsRead() {
        disposable += getUnreadNotifications()
                .applySchedulersIo()
                .subscribe ({notificationList ->
                    Timber.d("Notifications unread ${notificationList.size}")
                    for(notification in notificationList) {
                        notification.notificationId?.let {
                            fetcher.markNotificationRead(it)
                                    .applySchedulers()
                                    .subscribe ({
                                        Timber.d("Notification update response: ${it.toString()}")
                                        if(!Parser.containsError(it.toString())) {
                                            notification.read = true
                                            updateNotification(notification)
                                        }
                                    }, { error ->
                                        handleError(error)
                                    })
                        }
                    }
                }, {
                    error -> Timber.e("Notification Error $error.message")
                    handleError(error)
                })
    }

    // TODO make this extension
    private fun handleError(error: Throwable) {
        when (error) {
            is HttpException,
            is UnknownHostException ->  {
                val errorHandler = RetrofitErrorHandler(getApplication())
                val networkException = errorHandler.create(error)
                handleNetworkException(networkException)
            }
            is NetworkException -> handleNetworkException(error)
            is SocketTimeoutException -> Unit
            else -> {
                showAlertMessage(error.message)
            }
        }
    }

    private fun fetchExchange() {
        Timber.d("fetchExchange")
        disposable += exchangeFetcher.getExchangeRate()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertExchange(it)
                }, {
                    error -> Timber.e("Error fetching exchange ${error.message}")
                    handleError(error)
                })
    }

    private fun fetchContacts() {
        Timber.d("fetchContacts")
        disposable += fetcher.contacts()
                .applySchedulers()
                .subscribe ({
                    updateSyncMap(SYNC_CONTACTS, false)
                    insertContacts(it)
                    fetchNotifications()
                }, { error ->
                    handleError(error)
                    setSyncing(SYNC_ERROR)
                })
    }

    private fun fetchAdvertisements() {
        Timber.d("fetchAdvertisements")
        disposable += fetcher.advertisements()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertAdvertisements(it)
                    updateSyncMap(SYNC_ADVERTISEMENTS, false)
                }, { error ->
                    handleError(error)
                    setSyncing(SYNC_ERROR)
                })
    }

    private fun fetchNotifications() {
        Timber.d("fetchNotifications")
        disposable += fetcher.notifications()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    if(it != null) {
                        replaceNotifications(it)
                        updateSyncMap(SYNC_NOTIFICATIONS, false)
                        fetchAdvertisements()
                    }
                }, { error ->
                    handleError(error)
                    setSyncing(SYNC_ERROR)
                })
    }

    private fun updateNotification(notification: Notification) {
        disposable += Completable.fromAction {
            notificationsDao.updateItem(notification)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification update error" + error.message)})
    }

    private fun insertExchange(items: ExchangeRate) {
        disposable += Completable.fromAction {
            exchangeRateDao.updateItem(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Exchange insert error" + error.message)})
    }

    private fun insertContacts(items: List<Contact>) {
        disposable += Completable.fromAction {
            contactsDao.insertItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Contacts insert error" + error.message)})
    }

    private fun insertAdvertisements(items: List<Advertisement>) {
        disposable += Completable.fromAction {
            advertisementsDao.insertItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Advertisement insert error" + error.message)})
    }

    private fun replaceNotifications(items: List<Notification>) {
        disposable.add(Completable.fromAction {
            notificationsDao.replaceItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification insert error" + error.message)}))
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
            setSyncing(SplashViewModel.SYNC_COMPLETE)
        } else {
            setSyncing(SplashViewModel.SYNC_STARTED)
        }
    }

    /**
     * Prints the sync map for debugging
     */
    private fun printSyncMap() {
        for (o in syncMap.entries) {
            val pair = o as Map.Entry<String, Boolean>
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

    private fun resetSyncing() {
        syncMap = HashMap()
    }

    companion object {
        const val SYNC_CONTACTS = "SYNC_CONTACTS"
        const val SYNC_NOTIFICATIONS = "SYNC_NOTIFICATIONS"
        const val SYNC_ADVERTISEMENTS = "SYNC_ADVERTISEMENTS"
        const val SYNC_EXCHANGES = "SYNC_EXCHANGES"
        const val SYNC_IDLE = "SYNC_IDLE"
        const val SYNC_STARTED = "SYNC_STARTED"
        const val SYNC_COMPLETE = "SYNC_COMPLETE"
        const val SYNC_ERROR = "SYNC_ERROR"
    }
}