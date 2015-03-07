/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class MessagesContract extends BaseContract
{
    private MessagesContract()
    {
    }

    /**
     * Path component for "escrow"-type resources..
     */
    private static final String PATH_ITEMS= "messages";

    public static final String[] PROJECTION = new String[] {
            Message._ID,
            Message.COLUMN_NAME_CONTACT_ID,
            Message.COLUMN_NAME_MESSAGE,
            Message.COLUMN_NAME_SEEN,
            Message.COLUMN_NAME_CREATED_AT,
            Message.COLUMN_NAME_SENDER_ID,
            Message.COLUMN_NAME_SENDER_NAME,
            Message.COLUMN_NAME_SENDER_USERNAME,
            Message.COLUMN_NAME_SENDER_TRADE_COUNT,
            Message.COLUMN_NAME_SENDER_LAST_ONLINE,
            Message.COLUMN_NAME_IS_ADMIN
    };

    /**
     * Columns supported by "escrow" records.
     */
    public static class Message implements BaseColumns
    {

        /**
         * MIME type for lists of escrows.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.localtrader.messages";

        /**
         * MIME type for individual escrows.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.localtrader.message";

        /**
         * Fully qualified URI for "escrow" resources.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ITEMS).build();

        public static final String TABLE_NAME = "message_table";
     
        public static final String COLUMN_NAME_CONTACT_ID = "contact_id";
        public static final String COLUMN_NAME_MESSAGE= "message";
        public static final String COLUMN_NAME_SEEN = "seen";
        public static final String COLUMN_NAME_CREATED_AT = "created_at";
        public static final String COLUMN_NAME_SENDER_ID = "sender_id";
        public static final String COLUMN_NAME_SENDER_NAME = "sender_name";
        public static final String COLUMN_NAME_SENDER_USERNAME = "sender_username";
        public static final String COLUMN_NAME_SENDER_TRADE_COUNT= "sender_trade_count";
        public static final String COLUMN_NAME_SENDER_LAST_ONLINE= "sender_last_online";
        public static final String COLUMN_NAME_IS_ADMIN = "is_admin";

        // Constants representing column positions from PROJECTION.
        public static final int COLUMN_INDEX_ID = 0;
        public static final int COLUMN_INDEX_CONTACT_ID = 1;
        public static final int COLUMN_INDEX_MESSAGE= 2;
        public static final int COLUMN_INDEX_SEEN= 3;
        public static final int COLUMN_INDEX_CREATED_AT = 4;
        public static final int COLUMN_INDEX_SENDER_ID = 5;
        public static final int COLUMN_INDEX_SENDER_NAME = 6;
        public static final int COLUMN_INDEX_SENDER_USERNAME = 7;
        public static final int COLUMN_INDEX_SENDER_TRADE_COUNT = 8;
        public static final int COLUMN_INDEX_SENDER_LAST_ONLINE = 9;
        public static final int COLUMN_INDEX_IS_ADMIN = 10;
    }
}