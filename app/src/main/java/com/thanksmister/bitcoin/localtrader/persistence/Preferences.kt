/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */


package com.thanksmister.bitcoin.localtrader.persistence

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.text.TextUtils
import com.thanksmister.bitcoin.localtrader.network.api.ExchangeApi
import net.grandcentrix.tray.AppPreferences
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.lang.Exception
import java.util.*
import javax.inject.Inject

/**
 * Store preferences
 */
class Preferences @Inject
constructor(private val preferences: AppPreferences) {

    fun address():String? {
        return this.preferences.getString(PREF_SSID, null)
    }

    fun address(value:String) {
        this.preferences.put(PREF_SSID, value)
    }

    fun getCurrentVersion(context: Context): Int {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e.message)
        }

        return 0
    }

    var selectedExchange: String
        get() {
            return preferences.getString(ExchangeApi.PREFS_SELECTED_EXCHANGE, ExchangeApi.COINBASE_EXCHANGE) ?: return ExchangeApi.COINBASE_EXCHANGE
        }
        set(name) {
            preferences.put(ExchangeApi.PREFS_SELECTED_EXCHANGE, name)
        }

    var exchangeCurrency: String
        get() {
            return preferences.getString(ExchangeApi.PREFS_EXCHANGE_CURRENCY, ExchangeApi.USD) ?: return ExchangeApi.USD
        }
        set(currency) {
            preferences.put(ExchangeApi.PREFS_EXCHANGE_CURRENCY, currency)
        }

    /**
     * Returns the api service endpoint
     *
     * @param preference
     * @return
     */
    fun getServiceEndpoint(): String {
        val endpoint = preferences.getString(PREFS_API_ENDPOINT, BASE_URL) ?: return BASE_URL
        if(endpoint != BASE_URL && endpoint != ALT_BASE_URL) {
            setServiceEndPoint(BASE_URL)
            return BASE_URL
        }
        return endpoint
    }

    /**
     * Returns true if we have stored credentials
     *
     * @param sharedPreferences
     */
    fun hasCredentials(): Boolean {
        val accessToken = preferences.getString(ACCESS_TOKEN, null)
        val refreshToken = preferences.getString(REFRESH_TOKEN, null)
        return !TextUtils.isEmpty(accessToken) && !TextUtils.isEmpty(refreshToken)
    }

    /**
     * Get the stored access token
     *
     * @return
     */
    fun getAccessToken(): String {
        return preferences.getString(ACCESS_TOKEN, null) ?: return ""
    }

    /**
     * Get the stored refresh token
     */
    fun getRefreshToken(): String {
        return preferences.getString(REFRESH_TOKEN, null) ?: return ""
    }

    /**
     * Set current version number
     */
    fun setUpgradeVersion(context: Context) {
        val version = getCurrentVersion(context)
        preferences.put(PREFS_UPGRADE_VERSION, version)
    }

    /**
     * Set the api end point
     */
    fun setServiceEndPoint(value: String) {
        preferences.put(PREFS_API_ENDPOINT, value)
    }

    /**
     * Set the access token
     *
     * @param preference
     * @param key
     */
    fun setAccessToken(key: String) {
        preferences.put(ACCESS_TOKEN, key)
    }

    /**
     * Set the refresh token
     *
     * @param preference
     * @param secret
     */
    fun setRefreshToken(secret: String) {
        preferences.put(REFRESH_TOKEN, secret)
    }

    fun isFirstTime(): Boolean {
        return preferences.getBoolean(PREFS_FIRST_TIME, true)
    }

    fun setFirstTime(value: Boolean) {
        preferences.put(PREFS_FIRST_TIME, value)
    }


    /**
     * Reset the stored credentials
     *
     * @param sharedPreferences
     */
    private fun resetCredentials() {
        preferences.remove(ACCESS_TOKEN)
        preferences.remove(REFRESH_TOKEN)
        preferences.remove(PREFS_FIRST_TIME)
        preferences.remove(ACCESS_TOKEN)
    }

    /**
     * Reset the `SharedPreferences` and database
     */
    fun reset() {
        resetCredentials()
        preferences.clear()
    }

    companion object {
        const val PREF_SSID = "pref_ssid"
        const val PREFS_UPGRADE_VERSION = "upgradeVersion"
        const val ACCESS_TOKEN = "accessToken"
        const val REFRESH_TOKEN = "refreshToken"
        const val PREFS_FIRST_TIME = "firstTime"
        const val PREFS_API_ENDPOINT = "apiEndpoint"
        const val BASE_URL = "https://localbitcoins.com"
        const val ALT_BASE_URL = "https://localbitcoins.net"

    }
}