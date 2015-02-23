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

public class ContactContract extends BaseContract
{
    private ContactContract()
    {
    }

    /**
     * Path component for "escrow"-type resources..
     */
    private static final String PATH = "lbc_contacts";

    /**
     * Columns supported by "escrow" records.
     */
    public static class ContactData implements BaseColumns
    {

        /**
         * MIME type for lists of escrows.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.localtrader.lbc_contacts";

        /**
         * MIME type for individual escrows.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.localtrader.lbc_contact";

        /**
         * Fully qualified URI for "escrow" resources.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();

        public static final String TABLE_NAME = "lbc_contact_table";

        public static final String COLUMN_NAME_CONTACT_ID = "contact_id";
        public static final String COLUMN_NAME_REFERENCE_CODE = "reference_code";
        public static final String COLUMN_NAME_CURRENCY = "currency";
        public static final String COLUMN_NAME_AMOUNT = "amount";
        public static final String COLUMN_NAME_AMOUNT_BTC = "amount_btc";
        public static final String COLUMN_NAME_CREATED_AT = "created_at";
        
        public static final String COLUMN_NAME_PAYMENT_COMPLETED_AT = "payment_completed_at";
        public static final String COLUMN_NAME_CLOSED_AT = "closed_at";
        public static final String COLUMN_NAME_DISPUTED_AT = "disputed_at";
        public static final String COLUMN_NAME_FUNDED_AT = "funded_at";
        public static final String COLUMN_NAME_ESCROWED_AT = "escrowed_at";
        public static final String COLUMN_NAME_RELEASED_AT = "released_at";
        public static final String COLUMN_NAME_EXCHANGE_RATE_UPDATED_AT = "exchange_rate_updated_at";
        public static final String COLUMN_NAME_CANCELED_AT = "canceled_at";

        // BUYER
        public static final String COLUMN_NAME_BUYER_USERNAME = "buyer_username";
        public static final String COLUMN_NAME_BUYER_TRADES = "buyer_trade_count";
        public static final String COLUMN_NAME_BUYER_FEEDBACK = "buyer_feedback_score";
        public static final String COLUMN_NAME_BUYER_NAME = "buyer_name";
        public static final String COLUMN_NAME_BUYER_LAST_SEEN = "buyer_last_seen_on";

        // SELLER
        public static final String COLUMN_NAME_SELLER_USERNAME = "seller_username";
        public static final String COLUMN_NAME_SELLER_TRADES = "seller_trade_count";
        public static final String COLUMN_NAME_SELLER_FEEDBACK = "seller_feedback_score";
        public static final String COLUMN_NAME_SELLER_NAME = "seller_name";
        public static final String COLUMN_NAME_SELLER_LAST_SEEN = "seller_last_seen_on";

        // details
        public static final String COLUMN_NAME_RECEIVER = "receiver_name";
        public static final String COLUMN_NAME_IBAN = "iban";
        public static final String COLUMN_NAME_SWIFT_BIC = "swift_bic";
        public static final String COLUMN_NAME_REFERENCE = "reference";

        // advertisement
        public static final String COLUMN_NAME_ADVERTISEMENT_ID = "ad_id";
        public static final String COLUMN_NAME_TRADE_TYPE = "trade_type";
        public static final String COLUMN_NAME_PAYMENT_METHOD = "payment_method";

        // ADVERTISER
        public static final String COLUMN_NAME_ADVERTISER_USERNAME = "advertiser_username";
        public static final String COLUMN_NAME_ADVERTISER_TRADES = "advertiser_trade_count";
        public static final String COLUMN_NAME_ADVERTISER_FEEDBACK = "advertiser_feedback_score";
        public static final String COLUMN_NAME_ADVERTISER_NAME = "advertiser_name";
        public static final String COLUMN_NAME_ADVERTISER_LAST_SEEN = "advertiser_last_seen_on";

        // actions
        public static final String COLUMN_NAME_RELEASE_URL = "release_url";
        public static final String COLUMN_NAME_MESSAGE_URL = "message_url";
        public static final String COLUMN_NAME_MESSAGE_POST_URL = "message_post_url";
        public static final String COLUMN_NAME_PAID_URL = "mark_as_paid_url";
        public static final String COLUMN_NAME_ADVERTISEMENT_URL = "ad_url";
        public static final String COLUMN_NAME_DISPUTE_URL = "dispute_url";
        public static final String COLUMN_NAME_CANCEL_URL = "cancel_url";
        public static final String COLUMN_NAME_FUND_URL = "fund_url";
        public static final String COLUMN_NAME_IS_FUNDED = "is_funded";


        public static final String[] PROJECTION_SMALL = new String[]{
                ContactData._ID,
                ContactData.COLUMN_NAME_CONTACT_ID,
                ContactData.COLUMN_NAME_TRADE_TYPE,
                ContactData.COLUMN_NAME_CREATED_AT,
                ContactData.COLUMN_NAME_PAYMENT_COMPLETED_AT,
                ContactData.COLUMN_NAME_AMOUNT,
                ContactData.COLUMN_NAME_AMOUNT_BTC,
                ContactData.COLUMN_NAME_CURRENCY,
                ContactData.COLUMN_NAME_REFERENCE_CODE};

        public static final String[] PROJECTION = new String[] {
                
                ContactData._ID,
                
                ContactData.COLUMN_NAME_CONTACT_ID,
                ContactData.COLUMN_NAME_TRADE_TYPE,
                ContactData.COLUMN_NAME_CREATED_AT,
                
                ContactData.COLUMN_NAME_AMOUNT,
                ContactData.COLUMN_NAME_AMOUNT_BTC,
                ContactData.COLUMN_NAME_CURRENCY,
                ContactData.COLUMN_NAME_REFERENCE_CODE,

                ContactData.COLUMN_NAME_PAYMENT_COMPLETED_AT,
                ContactData.COLUMN_NAME_CLOSED_AT,
                ContactData.COLUMN_NAME_DISPUTED_AT,
                ContactData.COLUMN_NAME_FUNDED_AT,
                ContactData.COLUMN_NAME_ESCROWED_AT,
                ContactData.COLUMN_NAME_RELEASED_AT,
                ContactData.COLUMN_NAME_EXCHANGE_RATE_UPDATED_AT,
                ContactData.COLUMN_NAME_CLOSED_AT,

                ContactData.COLUMN_NAME_BUYER_USERNAME,
                ContactData.COLUMN_NAME_BUYER_TRADES,
                ContactData.COLUMN_NAME_BUYER_FEEDBACK,
                ContactData.COLUMN_NAME_BUYER_NAME,
                ContactData.COLUMN_NAME_BUYER_LAST_SEEN,

                ContactData.COLUMN_NAME_SELLER_USERNAME,
                ContactData.COLUMN_NAME_SELLER_TRADES,
                ContactData.COLUMN_NAME_SELLER_FEEDBACK,
                ContactData.COLUMN_NAME_SELLER_NAME,
                ContactData.COLUMN_NAME_SELLER_LAST_SEEN,

                ContactData.COLUMN_NAME_RECEIVER,
                ContactData.COLUMN_NAME_IBAN,
                ContactData.COLUMN_NAME_SWIFT_BIC,
                ContactData.COLUMN_NAME_REFERENCE,

                ContactData.COLUMN_NAME_ADVERTISEMENT_ID,
                ContactData.COLUMN_NAME_PAYMENT_METHOD,

                ContactData.COLUMN_NAME_ADVERTISER_USERNAME,
                ContactData.COLUMN_NAME_ADVERTISER_TRADES,
                ContactData.COLUMN_NAME_ADVERTISER_FEEDBACK,
                ContactData.COLUMN_NAME_ADVERTISER_NAME,
                ContactData.COLUMN_NAME_ADVERTISER_LAST_SEEN,

                ContactData.COLUMN_NAME_RELEASE_URL,
                ContactData.COLUMN_NAME_ADVERTISEMENT_URL,
                ContactData.COLUMN_NAME_MESSAGE_URL,
                ContactData.COLUMN_NAME_MESSAGE_POST_URL,
                ContactData.COLUMN_NAME_PAID_URL,
                ContactData.COLUMN_NAME_DISPUTE_URL,
                ContactData.COLUMN_NAME_CANCEL_URL,
                ContactData.COLUMN_NAME_FUND_URL,
                ContactData.COLUMN_NAME_IS_FUNDED
        };

        // Constants representing column positions from PROJECTION.
        public static final int COLUMN_INDEX_ID = 0;
        public static final int COLUMN_INDEX_CONTACT_ID = 1;

        public static final int COLUMN_INDEX_TRADE_TYPE = 2; // advertisement trade type
        public static final int COLUMN_INDEX_CREATED_AT = 3;
        
        public static final int COLUMN_INDEX_PAYMENT_COMPLETED_AT = 4;
        public static final int COLUMN_INDEX_CLOSED_AT = 5;
        public static final int COLUMN_INDEX_DISPUTED_AT = 6;
        public static final int COLUMN_INDEX_FUNDED_AT = 7;
        public static final int COLUMN_INDEX_ESCROWED_AT = 8;
        public static final int COLUMN_INDEX_RELEASED_AT = 9;
        public static final int COLUMN_INDEX_EXCHANGE_RATE_UPDATED_AT = 10;
        public static final int COLUMN_INDEX_CANCELED_AT = 11;

        public static final int COLUMN_INDEX_AMOUNT = 12;
        public static final int COLUMN_INDEX_AMOUNT_BTC = 13;
        public static final int COLUMN_INDEX_CURRENCY = 14;
        public static final int COLUMN_INDEX_REFERENCE_CODE = 15;

        // BUYER
        public static final int COLUMN_INDEX_BUYER_USERNAME = 16;
        public static final int COLUMN_INDEX_BUYER_TRADES = 17;
        public static final int COLUMN_INDEX_BUYER_FEEDBACK = 18;
        public static final int COLUMN_INDEX_BUYER_NAME = 19;
        public static final int COLUMN_INDEX_BUYER_LAST_SEEN = 20;

        // SELLER
        public static final int COLUMN_INDEX_SELLER_USERNAME = 21;
        public static final int COLUMN_INDEX_SELLER_TRADES = 22;
        public static final int COLUMN_INDEX_SELLER_FEEDBACK = 23;
        public static final int COLUMN_INDEX_SELLER_NAME = 24;
        public static final int COLUMN_INDEX_SELLER_LAST_SEEN = 25;

        // details
        public static final int COLUMN_INDEX_RECEIVER = 26;
        public static final int COLUMN_INDEX_IBAN = 27;
        public static final int COLUMN_INDEX_SWIFT_BIC = 28;
        public static final int COLUMN_INDEX_REFERENCE = 29;

        // advertisement
        public static final int COLUMN_INDEX_ADVERTISEMENT_ID = 30;
        public static final int COLUMN_INDEX_PAYMENT_METHOD = 31;

        // advertiser
        public static final int COLUMN_INDEX_ADVERTISER_USERNAME = 32;
        public static final int COLUMN_INDEX_ADVERTISER_TRADES = 33;
        public static final int COLUMN_INDEX_ADVERTISER_FEEDBACK = 34;
        public static final int COLUMN_INDEX_ADVERTISER_NAME = 35;
        public static final int COLUMN_INDEX_ADVERTISER_LAST_SEEN = 36;

        // actions
        public static final int COLUMN_INDEX_RELEASE_URL = 37;
        public static final int COLUMN_INDEX_ADVERTISEMENT_URL = 38;
        public static final int COLUMN_INDEX_MESSAGE_URL = 39;
        public static final int COLUMN_INDEX_MESSAGE_POST_URL = 40;
        public static final int COLUMN_INDEX_PAID_URL = 41;
        public static final int COLUMN_INDEX_DISPUTE_URL = 42;
        public static final int COLUMN_INDEX_CANCEL_URL = 43;
        public static final int COLUMN_INDEX_FUND_URL = 44;
        public static final int COLUMN_INDEX_IS_FUNDED = 45;
    }
}