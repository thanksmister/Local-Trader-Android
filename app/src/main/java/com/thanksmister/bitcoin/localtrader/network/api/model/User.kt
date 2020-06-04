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

@Entity(tableName = "User")
class User {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @SerializedName("username")
    @ColumnInfo(name = "username")
    @Expose
    var username: String? = null

    @SerializedName("age_text")
    @ColumnInfo(name = "age_text")
    @Expose
    var ageText: String? = null

    @SerializedName("trading_partners_count")
    @ColumnInfo(name = "trading_partners_count")
    @Expose
    var tradingPartnersCount: Int = 0

    @SerializedName("feedbacks_unconfirmed_count")
    @ColumnInfo(name = "feedbacks_unconfirmed_count")
    @Expose
    var feedbacksUnconfirmedCount: Int = 0

    @SerializedName("trade_volume_text")
    @ColumnInfo(name = "trade_volume_text")
    @Expose
    var tradeVolumeText: String? = null

    @SerializedName("has_common_trades")
    @ColumnInfo(name = "has_common_trades")
    @Expose
    var hasCommonTrades: Boolean = false

    @SerializedName("confirmed_trade_count_text")
    @ColumnInfo(name = "confirmed_trade_count_text")
    @Expose
    var confirmedTradeCountText: String? = null

    @SerializedName("blocked_count")
    @ColumnInfo(name = "blocked_count")
    @Expose
    var blockedCount: Int = 0

    @SerializedName("feedback_count")
    @ColumnInfo(name = "feedback_count")
    @Expose
    var feedbackCount: Int = 0

    @SerializedName("feedback_score")
    @ColumnInfo(name = "feedback_score")
    @Expose
    var feedbackScore: String? = null

    @SerializedName("trusted_count")
    @ColumnInfo(name = "trusted_count")
    @Expose
    var trustedCount: Int = 0

    @SerializedName("url")
    @ColumnInfo(name = "url")
    @Expose
    var url: String? = null

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    @Expose
    var createdAt: String? = null
}
