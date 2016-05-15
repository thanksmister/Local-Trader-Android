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

package com.thanksmister.bitcoin.localtrader.ui.settings;


import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.Injector;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @Inject
    DbManager db;

    @Inject
    ExchangeService exchangeService;
    
    @Inject
    SharedPreferences sharedPreferences;

    private Subscription subscription = Subscriptions.empty();
    private Subscription currencySubscription = Subscriptions.empty();
    
    private Observable<List<ExchangeCurrency>> currencyObservable;

    ListPreference marketCurrencyPreference;
    ListPreference unitsPreference;
    ListPreference currencyPreference;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Injector.inject(this);

        Preference buttonPreference = findPreference("reset");
        buttonPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                logOut();
                return true;
            }
        });

        currencyPreference = (ListPreference) findPreference("currency");
        unitsPreference = (ListPreference) findPreference(getString(R.string.pref_key_distance));

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String units = preferences.getString(getString(R.string.pref_key_distance), "0");
        Timber.d("Units: " + units);
        
        unitsPreference.setTitle((units.equals("0")? "Kilometers (km)":"Miles (mi)"));

        String currency = exchangeService.getExchangeCurrency();
        marketCurrencyPreference = (ListPreference) findPreference("exchange_currency");
        marketCurrencyPreference.setTitle("Market currency (" + currency + ")");
        
        String[] currencyList = {"USD"};
        String[] currencyValues= {"0"};
        marketCurrencyPreference.setEntries(currencyList);
        marketCurrencyPreference.setDefaultValue("0");
        marketCurrencyPreference.setEntryValues(currencyValues);
      
        currencyObservable = exchangeService.getGlobalTickers().cache();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        subscribeData();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscription.unsubscribe();;
        currencySubscription.unsubscribe();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0, 0, 0, 0);
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.inject(this, view);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals("exchange_currency")) {
            String marketCurrency = marketCurrencyPreference.getEntry().toString();
            String storedMarketCurrency = exchangeService.getExchangeCurrency();
            if(!storedMarketCurrency.equals(marketCurrency)) {
                marketCurrencyPreference.setTitle("Market currency (" + marketCurrencyPreference.getEntry() + ")");
                exchangeService.setExchangeCurrency(marketCurrency);
            }
        } else  if (key.equals("distance_units")) {
            String units = unitsPreference.getValue();
            unitsPreference.setTitle((units.equals("0")? "Kilometers (km)":"Miles (mi)"));
        }
    }
    
    private void subscribeData()
    {
        currencySubscription = currencyObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<ExchangeCurrency>>()
                {
                    @Override
                    public void call(List<ExchangeCurrency> currencies)
                    {
                        updateCurrencies(currencies);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        // TODO default to USD always
                        Timber.e(throwable.getLocalizedMessage());
                        Toast.makeText(getActivity(), "Unable to load currencies...", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateCurrencies(List<ExchangeCurrency> currencies)
    {
        ArrayList<String> currencyList = new ArrayList<>();
        ArrayList<String> currencyValues = new ArrayList<>();
        String exchangeCurrency = exchangeService.getExchangeCurrency();

        if(currencies.isEmpty()) {
            ExchangeCurrency tempCurrency = new ExchangeCurrency(exchangeCurrency, "https://api.bitcoinaverage.com/ticker/USD");
            currencies.add(tempCurrency); // just revert back to USD if we can
        }

        int value = 0;
        int selectedValue = 0;
        for (ExchangeCurrency item : currencies) {
            currencyList.add(item.getName());
            currencyValues.add(String.valueOf(value));
            if(exchangeCurrency.equals(item.getName())) {
                selectedValue = value;
            }
            value++;
        }

        String[] stringExchanges = new String[currencyList.size()];
        stringExchanges = currencyList.toArray(stringExchanges);

        String[] stringValues = new String[currencyValues.size()];
        stringValues = currencyValues.toArray(stringValues);

        marketCurrencyPreference.setEntries(stringExchanges);
        marketCurrencyPreference.setDefaultValue("0");
        marketCurrencyPreference.setEntryValues(stringValues);
        marketCurrencyPreference.setValue(String.valueOf(selectedValue));
    }
    
    private void logOut()
    {
        ((SettingsActivity) getActivity()).logOutConfirmation();
    }
}
