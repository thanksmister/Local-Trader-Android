/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */
package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.*
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