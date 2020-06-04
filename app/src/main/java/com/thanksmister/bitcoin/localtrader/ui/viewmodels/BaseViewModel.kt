/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.architecture.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.utils.disposeProper
import io.reactivex.disposables.CompositeDisposable
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
            ExceptionCodes.CODE_THREE == error.code -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_bad_token), ExceptionCodes.AUTHENTICATION_ERROR_CODE)
            ExceptionCodes.INVALID_GRANT == error.code -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_bad_token), ExceptionCodes.AUTHENTICATION_ERROR_CODE)
            ExceptionCodes.INSUFFICIENT_BALANCE == error.code -> showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_not_enough_balance), ExceptionCodes.INSUFFICIENT_BALANCE)
            else -> showNetworkMessage(error.message, error.code)
        }
    }
}