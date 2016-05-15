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

import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToExchange;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToExchangeCurrencyList;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import timber.log.Timber;

@Singleton
public class ExchangeService
{
    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_SELECTED_EXCHANGE = "selected_exchange";
    public static final String PREFS_EXCHANGE_CURRENCY = "exchange_currency";

    public static final int CHECK_EXCHANGE_DATA = 2 * 60 * 1000;// 5 minutes
   
    public static final String USD = "USD";
    public static final String EUR = "EUR";
    public static final String EXCHANGE = "Bitstamp";
    
    private final BitcoinAverage bitcoinAverage;
    private final SharedPreferences sharedPreferences;
    
    @Inject
    public ExchangeService(SharedPreferences sharedPreferences, BitcoinAverage bitcoinAverage)
    {
        this.bitcoinAverage = bitcoinAverage;
        this.sharedPreferences = sharedPreferences;
    }

    public void setSelectedExchange(String name)
    {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, EXCHANGE);
        preference.set(name);
    }

    public String getSelectedExchangeName()
    {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, EXCHANGE);
        Timber.d("Selected Name: " + preference.get());
        if(preference.get().equals("")) return EXCHANGE;
        return preference.get();
    }

    public void setExchangeCurrency(String currency)
    {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, USD);
        preference.set(currency);
    }

    public String getExchangeCurrency()
    {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, USD);
        if(preference.get().equals("")) return USD;
        return preference.get();
    }

    public Observable<List<ExchangeCurrency>> getGlobalTickers()
    {
        return bitcoinAverage.globalTickers()
                .map(new ResponseToExchangeCurrencyList());
    }

    public Observable<List<ExchangeCurrency>> getMarketTickers()
    {
        return bitcoinAverage.marketTickers()
                .map(new ResponseToExchangeCurrencyList());
    }

    public Observable<Exchange> getMarket(boolean force)
    {
       /* if (!needToRefreshExchanges() && !force) {
            return Observable.empty();
        }*/

        String currency = getExchangeCurrency();
        
        Timber.d("WalletFragment Currency: " + currency);
        
        return bitcoinAverage.globalCurrency(currency)
                .map(new ResponseToExchange());
                /*.doOnNext(new Action1<Exchange>()
                {
                    @Override
                    public void call(Exchange exchange)
                    {
                        setExchangeExpireTime();
                    }
                });*/
    }
    
    /*public Observable<List<Exchange>> getExchangesObservable(boolean force)
    {
        if (!needToRefreshExchanges() && !force) {
            return Observable.empty();
        }
        
        String currency = getExchangeCurrency();
        return bitcoinAverage.exchanges(currency)
                .map(new ResponseToExchangeList())
                .doOnNext(new Action1<List<Exchange>>()
                {
                    @Override
                    public void call(List<Exchange> exchanges)
                    {
                        setExchangeExpireTime();
                    }
                });
    }*/

    private class ExchangeNameComparator implements Comparator<Exchange>
    {
        @Override
        public int compare(Exchange o1, Exchange o2) {
            return o1.getDisplay_name().toLowerCase().compareTo(o2.getDisplay_name().toLowerCase());
        }
    }
    
    private void setExchangeExpireTime()
    {
        synchronized (this) {
            long expire = System.currentTimeMillis() + CHECK_EXCHANGE_DATA; // 1 hours
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(PREFS_EXCHANGE_EXPIRE_TIME, expire);
            editor.apply();
        }
    }
    
    private boolean needToRefreshExchanges()
    {
        synchronized (this) {
            long expiresAt = sharedPreferences.getLong(PREFS_EXCHANGE_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
