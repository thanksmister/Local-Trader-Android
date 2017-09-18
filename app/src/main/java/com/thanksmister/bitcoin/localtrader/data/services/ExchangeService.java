/*
 * Copyright (c) 2015 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.data.services;

import android.content.SharedPreferences;

import com.thanksmister.bitcoin.localtrader.data.api.Coinbase;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToExchange;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToExchangeCurrencies;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.client.Response;
import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

@Singleton
public class ExchangeService {
    
    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_SELECTED_EXCHANGE = "selected_exchange";
    public static final String PREFS_EXCHANGE_CURRENCY = "exchange_currency";

    public static final int CHECK_EXCHANGE_DATA = 2 * 60 * 1000;// 5 minutes
   
    public static final String USD = "USD";
    public static final String EUR = "EUR";
    public static final String EXCHANGE = "Bitstamp";
    
    private final Coinbase coinbase;
    private final SharedPreferences sharedPreferences;
    
    @Inject
    public ExchangeService(SharedPreferences sharedPreferences, Coinbase coinbase) {
        this.coinbase = coinbase;
        this.sharedPreferences = sharedPreferences;
    }

    public void setSelectedExchange(String name) {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, EXCHANGE);
        preference.set(name);
    }

    public String getSelectedExchangeName() {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, EXCHANGE);
        Timber.d("Selected Name: " + preference.get());
        if(preference.get().equals("")) return EXCHANGE;
        return preference.get();
    }

    public void setExchangeCurrency(String currency) {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, USD);
        preference.set(currency);
    }

    public String getExchangeCurrency() {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, USD);
        if(preference.get().equals("")) return USD;
        return preference.get();
    }
    
    public Observable<List<ExchangeCurrency>> getCurrencies() {
        return coinbase.currencies()
                .map(new ResponseToExchangeCurrencies());
    }
    
    public Observable<ExchangeRate> getSpotPrice() {
        if(needToRefreshExchanges()) {
            String currency = getExchangeCurrency();
            return coinbase.spotPrice(currency)
                    .doOnNext(new Action1<Response>() {
                        @Override
                        public void call(Response response) {
                            setExchangeExpireTime();
                        }
                    })
                    .map(new ResponseToExchange());
        } else {
            return Observable.just(null);
        }
    }

    public void clearExchangeExpireTime() {
        synchronized (this) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(PREFS_EXCHANGE_EXPIRE_TIME).apply();
        }
    }
    
    public void setExchangeExpireTime() {
        synchronized (this) {
            long expire = System.currentTimeMillis() + CHECK_EXCHANGE_DATA; // 1 hours
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(PREFS_EXCHANGE_EXPIRE_TIME, expire);
            editor.apply();
        }
    }
    
    public boolean needToRefreshExchanges() {
        synchronized (this) {
            long expiresAt = sharedPreferences.getLong(PREFS_EXCHANGE_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}