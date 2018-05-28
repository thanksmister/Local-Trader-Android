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
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

@Entity(tableName = "User")
class User()  {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "username")
    @SerializedName("username")
    var username: String? = null

    @ColumnInfo(name = "age_text")
    @SerializedName("age_text")
    var age_text: String? = null

    @ColumnInfo(name = "trading_partners_count")
    @SerializedName("trading_partners_count")
    var trading_partners_count: Int = 0

    @ColumnInfo(name = "feedbacks_unconfirmed_count")
    @SerializedName("feedbacks_unconfirmed_count")
    var feedbacks_unconfirmed_count: Int = 0

    @ColumnInfo(name = "trade_volume_text")
    @SerializedName("trade_volume_text")
    var trade_volume_text: String? = null

    @ColumnInfo(name = "has_common_trades")
    @SerializedName("has_common_trades")
    var has_common_trades: Boolean = false

    @ColumnInfo(name = "confirmed_trade_count_text")
    @SerializedName("confirmed_trade_count_text")
    var confirmed_trade_count_text: String? = null

    @ColumnInfo(name = "blocked_count")
    @SerializedName("blocked_count")
    var blocked_count: Int = 0

    @ColumnInfo(name = "feedback_count")
    @SerializedName("feedback_count")
    var feedback_count: Int = 0

    @ColumnInfo(name = "feedback_score")
    @SerializedName("feedback_score")
    var feedback_score: Int = 0

    @ColumnInfo(name = "trusted_count")
    @SerializedName("trusted_count")
    var trusted_count: Int = 0

    @ColumnInfo(name = "url")
    @SerializedName("url")
    var url: String? = null

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    var created_at: String? = null

    @ColumnInfo(name = "real_name_verifications_trusted")
    @SerializedName("real_name_verifications_trusted")
    var real_name_verifications_trusted: String? = null

    @ColumnInfo(name = "real_name_verifications_rejected")
    @SerializedName("real_name_verifications_rejected")
    var real_name_verifications_rejected: String? = null

    @ColumnInfo(name = "real_name_verifications_untrusted")
    @SerializedName("real_name_verifications_untrusted")
    var real_name_verifications_untrusted: String? = null

    @ColumnInfo(name = "hasFeedback")
    @SerializedName("hasFeedback")
    var hasFeedback: String? = null

    @ColumnInfo(name = "identity_verified_at")
    @SerializedName("identity_verified_at")
    var identity_verified_at: String? = null
}