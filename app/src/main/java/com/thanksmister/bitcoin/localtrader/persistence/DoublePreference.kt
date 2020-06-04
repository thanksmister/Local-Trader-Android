/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import android.content.SharedPreferences

class DoublePreference @JvmOverloads constructor(private val preferences: SharedPreferences, private val key: String, private val defaultValue: Double = 0.0) {

    val isSet: Boolean
        get() = preferences.contains(key)

    fun get(): Double {
        return java.lang.Double.longBitsToDouble(preferences.getLong(key, java.lang.Double.doubleToLongBits(defaultValue)))
    }

    //edit.putLong(key, Double.doubleToRawLongBits(value))
    fun set(value: Double) {
        preferences.edit().putLong(key, java.lang.Double.doubleToRawLongBits(value)).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}
