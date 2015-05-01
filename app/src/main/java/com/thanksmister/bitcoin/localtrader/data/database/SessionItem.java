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

import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import rx.functions.Func1;

import static com.squareup.sqlbrite.SqlBrite.Query;

/**
 * https://github.com/square/sqlbrite/
 */
@AutoParcel
public abstract class SessionItem
{
    public static final String TABLE = "session_item";

    public static final String ID = "_id";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    
    public static final String QUERY = "SELECT * FROM "
            + SessionItem.TABLE + " LIMIT 1";
    
    public abstract long id();
    public abstract String access_token();
    public abstract String refresh_token();

    public static final Func1<Query, SessionItem> MAP = new Func1<Query, SessionItem>() {
        @Override
        public SessionItem call(Query query) {
            Cursor cursor = query.run();
            try {
                if(cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    long id = Db.getLong(cursor, ID);
                    String access_token = Db.getString(cursor, ACCESS_TOKEN);
                    String refresh_token = Db.getString(cursor, REFRESH_TOKEN);
                    return new AutoParcel_SessionItem(id, access_token, refresh_token);
                }
                return null;
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
        
        public Builder access_token(String value) {
            values.put(ACCESS_TOKEN, value);
            return this;
        }
        
        public Builder refresh_token(String value) {
            values.put(REFRESH_TOKEN, value);
            return this;
        }

        public ContentValues build() {
            return values; 
        }
    }
}
