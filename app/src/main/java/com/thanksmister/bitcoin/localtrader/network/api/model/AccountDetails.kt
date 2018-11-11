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
 */

package com.thanksmister.bitcoin.localtrader.network.api.model

import android.arch.persistence.room.Entity
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity
class AccountDetails() : Parcelable {

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("receiver_name")
    @Expose
    var receiverName: String? = null

    @SerializedName("receiver_email")
    @Expose
    var receiverEmail: String? = null

    @SerializedName("ethereum_address")
    @Expose
    var ethereumAddress: String? = null

    @SerializedName("iban")
    @Expose
    var iban: String? = null

    @SerializedName("swift_bic")
    @Expose
    var swiftBic: String? = null

    @SerializedName("reference")
    @Expose
    var reference: String? = null

    @SerializedName("phone_number")
    @Expose
    var phoneNumber: String? = null

    @SerializedName("biller_code")
    @Expose
    var billerCode: String? = null

    @SerializedName("account_number")
    @Expose
    var accountNumber: String? = null

    @SerializedName("message")
    @Expose
    var message: String? = null

    @SerializedName("sort_code")
    @Expose
    var sortCode: String? = null

    @SerializedName("bsb")
    @Expose
    var bsb: String? = null

    constructor(parcel: Parcel) : this() {
        username = parcel.readString()
        receiverName = parcel.readString()
        receiverEmail = parcel.readString()
        ethereumAddress = parcel.readString()
        iban = parcel.readString()
        swiftBic = parcel.readString()
        reference = parcel.readString()
        phoneNumber = parcel.readString()
        billerCode = parcel.readString()
        accountNumber = parcel.readString()
        message = parcel.readString()
        sortCode = parcel.readString()
        bsb = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeString(receiverName)
        parcel.writeString(receiverEmail)
        parcel.writeString(ethereumAddress)
        parcel.writeString(iban)
        parcel.writeString(swiftBic)
        parcel.writeString(reference)
        parcel.writeString(phoneNumber)
        parcel.writeString(billerCode)
        parcel.writeString(accountNumber)
        parcel.writeString(message)
        parcel.writeString(sortCode)
        parcel.writeString(bsb)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AccountDetails> {
        override fun createFromParcel(parcel: Parcel): AccountDetails {
            return AccountDetails(parcel)
        }

        override fun newArray(size: Int): Array<AccountDetails?> {
            return arrayOfNulls(size)
        }
    }
}