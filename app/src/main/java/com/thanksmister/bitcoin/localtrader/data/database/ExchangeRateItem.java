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

import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;

import java.util.ArrayList;
import java.util.List;

public abstract class ExchangeRateItem implements Parcelable {
    public static final String TABLE = "exchange_rate_item";

    public static final String ID = "_id";
    static final String EXCHANGE = "exchange";
    static final String CURRENCY = "currency";
    static final String RATE = "rate";

    public static final String QUERY = "SELECT * FROM " + ExchangeRateItem.TABLE;

    public abstract long id();

    public abstract String exchange();

    public abstract String rate();

    public abstract String currency();

    /*public static final Func1<Query, List<ExchangeRateItem>> MAP = new Func1<Query, List<ExchangeRateItem>>() {
        @Override
        public List<ExchangeRateItem> call(Query query) {
            Cursor cursor = query.run();
            try {
                List<ExchangeRateItem> values = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    long id = Db.getLong(cursor, ID);
                    String exchange = Db.getString(cursor, EXCHANGE);
                    String currency = Db.getString(cursor, CURRENCY);
                    String rate = Db.getString(cursor, RATE);
                    values.add(new AutoParcel_ExchangeRateItem(id, exchange, rate, currency));
                }
                return values;
            } finally {
                cursor.close();
            }
        }
    };

    public static final Func1<Query, ExchangeRateItem> MAP_SINGLE = new Func1<Query, ExchangeRateItem>() {
        @Override
        public ExchangeRateItem call(Query query) {
            Cursor cursor = query.run();
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, ID);
                String exchange = Db.getString(cursor, EXCHANGE);
                String currency = Db.getString(cursor, CURRENCY);
                String rate = Db.getString(cursor, RATE);
                return new AutoParcel_ExchangeRateItem(id, exchange, rate, currency);
            }
            return null;
        }
    };*/

    public static Builder createBuilder(ExchangeRate item) {
        return new Builder()
                .exchange(item.getDisplay_name())
                .rate(item.getRate())
                .currency(item.getCurrency());
    }

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

        public Builder currency(String value) {
            values.put(CURRENCY, value);
            return this;
        }

        public Builder rate(String value) {
            values.put(RATE, value);
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }
}