/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import android.content.SharedPreferences

class IntPreference @JvmOverloads constructor(private val preferences: SharedPreferences, private val key: String, private val defaultValue: Int = 0) {

    val isSet: Boolean
        get() = preferences.contains(key)

    fun get(): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun set(value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}
