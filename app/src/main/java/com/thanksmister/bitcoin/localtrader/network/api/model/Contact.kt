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


import androidx.annotation.NonNull
import androidx.room.*
import com.google.gson.annotations.SerializedName


@Entity(tableName = "Contacts", indices = [(Index(value = arrayOf("contactId"), unique = true))])
class Contact {

    @PrimaryKey
    @ColumnInfo(name = "contactId")
    @SerializedName("contact_id")
    @NonNull
    var contactId: Int = 0

    @ColumnInfo(name = "referenceCode")
    @SerializedName("reference_code")
    var referenceCode: String? = null

    @ColumnInfo(name = "currency")
    @SerializedName("currency")
    var currency: String? = null

    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    var amount: String? = null

    @ColumnInfo(name = "amountBtc")
    @SerializedName("amount_btc")
    var amountBtc: String? = null

    @ColumnInfo(name = "isFunded")
    @SerializedName("is_funded")
    var isFunded: Boolean = false //contacts with escrow enabled and funded

    @ColumnInfo(name = "isSelling")
    @SerializedName("is_selling")
    var isSelling: Boolean = false // you are selling

    @ColumnInfo(name = "isBuying")
    @SerializedName("is_buying")
    var isBuying: Boolean = false // you are buying

    @ColumnInfo(name = "createdAt")
    @SerializedName("created_at")
    var createdAt: String? = null

    @ColumnInfo(name = "closedAt")
    @SerializedName("closed_at")
    var closedAt: String? = null

    @ColumnInfo(name = "disputedAt")
    @SerializedName("disputed_at")
    var disputedAt: String? = null

    @ColumnInfo(name = "fundedAt")
    @SerializedName("funded_at")
    var fundedAt: String? = null

    @ColumnInfo(name = "escrowedAt")
    @SerializedName("escrowed_at")
    var escrowedAt: String? = null

    @ColumnInfo(name = "canceledAt")
    @SerializedName("canceled_at")
    var canceledAt: String? = null

    @ColumnInfo(name = "releasedAt")
    @SerializedName("released_at")
    var releasedAt: String? = null

    @ColumnInfo(name = "paymentCompletedAt")
    @SerializedName("payment_completed_at")
    var paymentCompletedAt: String? = null

    @ColumnInfo(name = "exchangeRateUpdatedAt")
    @SerializedName("exchange_rate_updated_at")
    var exchangeRateUpdatedAt: String? = null

    @SerializedName("seller")
    @TypeConverters(SellerConverter::class)
    @ColumnInfo(name = "seller")
    var seller: Seller = Seller()

    @SerializedName("buyer")
    @TypeConverters(BuyerConverter::class)
    @ColumnInfo(name = "buyer")
    var buyer:Buyer = Buyer()

    @SerializedName("advertisement")
    @TypeConverters(AdvertisementConverter::class)
    @ColumnInfo(name = "advertisement")
    var advertisement:ContactAdvertisement = ContactAdvertisement()

    @SerializedName("actions")
    @TypeConverters(ActionsConverter::class)
    @ColumnInfo(name = "actions")
    var actions: Actions = Actions()

    @SerializedName("account_details")
    @ColumnInfo(name = "accountDetails")
    @TypeConverters(AccountDetailsConverter::class)
    var accountDetails:AccountDetails = AccountDetails()
}