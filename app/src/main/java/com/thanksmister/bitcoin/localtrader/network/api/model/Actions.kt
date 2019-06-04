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