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
import com.thanksmister.bitcoin.localtrader.architecture.AlertMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class DashboardViewModel @Inject
constructor(application: Application, private val advertisementsDao: AdvertisementsDao,  private val contactsDao: ContactsDao,
            private val notificationsDao: NotificationsDao, private val methodsDao: MethodsDao, private val exchangeRateDao: ExchangeRateDao,
            private val userDao: UserDao, private val preferences: Preferences) : AndroidViewModel(application) {

    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val disposable = CompositeDisposable()

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    init {
    }

    public override fun onCleared() {
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
    }

    private fun showAlertMessage(message: String?) {
        alertText.value = message
    }

    private fun showToastMessage(message: String?) {
        toastText.value = message
    }

    fun getDashboardData() {
        //fetchContacts()
        //fetchNotifications()
        //fetchAdvertisements()
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

    fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    fun getAdvertisements(): Flowable<List<Advertisement>> {
        return advertisementsDao.getItems()
                .filter {items -> items.isNotEmpty()}
    }

    fun getContacts(): Flowable<List<Contact>> {
        return contactsDao.getItems()
                .filter {items -> items.isNotEmpty()}
    }

    fun getNotifications(): Flowable<List<Notification>> {
        return notificationsDao.getItems()
                .filter {items -> items.isNotEmpty()}
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
                    if(error is NetworkException) {

                    }
                    showAlertMessage(error.message)
                }))
    }

    private fun fetchContacts() {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.contacts
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertContacts(it)
                }, {
                    error -> Timber.e("Error fetching contacts ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            // TODO we have authentication error
                        }
                    }
                    showAlertMessage(error.message)
                }))
    }

    private fun fetchAdvertisements() {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.advertisements
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertAdvertisements(it)
                }, {
                    error -> Timber.e("Error fetching advertisement ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            // TODO we have authentication error
                        }
                    }
                    showAlertMessage(error.message)
                }))
    }

    private fun fetchNotifications() {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.notifications
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertNotifications(it)
                }, {
                    error -> Timber.e("Error fetching notification ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            // TODO we have authentication error
                        }
                    }
                    showAlertMessage(error.message)
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

    private fun insertNotifications(items: List<Notification>) {
        disposable.add(Completable.fromAction {
            notificationsDao.insertItems(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Notification insert error" + error.message)}))
    }
}