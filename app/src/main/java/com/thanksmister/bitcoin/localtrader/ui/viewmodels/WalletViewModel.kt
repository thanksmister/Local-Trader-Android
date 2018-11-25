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
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.persistence.ExchangeRateDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao
import com.thanksmister.bitcoin.localtrader.utils.Calculations
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class WalletViewModel @Inject
constructor(application: Application, private val walletDao: WalletDao,
            private val exchangeDao: ExchangeRateDao, private val preferences: Preferences) : BaseViewModel(application) {

    private val qrCodeBitmap = MutableLiveData<Bitmap>()

    fun getBitmap(): LiveData<Bitmap> {
        return qrCodeBitmap
    }

    private fun setBitmap(value: Bitmap) {
        this.qrCodeBitmap.value = value
    }

    init {

    }

    inner class WalletData {
        var bitcoinValue: String? = null
        var address: String? = null
        var bitcoinAmount: String? = null
        var currency: String? = null
        var exchange: String? = null
        var balance: String? = null
        var rate: String? = null
        var transactions = emptyList<Transaction>()
    }

    inner class NetworkData {
        var exchangeRate: ExchangeRate? = null
        var wallet: Wallet? = null
    }

    fun getWalletData(): Flowable<WalletData> {
       return Flowable.combineLatest(getWallet(), getExchange(),
               BiFunction { wallet, exchange  ->
                   val data = WalletData()
                   val currency = preferences.exchangeCurrency
                   val btcValue = Calculations.computedValueOfBitcoin(exchange.rate, wallet.total.balance)
                   val btcAmount = Conversions.formatBitcoinAmount(wallet.total.balance) + " " + getApplication<BaseApplication>().getString(R.string.btc)
                   data.address = wallet.address
                   data.transactions = wallet.transactions
                   data.bitcoinAmount = btcAmount
                   data.bitcoinValue = btcValue
                   data.balance = wallet.total.balance
                   data.currency = currency
                   data.exchange = exchange.name
                   data.rate = exchange.rate
                   data
               })
    }

    private fun getWallet(): Flowable<Wallet> {
        return walletDao.getItems()
                .filter {items -> items.isNotEmpty()}
                .map {items -> items[0]}
    }

    private fun getExchange(): Flowable<ExchangeRate> {
        return exchangeDao.getItems()
                .filter { items -> items.isNotEmpty() }
                .map { items -> items[0] }
    }

    fun fetchNetworkData() {
        Timber.d("fetchNetworkData")
        disposable.add(getNetworkData()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.wallet != null) {
                        insertWallet(it.wallet!!)
                    }
                    if(it.exchangeRate != null) {
                        insertExchange(it.exchangeRate!!)
                    }
                }, { error ->
                    Timber.e("Error: " + error.toString())
                    if(error is NetworkException) {
                        Timber.e("Error getting data ${error.code}")
                        showAlertMessage(error.message)
                    }
                    Timber.e("Error getting data ${error.message}")
                    showAlertMessage(error.message)
                }))
    }

    private fun getNetworkData() : Observable<NetworkData> {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        val exchangeApi = ExchangeApi(preferences)
        val exchangeFetcher = ExchangeFetcher(exchangeApi, preferences)
        return Observable.zip(
                fetcher.wallet,
                exchangeFetcher.getExchangeRate(),
                        BiFunction { wallet, exchange  ->
                            val networkData = NetworkData()
                            networkData.exchangeRate = exchange
                            networkData.wallet = wallet
                            networkData
                        })
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

    private fun insertExchange(items: ExchangeRate) {
        disposable.add(Completable.fromAction {
            exchangeDao.updateItem(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Exchange insert error" + error.message)}))
    }

    private fun insertWallet(item: Wallet) {
        Timber.d("insertWallet")
        disposable.add(Completable.fromAction {
            walletDao.updateItem(item)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Wallet insert error" + error.message) }))
    }

    fun generateAddressBitmap(bitcoinAddress: String) {
        disposable.add(
                generateBitmapObservable(bitcoinAddress)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ bitmap ->
                            if(bitmap != null) {
                                setBitmap(bitmap)
                            }
                        }, { error ->
                            // TODO we want to report theset to crashalytics
                            Timber.e(error.message)
                        }))
    }

    private fun generateBitmapObservable(address: String): Observable<Bitmap> {
        return Observable.create { subscriber ->
            try {
                val bitmap = WalletUtils.encodeAsBitmap(address, getApplication())
                subscriber.onNext(bitmap)
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    interface OnCompleteListener {
        fun onComplete(byteArray: Bitmap?)
    }

    class ByteArrayTask(context: Context, private val onCompleteListener: OnCompleteListener) : AsyncTask<Any, Void, Bitmap?>() {
        private val contextRef: WeakReference<Context> = WeakReference(context)
        override fun doInBackground(vararg params: kotlin.Any): Bitmap? {
            if (isCancelled) {
                return null
            }
            val address = params[0] as String
            return WalletUtils.encodeAsBitmap(address, contextRef.get())
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            if (isCancelled) {
                return
            }
            onCompleteListener.onComplete(result)
        }
    }
}