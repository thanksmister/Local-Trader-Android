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
package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcelable;

import auto.parcel.AutoParcel;
import rx.functions.Func1;

import static com.squareup.sqlbrite.SqlBrite.Query;

/**
 * https://github.com/square/sqlbrite/
 */

@AutoParcel
public abstract class ExchangeItem implements Parcelable {
    public static final String TABLE = "exchange_item";

    public static final String ID = "_id";
    public static final String EXCHANGE = "exchange";
    public static final String ASK = "ask";
    public static final String BID = "bid";
    public static final String LAST = "last";

    //public static final String QUERY = "SELECT * FROM " + ExchangeItem.TABLE + " LIMIT 1";
    public static final String QUERY = "SELECT * FROM " + ExchangeItem.TABLE;

    public abstract long id();

    public abstract String exchange();

    public abstract String ask();

    public abstract String bid();

    public abstract String last();

    public static final Func1<Query, ExchangeItem> MAP = new Func1<Query, ExchangeItem>() {
        @Override
        public ExchangeItem call(Query query) {
            Cursor cursor = query.run();
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    long id = Db.getLong(cursor, ID);
                    String exchange = Db.getString(cursor, EXCHANGE);
                    String ask = Db.getString(cursor, ASK);
                    String bid = Db.getString(cursor, BID);
                    String last = Db.getString(cursor, LAST);
                    return new AutoParcel_ExchangeItem(id, exchange, ask, bid, last);
                } else {
                    return null;
                }
            } finally {
                cursor.close();
            }
        }
    };

    public static final class Builder {
        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(ID, id);
            return this;
        }

        public Builder exchange(String value) {
            values.put(EXCHANGE, value);
            return this;
        }

        public Builder ask(String value) {
            values.put(ASK, value);
            return this;
        }

        public Builder bid(String value) {
            values.put(BID, value);
            return this;
        }

        public Builder last(String value) {
            values.put(LAST, value);
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }
}
