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
class Actions() : Parcelable {

    @SerializedName("html_form")
    @Expose
    var htmlForm: String? = null

    @SerializedName("public_view")
    @Expose
    var publicView: String? = null

    @SerializedName("contact_form")
    @Expose
    var contactForm: String? = null

    @SerializedName( "message_post_url")
    @Expose
    var messagePostUrl: String? = null

    @SerializedName("dispute_url")
    @Expose
    var disputeUrl: String? = null

    @SerializedName("fund_url")
    @Expose
    var fundUrl: String? = null

    @SerializedName("messages_url")
    @Expose
    var messagesUrl: String? = null

    @SerializedName("advertisement_url")
    @Expose
    var advertisementUrl: String? = null

    @SerializedName("release_url")
    @Expose
    var releaseUrl: String? = null

    @SerializedName("mark_as_paid_url")
    @Expose
    var markAsPaidUrl: String? = null

    @SerializedName("cancel_url")
    @Expose
    var cancelUrl: String? = null

    @SerializedName("advertisement_public_view")
    @Expose
    var advertisementPublicView: String? = null

    constructor(parcel: Parcel) : this() {
        htmlForm = parcel.readString()
        publicView = parcel.readString()
        contactForm = parcel.readString()
        messagePostUrl = parcel.readString()
        disputeUrl = parcel.readString()
        fundUrl = parcel.readString()
        messagesUrl = parcel.readString()
        advertisementUrl = parcel.readString()
        releaseUrl = parcel.readString()
        markAsPaidUrl = parcel.readString()
        cancelUrl = parcel.readString()
        advertisementPublicView = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(htmlForm)
        parcel.writeString(publicView)
        parcel.writeString(contactForm)
        parcel.writeString(messagePostUrl)
        parcel.writeString(disputeUrl)
        parcel.writeString(fundUrl)
        parcel.writeString(messagesUrl)
        parcel.writeString(advertisementUrl)
        parcel.writeString(releaseUrl)
        parcel.writeString(markAsPaidUrl)
        parcel.writeString(cancelUrl)
        parcel.writeString(advertisementPublicView)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Actions> {
        override fun createFromParcel(parcel: Parcel): Actions {
            return Actions(parcel)
        }

        override fun newArray(size: Int): Array<Actions?> {
            return arrayOfNulls(size)
        }
    }
}