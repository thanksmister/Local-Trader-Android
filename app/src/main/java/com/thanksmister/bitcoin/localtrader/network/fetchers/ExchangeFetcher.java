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

package com.thanksmister.bitcoin.localtrader.network.fetchers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.network.ApiErrorHandler;
import com.thanksmister.bitcoin.localtrader.network.AuthenticationException;
import com.thanksmister.bitcoin.localtrader.network.CoinbaseApi;
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.network.api.model.OauthResponse;
import com.thanksmister.bitcoin.localtrader.persistence.Notification;
import com.thanksmister.bitcoin.localtrader.persistence.Preferences;
import com.thanksmister.bitcoin.localtrader.persistence.Rate;
import com.thanksmister.bitcoin.localtrader.persistence.User;
import com.thanksmister.bitcoin.localtrader.persistence.Wallet;
import com.thanksmister.bitcoin.localtrader.utils.Parser;

import org.json.JSONObject;

import java.util.List;
import java.util.TreeMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.persistence.Preferences.COINBASE_EXCHANGE;

public class ExchangeFetcher {

    private final CoinbaseApi networkApi;
    private final Preferences preferences;
    private final Context context;

    public ExchangeFetcher(@NonNull Context context, @NonNull CoinbaseApi networkApi, @NonNull Preferences preferences) {
        this.networkApi = networkApi;
        this.preferences = preferences;
        this.context = context;
    }

    public Observable<Rate> getSpotPrice() {
        //if(needToRefreshExchanges()) {
        String currency = preferences.getExchangeCurrency();
        if(TextUtils.isEmpty(currency)) currency = "USD";
        if (COINBASE_EXCHANGE.equals(preferences.getSelectedExchange())) {
            return networkApi.getSpotPrice(currency)
                    .doOnNext(new Consumer<ResponseBody>() {
                        @Override
                        public void accept(ResponseBody response) throws Exception {
                            preferences.setExchangeExpireTime();
                        }
                    }).flatMap(new Function<ResponseBody, ObservableSource<Rate>>() {
                        @Override
                        public ObservableSource<Rate> apply(ResponseBody response) throws Exception {
                            Rate exchangeRate = Parser.parseCoinbaseExchangeRate(response.string());
                            if(exchangeRate != null) {
                                return Observable.just(exchangeRate);
                            } else {
                                return Observable.empty();
                            }
                        }
                    });
        } /*else if (preferences.getSelectedExchange().equals(BITSTAMP_EXCHANGE)) {
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
        } else if (getSelectedExchange().equals(BITFINEX_EXCHANGE)) {
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
                            exchangeRate.setDisplayName(BITFINEX_EXCHANGE);
                            exchangeRate.setCurrency(currency);
                            return Observable.just(exchangeRate);
                        }
                    });
        } else if (getSelectedExchange().equals(BITCOINAVERAGE_EXCHANGE)) {
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
        } else {
            return Observable.just(null);
        }*/
        //}
        return Observable.empty();
    }
}