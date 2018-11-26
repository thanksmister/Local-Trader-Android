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
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.AdvertisementsDao
import com.thanksmister.bitcoin.localtrader.persistence.ContactsDao
import com.thanksmister.bitcoin.localtrader.persistence.NotificationsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.Parser
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PinCodeViewModel @Inject
constructor(application: Application, private val preferences: Preferences) : BaseViewModel(application) {

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
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun validatePinCode(pinCode: String) {
        disposable.add(fetcher!!.validatePinCode(pinCode)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    Timber.d(it.asString)
                    val jsonObject = JSONObject(it.asString)
                    val data = jsonObject.getJSONObject("data");
                    val valid = (data.getString("pincode_ok") == "true");
                    setPinCodeStatus(valid)
                }, {
                    error -> Timber.e("Pin Code Error" + error.message)
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_contact_action))
                    }
                }))


        /*disposable.add(fetcher!!.contactAction(contactId, pinCode, action)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    Timber.d(it.asString)
                    if (action == ContactAction.RELEASE || action == ContactAction.CANCEL) {
                        contactsDao.deleteItem(contactId)
                        if (action == ContactAction.RELEASE) {
                            showToastMessage(getApplication<BaseApplication>().getString(R.string.trade_released_toast_text));
                        } else if (action == ContactAction.CANCEL) {
                            showToastMessage(getApplication<BaseApplication>().getString(R.string.trade_canceled_toast_text));
                        }
                        setContactDeleted(true)
                    } else {
                        fetchContact(contactId) // refresh contact
                    }
                }, {
                    error -> Timber.e("Contact Action Error" + error.message)
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_contact_action))
                    }
                }))*/
    }
}