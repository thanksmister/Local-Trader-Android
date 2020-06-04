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