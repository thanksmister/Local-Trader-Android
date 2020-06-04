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
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Messages", indices = [(Index(value = arrayOf("contactId"), unique = false)),
    (Index(value = arrayOf("createdAt"), unique = true))])
class Message {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @SerializedName("msg")
    @ColumnInfo(name = "message")
    var message: String? = null

    @SerializedName("contact_id")
    @ColumnInfo(name = "contactId")
    var contactId: Int = 0

    @SerializedName("created_at")
    @ColumnInfo(name = "createdAt")
    var createdAt: String? = null

    @SerializedName("is_admin")
    @ColumnInfo(name = "isAdmin")
    var isAdmin: Boolean? = null

    @SerializedName("attachment_name")
    @ColumnInfo(name = "attachmentName")
    var attachmentName: String? = null

    @SerializedName("attachment_type")
    @ColumnInfo(name = "attachmentType")
    var attachmentType: String? = null

    @SerializedName("attachment_url")
    @ColumnInfo(name = "attachmentUrl")
    var attachmentUrl: String? = null

    @SerializedName("sender")
    @ColumnInfo(name = "sender")
    @TypeConverters(SenderConverter::class)
    var sender = Sender()
}