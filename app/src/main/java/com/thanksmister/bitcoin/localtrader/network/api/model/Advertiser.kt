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
class Advertiser() : Parcelable {

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("feedback_score")
    @Expose
    var feedbackScore: Int? = 0

    @SerializedName("trade_count")
    @Expose
    var tradeCount: String? = null

    @SerializedName("last_online")
    @Expose
    var lastOnline: String? = null

    @ColumnInfo(name = "name")
    @Expose
    var name: String? = null

    constructor(parcel: Parcel) : this() {
        username = parcel.readString()
        feedbackScore = parcel.readValue(Int::class.java.classLoader) as? Int
        tradeCount = parcel.readString()
        lastOnline = parcel.readString()
        name = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeValue(feedbackScore)
        parcel.writeString(tradeCount)
        parcel.writeString(lastOnline)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Advertiser> {
        override fun createFromParcel(parcel: Parcel): Advertiser {
            return Advertiser(parcel)
        }

        override fun newArray(size: Int): Array<Advertiser?> {
            return arrayOfNulls(size)
        }
    }
}