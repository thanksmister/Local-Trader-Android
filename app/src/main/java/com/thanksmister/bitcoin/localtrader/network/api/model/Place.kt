/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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
