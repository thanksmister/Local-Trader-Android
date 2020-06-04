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
class Buyer() : Parcelable {

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("feedback_score")
    @Expose
    var feedbackScore: Int = 0

    @SerializedName("trade_count")
    @Expose
    var tradeCount: String? = null

    @SerializedName("last_online")
    @Expose
    var lastOnline: String? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    constructor(parcel: Parcel) : this() {
        username = parcel.readString()
        feedbackScore = parcel.readInt()
        tradeCount = parcel.readString()
        lastOnline = parcel.readString()
        name = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeInt(feedbackScore)
        parcel.writeString(tradeCount)
        parcel.writeString(lastOnline)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Buyer> {
        override fun createFromParcel(parcel: Parcel): Buyer {
            return Buyer(parcel)
        }

        override fun newArray(size: Int): Array<Buyer?> {
            return arrayOfNulls(size)
        }
    }
}