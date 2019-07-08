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
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LifecycleObserver
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
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
    private val progressBarMessage = ProgressBarMessage()
    private val pendingMessage = PendingMessage()

    val disposable = CompositeDisposable()

    fun getShowProgress(): ProgressMessage {
        return progressText
    }

    fun getShowProgressBar(): ProgressBarMessage {
        return progressBarMessage
    }

    fun getPendingMessage(): PendingMessage {
        return pendingMessage
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

    fun showPending(show: Boolean) {
        Timber.d("showPending")
        pendingMessage.value = show
    }

    fun showProgressMessage(message: String?) {
        progressBarMessage.value = message
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

    fun handleSocketTimeoutException() {
        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_search_timeout))
    }

    fun handleNetworkException(error: NetworkException) {
       Timber.e("Network Error Message" + error.message)
       Timber.e("Network Error Code" + error.code)
        when {
            RetrofitErrorHandler.isHttp403Error(error.code) -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_bad_token), ExceptionCodes.AUTHENTICATION_ERROR_CODE)
            RetrofitErrorHandler.isHttp400Error(error.code) -> Unit
            RetrofitErrorHandler.isHttp409Error(error.code) -> Unit
            RetrofitErrorHandler.isHttp404Error(error.code) -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_network_retry), ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE)
            RetrofitErrorHandler.isHttp503Error(error.code) -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_network_retry), ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE)
            RetrofitErrorHandler.isHttp405Error(error.code) -> Unit
            ExceptionCodes.INVALID_GRANT == error.code -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_network_retry), ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE)
            ExceptionCodes.INSUFFICIENT_BALANCE == error.code -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_not_enough_balance), ExceptionCodes.INSUFFICIENT_BALANCE)
            else -> showNetworkMessage(error.message, error.code)
        }
    }
}