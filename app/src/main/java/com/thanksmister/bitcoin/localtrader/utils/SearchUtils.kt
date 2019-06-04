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

package com.thanksmister.bitcoin.localtrader.utils

import android.content.SharedPreferences
import android.location.Address
import android.text.TextUtils

import com.thanksmister.bitcoin.localtrader.persistence.DoublePreference
import com.thanksmister.bitcoin.localtrader.persistence.StringPreference
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType

import org.json.JSONException
import org.json.JSONObject

import java.util.Locale

import com.thanksmister.bitcoin.localtrader.utils.NumberUtils.parseDouble

object SearchUtils {

    private val PREFS_SEARCH_LOCATION_ADDRESS = "search_location_address"
    private val PREFS_SEARCH_CURRENCY = "search_currency"
    private val PREFS_SEARCH_LONGITUDE = "search_longitude_double"
    private val PREFS_SEARCH_LATITUDE = "search_latitude_double"
    private val PREFS_SEARCH_COUNTRY_NAME = "search_country_name"
    private val PREFS_SEARCH_COUNTRY_CODE = "search_country_code"
    private val PREFS_SEARCH_PAYMENT_METHOD = "searchPaymentMethod"
    private val PREFS_SEARCH_TRADE_TYPE = "searchTradeType"

    fun coordinatesValid(latitude: String, longitude: String): Boolean {
        val lat = parseDouble(latitude, 0.0)
        val lon = NumberUtils.parseDouble(longitude, 0.0)
        return coordinatesValid(lat, lon)
    }

    fun coordinatesValid(lat: Double, lon: Double): Boolean {
        return lat != 0.0 && lat >= -90 && lat <= 90 && lon != 0.0 && lon >= -180 && lon <= 180
    }

    fun coordinatesToAddress(latitude: String, longitude: String): Address {
        val address = Address(Locale.getDefault())
        address.latitude = parseDouble(latitude, 0.0)
        address.longitude = parseDouble(longitude, 0.0)
        return address
    }

    fun getDisplayAddress(address: Address): String? {

        var addressText: String? = ""
        var addressLine: String? = null
        var locality: String? = null
        var country: String? = null
        var latitude = 0.0
        var longitude = 0.0

        if (!TextUtils.isEmpty(address.getAddressLine(0))) {
            addressLine = address.getAddressLine(0)
        }

        if (!TextUtils.isEmpty(address.locality)) {
            locality = address.locality
        }

        if (!TextUtils.isEmpty(address.countryName)) {
            country = address.countryName
        }

        if (!TextUtils.isEmpty(address.countryName)) {
            country = address.countryName
        }

        if (address.hasLongitude() && address.hasLatitude()) {
            latitude = address.latitude
            longitude = address.longitude
        }

        if (!TextUtils.isEmpty(addressLine) && !TextUtils.isEmpty(country)) {
            addressText = String.format("%s, %s", addressLine, country)
        } else if (!TextUtils.isEmpty(locality) && !TextUtils.isEmpty(country) && !TextUtils.isEmpty(addressLine)) {
            addressText = String.format("%s, %s, %s", addressLine, locality, country)
        } else if (latitude != 0.0 && longitude != 0.0) {
            addressText = String.format("%s, %s", latitude, longitude)
        }

        if (addressText != null) {
            addressText = addressText.replace("0.0,", "")
            addressText = addressText.replace("0,", "")
            addressText = addressText.replace("null,", "")
        }

        return addressText
    }

    /**
     * Returns true if we a stored search address
     * @param sharedPreferences
     */
    fun hasSearchAddress(sharedPreferences: SharedPreferences): Boolean {
        val address = sharedPreferences.getString(PREFS_SEARCH_LOCATION_ADDRESS, null)
        return !TextUtils.isEmpty(address)
    }

    fun getSearchPaymentMethod(sharedPreferences: SharedPreferences): String {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_PAYMENT_METHOD, "all")
        return stringPreference.get()
    }

    fun setSearchPaymentMethod(sharedPreferences: SharedPreferences, method: String) {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_PAYMENT_METHOD)
        stringPreference.set(method)
    }

    fun getSearchTradeType(sharedPreferences: SharedPreferences): String {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_TRADE_TYPE, TradeType.LOCAL_BUY.name)
        return stringPreference.get()
    }

    fun setSearchTradeType(sharedPreferences: SharedPreferences, type: String) {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_TRADE_TYPE)
        stringPreference.set(type)
    }

    fun getSearchLocationAddress(sharedPreferences: SharedPreferences): Address {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_LOCATION_ADDRESS, "")
        val addressJson = stringPreference.get()
        val address = Address(Locale.US)
        if (addressJson.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(addressJson)
                address.setAddressLine(0, jsonObject.getString("addressline"))
                address.countryCode = jsonObject.getString("countrycode")
                address.countryName = jsonObject.getString("countryname")
                address.locality = jsonObject.getString("locality")
                address.latitude = jsonObject.getDouble("latitude")
                address.longitude = jsonObject.getDouble("longitude")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return address
    }

    fun setSearchLocationAddress(sharedPreferences: SharedPreferences, address: Address) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("addressline", address.getAddressLine(0))
            jsonObject.put("countrycode", address.countryCode)
            jsonObject.put("countryname", address.countryName)
            jsonObject.put("locality", address.locality)
            jsonObject.put("latitude", address.latitude)
            jsonObject.put("longitude", address.longitude)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            return
        } catch (e: JSONException) {
            e.printStackTrace()
            return
        }

        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_LOCATION_ADDRESS)
        stringPreference.set(jsonObject.toString())
    }

    fun clearSearchLocationAddress(sharedPreferences: SharedPreferences) {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_LOCATION_ADDRESS)
        stringPreference.delete()
    }

    fun setSearchCurrency(sharedPreferences: SharedPreferences, currency: String) {
        val preference = StringPreference(sharedPreferences, PREFS_SEARCH_CURRENCY)
        preference.set(currency)
    }

    fun getSearchCurrency(sharedPreferences: SharedPreferences): String {
        val preference = StringPreference(sharedPreferences, PREFS_SEARCH_CURRENCY, "USD")
        return preference.get()
    }

    fun getSearchCountryCode(sharedPreferences: SharedPreferences): String {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_CODE, "")
        return stringPreference.get()
    }

    fun setSearchCountryCode(sharedPreferences: SharedPreferences, value: String) {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_CODE)
        stringPreference.set(value)
    }

    fun getSearchCountryName(sharedPreferences: SharedPreferences): String {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_NAME, "Any")
        return if (stringPreference.get() == "") "Any" else stringPreference.get()
    }

    fun setSearchCountryName(sharedPreferences: SharedPreferences, value: String) {
        val stringPreference = StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_NAME)
        stringPreference.set(value)
    }

    fun getSearchLatitude(sharedPreferences: SharedPreferences): Double {
        val preference = DoublePreference(sharedPreferences, PREFS_SEARCH_LATITUDE, 0.0)
        return preference.get()
    }

    fun setSearchLatitude(sharedPreferences: SharedPreferences, latitude: Double) {
        val preference = DoublePreference(sharedPreferences, PREFS_SEARCH_LATITUDE)
        preference.set(latitude)
    }

    fun getSearchLongitude(sharedPreferences: SharedPreferences): Double {
        val preference = DoublePreference(sharedPreferences, PREFS_SEARCH_LONGITUDE, 0.0)
        return preference.get()
    }

    fun setSearchLongitude(sharedPreferences: SharedPreferences, longitude: Double) {
        val preference = DoublePreference(sharedPreferences, PREFS_SEARCH_LONGITUDE)
        preference.set(longitude)
    }
}