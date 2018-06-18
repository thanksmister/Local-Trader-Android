/*
 * Copyright (c) 2018 ThanksMister
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
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertMessage
import com.thanksmister.bitcoin.localtrader.events.SnackbarMessage
import com.thanksmister.bitcoin.localtrader.events.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.CoinbaseApi
import com.thanksmister.bitcoin.localtrader.network.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.Rate
import com.thanksmister.bitcoin.localtrader.persistence.RateDao
import com.thanksmister.bitcoin.localtrader.utils.DateUtils
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

class MainViewModel @Inject
constructor(application: Application, private val dataSource: RateDao, private val preferences: Preferences) : AndroidViewModel(application) {

    private val disposable = CompositeDisposable()
    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val snackbarText = SnackbarMessage()

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getSnackBarMessage(): SnackbarMessage {
        return snackbarText
    }

    private fun showSnackbarMessage(message: Int) {
        snackbarText.value = message
    }

    private fun showAlertMessage(message: String) {
        Timber.d("alert message: " + message)
        alertText.value = message
    }

    private fun showToastMessage(message: String) {
        toastText.value = message
    }

    init {
    }

    public override fun onCleared() {
        //prevents memory leaks by disposing pending observable objects
        if ( !disposable.isDisposed) {
            disposable.clear()
        }
    }

    fun getExchangeRate(): Flowable<Rate> {
        return dataSource.getItems()
                .filter {items -> items.isNotEmpty()}
                .map {items -> items[items.size - 1]}
    }

    private fun insertExchangePrice(rate: Rate) {
        disposable.add(Completable.fromAction {
            val createdAt = DateUtils.generateCreatedAtDate()
            rate.createdAt = createdAt
            dataSource.insertItem(rate)}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Database error" + error.message)}))
    }

    fun getExchangePrice() {
        val api = CoinbaseApi()
        val fetcher = ExchangeFetcher(this.getApplication(), api, preferences)
        disposable.add(fetcher.spotPrice
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({rate ->
                    Timber.d("username: " + rate.displayName)
                    insertExchangePrice(rate)
                }, { error ->
                    Timber.e("Error message: " + error.message)
                    val errorMessage = getApplication<BaseApplication>().getString(R.string.error_update_exchange_rate)
                    if (error is HttpException) {
                        val errorCode = error.code()
                        Timber.e("Error code: " + errorCode)
                        Timber.e("Error response body: " + error.response().body())
                        Timber.e("Error response message: " + error.response().message())
                    }
                    showAlertMessage(errorMessage)
                }))
    }

    companion object {

    }
}