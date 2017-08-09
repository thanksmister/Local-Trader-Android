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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "localtrader.db";
    // TODO back to 38
    private static final int DATABASE_VERSION = 40;
    private static final int ADDED_CONTACT_PARAMS = 38;
    private static final int UPDATED_EXCHANGES = 37;
    private static final int ADDED_DATABASE_NOTIFICATIONS = 34;
    private static final int DATABASE_NOTIFICATIONS = 33;
    private static final int DATABASE_VERSION_FIRST_TIME_BTC_LIMIT = 32;
    private static final int DATABASE_VERSION_TRANSACTIONS = 29;
    private static final int DATABASE_VERSION_MESSAGES = 25;
    private static final int CONTACT_VERSION_MESSAGES = 27;
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_TEXT_NOT_NULL = " TEXT NOT NULL";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";
    private static final String COMMA_SEP = ", ";

    private static final String CREATE_WALLET =
            "CREATE TABLE IF NOT EXISTS " + WalletItem.TABLE + " (" +
                    WalletItem.ID + " INTEGER PRIMARY KEY," +
                    WalletItem.BALANCE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.SENDABLE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.ADDRESS + TYPE_TEXT + COMMA_SEP +
                    WalletItem.RECEIVABLE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.MESSAGE + TYPE_TEXT + COMMA_SEP + ")";

    private static final String CREATE_METHOD = ""
            + "CREATE TABLE IF NOT EXISTS " + MethodItem.TABLE + "("
            + MethodItem.ID + " INTEGER NOT NULL PRIMARY KEY,"
            + MethodItem.KEY + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.CODE + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.NAME + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.COUNTRY_CODE + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.COUNTRY_NAME + TYPE_TEXT_NOT_NULL
            + ")";

    private static final String CREATE_CONTACTS =
            "CREATE TABLE IF NOT EXISTS " + ContactItem.TABLE + " ("
                    + ContactItem._ID + " INTEGER NOT NULL PRIMARY KEY" + COMMA_SEP
                    + ContactItem.CONTACT_ID + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISEMENT_TRADE_TYPE + TYPE_TEXT + COMMA_SEP
                    + ContactItem.CREATED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.PAYMENT_COMPLETED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.DISPUTED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.FUNDED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ESCROWED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.RELEASED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.EXCHANGE_RATE_UPDATED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.CANCELED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.CLOSED_AT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.REFERENCE_CODE + TYPE_TEXT + COMMA_SEP
                    + ContactItem.CURRENCY + TYPE_TEXT + COMMA_SEP
                    + ContactItem.AMOUNT + TYPE_TEXT + COMMA_SEP
                    + ContactItem.AMOUNT_BTC + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BUYER_USERNAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BUYER_TRADES + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BUYER_FEEDBACK + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BUYER_NAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BUYER_LAST_SEEN + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SELLER_USERNAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SELLER_TRADES + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SELLER_FEEDBACK + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SELLER_NAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SELLER_LAST_SEEN + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISER_USERNAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISER_TRADES + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISER_FEEDBACK + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISER_NAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISER_LAST_SEEN + TYPE_TEXT + COMMA_SEP

                    + ContactItem.RECEIVER_EMAIL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.IBAN + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SWIFT_BIC + TYPE_TEXT + COMMA_SEP
                    + ContactItem.REFERENCE + TYPE_TEXT + COMMA_SEP

                    // TODO add these
                    + ContactItem.RECEIVER_NAME + TYPE_TEXT + COMMA_SEP
                    + ContactItem.MESSAGE + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BILLER_CODE + TYPE_TEXT + COMMA_SEP
                    + ContactItem.SORT_CODE + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ETHEREUM_ADDRESS + TYPE_TEXT + COMMA_SEP
                    + ContactItem.BSB + TYPE_TEXT + COMMA_SEP
                    + ContactItem.PHONE_NUMBER + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ACCOUNT_NUMBER + TYPE_TEXT + COMMA_SEP

                    + ContactItem.ADVERTISEMENT_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.RELEASE_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.MESSAGE_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.MESSAGE_POST_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.MARK_PAID_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.DISPUTE_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.CANCEL_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.FUND_URL + TYPE_TEXT + COMMA_SEP
                    + ContactItem.IS_FUNDED + " INTEGER NOT NULL DEFAULT 0" + COMMA_SEP
                    + ContactItem.IS_SELLING + " INTEGER NOT NULL DEFAULT 0" + COMMA_SEP
                    + ContactItem.IS_BUYING + " INTEGER NOT NULL DEFAULT 0" + COMMA_SEP
                    + ContactItem.ADVERTISEMENT_PAYMENT_METHOD + TYPE_TEXT + COMMA_SEP
                    + ContactItem.ADVERTISEMENT_ID + TYPE_TEXT + COMMA_SEP
                    + ContactItem.MESSAGE_COUNT + " INTEGER NOT NULL DEFAULT 0" + COMMA_SEP
                    + ContactItem.UNSEEN_MESSAGES + " INTEGER NOT NULL DEFAULT 0"
                    + ")";

    private static final String CREATE_MESSAGES =
            "CREATE TABLE IF NOT EXISTS " + MessageItem.TABLE + " ("
                    + MessageItem.ID + " INTEGER NOT NULL PRIMARY KEY" + COMMA_SEP
                    + MessageItem.CONTACT_ID + TYPE_INTEGER + COMMA_SEP
                    + MessageItem.MESSAGE + TYPE_TEXT + COMMA_SEP
                    + MessageItem.CREATED_AT + TYPE_TEXT + COMMA_SEP
                    + MessageItem.SENDER_ID + TYPE_TEXT + COMMA_SEP
                    + MessageItem.SENDER_NAME + TYPE_TEXT + COMMA_SEP
                    + MessageItem.SENDER_USERNAME + TYPE_TEXT + COMMA_SEP
                    + MessageItem.SENDER_TRADE_COUNT + TYPE_TEXT + COMMA_SEP
                    + MessageItem.SENDER_LAST_ONLINE + TYPE_TEXT + COMMA_SEP
                    + MessageItem.IS_ADMIN + TYPE_INTEGER + COMMA_SEP
                    + MessageItem.ATTACHMENT_NAME + TYPE_TEXT + COMMA_SEP
                    + MessageItem.ATTACHMENT_URL + TYPE_TEXT + COMMA_SEP
                    + MessageItem.ATTACHMENT_TYPE + TYPE_TEXT + COMMA_SEP
                    + MessageItem.SEEN + " INTEGER NOT NULL DEFAULT 0"
                    + ")";

    private static final String CREATE_RECENT_MESSAGES =
            "CREATE TABLE IF NOT EXISTS " + RecentMessageItem.TABLE + " ("
                    + RecentMessageItem.ID + " INTEGER NOT NULL PRIMARY KEY" + COMMA_SEP
                    + RecentMessageItem.CONTACT_ID + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.MESSAGE + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.CREATED_AT + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.SENDER_ID + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.SENDER_NAME + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.SENDER_USERNAME + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.SENDER_TRADE_COUNT + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.SENDER_LAST_ONLINE + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.IS_ADMIN + TYPE_INTEGER + COMMA_SEP
                    + RecentMessageItem.ATTACHMENT_NAME + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.ATTACHMENT_URL + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.ATTACHMENT_TYPE + TYPE_TEXT + COMMA_SEP
                    + RecentMessageItem.SEEN + " INTEGER NOT NULL DEFAULT 0"
                    + ")";

    private static final String CREATE_CURRENCIES =
            "CREATE TABLE IF NOT EXISTS " + CurrencyItem.TABLE + " (" +
                    CurrencyItem.ID + " INTEGER PRIMARY KEY," +
                    CurrencyItem.CURRENCY + TYPE_TEXT +
                    ")";

    private static final String CREATE_EXCHANGE_CURRENCIES =
            "CREATE TABLE IF NOT EXISTS " + ExchangeCurrencyItem.TABLE + " (" +
                    ExchangeCurrencyItem.ID + " INTEGER PRIMARY KEY," +
                    ExchangeCurrencyItem.CURRENCY + TYPE_TEXT +
                    ")";

    private static final String CREATE_EXCHANGE_RATES =
            "CREATE TABLE IF NOT EXISTS " + ExchangeRateItem.TABLE + " (" +
                    ExchangeRateItem.ID + " INTEGER PRIMARY KEY," +
                    ExchangeRateItem.EXCHANGE + TYPE_TEXT + COMMA_SEP +
                    ExchangeRateItem.CURRENCY + TYPE_TEXT + COMMA_SEP +
                    ExchangeRateItem.RATE + TYPE_TEXT +
                    ")";

    private static final String CREATE_NOTIFICATIONS =
            "CREATE TABLE IF NOT EXISTS " + NotificationItem.TABLE + " ("
                    + NotificationItem.ID + " INTEGER PRIMARY KEY,"
                    + NotificationItem.NOTIFICATION_ID + TYPE_TEXT + COMMA_SEP
                    + NotificationItem.URL + TYPE_TEXT + COMMA_SEP
                    + NotificationItem.CONTACT_ID + TYPE_TEXT + COMMA_SEP
                    + NotificationItem.ADVERTISEMENT_ID + TYPE_TEXT + COMMA_SEP
                    + NotificationItem.MESSAGE + TYPE_TEXT + COMMA_SEP
                    + NotificationItem.CREATED_AT + TYPE_TEXT + COMMA_SEP
                    + NotificationItem.READ + " INTEGER NOT NULL DEFAULT 0"
                    + ")";

    private static final String CREATE_ADVERTISEMENTS =
            "CREATE TABLE IF NOT EXISTS " + AdvertisementItem.TABLE + " (" +
                    AdvertisementItem.ID + " INTEGER PRIMARY KEY," +
                    AdvertisementItem.AD_ID + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.CREATED_AT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.VISIBLE + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementItem.EMAIL + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.LOCATION_STRING + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.COUNTRY_CODE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.CITY + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.TRADE_TYPE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.ONLINE_PROVIDER + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.SMS_VERIFICATION_REQUIRED + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementItem.PRICE_EQUATION + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.ATM_MODEL + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.REQUIRE_FEEDBACK_SCORE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.REQUIRE_TRADE_VOLUME + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.FIRST_TIME_LIMIT_BTC + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.REFERENCE_TYPE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.CURRENCY + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.ACCOUNT_INFO + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.LAT + TYPE_REAL + COMMA_SEP +
                    AdvertisementItem.LON + TYPE_REAL + COMMA_SEP +
                    AdvertisementItem.MIN_AMOUNT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.MAX_AMOUNT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.MAX_AMOUNT_AVAILABLE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.ACTION_PUBLIC_VIEW + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.TEMP_PRICE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.TEMP_PRICE_USD + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.PROFILE_NAME + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.PROFILE_USERNAME + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.PROFILE_LAST_ONLINE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.PROFILE_FEEDBACK_SCORE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.PROFILE_TRADE_COUNT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.TRUSTED_REQUIRED + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementItem.REQUIRE_IDENTIFICATION + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementItem.MESSAGE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.PHONE_NUMBER + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.OPENING_HOURS + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.TRACK_MAX_AMOUNT + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementItem.BANK_NAME + TYPE_TEXT + ")";

    private static final String CREATE_TRANSACTIONS = ""
            + "CREATE TABLE IF NOT EXISTS " + TransactionItem.TABLE + "("
            + TransactionItem.ID + " INTEGER NOT NULL PRIMARY KEY,"
            + TransactionItem.TRANSACTION_ID + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + TransactionItem.AMOUNT + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + TransactionItem.DESCRIPTION + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + TransactionItem.TRANSACTION_TYPE + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + TransactionItem.CREATED_AT + TYPE_TEXT_NOT_NULL + ")";

    private static final String CREATE_CONTACT_LIST_ID_INDEX =
            "CREATE INDEX contact_list_id ON " + MessageItem.TABLE + " (" + MessageItem.CONTACT_ID + ")";

    public DbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null /* factory */, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_METHOD);
        db.execSQL(CREATE_CONTACTS);
        db.execSQL(CREATE_MESSAGES);
        db.execSQL(CREATE_EXCHANGE_RATES);
        db.execSQL(CREATE_WALLET);
        db.execSQL(CREATE_ADVERTISEMENTS);
        db.execSQL(CREATE_TRANSACTIONS);
        db.execSQL(CREATE_CONTACT_LIST_ID_INDEX);
        db.execSQL(CREATE_RECENT_MESSAGES);
        db.execSQL(CREATE_NOTIFICATIONS);
        db.execSQL(CREATE_EXCHANGE_CURRENCIES);
        db.execSQL(CREATE_CURRENCIES);
        /*

    long workListId = db.insert(TodoList.TABLE, null, new TodoList.Builder()
        .name("Work Items")
        .build());
        
    db.insert(TodoItem.TABLE, null, new TodoItem.Builder()
        .listId(workListId)
        .description("Finish SqlBrite library")
        .complete(true)
        .build());
        
    db.insert(TodoItem.TABLE, null, new TodoItem.Builder()
        .listId(workListId)
        .description("Finish SqlBrite sample app")
        .build());
        
    db.insert(TodoItem.TABLE, null, new TodoItem.Builder()
        .listId(workListId)
        .description("Publish SqlBrite to GitHub")
        .build());
         */
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
        if (oldVersion < newVersion) {
            
            if(!isColumnExists(db, AdvertisementItem.TABLE, AdvertisementItem.PHONE_NUMBER)) {
                final String ALTER_TBL1 =
                        "ALTER TABLE " + AdvertisementItem.TABLE +
                                " ADD COLUMN " + AdvertisementItem.PHONE_NUMBER + TYPE_TEXT;

                db.execSQL(ALTER_TBL1);
            }

            if(!isColumnExists(db, AdvertisementItem.TABLE, AdvertisementItem.OPENING_HOURS)) {
                final String ALTER_TBL2 =
                        "ALTER TABLE " + AdvertisementItem.TABLE +
                                " ADD COLUMN " + AdvertisementItem.OPENING_HOURS + TYPE_TEXT;

                db.execSQL(ALTER_TBL2);
            }
        }
            
            
        if (oldVersion < ADDED_CONTACT_PARAMS) {

                if(!isColumnExists(db, ContactItem.TABLE, ContactItem.RECEIVER_NAME)) {
                    final String ALTER_TBL0 =
                            "ALTER TABLE " + ContactItem.TABLE +
                                    " ADD COLUMN " + ContactItem.RECEIVER_NAME + TYPE_TEXT;

                    db.execSQL(ALTER_TBL0);
                }

                if(!isColumnExists(db, ContactItem.TABLE, ContactItem.MESSAGE)) {
                    final String ALTER_TBL1 =
                            "ALTER TABLE " + ContactItem.TABLE +
                                    " ADD COLUMN " + ContactItem.MESSAGE + TYPE_TEXT;

                    db.execSQL(ALTER_TBL1);
                }

                final String ALTER_TBL2 =
                        "ALTER TABLE " + ContactItem.TABLE +
                                " ADD COLUMN " + ContactItem.BILLER_CODE + TYPE_TEXT;

                db.execSQL(ALTER_TBL2);

                final String ALTER_TBL3 =
                        "ALTER TABLE " + ContactItem.TABLE +
                                " ADD COLUMN " + ContactItem.SORT_CODE + TYPE_TEXT;

                db.execSQL(ALTER_TBL3);

                final String ALTER_TBL4 =
                        "ALTER TABLE " + ContactItem.TABLE +
                                " ADD COLUMN " + ContactItem.ETHEREUM_ADDRESS + TYPE_TEXT;

                db.execSQL(ALTER_TBL4);

                final String ALTER_TBL5 =
                        "ALTER TABLE " + ContactItem.TABLE +
                                " ADD COLUMN " + ContactItem.BSB + TYPE_TEXT;

                db.execSQL(ALTER_TBL5);

                final String ALTER_TBL6 =
                        "ALTER TABLE " + ContactItem.TABLE +
                                " ADD COLUMN " + ContactItem.PHONE_NUMBER + TYPE_TEXT;

                db.execSQL(ALTER_TBL6);

                final String ALTER_TBL7 =
                        "ALTER TABLE " + ContactItem.TABLE +
                                " ADD COLUMN " + ContactItem.ACCOUNT_NUMBER + TYPE_TEXT;

                db.execSQL(ALTER_TBL7);
            
        }
        
        if (oldVersion < UPDATED_EXCHANGES) {
            db.execSQL("DROP TABLE IF EXISTS exchange_item");
            db.execSQL(CREATE_EXCHANGE_RATES);
            db.execSQL(CREATE_EXCHANGE_CURRENCIES);
            db.execSQL(CREATE_CURRENCIES);
        }

        if (oldVersion < ADDED_DATABASE_NOTIFICATIONS) {
            db.execSQL(CREATE_NOTIFICATIONS);
        }

        if (oldVersion < DATABASE_NOTIFICATIONS) {
            final String ALTER_TBL =
                    "ALTER TABLE " + AdvertisementItem.TABLE +
                            " ADD COLUMN " + AdvertisementItem.FIRST_TIME_LIMIT_BTC + TYPE_TEXT;

            db.execSQL(ALTER_TBL);

            final String ALTER_TBL1 =
                    "ALTER TABLE " + AdvertisementItem.TABLE +
                            " ADD COLUMN " + AdvertisementItem.REQUIRE_IDENTIFICATION + TYPE_INTEGER;

            db.execSQL(ALTER_TBL1);
        }

        if (oldVersion < DATABASE_VERSION_FIRST_TIME_BTC_LIMIT) {
            db.execSQL("DROP TABLE IF EXISTS session_table");
            db.execSQL(CREATE_RECENT_MESSAGES);
        }

        // check older version for transactions table
        if (oldVersion < DATABASE_VERSION_TRANSACTIONS) {
            db.execSQL(CREATE_TRANSACTIONS);
        }

        // check versions for upgrade
        if (oldVersion < DATABASE_VERSION_MESSAGES) {

            //db.execSQL(CREATE_CONTACTS);
            //db.execSQL(CREATE_MESSAGES);

            final String ALTER_TBL =
                    "ALTER TABLE " + MessageItem.TABLE +
                            " ADD COLUMN " + MessageItem.ATTACHMENT_NAME + TYPE_TEXT;

            db.execSQL(ALTER_TBL);

            final String ALTER_TBL1 =
                    "ALTER TABLE " + MessageItem.TABLE +
                            " ADD COLUMN " + MessageItem.ATTACHMENT_TYPE + TYPE_TEXT;

            db.execSQL(ALTER_TBL1);

            final String ALTER_TBL2 =
                    "ALTER TABLE " + MessageItem.TABLE +
                            " ADD COLUMN " + MessageItem.ATTACHMENT_URL + TYPE_TEXT;

            db.execSQL(ALTER_TBL2);

        }

        if (oldVersion < CONTACT_VERSION_MESSAGES) {

            final String ALTER_TBL =
                    "ALTER TABLE " + ContactItem.TABLE +
                            " ADD COLUMN " + ContactItem.MESSAGE_COUNT + TYPE_INTEGER;

            db.execSQL(ALTER_TBL);

            final String ALTER_TBL1 =
                    "ALTER TABLE " + ContactItem.TABLE +
                            " ADD COLUMN " + ContactItem.UNSEEN_MESSAGES + TYPE_INTEGER;

            db.execSQL(ALTER_TBL1);
        }
    }

    //https://stackoverflow.com/questions/3604310/alter-table-add-column-if-not-exists-in-sqlite
    public boolean isColumnExists (SQLiteDatabase db, String table, String column) {
        Cursor cursor = db.rawQuery("PRAGMA table_info("+ table +")", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                if (column.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
