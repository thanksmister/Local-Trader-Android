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
import android.content.SharedPreferences
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.CurrenciesDao
import com.thanksmister.bitcoin.localtrader.persistence.MethodsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class SearchViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val methodsDao: MethodsDao,
            private val currenciesDao: CurrenciesDao,
            private val sharedPreferences: SharedPreferences,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val advertisements = MutableLiveData<List<Advertisement>>()
    
    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getAdvertisements(): LiveData<List<Advertisement>> {
        return advertisements
    }

    private fun setAdvertisements(advertisements: List<Advertisement>) {
        this.advertisements.value = advertisements
    }

    init {
    }

    fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    fun getCurrencies(): Flowable<List<Currency>> {
        return currenciesDao.getItems()
    }

    fun setSearchCurrency(value: String) {
        SearchUtils.setSearchCurrency(sharedPreferences, value)
    }

    fun getSearchCurrency():String {
        return SearchUtils.getSearchCurrency(sharedPreferences)
    }

    // this is the key
    fun setSearchPaymentMethod(value: String) {
        SearchUtils.setSearchPaymentMethod(sharedPreferences, value)
    }

    fun getSearchPaymentMethod():String {
        return SearchUtils.getSearchPaymentMethod(sharedPreferences)
    }

    fun setSearchTradeType(value: String) {
        SearchUtils.setSearchTradeType(sharedPreferences, value)
    }

    fun getSearchTradeType():String {
        return SearchUtils.getSearchTradeType(sharedPreferences)
    }

    fun setSearchCountryName(value: String) {
        SearchUtils.setSearchCountryName(sharedPreferences, value)
    }

    fun getSearchCountryName():String {
        return SearchUtils.getSearchCountryName(sharedPreferences)
    }

    fun setSearchCountryCode(value: String) {
        SearchUtils.setSearchCountryCode(sharedPreferences, value)
    }

    private fun getSearchCountryCode():String {
        return SearchUtils.getSearchCountryCode(sharedPreferences)
    }

    fun createContact(tradeType: TradeType?, countryCode: String?, onlineProvider: String?,
                      adId: Int, amount: String, name: String, phone: String,
                      email: String, iban: String, bic: String, reference: String,
                      message: String, sortCode: String, billerCode: String,
                      accountNumber: String, bsb: String, ethereumAddress: String) {

        disposable += fetcher.createContact(adId.toString(), tradeType, countryCode, onlineProvider, amount, name, phone, email,
                iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
                .applySchedulers()
                .subscribe ({
                    if(it != null) {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_trade_request_sent))
                    }
                }, {
                    error -> Timber.e("Error createContact ${error.message}")
                    //{"error": {"message": "Payment method not available.", "error_code": 0}}
                    val errorHandler = RetrofitErrorHandler(getApplication())
                    val networkException = errorHandler.create(error)
                    if (networkException.code == 31) {
                        showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_trade_requirements), networkException.code)
                    } else {
                        showNetworkMessage(networkException.message, networkException.code)
                    }
                })
    }

    // TODO we need a way to load next page of search results
    fun getOnlineAdvertisements(tradeType: TradeType) {
        val currency = getSearchCurrency()
        val paymentKey = getSearchPaymentMethod()
        val countryName = getSearchCountryName()
        val countryCode = getSearchCountryCode()
        val url = if (tradeType == TradeType.ONLINE_BUY) {
            "buy-bitcoins-online";
        } else {
            "sell-bitcoins-online";
        }
        if (countryName.toLowerCase() != "any" && paymentKey.toLowerCase() == "all") {
            val countryNameFix = countryName.replace(" ", "-");
            disposable += fetcher.searchOnlineAds(url, countryCode, countryNameFix)
                    .applySchedulers()
                    .subscribe ({
                       setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
                        //{"error": {"message": "Payment method not available.", "error_code": 0}}
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
        } else if (countryName.toLowerCase() != "any" && paymentKey.toLowerCase() != "all") {
            val countryNameFix = countryName.replace(" ", "-");
            disposable += fetcher.searchOnlineAds(url, countryCode, countryNameFix, paymentKey)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
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
        } else if (paymentKey.toLowerCase() == "all" && currency.toLowerCase() != "any") {
            disposable += fetcher.searchOnlineAdsCurrency(url, currency)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
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
        } else if (paymentKey.toLowerCase() != "all" && currency.toLowerCase() == "any") {
            disposable += fetcher.searchOnlineAdsPayment(url, paymentKey)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
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
        } else if (paymentKey.toLowerCase() != "all" && currency.toLowerCase() != "any") {
            disposable += fetcher.searchOnlineAdsCurrencyPayment(url, currency, paymentKey)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
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
        } else {
            disposable += fetcher.searchOnlineAdsAll(url)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error ->
                        Timber.e("Error getOnlineAdvertisements ${error.message}")
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
    }
}