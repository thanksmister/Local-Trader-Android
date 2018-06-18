/*
 * Copyright (c) 2018 LocalBuzz
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

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.location.*
import android.os.Bundle
import android.text.TextUtils
import com.google.android.gms.location.LocationRequest
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertMessage
import com.thanksmister.bitcoin.localtrader.events.SnackbarMessage
import com.thanksmister.bitcoin.localtrader.events.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.CoinbaseApi
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.network.fetchers.ExchangeFetcher
import com.thanksmister.bitcoin.localtrader.persistence.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SearchViewModel @Inject
constructor(application: Application, private val currencyData: CurrencyDao, private val methodData: MethodDao,
            private val preferences: Preferences, private val locationManager: LocationManager) : AndroidViewModel(application) {

    private val disposable = CompositeDisposable()
    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val snackbarText = SnackbarMessage()
    private val progress = MutableLiveData<Boolean>()
    private val interaction = MutableLiveData<Boolean>()
    private val searchAddress = MutableLiveData<Address>()
    private val searchAddresses = MutableLiveData<List<Address>>()
    private var locationProvider: ReactiveLocationProvider? = null

    fun getProgress(): LiveData<Boolean> {
        return progress
    }

    private fun setProgress(value: Boolean) {
        progress.value = value
    }

    fun getInteraction(): LiveData<Boolean> {
        return interaction
    }

    private fun setInteraction(value: Boolean) {
        interaction.value = value
    }

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getSnackBarMessage(): SnackbarMessage {
        return snackbarText
    }

    fun getSearchAddresses(): LiveData<List<Address>> {
        return searchAddresses
    }

    fun setSearchAddresses(addresses: List<Address>) {
        searchAddresses.value = addresses
    }

    fun getSearchAddress(): LiveData<Address> {
        if(searchAddress.value == null) {
            searchAddress.value = preferences.getSearchLocationAddress()
        }
        return searchAddress
    }

    fun setSearchAddress(address: Address) {
        searchAddress.value = address
        preferences.setSearchLocationAddress(address)
        saveAddress(address)
    }

    fun setLatitude(value: Double) {
        preferences.setSearchLatitude( 0.0)
    }

    fun setLongitude(value: Double) {
        preferences.setSearchLongitude( 0.0)
    }

    private fun saveAddress(address: Address) {
        Timber.d("SaveAddress: " + address.toString())
        if (address.hasLatitude()) {
            setLatitude(address.latitude)
        }
        if (address.hasLongitude()) {
            setLongitude(address.longitude)
        }
        if (!TextUtils.isEmpty(address.countryCode)) {
            preferences.setSearchCountryCode(address.countryCode)
        }
        if (!TextUtils.isEmpty(address.getAddressLine(0))) {
            preferences.setSearchCountryName(address.getAddressLine(0))
        }
    }

    fun getTradeType(): TradeType {
        return TradeType.valueOf(preferences.getSearchTradeType())
    }

    fun setTradeType(type: TradeType) {
        preferences.setSearchTradeType(type.name)
    }
    private fun showSnackbarMessage(message: Int) {
        snackbarText.value = message
    }

    private fun showAlertMessage(message: String) {
        Timber.d("alert message: " + message)
        alertText.value = message
    }

    private fun showToastMessage(message: String) {
        toastText.value = message
    }

    init {
        locationProvider = ReactiveLocationProvider(getApplication())
    }

    fun getCurrencies():Flowable<ArrayList<Currency>> {
        return currencyData.getItems()
                .map {items ->
                    val currenciesList = ArrayList(items)
                    if(currenciesList.isEmpty()) {
                        val searchCurrency = preferences.getSearchCurrency()
                        if (currenciesList.isEmpty()) {
                            val currency = Currency()
                            currency.name = searchCurrency
                            currency.code = searchCurrency
                            currenciesList.add(currency)
                        }
                    }
                    val anyCurrency = Currency()
                    anyCurrency.name = getApplication<BaseApplication>().getString(R.string.text_currency_any)
                    anyCurrency.code = "any"
                    currenciesList.add(0, anyCurrency)
                    currenciesList
                }
    }

    fun getMethods():Flowable<ArrayList<Method>> {
        return methodData.getItems()
                .map {items ->
                    val methodList = ArrayList(items)
                    val method = Method();
                    method.code = "all";
                    method.name = getApplication<BaseApplication>().getString(R.string.text_method_name_all);
                    methodList.add(method)
                    methodList
                }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        setProgress(true)
        val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100)

        disposable.add(locationProvider!!.getUpdatedLocation(request)
                .timeout(20000, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Function { Observable.just(null); })
                .subscribe({location ->
                    if (location != null) {
                        reverseLocationLookup(location);
                    } else {
                        getLocationFromLocationManager();
                    }
                },{
                    error -> Timber.e("Error address lookup: ${error.message}")
                    getLocationFromLocationManager()
                }))
    }

    /**
     * Used as a backup location service in case the first one fails, which
     * switch to using the LocationManager instead.
     */
    private fun getLocationFromLocationManager() {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_COARSE
        try {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    if (location != null) {
                        locationManager.removeUpdates(this)
                        reverseLocationLookup(location)
                    } else {
                        setProgress(false)
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_location_message))
                    }
                }
                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                    Timber.d("onStatusChanged: $status")
                }
                override fun onProviderEnabled(provider: String) {
                    Timber.d("onProviderEnabled")
                }
                override fun onProviderDisabled(provider: String) {
                    Timber.d("onProviderDisabled")
                }
            }
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 100, 5f, locationListener)
        } catch (e: IllegalArgumentException) {
            Timber.e("Location manager could not use network provider", e)
            setProgress(false)
            showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_location_message))
        } catch (e: SecurityException) {
            Timber.e("Location manager could not use network provider", e)
            setProgress(false)
            showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_location_message))
        }
    }

    fun addressLookup(locationName: String) {
        disposable.add(locationProvider!!.getGeocodeObservable(locationName, 5)
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({addresses -> setSearchAddresses(addresses)
                },{
                    error -> Timber.e("Error address lookup: ${error.message}")
                    showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_dialog_no_address_edit))
                }))
    }


    /**
     * Used from the location manager to do a reverse lookup of the address from the coordinates.
     * @param location
     */
    private fun reverseLocationLookup(location: Location) {
        disposable.add(locationProvider!!.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .filter { addresses -> !addresses.isEmpty() }
                .map { addresses -> addresses[0] }
                .subscribe({address ->
                    setProgress(false)
                    setSearchAddress(address)
                },{
                    error -> Timber.e("Error address lookup: ${error.message}")
                    setProgress(false)
                    showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_dialog_no_address_edit))
                }))
    }

    public override fun onCleared() {
        //prevents memory leaks by disposing pending observable objects
        if ( !disposable.isDisposed) {
            disposable.clear()
        }
    }

    /**
     * Network connectivity receiver to notify client of the network disconnect issues and
     * to clear any network notifications when reconnected. It is easy for network connectivity
     * to run amok that is why we only notify the user once for network disconnect with
     * a boolean flag.
     */
    companion object {

    }
}