/*
 * Copyright (c) 2018 LocalBuzz
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
import android.location.Address
import android.text.TextUtils
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertMessage
import com.thanksmister.bitcoin.localtrader.events.SnackbarMessage
import com.thanksmister.bitcoin.localtrader.events.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.CoinbaseApi
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.network.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.persistence.*
import com.thanksmister.bitcoin.localtrader.utils.DateUtils
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

class NotificationsViewModel @Inject
constructor(application: Application, private val dataSource: NotificationDao, private val preferences: Preferences) : AndroidViewModel(application) {

    private val disposable = CompositeDisposable()
    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val snackbarText = SnackbarMessage()
    private val notificationUrl = MutableLiveData<String>()

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getSnackBarMessage(): SnackbarMessage {
        return snackbarText
    }

    fun getNotificationUrl(): LiveData<String> {
        return notificationUrl
    }

    fun setNotificationUrl(value: String) {
        notificationUrl.value = value
    }

    private fun showSnackbarMessage(message: Int) {
        snackbarText.value = message
    }

    private fun showAlertMessage(message: String) {
        alertText.value = message
    }

    private fun showToastMessage(message: String) {
        toastText.value = message
    }

    init {
    }

    public override fun onCleared() {
        //prevents memory leaks by disposing pending observable objects
        if ( !disposable.isDisposed) {
            disposable.clear()
        }
    }

    fun getNotifications(): Flowable<ArrayList<Notification>> {
        return dataSource.getItems()
                .map { items -> ArrayList(items) }
    }

    fun markNotificationRead(notification: Notification) {
        val api = LocalBitcoinsApi(preferences.endPoint()!!)
        val fetcher = LocalBitcoinsFetcher(this.getApplication(), api, preferences)
        disposable.add(fetcher.markNotificationRead(notification.id)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({response ->
                    Timber.d("Notification Read Response: ${response.string()}")
                    updateNotificationRead(notification)
                    if(!TextUtils.isEmpty(notification.url)) {
                        setNotificationUrl(notification.url!!)
                    }
                }, { error -> Timber.e("Error Notifications Read: $error") }))
    }

    fun markNotificationsRead() {
        disposable.add(dataSource.getUnreadItems()
        .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ notifications ->
                    for(notification in notifications) {
                        markNotificationRead(notification)
                    }
                }, { error ->  showAlertMessage(error.message!!) }))
    }

    /**
     * Update the notification read value
     */
    private fun updateNotificationRead(notification: Notification) {
        Completable.fromAction {
            notification.read = true
            dataSource.insertItem(notification)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("onMQTTMessage error" + error.message)})
    }

    companion object {

    }
}