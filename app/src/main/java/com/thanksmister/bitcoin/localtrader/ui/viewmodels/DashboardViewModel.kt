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
import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class DashboardViewModel @Inject
constructor(application: Application, private val advertisementsDao: AdvertisementsDao,  private val contactsDao: ContactsDao,
            private val notificationsDao: NotificationsDao, private val methodsDao: MethodsDao, private val exchangeRateDao: ExchangeRateDao,
            private val userDao: UserDao, private val preferences: Preferences) : BaseViewModel(application) {

    private var fetcher: LocalBitcoinsFetcher? = null

    init {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getDashboardData() {
        fetchContacts()
        fetchNotifications()
        fetchAdvertisements()
        fetchExchange()
    }

    fun getUser(): Flowable<User> {
        return userDao.getItems()
                .filter {items -> items.isNotEmpty()}
                .map { items -> items[0] }
    }

    fun getExchange(): Flowable<ExchangeRate> {
        return exchangeRateDao.getItems()
                .filter {items -> items.isNotEmpty()}
                .map { items -> items[0] }
    }

    private fun fetchExchange() {
        Timber.d("fetchExchange")
        val api = ExchangeApi(preferences)
        val fetcher = ExchangeFetcher(api, preferences)
        disposable.add(fetcher.getExchangeRate()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertExchange(it)
                }, {
                    error -> Timber.e("Error fetching exchange ${error.message}")
                    showAlertMessage(error.message)
                }))
    }

    private fun fetchContacts() {
        disposable.add(fetcher!!.contacts
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertContacts(it)
                }, {
                    error -> Timber.e("Error fetching contacts ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                }))
    }

    private fun fetchAdvertisements() {
        disposable.add(fetcher!!.advertisements
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertAdvertisements(it)
                }, {
                    error -> Timber.e("Error fetching advertisement ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                }))
    }

    private fun fetchNotifications() {
        disposable.add(fetcher!!.notifications
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    if(it != null) {
                        Timber.d("Notifications: ${it}")
                        replaceNotifications(it)
                    }
                }, {
                    error -> Timber.e("Error fetching notification ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                }))
    }

    private fun insertExchange(items: ExchangeRate) {
        disposable.add(Completable.fromAction {
            exchangeRateDao.updateItem(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Exchange insert error" + error.message)}))
    }

    private fun insertContacts(items: List<Contact>) {
        disposable.add(Completable.fromAction {
            contactsDao.insertItems(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Contacts insert error" + error.message)}))
    }

    private fun insertAdvertisements(items: List<Advertisement>) {
        disposable.add(Completable.fromAction {
            advertisementsDao.insertItems(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Advertisement insert error" + error.message)}))
    }

    private fun replaceNotifications(items: List<Notification>) {
        disposable.add(Completable.fromAction {
            notificationsDao.replaceItems(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Notification insert error" + error.message)}))
    }
}