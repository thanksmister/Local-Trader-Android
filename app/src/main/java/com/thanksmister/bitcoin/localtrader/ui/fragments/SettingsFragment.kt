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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments


import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.activities.SettingsActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SettingsViewModel
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import com.thanksmister.bitcoin.localtrader.utils.disposeProper
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: SettingsViewModel

    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var preferences: Preferences

    private val disposable = CompositeDisposable()
    private var marketCurrencyPreference: ListPreference? = null
    private var exchangePreference: ListPreference? = null
    private var unitsPreference: ListPreference? = null
    private var apiPreference: EditTextPreference? = null
    private var currencyPreference: ListPreference? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SettingsViewModel::class.java)

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

        currencyPreference = findPreference(getString(R.string.pref_key_exchange_currency)) as ListPreference

        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: SettingsViewModel) {
        disposable.add(
                viewModel.getCurrencies()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( { currencies ->
                            updateCurrencies(currencies)
                        }, { error ->
                            Timber.e(error.message)
                        }))
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.disposeProper()
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
                marketCurrencyPreference!!.title = getString(R.string.text_market_currency, marketCurrencyPreference!!.entry)
                preferences.exchangeCurrency = marketCurrency
            }
        } else if (key == getString(R.string.pref_key_distance)) {
            val units = unitsPreference!!.value
            unitsPreference!!.title = if (units == "0") "Kilometers (km)" else "Miles (mi)"
        } else if (key == getString(R.string.pref_key_api)) {
            val endpoint = apiPreference!!.text.toString()
            val currentEndpoint = preferences.getServiceEndpoint()
            if (TextUtils.isEmpty(endpoint) && activity != null) {
                dialogUtils.showAlertDialog(activity!!, getString(R.string.alert_service_end_point))
            } else if (!Patterns.WEB_URL.matcher(endpoint).matches() && activity != null) {
                dialogUtils.showAlertDialog(activity!!, getString(R.string.alert_service_end_point))
            } else if (currentEndpoint != endpoint) {
                preferences.setServiceEndPoint(currentEndpoint)
                apiPreference!!.text = currentEndpoint
                apiPreference!!.summary = currentEndpoint
                apiPreference!!.setDefaultValue(currentEndpoint)
            }
        }
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