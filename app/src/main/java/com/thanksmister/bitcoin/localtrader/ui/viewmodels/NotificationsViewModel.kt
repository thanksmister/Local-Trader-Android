/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.NotificationsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.Parser
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Completable
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class NotificationsViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val notificationsDao: NotificationsDao,
            private val preferences: Preferences) : BaseViewModel(application)  {

    init {
    }

    fun getNotifications():Flowable<List<Notification>> {
        return notificationsDao.getItems()
    }

    fun markNotificationRead(notification: Notification) {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        notification.notificationId?.let {
            disposable += fetcher.markNotificationRead(it)
                    .applySchedulers()
                    .subscribe ({
                        notification.read = true
                        updateNotification(notification)
                    }, { error ->
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> {}
                            else -> showAlertMessage(error.message)
                        }
                    })
        }
    }

    private fun updateNotification(notification: Notification) {
        disposable.add(Completable.fromAction {
            notificationsDao.updateItem(notification)
            }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification update error" + error.message)}))
    }
}