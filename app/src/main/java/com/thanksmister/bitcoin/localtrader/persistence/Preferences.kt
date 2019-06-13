
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
import java.util.*
import javax.inject.Inject

/**
 * Store preferences
 */
class Preferences @Inject
constructor(private val preferences: AppPreferences) {

    fun inactivityTime():Long {
        return this.preferences.getLong(PREF_INACTIVITY_TIME, 300000)
    }

    fun inactivityTime(value:Long) {
        this.preferences.put(PREF_INACTIVITY_TIME, value)
    }

    fun address():String? {
        return this.preferences.getString(PREF_SSID, null)
    }

    fun address(value:String) {
        this.preferences.put(PREF_SSID, value)
    }

    fun ssID():String? {
        return this.preferences.getString(PREF_SSID, null)
    }

    fun ssId(value:String) {
        this.preferences.put(PREF_SSID, value)
    }

    fun password():String? {
        return this.preferences.getString(PREF_PASSWORD, null)
    }

    fun password(value:String) {
        this.preferences.put(PREF_PASSWORD, value)
    }

    fun getSearchLocationAddress(): Address {
        val addressJson = this.preferences.getString(PREFS_SEARCH_LOCATION_ADDRESS, null)
        val address = Address(Locale.US)
        if (addressJson != null) {
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

    fun setSearchLocationAddress(address: Address) {
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
        preferences.put(PREFS_SEARCH_LOCATION_ADDRESS, jsonObject.toString());
    }

    fun clearSearchLocationAddress() {
        preferences.remove(PREFS_SEARCH_CURRENCY)
    }

    fun setSearchCurrency(currency: String) {
        preferences.put(PREFS_SEARCH_CURRENCY, currency)
    }

    fun getSearchCurrency(): String {
        return this.preferences.getString(PREFS_SEARCH_CURRENCY, "USD")!!
    }

    fun getSearchCountryCode(): String {
        return this.preferences.getString(PREFS_SEARCH_COUNTRY_CODE, "")!!
    }

    fun setSearchCountryCode(value: String) {
        preferences.put(PREFS_SEARCH_COUNTRY_CODE, value)
    }

    fun getSearchCountryName(): String {
        return this.preferences.getString(PREFS_SEARCH_COUNTRY_NAME, "")!!
    }

    fun setSearchCountryName(value: String) {
        preferences.put(PREFS_SEARCH_COUNTRY_NAME, value)
    }

    fun getSearchLatitude(): Double {
        val value = this.preferences.getString(PREFS_SEARCH_LATITUDE, "0.0")
        return value!!.toDouble()
    }

    fun setSearchLatitude(latitude: Double) {
        preferences.put(PREFS_SEARCH_LATITUDE, latitude.toString())
    }

    fun getSearchLongitude(): Double {
        val value = this.preferences.getString(PREFS_SEARCH_LONGITUDE, "0.0")
        return value!!.toDouble()
    }

    fun setSearchLongitude(longitude: Double) {
        preferences.put(PREFS_SEARCH_LONGITUDE, longitude.toString())
    }

    /**
     * Returns the api service endpoint
     *
     * @param sharedPreferences
     * @return
     */
    fun showUpgradedMessage(context: Context): Boolean {
        val currentVersion = getCurrentVersion(context)
        val storedVersion = preferences.getInt(PREFS_UPGRADE_VERSION, 0)
        return currentVersion > storedVersion
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

    fun getCurrentVersionName(context: Context): String {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return "v" + packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e.message)
        }

        return "!"
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
        if(endpoint != BASE_URL || endpoint != ALT_BASE_URL) {
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
    fun resetCredentials() {
        preferences.remove(ACCESS_TOKEN)
        preferences.remove(REFRESH_TOKEN)
        preferences.remove(PREFS_FIRST_TIME)
        preferences.remove(ACCESS_TOKEN)
    }

    /**
     * Reset the `SharedPreferences` and database
     */
    fun reset() {
        preferences.clear()
    }

    companion object {
        const val PREF_SSID = "pref_ssid"
        const val PREF_PASSWORD = "pref_password"
        const val PREF_INACTIVITY_TIME = "pref_inactivity_time"
        const val PREFS_SEARCH_LOCATION_ADDRESS = "search_location_address"
        const val PREFS_SEARCH_CURRENCY = "search_currency"
        const val PREFS_SEARCH_LONGITUDE = "search_longitude_double"
        const val PREFS_SEARCH_LATITUDE = "search_latitude_double"
        const val PREFS_SEARCH_COUNTRY_NAME = "search_country_name"
        const val PREFS_SEARCH_COUNTRY_CODE = "search_country_code"
        const val PREFS_UPGRADE_VERSION = "upgradeVersion"
        const val ACCESS_TOKEN = "accessToken"
        const val REFRESH_TOKEN = "refreshToken"
        const val PREFS_USER = "userName"
        const val PREFS_USER_FEEDBACK = "userFeedback"
        const val PREFS_USER_TRADES = "userTrades"
        const val PREFS_FIRST_TIME = "firstTime"
        const val PREFS_API_ENDPOINT = "apiEndpoint"
        const val BASE_URL = "https://localbitcoins.com"
        const val ALT_BASE_URL = "https://localbitcoins.net"

    }
}