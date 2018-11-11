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

package com.thanksmister.bitcoin.localtrader.ui.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.activities.SettingsActivity
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils

import java.util.ArrayList

import javax.inject.Inject

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var preferences: Preferences

    private var marketCurrencyPreference: ListPreference? = null
    private var exchangePreference: ListPreference? = null
    private var unitsPreference: ListPreference? = null
    private var apiPreference: EditTextPreference? = null
    private var currencyPreference: ListPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        val resetPreference = findPreference("reset")
        resetPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            logOut()
            true
        }


        val endpoint = preferences.getServiceEndpoint()
        apiPreference = findPreference(getString(R.string.pref_key_api)) as EditTextPreference
        apiPreference!!.text = endpoint
        apiPreference!!.setDefaultValue(endpoint)
        apiPreference!!.summary = endpoint

        val units = sharedPreferences.getString(getString(R.string.pref_key_distance), "0")
        unitsPreference = findPreference(getString(R.string.pref_key_distance)) as ListPreference
        unitsPreference!!.title = if (units == "0") getString(R.string.pref_distance_km) else getString(R.string.pref_distance_mi)

        val currency = preferences.exchangeCurrency
        marketCurrencyPreference = findPreference(getString(R.string.pref_key_exchange_currency)) as ListPreference
        marketCurrencyPreference!!.title = getString(R.string.pref_market_currency, currency)

        exchangePreference = findPreference(getString(R.string.pref_key_exchange)) as ListPreference
        exchangePreference!!.setDefaultValue(preferences.selectedExchange)
        exchangePreference!!.title = getString(R.string.pref_exchange, preferences.selectedExchange)

        val currencyList = arrayOf("USD")
        val currencyValues = arrayOf("0")
        marketCurrencyPreference!!.entries = currencyList
        marketCurrencyPreference!!.setDefaultValue("0")
        marketCurrencyPreference!!.entryValues = currencyValues

        currencyPreference = findPreference("currency") as ListPreference
    }

    override fun onResume() {
        super.onResume()
        subscribeData()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        if (v != null) {
            val lv = v.findViewById<View>(android.R.id.list) as ListView
            lv.setPadding(0, 0, 0, 0)
        }
        return v
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_key_exchange)) {
            val exchange = exchangePreference!!.value
            preferences.selectedExchange = exchange
            exchangePreference!!.title = getString(R.string.pref_exchange, preferences.selectedExchange)
        } else if (key == getString(R.string.pref_key_exchange_currency)) {
            val marketCurrency = marketCurrencyPreference!!.entry.toString()
            val storedMarketCurrency = preferences.exchangeCurrency
            if (storedMarketCurrency != marketCurrency) {
                marketCurrencyPreference!!.title = "Market currency (" + marketCurrencyPreference!!.entry + ")"
                preferences.exchangeCurrency = marketCurrency
            }
        } else if (key == "distance_units") {
            val units = unitsPreference!!.value
            unitsPreference!!.title = if (units == "0") "Kilometers (km)" else "Miles (mi)"
        } else if (key == getString(R.string.pref_key_api)) {
            val endpoint = apiPreference!!.editText.text.toString()
            val currentEndpoint = preferences.getServiceEndpoint()
            if (TextUtils.isEmpty(endpoint)) {
                (activity as SettingsActivity).showAlertDialog("The service end point should be a valid URL.")
            } else if (!Patterns.WEB_URL.matcher(endpoint).matches()) {
                (activity as SettingsActivity).showAlertDialog("The service end point should be a valid URL.")
            } else if (currentEndpoint != endpoint) {
                preferences.setServiceEndPoint(currentEndpoint)
                apiPreference!!.text = currentEndpoint
                apiPreference!!.summary = currentEndpoint
                apiPreference!!.setDefaultValue(currentEndpoint)
            }
        }
    }

    // TODO move this to model

    private fun subscribeData() {
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

    private fun updateCurrencies(currencies: List<Currency>) {

        val currenciesSorted = CurrencyUtils.sortCurrencies(currencies)

        val currencyList = ArrayList<String>()
        val currencyValues = ArrayList<String>()
        val exchangeCurrency = preferences.exchangeCurrency

        if (currenciesSorted.isEmpty()) {
            val exchangeRate = Currency()
            exchangeRate.code = getString(R.string.usd)
            currenciesSorted.add(exchangeRate)
        }

        var value = 0
        var selectedValue = 0
        for (item in currenciesSorted) {
            if(item.code != null) {
                currencyList.add(item.code!!)
                currencyValues.add(value.toString())
                if (exchangeCurrency == item.code) {
                    selectedValue = value
                }
            }
            value++
        }

        var stringExchanges = arrayOfNulls<String>(currencyList.size)
        stringExchanges = currencyList.toTypedArray<String?>()

        var stringValues = arrayOfNulls<String>(currencyValues.size)
        stringValues = currencyValues.toTypedArray<String?>()

        //preferences.clearExchangeExpireTime();
        marketCurrencyPreference!!.entries = stringExchanges
        marketCurrencyPreference!!.setDefaultValue("0")
        marketCurrencyPreference!!.entryValues = stringValues
        marketCurrencyPreference!!.value = selectedValue.toString()
    }

    private fun logOut() {
        if (activity != null) {
            (activity as SettingsActivity).logOutConfirmation()
        }
    }
}