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
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

class LoginViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val authorized = MutableLiveData<Boolean>()

    fun getAuthorized(): LiveData<Boolean> {
        return authorized
    }

    private fun setAuthorized(value: Boolean) {
        this.authorized.value = value
    }

    init {
        setAuthorized(false)
    }

    fun setAuthorizationCode(code: String, endpoint: String) {
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
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
                    when {
                        error.message == "HTTP 404" -> showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_service_unreachable_error))
                        error.message == "HTTP 400" -> showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_authentication))
                        else -> showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_authentication))
                    }
                }))
    }
}

