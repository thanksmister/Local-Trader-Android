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
 *
 */
package com.thanksmister.bitcoin.localtrader.network.api.model

import android.arch.persistence.room.*
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