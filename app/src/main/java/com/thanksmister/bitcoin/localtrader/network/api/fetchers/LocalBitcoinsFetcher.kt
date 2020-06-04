/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.fetchers

import android.content.Context
import android.text.TextUtils
import com.google.gson.JsonElement
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.Parser
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber
import java.io.File
import java.net.SocketTimeoutException
import java.util.*

class LocalBitcoinsFetcher(
        private val context: Context,
        private val networkApi: LocalBitcoinsApi,
        private val preferences: Preferences) {

    companion object {
        const val grantType: String = "authorization_code"
    }

    fun myself(): Observable<User> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getMyself(accessToken)
    }

    fun currencies(): Observable<List<Currency>> {
        return networkApi.getCurrencies()
                .flatMap {
                    var currencies: List<Currency>? = Parser.parseCurrencies(it)
                    if (currencies == null) {
                        currencies = ArrayList()
                    }
                    Observable.just(currencies)
                }
                .onErrorReturn { throwable ->
                    Timber.e(throwable.message)
                    emptyList<Currency>()
                }
    }

    fun advertisements(): Observable<List<Advertisement>> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getAdvertisements(accessToken)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getAdvertisements(accessToken) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
                .flatMap { advertisements -> Observable.just(advertisements.getListItems()) }
    }

    fun methods(): Observable<List<Method>> {
        return networkApi.getOnlineProviders()
                .flatMap {
                    var methods: List<Method>? = Parser.parseMethods(it)
                    if (methods == null) methods = ArrayList()
                    Observable.just(methods)
                }
                .onErrorReturn { throwable ->
                    Timber.e(throwable.message)
                    emptyList<Method>()
                }
    }

    fun walletAddress(): Observable<NewAddress> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getWalletAddress(accessToken)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getWalletAddress(accessToken) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun wallet(): Observable<Wallet> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getWallet(accessToken)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getWallet(accessToken) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun walletBalance(): Observable<Wallet> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getWalletBalance(accessToken)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getWalletBalance(accessToken) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun contacts(): Observable<List<Contact>> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getDashboard(accessToken)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getDashboard(accessToken) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
                .flatMap { dashboard -> Observable.just(dashboard.getItems()) }
    }

    fun notifications(): Observable<List<Notification>> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getNotifications(accessToken)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    if (RetrofitErrorHandler.isHttp403Error(networkException.code)) {

                        return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken ->

                                    networkApi.getNotifications(accessToken)
                                }
                    } else if (RetrofitErrorHandler.isHttp400Error(networkException.code)) {

                        return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getNotifications(accessToken) }
                    } else if (ExceptionCodes.CODE_THREE == networkException.code) {

                        return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.getNotifications(accessToken) }
                    } else if (throwable is SocketTimeoutException) {
                        return@Function Observable.error(throwable)
                    }
                    Observable.error(networkException)
                })
                .flatMap { notifications -> Observable.just(notifications.getItems()) }
    }

    fun getAuthorization(code: String): Observable<Authorization> {
        return networkApi.getAuthorization(grantType, code, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
    }

    fun updateAdvertisement(advertisement: Advertisement): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        val city: String?
        if (TextUtils.isEmpty(advertisement.city)) {
            city = advertisement.location
        } else {
            city = advertisement.city
        }
        return networkApi.updateAdvertisement(
                accessToken, advertisement.adId.toString(), advertisement.accountInfo, advertisement.bankName, city, advertisement.countryCode, advertisement.currency,
                advertisement.lat.toString(), advertisement.location, advertisement.lon.toString(), advertisement.maxAmount, advertisement.minAmount,
                advertisement.message, advertisement.priceEquation, advertisement.trustedRequired.toString(), advertisement.smsVerificationRequired.toString(),
                advertisement.trackMaxAmount.toString(), advertisement.visible.toString(), advertisement.requireIdentification.toString(),
                advertisement.requireFeedbackScore, advertisement.requireTradeVolume, advertisement.firstTimeLimitBtc,
                advertisement.phoneNumber)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    if (RetrofitErrorHandler.isHttp403Error(networkException.code)) {

                        return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken ->

                                    networkApi.updateAdvertisement(
                                            accessToken, advertisement.adId.toString(), advertisement.accountInfo, advertisement.bankName, city, advertisement.countryCode, advertisement.currency,
                                            advertisement.lat.toString(), advertisement.location, advertisement.lon.toString(), advertisement.maxAmount, advertisement.minAmount,
                                            advertisement.message, advertisement.priceEquation, advertisement.trustedRequired.toString(), advertisement.smsVerificationRequired.toString(),
                                            advertisement.trackMaxAmount.toString(), advertisement.visible.toString(), advertisement.requireIdentification.toString(),
                                            advertisement.requireFeedbackScore, advertisement.requireTradeVolume, advertisement.firstTimeLimitBtc,
                                            advertisement.phoneNumber)
                                }
                    } else if (RetrofitErrorHandler.isHttp400Error(networkException.code)) {

                        return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken ->
                                    networkApi.updateAdvertisement(
                                            accessToken, advertisement.adId.toString(), advertisement.accountInfo, advertisement.bankName, city, advertisement.countryCode, advertisement.currency,
                                            advertisement.lat.toString(), advertisement.location, advertisement.lon.toString(), advertisement.maxAmount, advertisement.minAmount,
                                            advertisement.message, advertisement.priceEquation, advertisement.trustedRequired.toString(), advertisement.smsVerificationRequired.toString(),
                                            advertisement.trackMaxAmount.toString(), advertisement.visible.toString(), advertisement.requireIdentification.toString(),
                                            advertisement.requireFeedbackScore, advertisement.requireTradeVolume, advertisement.firstTimeLimitBtc,
                                            advertisement.phoneNumber)
                                }
                    } else if (ExceptionCodes.CODE_THREE == networkException.code) {

                        return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken ->
                                    networkApi.updateAdvertisement(
                                            accessToken, advertisement.adId.toString(), advertisement.accountInfo, advertisement.bankName, city, advertisement.countryCode, advertisement.currency,
                                            advertisement.lat.toString(), advertisement.location, advertisement.lon.toString(), advertisement.maxAmount, advertisement.minAmount,
                                            advertisement.message, advertisement.priceEquation, advertisement.trustedRequired.toString(), advertisement.smsVerificationRequired.toString(),
                                            advertisement.trackMaxAmount.toString(), advertisement.visible.toString(), advertisement.requireIdentification.toString(),
                                            advertisement.requireFeedbackScore, advertisement.requireTradeVolume, advertisement.firstTimeLimitBtc,
                                            advertisement.phoneNumber)
                                }
                    } else if (throwable is SocketTimeoutException) {
                        return@Function Observable.error(throwable) // bubble up the exception;
                    }
                    Observable.error(networkException) // bubble up the exception;
                })
    }

    fun deleteAdvertisement(adId: Int): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        return networkApi.deleteAdvertisement(accessToken, adId)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken ->
                                        networkApi.deleteAdvertisement(accessToken, adId)
                                    }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.deleteAdvertisement(accessToken, adId) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.deleteAdvertisement(accessToken, adId) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun getAdvertisement(adId: Int): Observable<List<Advertisement>> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getAdvertisement(accessToken, adId)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken ->

                                        networkApi.getAdvertisement(accessToken, adId)
                                    }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.getAdvertisement(accessToken, adId) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.getAdvertisement(accessToken, adId) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
                .flatMap { advertisements -> Observable.just(advertisements.getListItems()) }
    }

    fun getContact(contactId: Int): Observable<Contact> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getContactInfo(accessToken, contactId)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken ->

                                        networkApi.getContactInfo(accessToken, contactId)
                                    }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.getContactInfo(accessToken, contactId) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.getContactInfo(accessToken, contactId) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun getContactsByType(dashboardType: DashboardType): Observable<List<Contact>> {
        val accessToken = preferences.getAccessToken()
        return networkApi.getDashboard(accessToken, dashboardType.name.toLowerCase())
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken ->

                                        networkApi.getDashboard(accessToken, dashboardType.name.toLowerCase())
                                    }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.getDashboard(accessToken, dashboardType.name.toLowerCase()) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.getDashboard(accessToken, dashboardType.name.toLowerCase()) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                        else -> Observable.error(networkException)
                    }
                })
                .flatMap { dashboard -> Observable.just(dashboard.getItems()) }
    }

    fun sendPinCodeMoney(pinCode: String, address: String, amount: String): Observable<Boolean> {
        val accessToken = preferences.getAccessToken()
        return networkApi.walletSendPin(accessToken, pinCode, address, amount)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken ->

                                        networkApi.walletSendPin(accessToken, pinCode, address, amount)
                                    }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.walletSendPin(accessToken, pinCode, address, amount) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.walletSendPin(accessToken, pinCode, address, amount) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
                .flatMap { jsonElement ->
                    if (Parser.containsError(jsonElement.toString())) {
                        val exception = Parser.parseError(jsonElement.toString())
                        throw Error(exception)
                    }
                    Observable.just(true)
                }
    }

    fun markNotificationRead(notificationId: String): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        return networkApi.markNotificationRead(accessToken, notificationId)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.markNotificationRead(accessToken, notificationId) }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.markNotificationRead(accessToken, notificationId) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.markNotificationRead(accessToken, notificationId) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun getContactMessages(contactId: Int): Observable<List<Message>> {
        val accessToken = preferences.getAccessToken()
        return networkApi.contactMessages(accessToken, contactId)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) -> {

                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken ->

                                        networkApi.contactMessages(accessToken, contactId)
                                    }
                        }
                        RetrofitErrorHandler.isHttp400Error(networkException.code) -> {

                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.contactMessages(accessToken, contactId) }
                        }
                        ExceptionCodes.CODE_THREE == networkException.code -> {

                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.contactMessages(accessToken, contactId) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
                .flatMap { messages -> Observable.just(messages.items) }
    }

    fun searchOnlineAds(type: String, num: String, location: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAds(type, num, location)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun searchOnlineAds(type: String, num: String, location: String, paymentMethod: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAds(type, num, location, paymentMethod)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun searchOnlineAdsCurrency(type: String, currency: String, paymentMethod: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAdsCurrency(type, currency, paymentMethod)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun searchOnlineAdsCurrency(type: String, currency: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAdsCurrency(type, currency)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun searchOnlineAdsPayment(type: String, paymentMethod: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAdsPayment(type, paymentMethod)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun searchOnlineAdsCurrencyPayment(type: String, currency: String, paymentMethod: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAdsCurrencyPayment(type, currency, paymentMethod)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun searchOnlineAdsAll(type: String): Observable<List<Advertisement>> {
        return networkApi.searchOnlineAdsAll(type)
                .flatMap { advertisements -> Observable.just(advertisements.items) }
    }

    fun createContact(adId: String, tradeType: TradeType?, countryCode: String?,
                      onlineProvider: String?, amount: String, name: String,
                      phone: String, email: String, iban: String, bic: String,
                      reference: String, message: String, sortCode: String,
                      billerCode: String, accountNumber: String, bsb: String,
                      ethereumAddress: String): Observable<ContactRequest> {

        val accessToken = preferences.getAccessToken()
        if (tradeType == TradeType.ONLINE_BUY) {
            when (onlineProvider) {
                TradeUtils.NATIONAL_BANK -> return when (countryCode) {
                    "UK" -> networkApi.createContactNationalUK(accessToken, adId, amount, name, sortCode, reference, accountNumber, message)
                    "AU" -> networkApi.createContactNationalAU(accessToken, adId, amount, name, bsb, reference, accountNumber, message)
                    "FI" -> networkApi.createContactNationalFI(accessToken, adId, amount, name, iban, bic, reference, message)
                    else -> networkApi.createContactNational(accessToken, adId, amount, message)
                }
                TradeUtils.VIPPS, TradeUtils.EASYPAISA, TradeUtils.HAL_CASH, TradeUtils.QIWI, TradeUtils.LYDIA, TradeUtils.SWISH -> return networkApi.createContactPhone(accessToken, adId, amount, phone, message)
                TradeUtils.PAYPAL, TradeUtils.NETELLER, TradeUtils.INTERAC, TradeUtils.ALIPAY, TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> return networkApi.createContactEmail(accessToken, adId, amount, email, message)
                TradeUtils.SEPA -> return networkApi.createContactSepa(accessToken, adId, amount, name, iban, bic, reference, message)
                TradeUtils.ALTCOIN_ETH -> return networkApi.createContactEthereumAddress(accessToken, adId, amount, ethereumAddress, message)
                TradeUtils.BPAY -> return networkApi.createContactBPay(accessToken, adId, amount, billerCode, reference, message)
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            when (onlineProvider) {
                TradeUtils.QIWI, TradeUtils.SWISH, TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> return networkApi.createContactPhone(accessToken, adId, amount, phone, message)
            }
        }

        return networkApi.createContact(accessToken, adId, amount, message)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                        RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> {
                            return@Function refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap { accessToken -> networkApi.createContact(accessToken, adId, amount, message) }
                        }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun contactAction(contactId: Int, pinCode: String?, action: ContactAction): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        when (action) {
            ContactAction.RELEASE -> return networkApi.releaseContactPinCode(accessToken, contactId, pinCode!!)
                    .onErrorResumeNext(Function { throwable ->
                        val errorHandler = RetrofitErrorHandler(context)
                        val networkException = errorHandler.create(throwable)
                        when {
                            RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                    RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                            ExceptionCodes.CODE_THREE == networkException.code -> {
                                return@Function refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap { accessToken -> networkApi.releaseContactPinCode(accessToken, contactId, pinCode) }
                            }
                            throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                            else -> Observable.error(networkException)
                        }
                    })
            ContactAction.CANCEL -> return networkApi.contactCancel(accessToken, contactId)
                    .onErrorResumeNext(Function { throwable ->
                        val errorHandler = RetrofitErrorHandler(context)
                        val networkException = errorHandler.create(throwable)
                        when {
                            RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                    RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                            ExceptionCodes.CODE_THREE == networkException.code -> {
                                return@Function refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap { accessToken -> networkApi.contactCancel(accessToken, contactId) }
                            }
                            throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                            else -> Observable.error(networkException)
                        }
                    })
            ContactAction.DISPUTE -> return networkApi.contactDispute(accessToken, contactId)
                    .onErrorResumeNext(Function { throwable ->
                        val errorHandler = RetrofitErrorHandler(context)
                        val networkException = errorHandler.create(throwable)
                        when {
                            RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                    RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                            ExceptionCodes.CODE_THREE == networkException.code -> {
                                return@Function refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap { accessToken -> networkApi.contactDispute(accessToken, contactId) }
                            }
                            throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                            else -> Observable.error(networkException)
                        }
                    })
            ContactAction.PAID -> return networkApi.markAsPaid(accessToken, contactId)
                    .onErrorResumeNext(Function { throwable ->
                        val errorHandler = RetrofitErrorHandler(context)
                        val networkException = errorHandler.create(throwable)
                        when {
                            RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                    RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                            ExceptionCodes.CODE_THREE == networkException.code -> {
                                return@Function refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap { accessToken -> networkApi.markAsPaid(accessToken, contactId) }
                            }
                            throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                            else -> Observable.error(networkException)
                        }
                    })
            ContactAction.FUND -> return networkApi.contactFund(accessToken, contactId)
                    .onErrorResumeNext(Function { throwable ->
                        val errorHandler = RetrofitErrorHandler(context)
                        val networkException = errorHandler.create(throwable)
                        when {
                            RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                    RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                            ExceptionCodes.CODE_THREE == networkException.code -> {
                                return@Function refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap { accessToken -> networkApi.contactFund(accessToken, contactId) }
                            }
                            throwable is SocketTimeoutException -> return@Function Observable.error(throwable)
                            else -> Observable.error(networkException)
                        }
                    })
        }
    }

    fun validatePinCode(pinCode: String): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        return networkApi.checkPinCode(accessToken, pinCode)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.checkPinCode(accessToken, pinCode) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun postMessage(contactId: Int, message: String): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        return networkApi.contactMessagePost(accessToken, contactId, message)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.contactMessagePost(accessToken, contactId, message) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
    }

    fun postMessageWithAttachment(contactId: Int, message: String, file: File, fileName: String): Observable<JsonElement> {
        val accessToken = preferences.getAccessToken()
        val requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val multiPartBody = MultipartBody.Part.createFormData("document", fileName, requestBody)
        val params = LinkedHashMap<String, String>()
        params["msg"] = message
        return networkApi.contactMessagePostWithAttachment(accessToken, contactId, params, multiPartBody)
                .onErrorResumeNext(Function { throwable ->
                    val errorHandler = RetrofitErrorHandler(context)
                    val networkException = errorHandler.create(throwable)
                    when {
                        RetrofitErrorHandler.isHttp403Error(networkException.code) ||
                                RetrofitErrorHandler.isHttp400Error(networkException.code) ||
                        ExceptionCodes.CODE_THREE == networkException.code -> return@Function refreshTokens()
                                .subscribeOn(Schedulers.computation())
                                .flatMap { accessToken -> networkApi.contactMessagePostWithAttachment(accessToken, contactId, params, multiPartBody) }
                        throwable is SocketTimeoutException -> return@Function Observable.error(throwable) // bubble up the exception;
                        // bubble up the exception;
                        else -> Observable.error(networkException)
                    }
                })
    }

    private fun refreshTokens(): Observable<String> {
        Timber.d("accessToken: " + preferences.getAccessToken())
        Timber.d("refreshToken: " + preferences.getRefreshToken())
        return networkApi.refreshToken("refresh_token", preferences.getRefreshToken(), BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
                .flatMap { authorization ->
                    Timber.d("authorization " + authorization)
                    Timber.d("authorization.getAccessToken() " + authorization.accessToken)
                    Timber.d("authorization.getRefreshToken() " + authorization.refreshToken)
                    preferences.setAccessToken(authorization.accessToken)
                    preferences.setRefreshToken(authorization.refreshToken)
                    Observable.just(authorization.accessToken)
                }
    }

    /*@Deprecated
    private <T> Function<Throwable, ? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed) {

        Timber.d("refreshTokenAndRetry");

        return new Function<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> apply(Throwable throwable) throws Exception {
                RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                Timber.d("refreshTokenAndRetry error: " + throwable.getMessage());
                final NetworkException networkException = errorHandler.create(throwable);
                if (RetrofitErrorHandler.Companion.isHttp403Error(networkException.getCode())) {
                    Timber.e("Retrying error code: " + networkException.getCode());
                    return refreshTokens()
                            .subscribeOn(Schedulers.computation())
                            .flatMap(new Function<String, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(String s) throws Exception {
                                    return toBeResumed;
                                }
                            });
                } else if (RetrofitErrorHandler.Companion.isHttp400Error(networkException.getCode())) {
                    Timber.e("Retrying error code: " + networkException.getCode());
                    return refreshTokens()
                            .subscribeOn(Schedulers.computation())
                            .flatMap(new Function<String, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(String s) throws Exception {
                                    return toBeResumed;
                                }
                            });
                } else if (ExceptionCodes.INSTANCE.getCODE_THREE() == networkException.getCode()) {
                    Timber.e("Retrying error code: " + networkException.getCode());
                    return refreshTokens()
                            .subscribeOn(Schedulers.computation())
                            .flatMap(new Function<String, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(String s) throws Exception {
                                    return toBeResumed;
                                }
                            });
                } else if (throwable instanceof SocketTimeoutException) {
                    return Observable.error(throwable); // bubble up the exception;
                }
                return Observable.error(networkException); // bubble up the exception;
            }
        };
    }

    @Deprecated
    private class retryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {
        private final int maxRetries;
        private final int retryDelayMillis;
        private int retryCount;

        retryWithDelay(final int maxRetries, final int retryDelayMillis) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.retryCount = 0;
        }

        @Override
        public Observable<?> apply(final Observable<? extends Throwable> attempts) {
            return attempts
                    .flatMap(new Function<Throwable, Observable<?>>() {
                        @Override
                        public Observable<?> apply(final Throwable throwable) {
                            RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                            Timber.d("refreshTokenAndRetry error: " + throwable.getMessage());
                            final NetworkException networkException = errorHandler.create(throwable);
                            if (RetrofitErrorHandler.Companion.isHttp403Error(networkException.getCode())) {
                                Timber.e("Retrying error code: " + networkException.getCode());
                                return refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap(new Function<String, Observable<?>>() {
                                            @Override
                                            public Observable<?> apply(String s) throws Exception {
                                                if (++retryCount < maxRetries) {
                                                    // When this Observable calls onNext, the original
                                                    // Observable will be retried (i.e. re-subscribed).
                                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                                }
                                                return Observable.error(networkException); // bubble up the exception;
                                            }
                                        });
                            } else if (RetrofitErrorHandler.Companion.isHttp400Error(networkException.getCode())) {
                                Timber.e("Retrying error code: " + networkException.getCode());
                                return refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap(new Function<String, Observable<?>>() {
                                            @Override
                                            public Observable<?> apply(String s) throws Exception {
                                                if (++retryCount < maxRetries) {
                                                    // When this Observable calls onNext, the original
                                                    // Observable will be retried (i.e. re-subscribed).
                                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                                }
                                                return Observable.error(networkException); // bubble up the exception;
                                            }
                                        });
                            } else if (ExceptionCodes.INSTANCE.getCODE_THREE() == networkException.getCode()) {
                                Timber.e("Retrying error code: " + networkException.getCode());
                                return refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap(new Function<String, Observable<?>>() {
                                            @Override
                                            public Observable<?> apply(String s) throws Exception {
                                                if (++retryCount < maxRetries) {
                                                    // When this Observable calls onNext, the original
                                                    // Observable will be retried (i.e. re-subscribed).
                                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                                }
                                                return Observable.error(networkException); // bubble up the exception;
                                            }
                                        });
                            } else if (throwable instanceof SocketTimeoutException) {
                                return Observable.error(throwable); // bubble up the exception;
                            }
                            return Observable.error(networkException); // bubble up the exception;
                        }
                    });
        }
    }*/
}