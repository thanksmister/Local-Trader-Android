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
import android.support.annotation.Nullable;

import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.network.api.model.TransactionType;

import java.util.ArrayList;
import java.util.List;


public abstract class TransactionItem {
    public static final String TABLE = "transaction_item";

    public static final String ID = "_id";
    public static final String TRANSACTION_ID = "tx_id";
    public static final String AMOUNT = "amount";
    public static final String DESCRIPTION = "description";
    public static final String TRANSACTION_TYPE = "tx_type";
    public static final String CREATED_AT = "created_at";

    public static final String QUERY = "SELECT * FROM "
            + TransactionItem.TABLE
            + " ORDER BY "
            + TransactionItem.CREATED_AT
            + " DESC";

    public abstract long id();

    public abstract String tx_id();

    @Nullable
    public abstract String amount();

    @Nullable
    public abstract String description();

    public abstract TransactionType tx_type();

    public abstract String created_at();

    /*public static final Func1<SqlBrite.Query, List<TransactionItem>> MAP = new Func1<SqlBrite.Query, List<TransactionItem>>() {

        @Override
        public List<TransactionItem> call(SqlBrite.Query query) {

            Cursor cursor = query.run();

            try {
                List<TransactionItem> values = new ArrayList<>(cursor.getCount());

                while (cursor.moveToNext()) {

                    long id = Db.getLong(cursor, ID);
                    String tx_id = Db.getString(cursor, TRANSACTION_ID);
                    String amount = Db.getString(cursor, AMOUNT);
                    String description = Db.getString(cursor, DESCRIPTION);
                    TransactionType tx_type = TransactionType.valueOf(Db.getString(cursor, TRANSACTION_TYPE));
                    String created_at = Db.getString(cursor, CREATED_AT);

                    values.add(new AutoParcel_TransactionItem(id, tx_id, amount, description, tx_type, created_at));
                }
                return values;
            } finally {
                cursor.close();
            }
        }
    };*/

    public static Builder createBuilder(Transaction item) {
        return new Builder()
                .tx_id(item.txid)
                .amount(item.amount)
                .description(item.description)
                .tx_type(item.type)
                .created_at(item.created_at);
    }

    public static final class Builder {

        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(ID, id);
            return this;
        }

        public Builder tx_id(String value) {
            values.put(TRANSACTION_ID, value);
            return this;
        }

        public Builder amount(String value) {
            values.put(AMOUNT, value);
            return this;
        }

        public Builder description(String value) {
            values.put(DESCRIPTION, value);
            return this;
        }

        public Builder tx_type(TransactionType value) {
            values.put(TRANSACTION_TYPE, value.name());
            return this;
        }

        public Builder created_at(String value) {
            values.put(CREATED_AT, value);
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }
}

