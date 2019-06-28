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

package com.thanksmister.bitcoin.localtrader.network.api.fetchers

import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.Parser

import io.reactivex.Observable
import timber.log.Timber

class ExchangeFetcher(private val networkApi: ExchangeApi, private val preferences: Preferences) {

    fun getExchangeRate(): Observable<ExchangeRate>  {
        val currency = preferences.exchangeCurrency
        when (preferences.selectedExchange) {
            COINBASE_EXCHANGE -> {
                return networkApi.getCoinbaseRate(currency)
                        .flatMap {
                            Timber.d(it.toString())
                            val exchangeRate = Parser.parseCoinbaseExchangeRate(it.toString())
                            Observable.just(exchangeRate)
                        }
            }
            BITSTAMP_EXCHANGE -> {
                return networkApi.getBitstampRate(currency)
                        .flatMap {
                            Timber.d(it.toString())
                            val exchangeRate = ExchangeRate()
                            exchangeRate.currency = currency
                            exchangeRate.name = BITSTAMP_EXCHANGE
                            exchangeRate.rate = it.last
                            Observable.just(exchangeRate)
                        }
            }
            BITFINEX_EXCHANGE -> {
                return networkApi.getBitfinexRate(currency)
                        .flatMap {
                            Timber.d(it.toString())
                            val exchangeRate = Parser.parseBitfinexExchangeRate(it.toString())
                            Observable.just(exchangeRate)
                        }
            }
            BITCOINAVERAGE_EXCHANGE -> {
                return networkApi.getBitcoinAverageRate()
                        .flatMap {
                            Timber.d(it.toString())
                            val exchangeRate = Parser.parseBitcoinAverageExchangeRate(BITCOINAVERAGE_EXCHANGE, currency, it.toString());
                            Observable.just(exchangeRate)
                        }
            }
        }
        return Observable.just(null)
    }

    companion object {
        val USD = "USD"
        val COINBASE_EXCHANGE = "Coinbase"
        val BITSTAMP_EXCHANGE = "Bitstamp"
        val BITFINEX_EXCHANGE = "Bitfinex"
        val BITCOINAVERAGE_EXCHANGE = "BitcoinAverage"
    }
}