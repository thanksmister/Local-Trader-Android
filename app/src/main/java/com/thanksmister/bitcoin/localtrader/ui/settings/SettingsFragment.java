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


import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.LoginActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.R.xml.preferences;


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
    EditTextPreference apiPreference;
    ListPreference currencyPreference;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(preferences);

        Injector.inject(this);

        Preference resetPreference = findPreference("reset");
        resetPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                logOut();
                return true;
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        //String currentEndpoint = AuthUtils.getServiceEndpoint(sharedPreferences);
        String endpoint = preferences.getString(getString(R.string.pref_key_api), "");
        apiPreference = (EditTextPreference) findPreference(getString(R.string.pref_key_api));
        apiPreference.setText(endpoint);
        apiPreference.setDefaultValue(endpoint);
        apiPreference.setSummary(endpoint);

        String units = preferences.getString(getString(R.string.pref_key_distance), "0");
        unitsPreference = (ListPreference) findPreference(getString(R.string.pref_key_distance));
        unitsPreference.setTitle((units.equals("0")? "Kilometers (km)":"Miles (mi)"));

        String currency = exchangeService.getExchangeCurrency();
        marketCurrencyPreference = (ListPreference) findPreference("exchange_currency");
        marketCurrencyPreference.setTitle("Market currency (" + currency + ")");
        
        String[] currencyList = {"USD"};
        String[] currencyValues= {"0"};
        marketCurrencyPreference.setEntries(currencyList);
        marketCurrencyPreference.setDefaultValue("0");
        marketCurrencyPreference.setEntryValues(currencyValues);

        currencyPreference = (ListPreference) findPreference("currency");
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
    public void onDestroy()
    {
        super.onDestroy();
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
            
        } else  if (key.equals(getString(R.string.pref_key_api))) {

            final String endpoint = apiPreference.getEditText().getText().toString();
            final String currentEndpoint = AuthUtils.getServiceEndpoint(sharedPreferences);
            
            if(TextUtils.isEmpty(endpoint)) {
                ((SettingsActivity) getActivity()).showAlertDialog(new AlertDialogEvent(null, "The service end point should not be a valid URL."));
            } else if (!Patterns.WEB_URL.matcher(endpoint).matches()){
                ((SettingsActivity) getActivity()).showAlertDialog(new AlertDialogEvent(null, "The service end point should not be a valid URL."));
            } else if (!currentEndpoint.equals(endpoint)) {
                ((SettingsActivity) getActivity()).showAlertDialog(new AlertDialogEvent(null, "Changing the service end point requires an application restart. Do you want to update the end point and restart now?"), new Action0()
                {
                    @Override
                    public void call()
                    {
                        resetEndPoint(endpoint);
                    }
                }, new Action0()
                {
                    @Override
                    public void call()
                    {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                        SharedPreferences.Editor prefEditor = preferences.edit();
                        prefEditor.putString(getString(R.string.pref_key_api), currentEndpoint).apply();

                        apiPreference.setText(currentEndpoint);
                        apiPreference.setSummary(currentEndpoint);
                        apiPreference.setDefaultValue(currentEndpoint);
                    }
                });
            }
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
    
    private void resetEndPoint(String endpoint)
    {
        AuthUtils.setServiceEndPoint(sharedPreferences, endpoint);
        Intent intent = LoginActivity.createStartIntent(getActivity());
        PendingIntent restartIntent = PendingIntent.getActivity(getActivity(), 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);
        System.exit(0);
    }
    
    private void logOut()
    {
        ((SettingsActivity) getActivity()).logOutConfirmation();
    }
}
