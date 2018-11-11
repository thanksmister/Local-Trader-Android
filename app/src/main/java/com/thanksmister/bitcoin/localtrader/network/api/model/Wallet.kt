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