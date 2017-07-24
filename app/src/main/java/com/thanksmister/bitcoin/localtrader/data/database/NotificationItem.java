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
import android.support.annotation.Nullable;

import com.thanksmister.bitcoin.localtrader.data.api.model.Notification;

import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import rx.functions.Func1;

import static com.squareup.sqlbrite.SqlBrite.Query;

/**
 * https://github.com/square/sqlbrite/
 */
@AutoParcel
public abstract class NotificationItem
{
    public static final String TABLE = "notification_item";

    public static final String ID = "_id";
    public static final String CREATED_AT = "created_at";
    public static final String CONTACT_ID = "contact_id";
    public static final String ADVERTISEMENT_ID = "advertisement_id";
    public static final String NOTIFICATION_ID = "id";
    public static final String MESSAGE = "message";
    public static final String URL = "url";
    public static final String READ = "read";
    
    public abstract long id();
    public abstract String notification_id();
    public abstract String created_at();
    @Nullable  public abstract String contact_id();
    @Nullable  public abstract String advertisement_id();
    public abstract String message();
    public abstract String url();
    public abstract boolean read();

    public static final Func1<Query, List<NotificationItem>> MAP = new Func1<Query, List<NotificationItem>>() {
        @Override
        public List<NotificationItem> call(Query query) {
            Cursor cursor = query.run();
            try {
                List<NotificationItem> values = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    long id = Db.getLong(cursor, ID);
                    String notification_id = Db.getString(cursor, NOTIFICATION_ID);
                    String created_at = Db.getString(cursor, CREATED_AT);
                    String contact_id = Db.getString(cursor, CONTACT_ID);
                    String advertisement_id = Db.getString(cursor, ADVERTISEMENT_ID);
                    String message = Db.getString(cursor, MESSAGE);
                    String url = Db.getString(cursor, URL);
                    boolean read = Db.getBoolean(cursor, READ);
                    values.add(new AutoParcel_NotificationItem(id, notification_id, created_at, contact_id, advertisement_id, message, url, read));
                }
                return values;
            } finally {
                cursor.close();
            }
        }
    };
    
    public static Builder createBuilder(Notification item)
    {
        return new Builder()
                .notification_id(item.notification_id)
                .contact_id(item.contact_id)
                .advertisement_id(item.advertisement_id)
                .message(item.msg)
                .url(item.url)
                .read(item.read)
                .created_at(item.created_at);
    }

    public static final class Builder {
        private final ContentValues values = new ContentValues();
        
        public Builder contact_id(String value) {
            values.put(CONTACT_ID, value);
            return this;
        }

        public Builder advertisement_id(String value) {
            values.put(ADVERTISEMENT_ID, value);
            return this;
        }

        public Builder notification_id(String value) {
            values.put(NOTIFICATION_ID, value);
            return this;
        }

        public Builder created_at(String value) {
            values.put(CREATED_AT, value);
            return this;
        }

        public Builder message(String value) {
            values.put(MESSAGE, value);
            return this;
        }

        public Builder read(boolean value) {
            values.put(READ, value);
            return this;
        }

        public Builder url(String value) {
            values.put(URL, value);
            return this;
        }

        public ContentValues build() {
            return values; 
        }
    }
}
