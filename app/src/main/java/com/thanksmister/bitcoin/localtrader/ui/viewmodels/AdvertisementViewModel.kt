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
import com.thanksmister.bitcoin.localtrader.events.AlertMessage
import com.thanksmister.bitcoin.localtrader.events.SnackbarMessage
import com.thanksmister.bitcoin.localtrader.events.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.persistence.Notification
import com.thanksmister.bitcoin.localtrader.persistence.NotificationDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class AdvertisementViewModel @Inject
constructor(application: Application,
            private val dataSource: NotificationDao,
            private val preferences: Preferences) : AndroidViewModel(application) {

    private val disposable = CompositeDisposable()
    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val snackbarText = SnackbarMessage()

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getSnackBarMessage(): SnackbarMessage {
        return snackbarText
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
        if ( !disposable.isDisposed) {
            disposable.clear()
        }
    }

    fun getNotification(id: String): Flowable<Notification> {
        return dataSource.getItemById(id)
    }

    fun markNotificationRead(id: String) {
        val api = LocalBitcoinsApi(preferences.endPoint())
        val fetcher = LocalBitcoinsFetcher(this.getApplication(), api, preferences)
        disposable.add(fetcher.markNotificationRead(id)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({response ->
                    Timber.d("Error Notification Read: ${response.string()}")
                }, { error ->
                    showAlertMessage(error.message!!)
                }))
    }
}