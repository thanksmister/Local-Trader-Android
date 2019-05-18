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

import android.arch.persistence.room.Entity
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