/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import android.content.SharedPreferences

class LongPreference @JvmOverloads constructor(private val preferences: SharedPreferences, private val key: String, private val defaultValue: Long = 0) {

    val isSet: Boolean
        get() = preferences.contains(key)

    fun get(): Long {
        return preferences.getLong(key, defaultValue)
    }

    fun set(value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}
