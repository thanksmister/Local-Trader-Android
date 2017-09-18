/*
 * Copyright (c) 2016. DusApp
 */

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

import dpreference.DPreference;
import timber.log.Timber;

public class AuthUtils {

    private final static String PREFS_UPGRADE_VERSION = "upgradeVersion";
    private final static String ACCESS_TOKEN = "accessToken";
    private final static String REFRESH_TOKEN = "refreshToken";
    private static final String PREFS_USER = "userName";
    private static final String PREFS_USER_FEEDBACK = "userFeedback";
    private static final String PREFS_USER_TRADES = "userTrades";
    private static final String PREFS_FIRST_TIME = "firstTime";
    private static final String PREFS_DISTANCE_UNITS = "distanceUnits";
    private static final String PREFS_FORCE_UPDATES = "forceUpdates";
    private static final String PREFS_API_ENDPOINT = "apiEndpoint";
    private static final String BASE_URL = "https://localbitcoins.com";

    /**
     * Returns the api service endpoint
     *
     * @param sharedPreferences
     * @return
     */
    public static boolean showUpgradedMessage(@NonNull Context context, @NonNull DPreference sharedPreferences) {
        int currentVersion = getCurrentVersion(context);
        int storedVersion = sharedPreferences.getInt(PREFS_UPGRADE_VERSION, 0);
        return currentVersion > storedVersion;
    }

    public static int getCurrentVersion(@NonNull Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e.getMessage());
        }
        return 0;
    }

    public static String getCurrentVersionName(@NonNull Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return "v" + packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e.getMessage());
        }
        return "!";
    }

    /**
     * Returns the api service endpoint
     *
     * @param preference
     * @return
     */
    public static String getServiceEndpoint(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_API_ENDPOINT);
        String currentValue = stringPreference.get();
        if(!TextUtils.isEmpty(currentValue)) {
            setServiceEndPoint(preference, currentValue);
            stringPreference.delete();
        }
        return preference.getString(PREFS_API_ENDPOINT, BASE_URL);
    }

    /**
     * Returns true if we have stored credentials
     *
     * @param sharedPreferences
     */
    public static boolean hasCredentials(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, ACCESS_TOKEN);
        String currentAccessToken = stringPreference.get();
        if(!TextUtils.isEmpty(currentAccessToken)) {
            setAccessToken(preference, currentAccessToken);
            stringPreference.delete();
        }

        stringPreference = new StringPreference(sharedPreferences, REFRESH_TOKEN);
        String currentRefreshToken = getRefreshToken(preference, sharedPreferences);
        if(!TextUtils.isEmpty(currentRefreshToken)) {
            setRefreshToken(preference, currentRefreshToken);
            stringPreference.delete();
        }
        
        String accessToken = preference.getString(ACCESS_TOKEN, null);
        String refreshToken = preference.getString(REFRESH_TOKEN, null);
        return !TextUtils.isEmpty(accessToken) && !TextUtils.isEmpty(refreshToken);
    }

    /**
     * Get the stored access token
     * @return
     */
    public static String getAccessToken(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, ACCESS_TOKEN);
        String currentAccessToken = stringPreference.get();

        if(!TextUtils.isEmpty(currentAccessToken)) {
            setAccessToken(preference, currentAccessToken);
            stringPreference.delete();
        }
        
        return preference.getString(ACCESS_TOKEN, null);
    }

    /**
     * Get the stored refresh token
     */
    public static String getRefreshToken(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, REFRESH_TOKEN);
        String currentRefreshToken = stringPreference.get();
        
        if(!TextUtils.isEmpty(currentRefreshToken)) {
            setRefreshToken(preference, currentRefreshToken);
            stringPreference.delete();
        }
        
        try {
            return preference.getString(REFRESH_TOKEN, null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the username
     *
     * @param sharedPreferences
     * @return
     */
    public static String getUsername(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER);
        String currentUserName =  stringPreference.get();
        if(!TextUtils.isEmpty(currentUserName)) {
            setUsername(preference, currentUserName);
            stringPreference.delete();
        }
        
        return preference.getString(PREFS_USER, currentUserName);
    }
    
    public static String getTrades(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER_TRADES);
        String currentTrades = stringPreference.get();
        if(!TextUtils.isEmpty(currentTrades)) {
            setTrades(preference, currentTrades);
            stringPreference.delete();
        }
        return preference.getString(PREFS_USER_TRADES, currentTrades);
    }

    public static String getFeedbackScore(@NonNull DPreference preference, SharedPreferences sharedPreferences) {
        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER_FEEDBACK);
        String feedback = stringPreference.get();
        if(!TextUtils.isEmpty(feedback)) {
            setFeedbackScore(preference, feedback);
        }
        return preference.getString(PREFS_USER_FEEDBACK, feedback);
    }

    /**
     * Set current version number
     */
    public static void setUpgradeVersion(@NonNull Context context, @NonNull DPreference preference) {
        int version = getCurrentVersion(context);
        preference.putInt(PREFS_UPGRADE_VERSION, version);
    }

    /**
     * Set the api end point
     */
    public static void setServiceEndPoint(@NonNull DPreference preference, @NonNull String value) {
        preference.putString(PREFS_API_ENDPOINT, value);
    }

    /**
     * Set the access token
     *
     * @param preference
     * @param key
     */
    public static void setAccessToken(@NonNull DPreference preference, @NonNull String key) {
        preference.putString(ACCESS_TOKEN, key);
    }

    /**
     * Set the refresh token
     *
     * @param preference
     * @param secret
     */
    public static void setRefreshToken(@NonNull DPreference preference, @NonNull String secret) {
        preference.putString(REFRESH_TOKEN, secret);
    }

    /**
     * Set the username
     *
     * @param preference
     * @param username
     */
    public static void setUsername(@NonNull DPreference preference, @NonNull String username) {
        preference.putString(PREFS_USER, username);
    }

    /**
     * Set the user feedback
     */
    public static void setFeedbackScore(@NonNull DPreference preference, @NonNull String feedback) {
        preference.putString(PREFS_USER_FEEDBACK, feedback);
    }

    /**
     * Set the user trades
     */
    public static void setTrades(@NonNull DPreference preference, @NonNull String trades) {
        preference.putString(PREFS_USER_TRADES, trades);
    }
    
    public static boolean getForceUpdate(@NonNull DPreference preference) {
        return preference.getBoolean(PREFS_FORCE_UPDATES, false);
    }
    
    public static void setForceUpdate(@NonNull DPreference preference, boolean force) {
        preference.putBoolean(PREFS_FORCE_UPDATES, force);
    }

    public static boolean isFirstTime(@NonNull DPreference preference) {
        return preference.getBoolean(PREFS_FIRST_TIME, false);
    }

    public static void setFirstTime(@NonNull DPreference preference, boolean force) {
        preference.putBoolean(PREFS_FIRST_TIME, force);
    }

    /**
     * Reset the stored credentials
     *
     * @param sharedPreferences
     */
    public static void resetCredentials(@NonNull SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ACCESS_TOKEN, null);
        editor.putString(REFRESH_TOKEN, null);
        editor.clear();
        editor.apply();

    }
    
    public static void reset(DPreference preference) {
        preference.removePreference(ACCESS_TOKEN);
        preference.removePreference(REFRESH_TOKEN);
        preference.removePreference(PREFS_USER_TRADES);
        preference.removePreference(PREFS_FORCE_UPDATES);
        preference.removePreference(PREFS_USER_FEEDBACK);
        preference.removePreference(PREFS_USER);
        //preference.removePreference(PREFS_API_ENDPOINT);
    }
}