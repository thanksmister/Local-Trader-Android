/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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