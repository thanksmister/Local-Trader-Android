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

package com.thanksmister.bitcoin.localtrader.network.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.thanksmister.bitcoin.localtrader.network.api.adapters.DataTypeAdapterFactory
import com.thanksmister.bitcoin.localtrader.network.api.model.Bitstamp
import com.thanksmister.bitcoin.localtrader.network.services.BitcoinAverageService
import com.thanksmister.bitcoin.localtrader.network.services.BitfinexService
import com.thanksmister.bitcoin.localtrader.network.services.BitstampService
import com.thanksmister.bitcoin.localtrader.network.services.CoinbaseService
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class ExchangeApi(httpClient: OkHttpClient, preferences: Preferences) {

    private var coinbaseService: CoinbaseService? = null
    private var bitstampService: BitstampService? = null
    private var bitfinexService: BitfinexService? = null
    private var bitcoinAverageService: BitcoinAverageService? = null

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val gson = GsonBuilder()
                .registerTypeAdapterFactory(DataTypeAdapterFactory())
                .create()

        var retrofit = Retrofit.Builder()
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(COINBASE_ENDPOINT)
                .build()

        coinbaseService = retrofit.create(CoinbaseService::class.java)

        retrofit = Retrofit.Builder()
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(BITSTAMP_API_ENDPOINT)
                .build()

        bitstampService = retrofit.create(BitstampService::class.java)

        retrofit = Retrofit.Builder()
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(BITFINEX_API_ENDPOINT)
                .build()

        bitfinexService = retrofit.create(BitfinexService::class.java)

        retrofit = Retrofit.Builder()
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(preferences.getServiceEndpoint())
                .build()

        bitcoinAverageService = retrofit.create(BitcoinAverageService::class.java)
    }

    fun getBitcoinAverageRate(): Observable<JsonElement> {
        return bitcoinAverageService!!.ticker()
    }

    fun getBitfinexRate(currency: String): Observable<JsonElement> {
        return bitfinexService!!.ticker("tBTC" + currency.toUpperCase())
    }

    fun getBitstampRate(currency: String): Observable<Bitstamp> {
        return bitstampService!!.ticker("btc" + currency.toLowerCase())
    }

    fun getCoinbaseRate(currency: String): Observable<JsonElement>  {
        return coinbaseService!!.spotPrice(currency)
    }

    /*fun clearExchangeExpireTime() {
        synchronized(this) {
            val editor = sharedPreferences.edit()
            editor.remove(PREFS_EXCHANGE_EXPIRE_TIME).apply()
        }
    }

    private fun setExchangeExpireTime() {
        synchronized(this) {
            val expire = System.currentTimeMillis() + CHECK_EXCHANGE_DATA
            val editor = sharedPreferences.edit()
            editor.putLong(PREFS_EXCHANGE_EXPIRE_TIME, expire)
            editor.apply()
        }
    }

    private fun needToRefreshExchanges(): Boolean {
        synchronized(this) {
            val expiresAt = sharedPreferences.getLong(PREFS_EXCHANGE_EXPIRE_TIME, -1)
            return System.currentTimeMillis() >= expiresAt
        }
    }*/

    companion object {
        const val BITSTAMP_API_ENDPOINT = "https://www.bitstampService.net"
        const val BITFINEX_API_ENDPOINT = "https://api.bitfinexService.com"
        const val COINBASE_ENDPOINT = "https://api.coinbase.com"
        const val PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire"
        const val PREFS_SELECTED_EXCHANGE = "selected_exchange"
        const val PREFS_EXCHANGE_CURRENCY = "exchange_currency"
        const val CHECK_EXCHANGE_DATA = 3 * 60 * 1000// 3 minutes
        const val USD = "USD"
        const val COINBASE_EXCHANGE = "Coinbase"
    }
}