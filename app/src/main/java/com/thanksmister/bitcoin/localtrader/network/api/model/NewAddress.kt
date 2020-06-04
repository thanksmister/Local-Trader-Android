/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.*
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity
class NewAddress() : Parcelable {

    @SerializedName("message")
    @Expose
    var message: String? = null

    @SerializedName("address")
    @Expose
    var address: String? = null

    constructor(parcel: Parcel) : this() {
        message = parcel.readString()
        address = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(message)
        parcel.writeString(address)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NewAddress> {
        override fun createFromParcel(parcel: Parcel): NewAddress {
            return NewAddress(parcel)
        }

        override fun newArray(size: Int): Array<NewAddress?> {
            return arrayOfNulls(size)
        }
    }
}