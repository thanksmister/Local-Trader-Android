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
class Total() : Parcelable {

    @SerializedName("balance")
    @Expose
    var balance: String? = null

    @SerializedName("sendable")
    @Expose
    var sendable: String? = null

    constructor(parcel: Parcel) : this() {
        balance = parcel.readString()
        sendable = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(balance)
        parcel.writeString(sendable)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Total> {
        override fun createFromParcel(parcel: Parcel): Total {
            return Total(parcel)
        }

        override fun newArray(size: Int): Array<Total?> {
            return arrayOfNulls(size)
        }
    }
}