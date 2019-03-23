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
import android.arch.lifecycle.LifecycleObserver
import com.thanksmister.bitcoin.localtrader.architecture.*
import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.*
import com.thanksmister.bitcoin.localtrader.utils.disposeProper
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

open class BaseViewModel @Inject
constructor(application: Application) : AndroidViewModel(application), LifecycleObserver {

    private val networkMessage = NetworkMessage()
    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val progressText = ProgressMessage()
    val disposable = CompositeDisposable()

    fun getShowProgress(): ProgressMessage {
        return progressText
    }

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getNetworkMessage(): NetworkMessage {
        return networkMessage
    }

    init {
        Timber.d("init")
        progressText.value = false
    }

    public override fun onCleared() {
        Timber.d("onCleared")
        disposable.disposeProper()
    }

    fun showProgress(show: Boolean) {
        Timber.d("showProgress")
        progressText.value = show
    }

    fun showNetworkMessage(message: String?, code: Int) {
        message?.let {
            val messageData = MessageData()
            messageData.code = code
            messageData.message = message
            networkMessage.value = messageData
        }
    }

    fun showAlertMessage(message: String?) {
        alertText.value = message
    }

    fun showToastMessage(message: String?) {
        toastText.value = message
    }
}