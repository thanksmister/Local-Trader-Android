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