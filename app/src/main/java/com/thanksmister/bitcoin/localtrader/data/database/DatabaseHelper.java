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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.thanksmister.bitcoin.localtrader.data.services.SessionContract;


public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "bitcoin_local_trader_2.db";
    private static final int DATABASE_VERSION = 7;
  
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";
    private static final String COMMA_SEP = ", ";

    private static final String SQL_CREATE_SESSION =
            "CREATE TABLE IF NOT EXISTS " + SessionContract.Session.TABLE_NAME + " (" +
                    SessionContract.Session._ID + " INTEGER PRIMARY KEY," +
                    SessionContract.Session.COLUMN_ACCESS_TOKEN + TYPE_TEXT + COMMA_SEP +
                    SessionContract.Session.COLUMN_REFRESH_TOKEN + TYPE_TEXT  + ")";

    private static final String SQL_CREATE_WALLET =
            "CREATE TABLE IF NOT EXISTS " + WalletContract.Wallet.TABLE_NAME + " (" +
                    WalletContract.Wallet._ID + " INTEGER PRIMARY KEY," +
                    WalletContract.Wallet.COLUMN_WALLET_BALANCE + TYPE_TEXT + COMMA_SEP +
                    WalletContract.Wallet.COLUMN_WALLET_SENDABLE + TYPE_TEXT + COMMA_SEP +
                    WalletContract.Wallet.COLUMN_WALLET_ADDRESS + TYPE_TEXT + COMMA_SEP +
                    WalletContract.Wallet.COLUMN_WALLET_ADDRESS_RECEIVABLE + TYPE_TEXT + COMMA_SEP +
                    WalletContract.Wallet.COLUMN_WALLET_MESSAGE + TYPE_TEXT + COMMA_SEP +
                    WalletContract.Wallet.COLUMN_WALLET_QRCODE + TYPE_TEXT + ")";

    /** SQL statement to create "update" table. */
    private static final String SQL_CREATE_EXCHANGES =
            "CREATE TABLE IF NOT EXISTS " + ExchangeContract.Exchange.TABLE_NAME + " (" +
                    ExchangeContract.Exchange._ID + " INTEGER PRIMARY KEY," +
                    ExchangeContract.Exchange.COLUMN_NAME_EXCHANGE + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_TIME_STAMP + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_BID + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_ASK + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_LAST+ TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_HIGH + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_VOLUME + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_LOW + TYPE_TEXT + COMMA_SEP +
                    ExchangeContract.Exchange.COLUMN_NAME_MID + TYPE_TEXT + ")";

    private static final String SQL_CREATE_CONTACTS =
            "CREATE TABLE IF NOT EXISTS " + ContactContract.ContactData.TABLE_NAME + " (" +
                    
                    ContactContract.ContactData._ID + " INTEGER PRIMARY KEY," +
                    ContactContract.ContactData.COLUMN_NAME_CONTACT_ID + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_TRADE_TYPE + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_CREATED_AT + TYPE_TEXT + COMMA_SEP +
                    // general 4 

                    ContactContract.ContactData.COLUMN_NAME_PAYMENT_COMPLETED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_DISPUTED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_FUNDED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ESCROWED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_RELEASED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_EXCHANGE_RATE_UPDATED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_CANCELED_AT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_CLOSED_AT + TYPE_TEXT + COMMA_SEP +
                    //dates 8
                    
                    ContactContract.ContactData.COLUMN_NAME_REFERENCE_CODE + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_CURRENCY + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_AMOUNT + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_AMOUNT_BTC + TYPE_TEXT + COMMA_SEP +
                    // 4 contact

                    
                    ContactContract.ContactData.COLUMN_NAME_BUYER_USERNAME + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_BUYER_TRADES + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_BUYER_FEEDBACK + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_BUYER_NAME + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_BUYER_LAST_SEEN + TYPE_TEXT + COMMA_SEP +
                    //buyer 5
                    
                    ContactContract.ContactData.COLUMN_NAME_SELLER_USERNAME + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_SELLER_TRADES + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_SELLER_FEEDBACK + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_SELLER_NAME + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_SELLER_LAST_SEEN + TYPE_TEXT + COMMA_SEP +
                    //seller 5
                    
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISER_USERNAME + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISER_TRADES + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISER_FEEDBACK + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISER_NAME + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISER_LAST_SEEN + TYPE_TEXT + COMMA_SEP + 
                    //advertiser 5
                    
                    ContactContract.ContactData.COLUMN_NAME_RECEIVER + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_IBAN + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_SWIFT_BIC + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_REFERENCE + TYPE_TEXT + COMMA_SEP +
                    //details 4
                    
                    ContactContract.ContactData.COLUMN_NAME_RELEASE_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_MESSAGE_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_MESSAGE_POST_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_PAID_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_DISPUTE_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_CANCEL_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_FUND_URL + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_IS_FUNDED + TYPE_INTEGER + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISEMENT_URL + TYPE_TEXT + COMMA_SEP +
                    //actions 9
                    
                    ContactContract.ContactData.COLUMN_NAME_PAYMENT_METHOD + TYPE_TEXT + COMMA_SEP +
                    ContactContract.ContactData.COLUMN_NAME_ADVERTISEMENT_ID + TYPE_TEXT + ")";
                    // ad 2
  

    /** SQL statement to create "update" table. */
    private static final String SQL_CREATE_MESSAGES =
            "CREATE TABLE IF NOT EXISTS " + MessagesContract.Message.TABLE_NAME + " (" +
                    MessagesContract.Message._ID + " INTEGER PRIMARY KEY," +
                    MessagesContract.Message.COLUMN_NAME_CONTACT_ID + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_MESSAGE + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_SEEN + TYPE_INTEGER + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_CREATED_AT + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_SENDER_ID + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_SENDER_NAME + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_SENDER_USERNAME + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_SENDER_TRADE_COUNT + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_SENDER_LAST_ONLINE + TYPE_TEXT + COMMA_SEP +
                    MessagesContract.Message.COLUMN_NAME_IS_ADMIN + TYPE_INTEGER + ")";

    /** SQL statement to create "advertisement" table. */
    private static final String SQL_CREATE_ADVERTISEMENTS =
            "CREATE TABLE IF NOT EXISTS " + AdvertisementContract.Advertisement.TABLE_NAME + " (" +
                    AdvertisementContract.Advertisement._ID + " INTEGER PRIMARY KEY," +
                    AdvertisementContract.Advertisement.COLUMN_NAME_AD_ID + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_CREATED_AT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_VISIBLE + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_EMAIL + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_LOCATION + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_COUNTRY_CODE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_CITY + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_TRADE_TYPE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_ONLINE_PROVIDER + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_SMS_VERIFICATION_REQUIRED + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_PRICE_EQUATION + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_REFERENCE_TYPE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_CURRENCY + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_ACCOUNT_INFO + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_LAT + TYPE_REAL + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_LON + TYPE_REAL + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_MIN_AMOUNT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_MAX_AMOUNT + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_PUBLIC_VIEW + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_PRICE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_ID + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_NAME + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_PROFILE_USERNAME + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_TRUSTED_REQUIRED + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_MESSAGE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_TRACK_MAX_AMOUNT + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementContract.Advertisement.COLUMN_NAME_BANK_NAME + TYPE_TEXT +")";


    /** SQL statement to drop table. */
    private static final String SQL_DELETE_EXCHANGES = "DROP TABLE IF EXISTS " + "exchange_table";
    private static final String SQL_DELETE_CONTACTS = "DROP TABLE IF EXISTS " + "contacts_table";
    private static final String SQL_DELETE_MESSAGES = "DROP TABLE IF EXISTS " + "message_table";

    public DatabaseHelper(Context context) 
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) 
    {
        db.execSQL(SQL_CREATE_ADVERTISEMENTS);
        db.execSQL(SQL_CREATE_CONTACTS);
        db.execSQL(SQL_CREATE_MESSAGES);
        db.execSQL(SQL_CREATE_SESSION);
        db.execSQL(SQL_CREATE_WALLET);
        db.execSQL(SQL_CREATE_EXCHANGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
    {
        if (newVersion > oldVersion) {
            db.execSQL(SQL_CREATE_EXCHANGES);
        }
    }

    public void removeAll()
    {
        SQLiteDatabase db = getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        assert db != null;
        
        db.delete(ContactContract.ContactData.TABLE_NAME, null, null);
        db.delete(AdvertisementContract.Advertisement.TABLE_NAME, null, null);
        db.delete(MessagesContract.Message.TABLE_NAME, null, null);
        db.delete(SessionContract.Session.TABLE_NAME, null, null);
    }
}