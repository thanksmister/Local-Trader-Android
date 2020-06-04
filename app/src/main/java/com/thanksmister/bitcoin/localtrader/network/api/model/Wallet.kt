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
import android.text.TextUtils
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.thanksmister.bitcoin.localtrader.utils.ISO8601

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Date

@Entity(tableName = "Wallet")
class Wallet {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @SerializedName("message")
    @ColumnInfo(name = "message")
    @Expose
    var message: String? = null

    @SerializedName("sent_transactions_30d")
    @ColumnInfo(name = "sentTransactions")
    @TypeConverters(TransactionConverter::class)
    var sentTransactions = ArrayList<Transaction>()

    @SerializedName("received_transactions_30d")
    @ColumnInfo(name = "receivingTransactions")
    @TypeConverters(TransactionConverter::class)
    var receivingTransactions = ArrayList<Transaction>()

    @SerializedName("receiving_address_count")
    @ColumnInfo(name = "receivingAddressCount")
    @Expose
    var receivingAddressCount: Int = 0

    @SerializedName("receiving_address_list")
    @ColumnInfo(name = "receivingAddressList")
    @TypeConverters(AddressConverter::class)
    var receivingAddressList = ArrayList<Address>()

    @SerializedName("total")
    @ColumnInfo(name = "total")
    @TypeConverters(TotalConverter::class)
    var total: Total = Total()

    @SerializedName("receiving_address")
    @ColumnInfo(name = "receivingAddress")
    @Expose
    var receivingAddress: String? = null

    // Convenience method to get last address
    val address:String?
        get() {
            if(!TextUtils.isEmpty(receivingAddress)) {
                return receivingAddress
            } else if(receivingAddressList.isNotEmpty()) {
                return receivingAddressList[0].address
            }
            return null
        }

    // Convenience method to list all transactions
    val transactions: List<Transaction>
        get() {
            val transactions = ArrayList<Transaction>()
            if (!sentTransactions.isEmpty()) {
                transactions.addAll(sentTransactions)
            }
            if (!receivingTransactions.isEmpty()) {
                transactions.addAll(receivingTransactions)
            }
            Collections.sort(transactions, Comparator { t1, t2 ->
                var d1: Date? = null
                var d2: Date? = null
                try {
                    d1 = ISO8601.toCalendar(t1.createdAt).time
                    d2 = ISO8601.toCalendar(t2.createdAt).time
                } catch (e: java.text.ParseException) {
                    e.printStackTrace()
                }

                if (d1 == null || d2 == null)
                    return@Comparator -1

                if (d1.time > d2.time) -1 else 1
            })
            return transactions
        }
}