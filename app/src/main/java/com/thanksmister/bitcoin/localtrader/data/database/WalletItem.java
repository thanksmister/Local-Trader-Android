/*
 * Copyright (c) 2015 ThanksMister LLC
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
package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcelable;

import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;

import java.io.ByteArrayOutputStream;

import auto.parcel.AutoParcel;
import rx.functions.Func1;

import static com.squareup.sqlbrite.SqlBrite.Query;

/**
 * https://github.com/square/sqlbrite/
 */
@AutoParcel
public abstract class WalletItem implements Parcelable
{
    public static final String TABLE = "wallet_item";

    public static final String ID = "_id";
    public static final String MESSAGE = "message";
    public static final String BALANCE = "balance";
    public static final String SENDABLE = "sendable";
    public static final String ADDRESS = "address";
    public static final String RECEIVABLE = "receivable";
    public static final String QRCODE = "qrcode";
    
    public abstract long id();
    public abstract String message();
    public abstract String balance();
    public abstract String sendable();
    public abstract String address();
    public abstract byte[] qrcode();

    public static final Func1<Query, WalletItem> MAP = new Func1<Query, WalletItem>() {
        @Override
        public WalletItem call(Query query) {
            Cursor cursor = query.run();
            try {
                if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                    long id = Db.getLong(cursor, ID);
                    String message = Db.getString(cursor, MESSAGE);
                    String balance = Db.getString(cursor, BALANCE);
                    String sendable = Db.getString(cursor, SENDABLE);
                    String address = Db.getString(cursor, ADDRESS);
                    byte[] qrcode = Db.getBlob(cursor, QRCODE);
                    return new AutoParcel_WalletItem(id, message, balance, sendable, address, qrcode);
                }
                
                return null;
                
            } finally {
                cursor.close();
            }
        }
    };

    public static Builder createBuilder(Wallet wallet, ByteArrayOutputStream baos)
    {
        return new Builder()
                .message(wallet.message)
                .balance(wallet.balance)
                .sendable(wallet.sendable)
                .address(wallet.address)
                .qrcode(baos.toByteArray());
    }

    public static final class Builder {
        
        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(ID, id);
            return this;
        }
       
        public Builder message(String value) {
            values.put(MESSAGE, value);
            return this;
        }
        public Builder balance(String value) {
            values.put(BALANCE, value);
            return this;
        }
        public Builder sendable(String value) {
            values.put(SENDABLE, value);
            return this;
        }
        public Builder address(String value) {
            values.put(ADDRESS, value);
            return this;
        }
        public Builder qrcode(byte[] value) {
            values.put(QRCODE, value);
            return this;
        }
        
        public ContentValues build() {
            return values; 
        }
    }
}
