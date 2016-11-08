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
    //private final static String HMAC_PREFERENCES = "hmacPreferences";
    private final static String HMAC_KEY = "hmacKey";
    private final static String HMAC_SECRET = "hmacSecret";
    private static final String PREFS_USER = "userName";

    /**
     * Returns true if we have stored credentials
     * @param sharedPreferences
     */
    public static boolean hasCredentials(@NonNull SharedPreferences sharedPreferences)
    {
        String key = sharedPreferences.getString(HMAC_KEY, null);
        return !TextUtils.isEmpty(key);
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
     * Set the username secret
     * @param sharedPreferences
     * @param username
     */
    public static void setUsername(@NonNull SharedPreferences sharedPreferences, @NonNull String username)
    {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER);
        stringPreference.set(username);
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
