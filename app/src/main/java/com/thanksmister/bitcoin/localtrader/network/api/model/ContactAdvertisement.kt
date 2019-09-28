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

import androidx.room.*
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity
class ContactAdvertisement() : Parcelable {

    @SerializedName("id")
    @Expose
    var id: Int? = 0

    @SerializedName("trade_type")
    @Expose
    var tradeType: String = TradeType.NONE.name

    @SerializedName("payment_method")
    @Expose
    var paymentMethod: String? = null

    @SerializedName("advertiser")
    @Expose
    var advertiser: Advertiser = Advertiser()

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        tradeType = parcel.readString()
        paymentMethod = parcel.readString()
        advertiser = parcel.readParcelable(Advertiser::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(tradeType)
        parcel.writeString(paymentMethod)
        parcel.writeParcelable(advertiser, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ContactAdvertisement> {
        override fun createFromParcel(parcel: Parcel): ContactAdvertisement {
            return ContactAdvertisement(parcel)
        }

        override fun newArray(size: Int): Array<ContactAdvertisement?> {
            return arrayOfNulls(size)
        }
    }

}