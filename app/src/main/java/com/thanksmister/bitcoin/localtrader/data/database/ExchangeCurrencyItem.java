/*
 * Copyright (c) 2017 ThanksMister LLC
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

import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;

import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import rx.functions.Func1;

import static com.squareup.sqlbrite.SqlBrite.Query;

@AutoParcel
public abstract class ExchangeCurrencyItem implements Parcelable
{
    public static final String TABLE = "exchange_currency_item";
    
    public static final String ID = "_id";
    public static final String CURRENCY = "currency";

    public static final String QUERY = "SELECT * FROM " + ExchangeCurrencyItem.TABLE;

    public abstract long id();
    public abstract String currency();
    
    public static final Func1<Query, List<ExchangeCurrencyItem>> MAP = new Func1<Query, List<ExchangeCurrencyItem>>() {
        @Override
        public List<ExchangeCurrencyItem> call(Query query) {
            Cursor cursor = query.run();
            List<ExchangeCurrencyItem> values = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, ID);
                String currency = Db.getString(cursor, CURRENCY);
                values.add(new AutoParcel_ExchangeCurrencyItem(id, currency));
            }
            return values;
        }
    };
    
    public static List<ExchangeCurrency> getCurrencies(List<CurrencyItem> currencyItems) {
        List<ExchangeCurrency> exchangeCurrencies = new ArrayList<>();
        for(CurrencyItem exchangeCurrencyItem : currencyItems) {
            ExchangeCurrency exchangeCurrency = new ExchangeCurrency(exchangeCurrencyItem.currency());
            exchangeCurrencies.add(exchangeCurrency);
        }
        return exchangeCurrencies;
    }

    public static Builder createBuilder(ExchangeCurrency item) {
        return new Builder()
                .currency(item.getCurrency());
    }
    
    public static final class Builder {
        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(ID, id);
            return this;
        }
        
        public Builder currency(String value) {
            values.put(CURRENCY, value);
            return this;
        }
        
        public ContentValues build() {
            return values; 
        }
    }
}