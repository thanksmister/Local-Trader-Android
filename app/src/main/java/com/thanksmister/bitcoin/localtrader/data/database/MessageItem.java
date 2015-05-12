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
import android.support.annotation.Nullable;

import com.thanksmister.bitcoin.localtrader.data.api.model.Message;

import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import rx.functions.Func1;
import timber.log.Timber;

import static com.squareup.sqlbrite.SqlBrite.Query;

/**
 * https://github.com/square/sqlbrite/
 */
@AutoParcel
public abstract class MessageItem
{
    public static final String TABLE = "message_item";

    public static final String ID = "_id";
    public static final String CREATED_AT = "created_at";
    public static final String CONTACT_LIST_ID = "contact_list_id";
    public static final String MESSAGE = "message";
    public static final String SEEN = "seen";
    public static final String SENDER_ID = "sender_id";
    public static final String SENDER_NAME = "sender_name";
    public static final String SENDER_USERNAME = "sender_username";
    public static final String SENDER_TRADE_COUNT = "sender_trader_count";
    public static final String SENDER_LAST_ONLINE = "sender_last_online";
    public static final String IS_ADMIN = "is_admin";
    public static final String ATTACHMENT_NAME  = "attachment_name";
    public static final String ATTACHMENT_URL  = "attachment_url";
    public static final String ATTACHMENT_TYPE  = "attachment_type";
    
    public static final String QUERY = "SELECT * FROM "
            + MessageItem.TABLE
            + " WHERE "
            + MessageItem.CONTACT_LIST_ID
            + " = ? ORDER BY "
            + MessageItem.CREATED_AT
            + " DESC";
    
    public static final String COUNT_QUERY = "SELECT COUNT(*) FROM "
            + MessageItem.TABLE
            + " WHERE "
            + MessageItem.CONTACT_LIST_ID
            + " = ?";
    
    public abstract long id();
    public abstract String create_at();
    public abstract long contact_id();
    public abstract String message();
    public abstract boolean seen();
    @Nullable public abstract String sender_id();
    public abstract String sender_name();
    public abstract String sender_username();
    public abstract String sender_trade_count();
    public abstract String sender_last_online();
    public abstract boolean is_admin();
    @Nullable public abstract String attachment_name();
    @Nullable public abstract String attachment_type();
    @Nullable public abstract String attachment_url();

    public static final Func1<Query, List<MessageItem>> MAP = new Func1<Query, List<MessageItem>>() {
        @Override
        public List<MessageItem> call(Query query) {
            Cursor cursor = query.run();
            try {
                List<MessageItem> values = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    long id = Db.getLong(cursor, ID);
                    String created_at = Db.getString(cursor, CREATED_AT);
                    long contact_id = Db.getLong(cursor, CONTACT_LIST_ID);
                    String message = Db.getString(cursor, MESSAGE);
                    boolean seen = Db.getBoolean(cursor, SEEN);
                    String sender_id = Db.getString(cursor, SENDER_ID);
                    String sender_name = Db.getString(cursor, SENDER_NAME);
                    String sender_username = Db.getString(cursor, SENDER_USERNAME);
                    String sender_last_online = Db.getString(cursor, SENDER_LAST_ONLINE);
                    String sender_last_trade_count = Db.getString(cursor, SENDER_TRADE_COUNT);
                    boolean is_admin = Db.getBoolean(cursor, IS_ADMIN);
                    String attachment_name = Db.getString(cursor, ATTACHMENT_NAME);
                    String attachment_type = Db.getString(cursor, ATTACHMENT_TYPE);
                    String attachment_url = Db.getString(cursor, ATTACHMENT_URL);
                    values.add(new AutoParcel_MessageItem(id, created_at, contact_id, message, seen, sender_id, sender_name, sender_username,
                            sender_last_online, sender_last_trade_count, is_admin, attachment_name, attachment_type, attachment_url));
                }
                return values;
            } finally {
                cursor.close();
            }
        }
    };
    
    public static List<MessageItem> convertMessages(List<Message> messages, final String contactID)
    {
        List<MessageItem> values = new ArrayList<MessageItem>();
        for (Message message : messages) {
            try{
                values.add(new AutoParcel_MessageItem(0, message.created_at, Long.valueOf(contactID), message.msg, message.seen, message.sender.id, message.sender.name, message.sender.username,
                        message.sender.last_seen_on, message.sender.trade_count, message.is_admin, message.attachment_name, message.attachment_type, message.attachment_url));
            } catch (Exception e){
                Timber.e("Exception: " + e.getMessage());
            }
        }
        return values;
    }

    public static final class Builder {
        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(ID, id);
            return this;
        }

        public Builder contact_list_id(long value) {
            values.put(CONTACT_LIST_ID, value);
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

        public Builder seen(boolean value) {
            values.put(SEEN, value);
            return this;
        }

        public Builder sender_id(String value) {
            values.put(SENDER_ID, value);
            return this;
        }

        public Builder sender_name(String value) {
            values.put(SENDER_NAME, value);
            return this;
        }

        public Builder sender_username(String value) {
            values.put(SENDER_USERNAME, value);
            return this;
        }

        public Builder sender_last_online(String value) {
            values.put(SENDER_LAST_ONLINE, value);
            return this;
        }

        public Builder sender_trade_count(String value) {
            values.put(SENDER_TRADE_COUNT, value);
            return this;
        }

        public Builder is_admin(boolean value) {
            values.put(IS_ADMIN, value);
            return this;
        }

        public Builder attachment_name(String value) {
            values.put(ATTACHMENT_NAME, value);
            return this;
        }

        public Builder attachment_type(String value) {
            values.put(ATTACHMENT_TYPE, value);
            return this;
        }

        public Builder attachment_url(String value) {
            values.put(ATTACHMENT_URL, value);
            return this;
        }

        public ContentValues build() {
            return values; 
        }
    }
}
