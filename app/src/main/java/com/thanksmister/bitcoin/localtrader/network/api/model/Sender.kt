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
class Sender() : Parcelable {

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("feedback_score")
    @Expose
    var feedbackScore: Int? = 0

    @SerializedName("last_online")
    @Expose
    var lastOnline: String? = null

    @SerializedName("trade_count")
    @Expose
    var tradeCount: String? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    constructor(parcel: Parcel) : this() {
        username = parcel.readString()
        feedbackScore = parcel.readValue(Int::class.java.classLoader) as? Int
        lastOnline = parcel.readString()
        tradeCount = parcel.readString()
        name = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeValue(feedbackScore)
        parcel.writeString(lastOnline)
        parcel.writeString(tradeCount)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Sender> {
        override fun createFromParcel(parcel: Parcel): Sender {
            return Sender(parcel)
        }

        override fun newArray(size: Int): Array<Sender?> {
            return arrayOfNulls(size)
        }
    }

}