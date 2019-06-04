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
 *
 */
package com.thanksmister.bitcoin.localtrader.network.api.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull
import android.support.annotation.Nullable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

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