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

public class AdvertisementContract extends BaseContract
{
    private AdvertisementContract()
    {
    }

    private static final String PATH = "advertisements";

    /**
     * Columns supported by "escrow" records.
     */
    public static class Advertisement implements BaseColumns
    {
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.localtrader.advertisements";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.localtrader.advertisement";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();

        public static final String TABLE_NAME = "advertisement_table";

        /**
         * Projection for querying the content provider.
         */
        public static final String[] PROJECTION = new String[] {
                Advertisement._ID,
                Advertisement.COLUMN_NAME_AD_ID,
                Advertisement.COLUMN_NAME_CREATED_AT,
                Advertisement.COLUMN_NAME_VISIBLE,
                Advertisement.COLUMN_NAME_EMAIL,
                Advertisement.COLUMN_NAME_LOCATION,
                Advertisement.COLUMN_NAME_COUNTRY_CODE,
                Advertisement.COLUMN_NAME_CITY,
                Advertisement.COLUMN_NAME_TRADE_TYPE,
                Advertisement.COLUMN_NAME_ONLINE_PROVIDER,
                Advertisement.COLUMN_NAME_SMS_VERIFICATION_REQUIRED,
                Advertisement.COLUMN_NAME_PRICE_EQUATION,
                Advertisement.COLUMN_NAME_REFERENCE_TYPE,
                Advertisement.COLUMN_NAME_CURRENCY,
                Advertisement.COLUMN_NAME_ACCOUNT_INFO,
                Advertisement.COLUMN_NAME_LAT,
                Advertisement.COLUMN_NAME_LON,
                Advertisement.COLUMN_NAME_MIN_AMOUNT,
                Advertisement.COLUMN_NAME_MAX_AMOUNT,
                Advertisement.COLUMN_NAME_PUBLIC_VIEW,
                Advertisement.COLUMN_NAME_PRICE,
                Advertisement.COLUMN_NAME_PROFILE_ID,
                Advertisement.COLUMN_NAME_PROFILE_NAME,
                Advertisement.COLUMN_NAME_PROFILE_USERNAME,
                Advertisement.COLUMN_NAME_BANK_NAME,
                Advertisement.COLUMN_NAME_TRUSTED_REQUIRED,
                Advertisement.COLUMN_NAME_MESSAGE,
                Advertisement.COLUMN_NAME_TRACK_MAX_AMOUNT};

        public static final String COLUMN_NAME_AD_ID = "ad_id";
        public static final String COLUMN_NAME_CREATED_AT = "created_at";
        public static final String COLUMN_NAME_VISIBLE = "visible";
        public static final String COLUMN_NAME_EMAIL= "email";
        public static final String COLUMN_NAME_LOCATION = "location";
        public static final String COLUMN_NAME_COUNTRY_CODE = "country_code";
        public static final String COLUMN_NAME_CITY = "city";
        public static final String COLUMN_NAME_TRADE_TYPE= "trade_type";
        public static final String COLUMN_NAME_ONLINE_PROVIDER = "online_provider";

        public static final String COLUMN_NAME_SMS_VERIFICATION_REQUIRED = "sms_verification_required";
        public static final String COLUMN_NAME_PRICE_EQUATION = "price_equation";
        public static final String COLUMN_NAME_REFERENCE_TYPE = "reference_type";
        public static final String COLUMN_NAME_CURRENCY = "currency";
        public static final String COLUMN_NAME_ACCOUNT_INFO = "account_info";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON= "lon";
        public static final String COLUMN_NAME_MIN_AMOUNT = "min_amount";
        public static final String COLUMN_NAME_MAX_AMOUNT = "max_amount";
        public static final String COLUMN_NAME_PUBLIC_VIEW = "public_view";
        public static final String COLUMN_NAME_PRICE = "price";

        public static final String COLUMN_NAME_PROFILE_ID = "profile_id";
        public static final String COLUMN_NAME_PROFILE_NAME = "profile_name";
        public static final String COLUMN_NAME_PROFILE_USERNAME = "profile_username";
        public static final String COLUMN_NAME_BANK_NAME = "bank_name";
        public static final String COLUMN_NAME_TRUSTED_REQUIRED = "trusted_required";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_TRACK_MAX_AMOUNT = "track_max_amount";
        
        public static final int COLUMN_ID = 0;
        
        public static final int COLUMN_AD_ID = 1;
        public static final int COLUMN_CREATED_AT = 2;
        public static final int COLUMN_VISIBLE = 3;
        public static final int COLUMN_EMAIL= 4;
        public static final int COLUMN_LOCATION = 5;
        public static final int COLUMN_COUNTRY_CODE = 6;
        public static final int COLUMN_CITY = 7;
        public static final int COLUMN_TRADE_TYPE= 8;
        public static final int COLUMN_ONLINE_PROVIDER = 9;

        public static final int COLUMN_SMS_VERIFICATION_REQUIRED = 10;
        public static final int COLUMN_PRICE_EQUATION = 11;
        public static final int COLUMN_REFERENCE_TYPE = 12;
        public static final int COLUMN_CURRENCY = 13;
        public static final int COLUMN_ACCOUNT_INFO = 14;
        public static final int COLUMN_LAT = 15;
        public static final int COLUMN_LON= 16;
        public static final int COLUMN_MIN_AMOUNT = 17;
        public static final int COLUMN_MAX_AMOUNT = 18;
        public static final int COLUMN_PUBLIC_VIEW = 19;
        public static final int COLUMN_PRICE = 20;

        public static final int COLUMN_PROFILE_ID = 21;
        public static final int COLUMN_PROFILE_NAME = 22;
        public static final int COLUMN_PROFILE_USERNAME = 23;
        public static final int COLUMN_BANK_NAME = 24;
        public static final int COLUMN_TRUSTED_REQUIRED= 25;
        public static final int COLUMN_MESSAGE = 26;
        public static final int COLUMN_TRACK_MAX_AMOUNT= 27;
    }
}