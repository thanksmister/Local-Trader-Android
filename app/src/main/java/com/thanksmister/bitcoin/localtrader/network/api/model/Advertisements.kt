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
