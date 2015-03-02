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

/**
 * Field and table name constants for
 * {@link StubProvider}.
 */
public class ExchangeContract extends BaseContract
{
    private ExchangeContract()
    {
    }

    /**
     * Path component for "escrow"-type resources..
     */
    private static final String PATH_ITEMS= "exchanges";

    /**
     * Columns supported by "escrow" records.
     */
    public static class Exchange implements BaseColumns
    {

        public static final String[] PROJECTION = new String[] {
                ExchangeContract.Exchange._ID,
                ExchangeContract.Exchange.COLUMN_NAME_EXCHANGE,
                ExchangeContract.Exchange.COLUMN_NAME_ASK,
                ExchangeContract.Exchange.COLUMN_NAME_BID,
                ExchangeContract.Exchange.COLUMN_NAME_LAST,
                ExchangeContract.Exchange.COLUMN_NAME_VOLUME,
                ExchangeContract.Exchange.COLUMN_NAME_HIGH,
                ExchangeContract.Exchange.COLUMN_NAME_LOW,
                ExchangeContract.Exchange.COLUMN_NAME_TIME_STAMP,
                ExchangeContract.Exchange.COLUMN_NAME_MID};
        
        /**
         * MIME type for lists of escrows.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.localtrader.exchanges";

        /**
         * MIME type for individual escrows.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.localtrader.exchange";

        /**
         * Fully qualified URI for "escrow" resources.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ITEMS).build();

        public static final String TABLE_NAME = "exchange_table";

        public static final String COLUMN_NAME_EXCHANGE = "exchange_name";
        public static final String COLUMN_NAME_ASK = "ask";
        public static final String COLUMN_NAME_BID = "bid";
        public static final String COLUMN_NAME_LAST= "last";
        public static final String COLUMN_NAME_VOLUME = "volume";
        public static final String COLUMN_NAME_HIGH = "high";
        public static final String COLUMN_NAME_LOW = "low";
        public static final String COLUMN_NAME_TIME_STAMP = "timestamp";
        public static final String COLUMN_NAME_MID = "MID";

        // Constants representing column positions from PROJECTION.
        public static final int COLUMN_ID = 0;
        public static final int COLUMN_INDEX_NAME_EXCHANGE = 1;
        public static final int COLUMN_INDEX_NAME_ASK = 2;
        public static final int COLUMN_INDEX_NAME_BID = 3;
        public static final int COLUMN_INDEX_NAME_LAST = 4;
        public static final int COLUMN_INDEX_NAME_VOLUME = 5;
        public static final int COLUMN_INDEX_NAME_HIGH = 6;
        public static final int COLUMN_INDEX_NAME_LOW = 7;
        public static final int COLUMN_INDEX_NAME_TIME_STAMP = 8;
        public static final int COLUMN_INDEX_MID = 9;
    }
}