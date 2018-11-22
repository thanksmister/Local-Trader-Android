/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
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
