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

class Place {
    @SerializedName("sell_local_url")
    @Expose
    var sellLocalUrl: String? = null
    @SerializedName("location_string")
    @Expose
    var locationString: String? = null
    @SerializedName("url")
    @Expose
    var url: String? = null
    @SerializedName("lon")
    @Expose
    var lon: Double? = null
    @SerializedName("lat")
    @Expose
    var lat: Double? = null
    @SerializedName("buy_local_url")
    @Expose
    var buyLocalUrl: String? = null
}
