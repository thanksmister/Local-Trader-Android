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