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
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class PinCodeViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val pinCodeStatus = MutableLiveData<Boolean>()
    private var fetcher: LocalBitcoinsFetcher? = null

    fun getPinCodeStatus(): LiveData<Boolean> {
        return pinCodeStatus
    }

    private fun setPinCodeStatus(value: Boolean) {
        this.pinCodeStatus.value = value
    }

    init {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
        fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun validatePinCode(pinCode: String) {
        disposable.add(fetcher!!.validatePinCode(pinCode)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    try {
                        val jsonObject = JSONObject(it.toString())
                        val data = jsonObject.getJSONObject("data");
                        val valid = (data.getString("pincode_ok") == "true");
                        setPinCodeStatus(valid)
                    } catch (e:ClassCastException) {
                        if(!BuildConfig.DEBUG) {
                            Crashlytics.log(1, "PinCode", "Error parsing json object on validate pin code")
                            Crashlytics.logException(Exception("Error parsing json object on validate pin code ${e.message}"))
                        }
                    }
                }, {
                    error -> Timber.e("Pin Code Error" + error.message)
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_contact_action))
                    }
                }))
    }
}