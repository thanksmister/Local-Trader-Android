/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *   
 *  Mozilla Public License 2.0
 *  
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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