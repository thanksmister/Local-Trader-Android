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

package com.thanksmister.bitcoin.localtrader.persistence

import android.arch.persistence.room.*
import com.google.gson.annotations.SerializedName
import io.reactivex.annotations.NonNull

@Entity(tableName = "Notification")
class Notification {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @SerializedName("id")
    @ColumnInfo(name = "id")
    @NonNull
    var id: String = "0"

    @SerializedName("contact_id")
    @ColumnInfo(name = "contact_id")
    var contactId: String? = null

    @SerializedName("advertisement_id")
    @ColumnInfo(name = "advertisement_id")
    var advertisementId: String? = null

    @ColumnInfo(name = "read")
    @SerializedName("read")
    var read: Boolean = false

    @ColumnInfo(name = "msg")
    @SerializedName("msg")
    var msg: String? = null

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    var createdAt: String? = null

    @ColumnInfo(name = "url")
    @SerializedName("url")
    var url: String? = null
}