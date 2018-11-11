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
import com.thanksmister.bitcoin.localtrader.architecture.ProgressMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.persistence.ExchangeRateDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class WalletViewModel @Inject
constructor(application: Application, private val walletDao: WalletDao,
            private val exchangeDao: ExchangeRateDao, private val preferences: Preferences) : AndroidViewModel(application) {

    private val toastText = ToastMessage()
    private val progressText = ProgressMessage()
    private val alertText = AlertMessage()
    private val disposable = CompositeDisposable()

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getShowProgress(): ProgressMessage {
        return progressText
    }

    init {
        progressText.value = false
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

    private fun showProgress(show: Boolean) {
        Timber.d("showProgress")
        progressText.value = show
    }

    private fun showAlertMessage(message: String?) {
        Timber.d("showAlertMessage")
        alertText.value = message
    }

    private fun showToastMessage(message: String?) {
        Timber.d("showToastMessage")
        toastText.value = message
    }

    fun getWallet(): Flowable<Wallet> {
        return walletDao.getItems()
                .filter { items -> items.isNotEmpty() }
                .map { items -> items[0] }
    }

    fun getExchange(): Flowable<ExchangeRate> {
        return exchangeDao.getItems()
                .filter { items -> items.isNotEmpty() }
                .map { items -> items[0] }
    }

    fun fetchWallet() {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.wallet
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    insertAdvertisements(it)
                }, { error ->
                    Timber.e("Error: " + error.toString())
                    if(error is NetworkException) {
                        Timber.e("Error getting wallet ${error.code}")
                        showAlertMessage(error.message)
                    }
                    Timber.e("Error getting wallet ${error.message}")
                    showAlertMessage(error.message)
                }))
    }

    fun sendBitcoin(pinCode: String, address: String, amount: String) {
        showProgress(true)
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.sendPinCodeMoney(pinCode, address, amount)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showProgress(false)
                }, { error ->
                    Timber.e("Error: " + error.toString())
                    if(error is NetworkException) {
                        Timber.e("Error sending money ${error.code}")
                        // TODO test what code we have for specific messqage
                        showAlertMessage(error.message)
                    }
                    Timber.e("Error sending money  ${error.message}")
                    showAlertMessage(error.message)
                }))
    }

    private fun insertAdvertisements(item: Wallet) {
        disposable.add(Completable.fromAction {
            walletDao.updateItem(item)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Wallet insert error" + error.message) }))
    }
}