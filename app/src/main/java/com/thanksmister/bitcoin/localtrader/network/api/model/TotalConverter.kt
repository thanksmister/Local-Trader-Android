/*
 * Copyright (c) 2019 LocalBuzz
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

package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.TypeConverter

import com.google.gson.Gson

/**
 * Created by Michael Ritchie on 5/21/18.
 */
class TotalConverter {
    @TypeConverter
    fun fromString(value: String): Total? {
        return Gson().fromJson(value, Total::class.java)
    }

    @TypeConverter
    fun toString(value: Total): String {
        return Gson().toJson(value)
    }
}
