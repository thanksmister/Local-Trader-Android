/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import android.content.SharedPreferences

class StringPreference @JvmOverloads constructor(private val preferences: SharedPreferences, private val key: String, private val defaultValue: String = "") {

    val isSet: Boolean
        get() = preferences.contains(key)

    fun get(): String {
        return preferences.getString(key, defaultValue)
    }

    fun set(value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}