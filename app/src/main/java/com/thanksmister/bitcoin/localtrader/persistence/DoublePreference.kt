/*
 * Copyright (c) 2019 ThanksMister LLC
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
