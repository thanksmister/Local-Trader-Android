/*
 * Copyright (c) 2018 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.persistence

import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi
import net.grandcentrix.tray.AppPreferences
import timber.log.Timber
import javax.inject.Inject

/**
 * Store preferences
 */
class Preferences @Inject
constructor(private val preferences: AppPreferences) {

    fun endPoint():String? {
        val url  = this.preferences.getString(PREFS_API_ENDPOINT, BASE_URL)
        if(TextUtils.isEmpty(url)) {
           return BASE_URL
        }
        return this.preferences.getString(PREFS_API_ENDPOINT, BASE_URL)
    }

    fun endPoint(value:String) {
        this.preferences.put(PREFS_API_ENDPOINT, value)
    }

    fun accessToken():String? {
        return this.preferences.getString(PREF_ACCESS_TOKEN, null)
    }

    fun accessToken(value:String) {
        this.preferences.put(PREF_ACCESS_TOKEN, value)
    }

    fun refreshToken():String? {
        return this.preferences.getString(PREF_REFRESH_TOKEN, null)
    }

    fun refreshToken(value:String) {
        this.preferences.put(PREF_REFRESH_TOKEN, value)
    }

    fun userName():String? {
        return this.preferences.getString(PREFS_USER, null)
    }

    fun userName(value:String) {
        this.preferences.put(PREFS_USER, value)
    }

    fun userFeedback():String? {
        return this.preferences.getString(PREFS_USER_FEEDBACK, null)
    }

    fun userFeedback(value:String) {
        this.preferences.put(PREFS_USER_FEEDBACK, value)
    }

    fun userTrades():String? {
        return this.preferences.getString(PREFS_USER_TRADES, null)
    }

    fun userTrades(value:String) {
        this.preferences.put(PREFS_USER_TRADES, value)
    }

    fun firstTime():Boolean {
        return this.preferences.getBoolean(PREFS_FIRST_TIME, true)
    }

    fun firstTime(value:Boolean) {
        this.preferences.put(PREFS_FIRST_TIME, value)
    }

    fun forceUpdates():Boolean {
        return this.preferences.getBoolean(PREFS_FORCE_UPDATES, true)
    }

    fun forceUpdates(value:Boolean) {
        this.preferences.put(PREFS_FORCE_UPDATES, value)
    }

    fun hasCredentials(): Boolean {
        val accessToken = accessToken()
        val refreshToken = refreshToken()
        return !TextUtils.isEmpty(accessToken) && !TextUtils.isEmpty(refreshToken)
    }

    fun hasUserInfo(): Boolean {
        val userName = userName()
        return !TextUtils.isEmpty(userName)
    }

    fun upgradeVersion(context: Context) {
        val version = getCurrentVersion(context)
        preferences.put(PREFS_UPGRADE_VERSION, version)
    }

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

    fun setSelectedExchange(name: String) {
        preferences.put(PREFS_SELECTED_EXCHANGE, name)
    }

    fun getSelectedExchange(): String? {
        return preferences.getString(PREFS_SELECTED_EXCHANGE, COINBASE_EXCHANGE)
    }

    fun setExchangeCurrency(currency: String) {
        preferences.put(PREFS_EXCHANGE_CURRENCY, currency)
    }

    fun getExchangeCurrency(): String? {
        val currency = preferences.getString(PREFS_EXCHANGE_CURRENCY, USD)
        return if (TextUtils.isEmpty(currency)) USD else currency
    }

    fun needToRefreshCurrency(): Boolean {
        return System.currentTimeMillis() > preferences.getLong(PREFS_CURRENCY_EXPIRE_TIME, -1);
    }

    fun setCurrencyExpireTime() {
        val expire = System.currentTimeMillis() + CHECK_CURRENCY_DATA; // 1 hours
        preferences.put(PREFS_CURRENCY_EXPIRE_TIME, expire);
    }

    fun needToRefreshMethods(): Boolean {
        return System.currentTimeMillis() > preferences.getLong(PREFS_METHODS_EXPIRE_TIME, -1);
    }

    fun setMethodsExpireTime() {
        val expire = System.currentTimeMillis() + CHECK_METHODS_DATA; // 1 hours
        preferences.put(PREFS_METHODS_EXPIRE_TIME, expire);
    }

    fun needToRefreshWalletBalance(): Boolean {
        return System.currentTimeMillis() > preferences.getLong(PREFS_WALLET_BALANCE_EXPIRE_TIME, -1);
    }

    fun setWalletBalanceExpireTime() {
        val expire = System.currentTimeMillis() + CHECK_WALLET_BALANCE_DATA; // 1 hours
        preferences.put(PREFS_WALLET_BALANCE_EXPIRE_TIME, expire);
    }

    /**
     * Reset the `SharedPreferences` and database
     */
    fun reset() {
        preferences.clear()
        // TODO reset preferences
        preferences.remove(PREF_ACCESS_TOKEN)
        preferences.remove(PREF_REFRESH_TOKEN)
        preferences.remove(PREFS_USER)
        preferences.remove(PREFS_USER_FEEDBACK)
        preferences.remove(PREFS_USER_TRADES)
        preferences.remove(PREFS_FIRST_TIME)
        preferences.remove(PREFS_FORCE_UPDATES)
        preferences.remove(PREFS_API_ENDPOINT)
        preferences.remove(PREFS_UPGRADE_VERSION)
        preferences.remove(PREFS_EXCHANGE_EXPIRE_TIME)
        preferences.remove(PREFS_SELECTED_EXCHANGE)
        preferences.remove(PREFS_EXCHANGE_CURRENCY)
        preferences.remove(PREFS_EXCHANGE)
        preferences.remove(PREFS_METHODS_EXPIRE_TIME)
        preferences.remove(PREFS_CURRENCY_EXPIRE_TIME)
    }

    companion object {
        const val PREF_ACCESS_TOKEN = "pref_access_token"
        const val PREF_REFRESH_TOKEN = "pref_refresh_token"
        const val PREFS_USER = "pref_username"
        const val PREFS_USER_FEEDBACK = "pref_user_feedback"
        const val PREFS_USER_TRADES = "pref_user_trades"
        const val PREFS_FIRST_TIME = "pref_first_time"
        const val PREFS_FORCE_UPDATES = "pref_force_updates"
        const val PREFS_API_ENDPOINT = "pref_api_endpoint"
        const val PREFS_UPGRADE_VERSION = "pref_upgrade_version"
        const val PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire"
        const val PREFS_SELECTED_EXCHANGE = "selected_exchange"
        const val PREFS_EXCHANGE_CURRENCY = "exchange_currency"
        const val PREFS_EXCHANGE = "pref_exchange"
        const val PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire";
        const val PREFS_CURRENCY_EXPIRE_TIME = "pref_currency_expire";
        const val PREFS_WALLET_BALANCE_EXPIRE_TIME = "pref_wallet_balance_expire";

        // default values
        const val CHECK_CURRENCY_DATA = 604800000;// // 1 week 604800000
        const val CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
        const val CHECK_WALLET_DATA = 15 * 60 * 1000;// 15 minutes
        const val CHECK_WALLET_BALANCE_DATA = 15 * 60 * 1000;// 15 minutes
        const val BASE_URL = "https://localbitcoins.com"
        const val COINBASE_EXCHANGE = "Coinbase"
        const val USD = "USD"
    }
}