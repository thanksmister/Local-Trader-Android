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
import timber.log.Timber
import javax.inject.Inject

class NotificationsViewModel @Inject
constructor(application: Application, private val notificationsDao: NotificationsDao, private val preferences: Preferences) : BaseViewModel(application)  {

    init {
    }

    fun getNotifications():Flowable<List<Notification>> {
        return notificationsDao.getItems()
    }

    fun markNotificationRead(notification: Notification) {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable += fetcher.markNotificationRead(notification.notificationId)
                .applySchedulers()
                .subscribe ({
                    Timber.d("Notification update response: ${it}")
                    if(!Parser.containsError(it.toString())) {
                        notification.read = true
                        updateNotification(notification)
                    } else {
                        showToastMessage(getApplication<BaseApplication>().getString(R.string.error_notification))
                    }
                }, {
                    error -> Timber.e("Notification Error $error.message")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                })
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