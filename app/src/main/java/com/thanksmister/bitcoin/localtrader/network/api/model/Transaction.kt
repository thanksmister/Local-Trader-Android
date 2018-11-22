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

import android.arch.persistence.room.Entity
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import timber.log.Timber

@Entity
class Transaction() : Parcelable {

    @SerializedName("txid")
    var txid: String? = null

    @SerializedName("amount")
    var amount: String? = null

    @SerializedName("description")
    var description: String? = null

    @SerializedName("tx_type")
    var txType: String? = null

    @SerializedName("type")
    var type: Int? = null

    @SerializedName("created_at")
    var createdAt: String? = null

    val transactionType: TransactionType
     get() {
         if (description != null && description!!.toLowerCase().contains("fee")) {
             return TransactionType.FEE
         } else if (description != null && (description!!.toLowerCase().contains("contact")
                         || description!!.toLowerCase().contains("bitcoin sell"))) {
             return TransactionType.CONTACT_SENT
         } else if (description != null && description!!.toLowerCase().contains("internal")) {
             return TransactionType.INTERNAL
         } else if (description != null && description!!.toLowerCase().contains("reserve")) {
             return TransactionType.SENT
         } else if (description != null && description!!.toLowerCase().contains("payout")) {
             return TransactionType.AFFILIATE
         } else if (description != null && WalletUtils.validBitcoinAddress(description)) {
             return TransactionType.RECEIVED
         }
         return TransactionType.SENT
     }

    constructor(parcel: Parcel) : this() {
        txid = parcel.readString()
        amount = parcel.readString()
        description = parcel.readString()
        txType = parcel.readString()
        type = parcel.readValue(Int::class.java.classLoader) as? Int
        createdAt = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(txid)
        parcel.writeString(amount)
        parcel.writeString(description)
        parcel.writeString(txType)
        parcel.writeValue(type)
        parcel.writeString(createdAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Transaction> {
        override fun createFromParcel(parcel: Parcel): Transaction {
            return Transaction(parcel)
        }

        override fun newArray(size: Int): Array<Transaction?> {
            return arrayOfNulls(size)
        }
    }
}