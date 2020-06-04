/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */
package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


@Entity(tableName = "Notifications", indices = [(Index(value = arrayOf("notificationId"), unique = true))])
class Notification {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "notificationId")
    @SerializedName("id")
    var notificationId: String? = null

    @ColumnInfo(name = "message")
    @SerializedName("msg")
    @Expose
    var message: String? = null

    @ColumnInfo(name = "contactId")
    @SerializedName("contact_id")
    var contactId: Int? = null

    @ColumnInfo(name = "advertisementId")
    @SerializedName("advertisement_id")
    var advertisementId: Int? = null

    @ColumnInfo(name = "createdAt")
    @SerializedName("created_at")
    var createdAt: String? = null

    @ColumnInfo(name = "url")
    @SerializedName("url")
    var url: String? = null

    @ColumnInfo(name = "read")
    @SerializedName("read")
    var read: Boolean = false
}