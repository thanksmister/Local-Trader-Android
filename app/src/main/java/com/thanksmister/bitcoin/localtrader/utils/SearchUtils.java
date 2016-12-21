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
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

import java.util.Locale;
import java.util.StringTokenizer;

import timber.log.Timber;

public class SearchUtils
{
    private static final String PREFS_SEARCH_ADDRESS = "searchAddress";
    private static final String PREFS_SEARCH_PAYMENT_METHOD = "searchPaymentMethod";
    private static final String PREFS_SEARCH_TRADE_TYPE = "searchTradeType";

    /**
     * Convert Address to string value
     * 
     * [addressLines=[0:"Los Angeles, CA",1:"USA"],feature=Los Angeles,admin=California,
     * sub-admin=Los Angeles County,locality=Los Angeles,thoroughfare=null,postalCode=null,
     * countryCode=US,countryName=United States,hasLatitude=true,latitude=34.0522342,
     * hasLongitude=true,longitude=-118.2436849,phone=null,url=null,extras=null]
     *
     * @param address
     * @return
     */
    public static String addressToString(Address address)
    {
        String addressLine = "";
        if(address.getMaxAddressLineIndex() >= 0) {
            addressLine = address.getAddressLine(0);
        }
        
        try {
            if(!TextUtils.isEmpty(addressLine)) {
                return addressLine
                        + "|" + address.getLocality()
                        + "|" + address.getCountryCode()
                        + "|" + address.getCountryName()
                        + "|" + address.getLongitude()
                        + "|" + address.getLatitude();
            }

            return address.getLocality()
                    + "|" + address.getCountryCode()
                    + "|" + address.getCountryName()
                    + "|" + address.getLongitude()
                    + "|" + address.getLatitude();
            
        } catch (IllegalStateException e) {
            Timber.e(e, "Address has no lat/lon: " + address.toString());
            return null;
        }
    }

    public static Address stringToAddress(String value)
    {
        if(TextUtils.isEmpty(value)) 
            return null;

        Address address = null;

        String[] split = new String[0];

        if(!TextUtils.isEmpty(value)) {
            StringTokenizer tokens = new StringTokenizer(value, "|");
            split = new String[tokens.countTokens()];
            int index = 0;
            while(tokens.hasMoreTokens()){
                split[index] = tokens.nextToken();
                ++index;
            }
        }

        try {
            if (split.length > 0) {
                address = new Address(Locale.getDefault());
                address.setAddressLine(0, split[0]);
                address.setLocality(split[1]);
                address.setCountryCode(split[2]);
                address.setCountryName(split[3]);
                address.setLongitude(NumberUtils.parseDouble(split[4], 0));
                address.setLatitude(NumberUtils.parseDouble(split[5], 0));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
        
        return address;
    }

    public static String getAddressShort(Address address)
    {
        String addressText = "";
        String addressLine = "0";
        String locality = "0";
        String country = "0";

        if(address.getMaxAddressLineIndex() >= 0) {
            
            if(address.getAddressLine(0) != null)
                addressLine = address.getAddressLine(0);

            if(!TextUtils.isEmpty(address.getLocality()))
                locality = address.getLocality();

            if (!TextUtils.isEmpty(address.getCountryName()))
                country = address.getCountryName();

            //addressText = String.format("%s", addressLine);
            
            if(!TextUtils.isEmpty(addressLine)) {
                addressText = String.format(
                        "%s, %s",
                        // Locality is usually a city
                        addressLine,
                        // The country of the address
                        country
                );
            } else {
                addressText = String.format(
                        "%s, %s, %s",
                        // If there's a street address, add it
                        addressLine,
                        // Locality is usually a city
                        locality,
                        // The country of the address
                        country
                );
            }
        }

        addressText = addressText.replace("0,", "");
        addressText = addressText.replace("null,", "");
        return addressText;
    }
    
    /**
     * Returns true if we a stored search address
     * @param sharedPreferences
     */
    public static boolean hasSearchAddress(@NonNull SharedPreferences sharedPreferences)
    {
        String address = sharedPreferences.getString(PREFS_SEARCH_ADDRESS, null);
        return !TextUtils.isEmpty(address);
    }

    public static String getSearchAddress(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_ADDRESS, "");
        return stringPreference.get();
    }
    
    public static String getSearchPaymentMethod(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_PAYMENT_METHOD, "all");
        return stringPreference.get();
    }

    public static String getSearchTradeType(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_TRADE_TYPE, TradeType.LOCAL_BUY.name());
        return stringPreference.get();
    }

    /**
     * Set the search address
     * @param sharedPreferences
     * @param address
     */
    public static void setSearchAddress(@NonNull SharedPreferences sharedPreferences, @NonNull String address)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_ADDRESS);
        stringPreference.set(address);
    }

    /**
     * Set the search location type (local/online)
     * @param sharedPreferences
     * @param method
     */
    public static void setSearchPaymentMethod(@NonNull SharedPreferences sharedPreferences, @NonNull String method)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_PAYMENT_METHOD);
        stringPreference.set(method);
    }

    /**
     * Set the search trade type (buy/sell)
     * @param sharedPreferences
     * @param type
     */
    public static void setSearchTradeType(@NonNull SharedPreferences sharedPreferences, @NonNull String type)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_SEARCH_TRADE_TYPE);
        stringPreference.set(type);
    }

    /**
     * Reset the stored credentials
     * @param sharedPreferences
     */
    public static void resetCredentials(@NonNull SharedPreferences sharedPreferences)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}