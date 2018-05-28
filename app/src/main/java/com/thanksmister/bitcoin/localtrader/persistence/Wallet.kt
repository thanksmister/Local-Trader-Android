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
import android.arch.persistence.room.ColumnInfo
import android.os.Parcel
import android.os.Parcelable

@Entity(tableName = "Wallet")
class Wallet()  {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "receiving_address")
    @SerializedName("receiving_address")
    var address: String? = null

    @ColumnInfo(name = "message")
    @SerializedName("message")
    var message: String? = null

    @SerializedName("total")
    @TypeConverters(TotalConverter::class)
    @ColumnInfo(name = "total")
    var total: Total? = null

    @SerializedName("sent_transactions_30d")
    @TypeConverters(TransactionConverter::class)
    @ColumnInfo(name = "sent_transactions")
    var sentTransactions: ArrayList<AddressTransaction>? = null

    @SerializedName("received_transactions_30d")
    @TypeConverters(TransactionConverter::class)
    @ColumnInfo(name = "received_transactions")
    var receivedTransactions: ArrayList<AddressTransaction>? = null

    @SerializedName("receiving_address_list")
    @TypeConverters(ReceivingAddressConverter::class)
    @ColumnInfo(name = "receiving_address_list")
    var receivingAddressList: ArrayList<ReceivingAddress>? = null
}