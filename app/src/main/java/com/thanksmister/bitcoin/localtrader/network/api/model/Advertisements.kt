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

package com.thanksmister.bitcoin.localtrader.network.api.model


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

class Advertisements(items: List<Advertisement>) {

    @SerializedName("ad_list")
    @Expose
    var items: List<Advertisement> = ArrayList()

    @SerializedName("ad_count")
    @Expose
    var count: Int? = null

    init {
        this.items = items
    }

    fun getListItems(): List<Advertisement> {
        Collections.sort(items, Comparator { a1, a2 ->
            val b1 = a1.visible
            val b2 = a2.visible
            if (b1 && !b2) {
                return@Comparator +1
            }
            if (!b1 && b2) {
                -1
            } else 0
        })

        return items
    }

    fun getCount(): Int {
        return count!!
    }
}
