/*
 * Copyright (c) 2016. DusApp
 */

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

public class AuthUtils
{
    private final static String HMAC_KEY = "hmacKey";
    private final static String HMAC_SECRET = "hmacSecret";
    private static final String PREFS_USER = "userName";
    private static final String PREFS_USER_FEEDBACK = "userFeedback";
    private static final String PREFS_USER_TRADES = "userTrades";
    private static final String PREFS_API_ENDPOINT = "apiEndpoint";
    private static final String BASE_URL = "https://localbitcoins.com";
    
    /**
     * Returns the api service endpoint
     * @param sharedPreferences
     * @return
     */
    public static String getServiceEndpoint(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_API_ENDPOINT, BASE_URL);
        return stringPreference.get();
    }
    
    /**
     * Returns true if we have stored credentials
     * @param sharedPreferences
     */
    public static boolean hasCredentials(@NonNull SharedPreferences sharedPreferences)
    {
        String key = sharedPreferences.getString(HMAC_KEY, null);
        String secret = sharedPreferences.getString(HMAC_SECRET, null);
        return !TextUtils.isEmpty(key) && !TextUtils.isEmpty(secret);
    }
    
    /**
     * Get the stored hmac key
     * @param sharedPreferences
     * @return
     */
    public static String getHmacKey(@NonNull SharedPreferences sharedPreferences)
    {
        return sharedPreferences.getString(HMAC_KEY, null);
    }

    /**
     * Get the stored hmac secret
     * @param sharedPreferences
     * @return
     */
    public static String getHmacSecret(@NonNull SharedPreferences sharedPreferences)
    {
        return sharedPreferences.getString(HMAC_SECRET, null);
    }

    /**
     * Get the username
     * @param sharedPreferences
     * @return
     */
    public static String getUsername(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER);
        return stringPreference.get();
    }

    public static String getFeedback(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER_FEEDBACK);
        return stringPreference.get();
    }

    public static String getTrades(@NonNull SharedPreferences sharedPreferences)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER_TRADES);
        return stringPreference.get();
    }

    /**
     * Set the api end point
     * @param sharedPreferences
     * @param trades
     */
    public static void setServiceEndPoint(@NonNull SharedPreferences sharedPreferences, @NonNull String trades)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_API_ENDPOINT);
        stringPreference.set(trades);
    }

    /**
     * Set the hmac key
     * @param sharedPreferences
     * @param key
     */
    public static void setHmacKey(@NonNull SharedPreferences sharedPreferences, @NonNull String key)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HMAC_KEY, key);
        editor.apply();
    }

    /**
     * Set the hmac secret
     * @param sharedPreferences
     * @param secret
     */
    public static void setHmacSecret(@NonNull SharedPreferences sharedPreferences, @NonNull String secret)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HMAC_SECRET, secret);
        editor.apply();
    }
    
    /**
     * Set the username
     * @param sharedPreferences
     * @param username
     */
    public static void setUsername(@NonNull SharedPreferences sharedPreferences, @NonNull String username)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER);
        stringPreference.set(username);
    }

    /**
     * Set the user feedbacks
     * @param sharedPreferences
     * @param feedback
     */
    public static void setFeedbackScore(@NonNull SharedPreferences sharedPreferences, @NonNull String feedback)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER_FEEDBACK);
        stringPreference.set(feedback);
    }

    /**
     * Set the user trades
     * @param sharedPreferences
     * @param trades
     */
    public static void setTrades(@NonNull SharedPreferences sharedPreferences, @NonNull String trades)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER_TRADES);
        stringPreference.set(trades);
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
