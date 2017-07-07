/*
 * Copyright (c) 2016 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.SharedPreferences;
import android.location.Address;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.prefs.DoublePreference;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import static com.thanksmister.bitcoin.localtrader.data.services.ExchangeService.PREFS_EXCHANGE_CURRENCY;
import static com.thanksmister.bitcoin.localtrader.utils.NumberUtils.parseDouble;

public class SearchUtils {

    private static final String PREFS_SEARCH_LOCATION_ADDRESS = "search_location_address";
    private static final String PREFS_SEARCH_CURRENCY = "search_currency";
    
    private static final String PREFS_SEARCH_LONGITUDE = "search_longitude_double";
    private static final String PREFS_SEARCH_LATITUDE = "search_latitude_double";
    private static final String PREFS_SEARCH_COUNTRY_NAME = "search_country_name";
    private static final String PREFS_SEARCH_COUNTRY_CODE = "search_country_code";
    
    private static final String PREFS_SEARCH_PAYMENT_METHOD = "searchPaymentMethod";
    private static final String PREFS_SEARCH_TRADE_TYPE = "searchTradeType";
    
    public static boolean coordinatesValid(String latitude, String longitude) {
        double lat = parseDouble(latitude, 0);
        double lon = NumberUtils.parseDouble(longitude, 0);
        return coordinatesValid(lat, lon);
    }

    public static boolean coordinatesValid(double lat, double lon) {
        return (lat != 0 && lat >= -90 && lat <= 90 && lon != 0 && lon >= -180 && lon <= 180);
    }
    
    public static Address coordinatesToAddress(String latitude, String longitude) {
        Address address = new Address(Locale.getDefault());
        address.setLatitude(parseDouble(latitude, 0));
        address.setLongitude(parseDouble(longitude, 0));
        return address;
    }

    public static String getDisplayAddress(@NonNull Address address) {
        if(address == null) 
            return "";
        
        String addressText = "";
        String addressLine = null;
        String locality = null;
        String country = null;
        double latitude = 0;
        double longitude = 0;

        if (!TextUtils.isEmpty(address.getAddressLine(0))) {
            addressLine = address.getAddressLine(0);
        }

        if (!TextUtils.isEmpty(address.getLocality())) {
            locality = address.getLocality();
        }

        if (!TextUtils.isEmpty(address.getCountryName())) {
            country = address.getCountryName();
        }

        if (!TextUtils.isEmpty(address.getCountryName())) {
            country = address.getCountryName();
        }

        if (address.hasLongitude() && address.hasLatitude()) {
            latitude = address.getLatitude();
            longitude = address.getLongitude();
        }

        if (!TextUtils.isEmpty(addressLine) && !TextUtils.isEmpty(country)) {
            addressText = String.format("%s, %s", addressLine, country);
        } else if (!TextUtils.isEmpty(locality) && !TextUtils.isEmpty(country) && !TextUtils.isEmpty(addressLine)) {
            addressText = String.format("%s, %s, %s", addressLine, locality, country);
        } else if (latitude != 0 && longitude != 0) {
            addressText = String.format("%s, %s", latitude, longitude);
        }

        if (addressText != null) {
            addressText = addressText.replace("0.0,", "");
            addressText = addressText.replace("0,", "");
            addressText = addressText.replace("null,", "");
        }

        return addressText;
    }

    /**
     * Returns true if we a stored search address
     *
     * @param sharedPreferences
     */
    public static boolean hasSearchAddress(@NonNull SharedPreferences sharedPreferences) {
        String address = sharedPreferences.getString(PREFS_SEARCH_LOCATION_ADDRESS, null);
        return !TextUtils.isEmpty(address);
    }

    public static String getSearchPaymentMethod(@NonNull SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_PAYMENT_METHOD, "all");
        return stringPreference.get();
    }
    
    public static void setSearchPaymentMethod(@NonNull SharedPreferences sharedPreferences, @NonNull String method) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_PAYMENT_METHOD);
        stringPreference.set(method);
    }

    public static String getSearchTradeType(@NonNull SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_TRADE_TYPE, TradeType.LOCAL_BUY.name());
        return stringPreference.get();
    }
    
    public static void setSearchTradeType(@NonNull SharedPreferences sharedPreferences, @NonNull String type) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_TRADE_TYPE);
        stringPreference.set(type);
    }

    public static Address getSearchLocationAddress(@NonNull SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_LOCATION_ADDRESS, null);
        String addressJson = stringPreference.get();
        Address address = new Address(Locale.US);
        if(addressJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(addressJson);
                address.setAddressLine(0, jsonObject.getString("addressline"));
                address.setCountryCode(jsonObject.getString("countrycode"));
                address.setCountryName(jsonObject.getString("countryname"));
                address.setLocality(jsonObject.getString("locality"));
                address.setLatitude(jsonObject.getDouble("latitude"));
                address.setLongitude(jsonObject.getDouble("longitude"));
            } catch (JSONException e) {
                e.printStackTrace();
            } 
        }
        return address;
    }

    public static void setSearchLocationAddress(@NonNull SharedPreferences sharedPreferences, @NonNull Address address) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("addressline", address.getAddressLine(0));
            jsonObject.put("countrycode", address.getCountryCode());
            jsonObject.put("countryname", address.getCountryName());
            jsonObject.put("locality", address.getLocality());
            jsonObject.put("latitude", address.getLatitude());
            jsonObject.put("longitude", address.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_LOCATION_ADDRESS);
        stringPreference.set(jsonObject.toString());
    }

    public static void clearSearchLocationAddress(@NonNull SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_LOCATION_ADDRESS);
        stringPreference.delete();
    }

    public static void setSearchCurrency(SharedPreferences sharedPreferences, String currency) {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SEARCH_CURRENCY);
        preference.set(currency);
    }

    public static String getSearchCurrency(SharedPreferences sharedPreferences) {
        StringPreference userCurrencyPref = new StringPreference(sharedPreferences, PREFS_EXCHANGE_CURRENCY, "Any");
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SEARCH_CURRENCY, userCurrencyPref.get());
        if(preference.get().equals("")) return "Any";
        return preference.get();
    }

    public static String getSearchCountryCode(@NonNull SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_CODE, "");
        return stringPreference.get();
    }

    public static void setSearchCountryCode(@NonNull SharedPreferences sharedPreferences, @NonNull String value) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_CODE);
        stringPreference.set(value);
    }

    public static String getSearchCountryName(@NonNull SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_NAME, "Any");
        if(stringPreference.get().equals("")) return "Any";
        return stringPreference.get();
    }

    public static void setSearchCountryName(@NonNull SharedPreferences sharedPreferences, @NonNull String value) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_COUNTRY_NAME);
        stringPreference.set(value);
    }
    
    public static double getSearchLatitude(@NonNull SharedPreferences sharedPreferences) {
        DoublePreference preference = new DoublePreference(sharedPreferences, PREFS_SEARCH_LATITUDE, 0);
        return preference.get();
    }
    
    public static void setSearchLatitude(@NonNull SharedPreferences sharedPreferences, double latitude) {
        DoublePreference preference = new DoublePreference(sharedPreferences, PREFS_SEARCH_LATITUDE);
        preference.set(latitude);
    }
    
    public static double getSearchLongitude(@NonNull SharedPreferences sharedPreferences) {
        DoublePreference preference = new DoublePreference(sharedPreferences, PREFS_SEARCH_LONGITUDE, 0);
        return preference.get();
    }

    public static void setSearchLongitude(@NonNull SharedPreferences sharedPreferences, double longitude) {
        DoublePreference preference = new DoublePreference(sharedPreferences, PREFS_SEARCH_LONGITUDE);
        preference.set(longitude);
    }

    /**
     * Reset the stored credentials
     *
     * @param sharedPreferences
     */
    public static void resetCredentials(@NonNull SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}