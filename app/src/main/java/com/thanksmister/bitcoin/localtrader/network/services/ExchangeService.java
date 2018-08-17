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
 *
 */

package com.thanksmister.bitcoin.localtrader.network.services;

import android.content.SharedPreferences;

import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.network.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.network.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.network.api.BitstampExchange;
import com.thanksmister.bitcoin.localtrader.network.api.Coinbase;
import com.thanksmister.bitcoin.localtrader.network.api.model.Bitstamp;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.network.api.transforms.ResponseToBitfinex;
import com.thanksmister.bitcoin.localtrader.network.api.transforms.ResponseToExchange;
import com.thanksmister.bitcoin.localtrader.network.api.transforms.ResponseToExchangeCurrencies;
import com.thanksmister.bitcoin.localtrader.utils.Parser;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.client.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

@Singleton
public class ExchangeService {

    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_SELECTED_EXCHANGE = "selected_exchange";
    public static final String PREFS_EXCHANGE_CURRENCY = "exchange_currency";
    public static final String PREFS_EXCHANGE = "pref_exchange";

    public static final int CHECK_EXCHANGE_DATA = 3 * 60 * 1000;// 3 minutes

    public static final String USD = "USD";

    public static final String COINBASE_EXCHANGE = "Coinbase";
    public static final String BITSTAMP_EXCHANGE = "Bitstamp";
    public static final String BITFINEX_EXCHANGE = "Bitfinex";
    public static final String BITCOINAVERAGE_EXCHANGE = "BitcoinAverage";

    private final Coinbase coinbase;
    private final BitstampExchange bitstamp;
    private final BitfinexExchange bitfinex;
    private final BitcoinAverage bitcoinAverage;
    private final SharedPreferences sharedPreferences;

    @Inject
    public ExchangeService(SharedPreferences sharedPreferences, Coinbase coinbase, BitstampExchange bitstamp, BitfinexExchange bitfinex, BitcoinAverage bitcoinAverage) {
        this.coinbase = coinbase;
        this.bitstamp = bitstamp;
        this.bitfinex = bitfinex;
        this.bitcoinAverage = bitcoinAverage;
        this.sharedPreferences = sharedPreferences;
    }

    public void setSelectedExchange(String name) {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, COINBASE_EXCHANGE);
        preference.set(name);
    }

    public String getSelectedExchange() {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, COINBASE_EXCHANGE);
        return preference.get();
    }

    public void setExchangeCurrency(String currency) {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, USD);
        preference.set(currency);
    }

    public String getExchangeCurrency() {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, USD);
        if (preference.get().equals("")) return USD;
        return preference.get();
    }

    @Deprecated
    public Observable<List<ExchangeCurrency>> getCurrencies() {
        return coinbase.currencies()
                .map(new ResponseToExchangeCurrencies());
    }

    public Observable<ExchangeRate> getSpotPrice() {

        if (!needToRefreshExchanges()){
            return Observable.just(null);
        }

        final String currency = getExchangeCurrency();
        switch (getSelectedExchange()) {
            case COINBASE_EXCHANGE:
                return coinbase.spotPrice(currency)
                        .doOnNext(new Action1<Response>() {
                            @Override
                            public void call(Response response) {
                                setExchangeExpireTime();
                            }
                        })
                        .map(new ResponseToExchange());
            case BITSTAMP_EXCHANGE:
                return bitstamp.ticker("btc" + currency.toLowerCase())
                        .doOnNext(new Action1<Bitstamp>() {
                            @Override
                            public void call(Bitstamp bitstamp) {
                                setExchangeExpireTime();
                            }
                        })
                        .flatMap(new Func1<Bitstamp, Observable<ExchangeRate>>() {
                            @Override
                            public Observable<ExchangeRate> call(Bitstamp bitstamp) {
                                String currency = getExchangeCurrency();
                                ExchangeRate exchangeRate = new ExchangeRate(BITSTAMP_EXCHANGE, bitstamp.last, currency);
                                return Observable.just(exchangeRate);
                            }
                        });
            case BITFINEX_EXCHANGE:
                return bitfinex.ticker("tBTC" + currency.toUpperCase())
                        .doOnNext(new Action1<Response>() {
                            @Override
                            public void call(Response response) {
                                setExchangeExpireTime();
                            }
                        })
                        .map(new ResponseToBitfinex())
                        .flatMap(new Func1<ExchangeRate, Observable<ExchangeRate>>() {
                            @Override
                            public Observable<ExchangeRate> call(ExchangeRate exchangeRate) {
                                exchangeRate.setDisplay_name(BITFINEX_EXCHANGE);
                                exchangeRate.setCurrency(currency);
                                return Observable.just(exchangeRate);
                            }
                        });
            case BITCOINAVERAGE_EXCHANGE:
                return bitcoinAverage.ticker()
                        .doOnNext(new Action1<Response>() {
                            @Override
                            public void call(Response response) {
                                setExchangeExpireTime();
                            }
                        })
                        .flatMap(new Func1<Response, Observable<ExchangeRate>>() {
                            @Override
                            public Observable<ExchangeRate> call(Response response) {
                                ExchangeRate exchangeRate = Parser.parseBitcoinAverageExchangeRate(BITCOINAVERAGE_EXCHANGE, currency, response);
                                return Observable.just(exchangeRate);
                            }
                        });
            default:
                return Observable.just(null);
        }
        //}
    }

    public void clearExchangeExpireTime() {
        synchronized (this) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(PREFS_EXCHANGE_EXPIRE_TIME).apply();
        }
    }

    private void setExchangeExpireTime() {
        synchronized (this) {
            long expire = System.currentTimeMillis() + CHECK_EXCHANGE_DATA;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(PREFS_EXCHANGE_EXPIRE_TIME, expire);
            editor.apply();
        }
    }

    private boolean needToRefreshExchanges() {
        synchronized (this) {
            long expiresAt = sharedPreferences.getLong(PREFS_EXCHANGE_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}