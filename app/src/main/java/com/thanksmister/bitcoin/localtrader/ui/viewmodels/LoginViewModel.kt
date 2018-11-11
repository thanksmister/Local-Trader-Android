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
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.architecture.AlertMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.UserDao
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class LoginViewModel @Inject
constructor(application: Application, private val userDao: UserDao, private val preferences: Preferences) : AndroidViewModel(application) {

    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val authorized = MutableLiveData<Boolean>()
    private val disposable = CompositeDisposable()

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getAuthorized(): LiveData<Boolean> {
        return authorized
    }

    private fun setAuthorized(value: Boolean) {
        this.authorized.value = value
    }

    init {
        setAuthorized(false)
    }

    public override fun onCleared() {
        Timber.d("onCleared")
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
        Timber.d("showAlertMessage")
        alertText.value = message
    }

    private fun showToastMessage(message: String?) {
        Timber.d("showToastMessage")
        toastText.value = message
    }

    fun setAuthorizationCode(code: String, endpoint: String) {
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        Timber.d("endPoint: $endpoint")
        disposable.add(fetcher.getAuthorization(code)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    preferences.setAccessToken(it.accessToken)
                    preferences.setRefreshToken(it.refreshToken)
                    preferences.setServiceEndPoint(endpoint)
                    setAuthorized(true)
                }, {
                    error -> Timber.e("Error authentication " + error.message)
                    if(error.message == "HTTP 404") {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_service_unreachable_error))
                    } else if (error.message == "HTTP 400") {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_authentication))
                    } else {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_authentication))
                    }
                }))
    }
}

