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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.graphics.Bitmap
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
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.ExchangeRateDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao
import com.thanksmister.bitcoin.localtrader.utils.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class WalletViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val walletDao: WalletDao,
            private val exchangeDao: ExchangeRateDao,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val qrCodeBitmap = MutableLiveData<Bitmap>()

    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    private val exchangeFetcher: ExchangeFetcher by lazy {
        val api = ExchangeApi(okHttpClient, preferences)
        ExchangeFetcher(api, preferences)
    }

    fun getBitmap(): LiveData<Bitmap> {
        return qrCodeBitmap
    }

    private fun setBitmap(value: Bitmap) {
        this.qrCodeBitmap.value = value
    }

    inner class WalletData {
        var bitcoinValue: String? = null
        var address: String? = null
        var bitcoinAmount: String? = null
        var currency: String? = null
        var exchange: String? = null
        var balance: String? = null
        var sendable: String? = null
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
                   data.sendable = wallet.total.sendable
                   data.currency = currency
                   data.exchange = exchange.name
                   data.rate = exchange.rate
                   if(!wallet.address.isNullOrEmpty()) {
                       generateAddressBitmap(wallet.address!!)
                   }
                   data
               })
    }

    private fun getWallet(): Flowable<Wallet> {
        return walletDao.getItems()
                .map {items ->
                    if(items.isNotEmpty()) {
                        items.first()
                    } else {
                        Wallet()
                    }
                }
    }

    private fun getExchange(): Flowable<ExchangeRate> {
        return exchangeDao.getItems()
                /*.filter { items -> items.isNotEmpty() }*/
                .map {items ->
                    if(items.isNotEmpty()) {
                        items.first()
                    } else {
                        ExchangeRate()
                    }
                }
    }

    fun fetchNetworkData() {
        disposable += getNetworkData()
                .applySchedulers()
                .subscribe({
                    if(it.wallet != null) {
                        insertWallet(it.wallet!!)
                    }
                    if(it.exchangeRate != null) {
                        insertExchange(it.exchangeRate!!)
                    }
                    showProgress(false)
                }, { error ->
                    Timber.e("Wallet Error: " + error.toString())
                    showProgress(false)
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> {}
                        else -> showAlertMessage(error.message)
                    }
                })
    }

    private fun getNetworkData() : Observable<NetworkData> {

        return Observable.zip(
                fetcher.wallet(),
                exchangeFetcher.getExchangeRate(),
                        BiFunction { wallet, exchange  ->
                            val networkData = NetworkData()
                            networkData.exchangeRate = exchange
                            networkData.wallet = wallet
                            networkData
                        })
    }

    fun getWalletAddress() {
        disposable += fetcher.walletAddress()
                .applySchedulers()
                .subscribe({
                    if(!it.address.isNullOrEmpty()) {
                        fetchNetworkData()
                    } else {
                        showProgress(false)
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_unknown_error))
                    }
                }, { error ->
                    Timber.e("Error: " + error.toString())
                    showProgress(false)
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(error.message)
                    }
                })
    }

    fun sendBitcoin(pinCode: String, address: String, amount: String) {
        showPending(true)
        disposable += fetcher.sendPinCodeMoney(pinCode, address, amount)
                .applySchedulers()
                .subscribe({
                    showPending(false)
                }, { error ->
                    Timber.e("Error sending money  ${error.message}")
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(error.message)
                    }
                })
    }

    private fun insertExchange(items: ExchangeRate) {
        disposable += Completable.fromAction {
            exchangeDao.updateItem(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Exchange insert error" + error.message)})
    }

    private fun insertWallet(item: Wallet) {
        Timber.d("insertWallet")
        disposable += Completable.fromAction {
            walletDao.updateItem(item)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Wallet insert error" + error.message) })
    }

    private fun deleteWallet() {
        disposable += Completable.fromAction {
            walletDao.deleteAllItems()
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Wallet insert error" + error.message) })

    }

    private fun generateAddressBitmap(bitcoinAddress: String) {
        disposable += generateBitmapObservable(bitcoinAddress)
                        .applySchedulers()
                        .subscribe({ bitmap ->
                            if(bitmap != null) {
                                showProgress(false)
                                setBitmap(bitmap)
                            }
                        }, { error ->
                            showProgress(false)
                            Timber.e(error.message)
                            when (error) {
                                is HttpException -> {
                                    val errorHandler = RetrofitErrorHandler(getApplication())
                                    val networkException = errorHandler.create(error)
                                    handleNetworkException(networkException)
                                }
                                is NetworkException -> handleNetworkException(error)
                                is SocketTimeoutException -> handleSocketTimeoutException()
                                else -> showAlertMessage(error.message)
                            }
                        })
    }

    private fun generateBitmapObservable(address: String): Observable<Bitmap> {
        return Observable.create { subscriber ->
            try {
                val bitmap = WalletUtils.encodeAsBitmap(address, getApplication())
                if(bitmap != null) {
                    subscriber.onNext(bitmap)
                } else {
                    subscriber.onError(Throwable(getApplication<BaseApplication>().getString(R.string.toast_error_qrcode)))
                }

            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }
}