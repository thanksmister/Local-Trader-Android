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
package com.thanksmister.bitcoin.localtrader.network.api.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Transaction implements Parcelable {
    public String txid;
    public String amount;
    public String description;
    public String tx_type;
    public TransactionType type;
    public String created_at;

    public Transaction() {
    }

    public Transaction(Parcel parcel) {
        this();
        txid = parcel.readString();
        amount = parcel.readString();
        description = parcel.readString();
        type = TransactionType.valueOf(parcel.readString());
        created_at = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(txid);
        parcel.writeString(amount);
        parcel.writeString(description);
        parcel.writeString(type.name());
        parcel.writeString(created_at);
    }

    public static Creator<Transaction> CREATOR = new Creator<Transaction>() {
        public Transaction createFromParcel(Parcel parcel) {
            return new Transaction(parcel);
        }

        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };
}
