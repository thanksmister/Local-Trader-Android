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
import android.support.annotation.NonNull
import com.google.gson.annotations.SerializedName
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils

@Entity(tableName = "Advertisements", indices = [(Index(value = arrayOf("adId"), unique = true))])
class Advertisement {

    @PrimaryKey
    @ColumnInfo(name = "adId")
    @SerializedName("ad_id")
    @NonNull
    var adId: Int = 0

    @SerializedName("created_at")
    @ColumnInfo(name = "createdAt")
    var createdAt: String? = null

    @SerializedName("visible")
    @ColumnInfo(name = "visible")
    var visible = true

    @SerializedName("email")
    @ColumnInfo(name = "email")
    var email: String? = null

    @ColumnInfo(name = "locationString")
    @SerializedName("location_string")
    var location: String? = null

    @ColumnInfo(name = "limitToFiatAmount")
    @SerializedName("limit_to_fiat_amounts")
    var limitToFiatAmount: String? = null

    @ColumnInfo(name = "countryCode")
    @SerializedName("countrycode")
    var countryCode: String? = null

    @SerializedName("city")
    @ColumnInfo(name = "city")
    var city: String? = null

    @SerializedName("trade_type")
    @ColumnInfo(name = "tradeType")
    var tradeType: String = TradeType.NONE.name

    @SerializedName("min_amount")
    @ColumnInfo(name = "minAmount")
    var minAmount: String? = null

    @SerializedName("max_amount")
    @ColumnInfo(name = "maxAmount")
    var maxAmount: String? = null

    @SerializedName("max_amount_available")
    @ColumnInfo(name = "maxAmountAvailable")
    var maxAmountAvailable: String? = null

    @SerializedName("price_equation")
    @ColumnInfo(name = "priceEquation")
    var priceEquation: String? = null

    @SerializedName("currency")
    @ColumnInfo(name = "currency")
    var currency = "USD"

    @SerializedName("account_info")
    @ColumnInfo(name = "accountInfo")
    var accountInfo: String? = null

    @SerializedName("msg")
    @ColumnInfo(name = "message")
    var message: String? = null

    @SerializedName("lat")
    @ColumnInfo(name = "lat")
    var lat: Double = 0.toDouble()

    @SerializedName("lon")
    @ColumnInfo(name = "lon")
    var lon: Double = 0.toDouble()

    @SerializedName("temp_price")
    @ColumnInfo(name = "tempPrice")
    var tempPrice: String? = null

    @SerializedName("temp_price_usd")
    @ColumnInfo(name = "tempPriceUsd")
    var tempPriceUsd: String? = null

    @SerializedName("bank_name")
    @ColumnInfo(name = "bankName")
    var bankName: String? = null

    @SerializedName("nextUrl")
    @ColumnInfo(name = "nextUrl")
    var nextUrl: String? = null

    @SerializedName("atm_model")
    @ColumnInfo(name = "atmModel")
    var atmModel: String? = null

    @SerializedName("track_max_amount")
    @ColumnInfo(name = "trackMaxAmount")
    var trackMaxAmount = false

    @SerializedName("sms_verification_required")
    @ColumnInfo(name = "smsVerificationRequired")
    var smsVerificationRequired = false

    @SerializedName("trusted_required")
    @ColumnInfo(name = "trustedRequired")
    var trustedRequired = false

    @SerializedName("require_identification")
    @ColumnInfo(name = "requireIdentification")
    var requireIdentification = false

    @SerializedName("online_provider")
    @ColumnInfo(name = "onlineProvider")
    var onlineProvider: String? = TradeUtils.NATIONAL_BANK

    @SerializedName("distance")
    @ColumnInfo(name = "distance")
    var distance: String? = null

    @SerializedName("require_trade_volume")
    @ColumnInfo(name = "requireTradeVolume")
    var requireTradeVolume: String? = null

    @SerializedName("first_time_limit_btc")
    @ColumnInfo(name = "firstTimeLimitBtc")
    var firstTimeLimitBtc: String? = null

    @SerializedName("require_feedback_score")
    @ColumnInfo(name = "requireFeedbackScore")
    var requireFeedbackScore: String? = null

    @SerializedName("reference_type")
    @ColumnInfo(name = "referenceType")
    var referenceType: String? = null

    @SerializedName("phone_number")
    @ColumnInfo(name = "phoneNumber")
    var phoneNumber: String? = null

    @SerializedName("opening_hours")
    @ColumnInfo(name = "openingHours")
    var openingHours: String? = null

    @SerializedName("require_trusted_by_advertiser")
    @ColumnInfo(name = "requireTrustedByAdvertiser")
    var requireTrustedByAdvertiser = false

    @SerializedName("is_local_office")
    @ColumnInfo(name = "isLocalOffice")
    var isLocalOffice = false

    @SerializedName("hidden_by_opening_hours")
    @ColumnInfo(name = "hiddenByOpeningHours")
    var hiddenByOpeningHours = false

    @SerializedName("is_low_risk")
    @ColumnInfo(name = "isLowRisk")
    var isLowRisk = false

    @SerializedName("payment_window_minutes")
    @ColumnInfo(name = "paymentWindowMinutes")
    var paymentWindowMinutes: Int = 0

    @SerializedName("age_days_coefficient_limit")
    @ColumnInfo(name = "ageDaysCoefficientLimit")
    var ageDaysCoefficientLimit: String? = null

    @SerializedName("volume_coefficient_btc")
    @ColumnInfo(name = "volumeCoefficientBtc")
    var volumeCoefficientBtc: String? = null

    @SerializedName("floating")
    @ColumnInfo(name = "floating")
    var floating: Boolean = false

    @SerializedName("display_reference")
    @ColumnInfo(name = "displayReference")
    var displayReference: Boolean = false

    @SerializedName("profile")
    @TypeConverters(ProfileConverter::class)
    @ColumnInfo(name = "profile")
    var profile: Profile = Profile()

    @SerializedName("actions")
    @TypeConverters(ActionsConverter::class)
    @ColumnInfo(name = "actions")
    var actions: Actions = Actions()

    val isATM: Boolean
        get() = atmModel != null
}
