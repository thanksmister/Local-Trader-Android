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

package com.thanksmister.bitcoin.localtrader.ui.fragments;


import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.network.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.persistence.Preferences;
import com.thanksmister.bitcoin.localtrader.ui.activities.LoginActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.SettingsActivity;
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.thanksmister.bitcoin.localtrader.R.xml.preferences;
import static com.thanksmister.bitcoin.localtrader.network.services.ExchangeService.PREFS_EXCHANGE;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    Preferences appPreferences;

    @Inject
    SharedPreferences sharedPreferences;

    ListPreference marketCurrencyPreference;
    ListPreference exchangePreference;
    ListPreference unitsPreference;
    EditTextPreference apiPreference;
    ListPreference currencyPreference;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(preferences);

        Preference resetPreference = findPreference("reset");
        resetPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                logOut();
                return true;
            }
        });

        String endpoint = appPreferences.endPoint();
        apiPreference = (EditTextPreference) findPreference(getString(R.string.pref_key_api));
        apiPreference.setText(endpoint);
        apiPreference.setDefaultValue(endpoint);
        apiPreference.setSummary(endpoint);

        String units = sharedPreferences.getString(getString(R.string.pref_key_distance), "0");
        unitsPreference = (ListPreference) findPreference(getString(R.string.pref_key_distance));
        unitsPreference.setTitle((units.equals("0") ? getString(R.string.pref_distance_km) : getString(R.string.pref_distance_mi)));

        String currency = appPreferences.getExchangeCurrency();
        marketCurrencyPreference = (ListPreference) findPreference("exchange_currency");
        marketCurrencyPreference.setTitle(getString(R.string.pref_market_currency, currency));

        exchangePreference = (ListPreference) findPreference(PREFS_EXCHANGE);
        exchangePreference.setDefaultValue(appPreferences.getSelectedExchange());
        exchangePreference.setTitle(getString(R.string.pref_exchange, appPreferences.getSelectedExchange()));

        String[] currencyList = {"USD"};
        String[] currencyValues = {"0"};
        marketCurrencyPreference.setEntries(currencyList);
        marketCurrencyPreference.setDefaultValue("0");
        marketCurrencyPreference.setEntryValues(currencyValues);

        currencyPreference = (ListPreference) findPreference("currency");
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0, 0, 0, 0);
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {

        if (key.equals(PREFS_EXCHANGE)) {

            String exchange = exchangePreference.getValue();
            appPreferences.setSelectedExchange(exchange);
            exchangePreference.setTitle(getString(R.string.pref_exchange, appPreferences.getSelectedExchange()));

        } else if (key.equals("exchange_currency")) {

            String marketCurrency = marketCurrencyPreference.getEntry().toString();
            String storedMarketCurrency = appPreferences.getExchangeCurrency();
            if (!storedMarketCurrency.equals(marketCurrency)) {
                marketCurrencyPreference.setTitle("Market currency (" + marketCurrencyPreference.getEntry() + ")");
                appPreferences.setExchangeCurrency(marketCurrency);
            }
        } else if (key.equals("distance_units")) {

            String units = unitsPreference.getValue();
            // TODO internationalize
            unitsPreference.setTitle((units.equals("0") ? getString(R.string.pref_distance_km) : getString(R.string.pref_distance_mi)));

        } else if (key.equals(getString(R.string.pref_key_api))) {

            final String endpoint = apiPreference.getEditText().getText().toString();
            final String currentEndpoint = appPreferences.endPoint();
            if (TextUtils.isEmpty(endpoint)) {
                ((SettingsActivity) getActivity()).showAlertDialog(new AlertDialogEvent(null, "The service end point should be a valid URL."));
            } else if (!Patterns.WEB_URL.matcher(endpoint).matches()) {
                ((SettingsActivity) getActivity()).showAlertDialog(new AlertDialogEvent(null, "The service end point should be a valid URL."));
            } else if (!TextUtils.isEmpty(currentEndpoint) && !currentEndpoint.equals(endpoint)) {
                /*((SettingsActivity) getActivity()).showAlertDialog(new AlertDialogEvent(null, "Changing the service end point requires an application restart. Do you want to update the end point and restart now?"), new Action0() {
                    @Override
                    public void call() {
                        Timber.d("endpoint: " + endpoint);
                        Timber.d("currentEndpoint: " + currentEndpoint);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((SettingsActivity) getActivity()).showProgressDialog(new ProgressDialogEvent("Restarting..."));
                                apiPreference.setText(endpoint);
                                apiPreference.setSummary(endpoint);
                                apiPreference.setDefaultValue(endpoint);
                                AuthUtils.setServiceEndPoint(preference, endpoint);
                                handler = new Handler();
                                handler.postDelayed(refreshRunnable, 100);
                            }
                        });

                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        AuthUtils.setServiceEndPoint(preference, currentEndpoint);
                        apiPreference.setText(currentEndpoint);
                        apiPreference.setSummary(currentEndpoint);
                        apiPreference.setDefaultValue(currentEndpoint);
                    }
                });*/
            }
        }
    }

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = LoginActivity.Companion.createStartIntent(getActivity());
            PendingIntent restartIntent = PendingIntent.getActivity(getActivity(), 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);
            System.exit(0);
        }
    };

    private void subscribeData() {
       /* db.currencyQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<CurrencyItem>>() {
                    @Override
                    public void call(List<CurrencyItem> currencyItems) {
                        List<ExchangeCurrency> exchangeCurrencies = new ArrayList<ExchangeCurrency>();
                        exchangeCurrencies = ExchangeCurrencyItem.getCurrencies(currencyItems);
                        updateCurrencies(exchangeCurrencies);
                    }
                });*/
    }

    private void updateCurrencies(List<ExchangeCurrency> currencies) {

        currencies = CurrencyUtils.sortCurrencies(currencies);

        ArrayList<String> currencyList = new ArrayList<>();
        ArrayList<String> currencyValues = new ArrayList<>();
        String exchangeCurrency = appPreferences.getExchangeCurrency();

        if (currencies.isEmpty()) {
            ExchangeCurrency exchangeRate = new ExchangeCurrency(getString(R.string.usd));
            currencies.add(exchangeRate);
        }

        int value = 0;
        int selectedValue = 0;
        for (ExchangeCurrency item : currencies) {
            currencyList.add(item.getCurrency());
            currencyValues.add(String.valueOf(value));
            if (exchangeCurrency.equals(item.getCurrency())) {
                selectedValue = value;
            }
            value++;
        }

        String[] stringExchanges = new String[currencyList.size()];
        stringExchanges = currencyList.toArray(stringExchanges);

        String[] stringValues = new String[currencyValues.size()];
        stringValues = currencyValues.toArray(stringValues);

        //appPreferences.clearExchangeExpireTime();
        marketCurrencyPreference.setEntries(stringExchanges);
        marketCurrencyPreference.setDefaultValue("0");
        marketCurrencyPreference.setEntryValues(stringValues);
        marketCurrencyPreference.setValue(String.valueOf(selectedValue));
    }

    private void logOut() {
        ((SettingsActivity) getActivity()).logOutConfirmation();
    }
}
