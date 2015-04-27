package com.thanksmister.bitcoin.localtrader.data.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Author: Michael Ritchie
 * Date: 3/27/15
 * Copyright 2013, ThanksMister LLC
 */
import android.content.Context;

public class DbOpenHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "localtrader.db";
    private static final int DATABASE_VERSION = 25;
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_TEXT_NOT_NULL = " TEXT NOT NULL";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";
    private static final String COMMA_SEP = ", ";

    private static final String CREATE_SESSION =
            "CREATE TABLE IF NOT EXISTS " + SessionItem.TABLE + " (" +
                    SessionItem.ID + " INTEGER PRIMARY KEY," +
                    SessionItem.ACCESS_TOKEN + TYPE_TEXT + COMMA_SEP +
                    SessionItem.REFRESH_TOKEN + TYPE_TEXT  + ")";

    private static final String CREATE_WALLET =
            "CREATE TABLE IF NOT EXISTS " + WalletItem.TABLE + " (" +
                    WalletItem.ID + " INTEGER PRIMARY KEY," +
                    WalletItem.BALANCE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.SENDABLE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.ADDRESS + TYPE_TEXT + COMMA_SEP +
                    WalletItem.RECEIVABLE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.MESSAGE + TYPE_TEXT + COMMA_SEP +
                    WalletItem.QRCODE + TYPE_TEXT + ")";
    
    private static final String CREATE_METHOD = ""
            + "CREATE TABLE " + MethodItem.TABLE + "("
            + MethodItem.ID + " INTEGER NOT NULL PRIMARY KEY,"
            + MethodItem.KEY + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.CODE + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.NAME + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.COUNTRY_CODE + TYPE_TEXT_NOT_NULL + COMMA_SEP
            + MethodItem.COUNTRY_NAME + TYPE_TEXT_NOT_NULL
            + ")";

    private static final String CREATE_CONTACTS =
            "CREATE TABLE " + ContactItem.TABLE + " (" 
            + ContactItem.ID + " INTEGER NOT NULL PRIMARY KEY" + COMMA_SEP
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
            + ContactItem.RECEIVER + TYPE_TEXT + COMMA_SEP
            + ContactItem.IBAN + TYPE_TEXT + COMMA_SEP
            + ContactItem.SWIFT_BIC + TYPE_TEXT + COMMA_SEP
            + ContactItem.REFERENCE + TYPE_TEXT + COMMA_SEP
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
            + ContactItem.RECEIVER_EMAIL + TYPE_TEXT
            + ")";

    private static final String CREATE_MESSAGES =
            "CREATE TABLE " + MessageItem.TABLE + " (" 
            + MessageItem.ID + " INTEGER NOT NULL PRIMARY KEY"  + COMMA_SEP
            + MessageItem.CONTACT_LIST_ID + " INTEGER NOT NULL REFERENCES " + ContactItem.TABLE + "(" + ContactItem.ID + "),"
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

    private static final String CREATE_EXCHANGES =
            "CREATE TABLE IF NOT EXISTS " + ExchangeItem.TABLE + " (" +
            ExchangeItem.ID + " INTEGER PRIMARY KEY," +
            ExchangeItem.EXCHANGE + TYPE_TEXT + COMMA_SEP +
            ExchangeItem.BID + TYPE_TEXT + COMMA_SEP +
            ExchangeItem.ASK + TYPE_TEXT + COMMA_SEP +
            ExchangeItem.LAST + TYPE_TEXT +
            ")";

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
                    AdvertisementItem.MESSAGE + TYPE_TEXT + COMMA_SEP +
                    AdvertisementItem.TRACK_MAX_AMOUNT + TYPE_INTEGER + COMMA_SEP +
                    AdvertisementItem.BANK_NAME + TYPE_TEXT +")";


    private static final String CREATE_CONTACT_LIST_ID_INDEX =
            "CREATE INDEX contact_list_id ON " + MessageItem.TABLE + " (" + MessageItem.CONTACT_LIST_ID + ")";
    
    public DbOpenHelper(Context context)
    {
        super(context, DATABASE_NAME, null /* factory */, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_METHOD);
        db.execSQL(CREATE_CONTACTS);
        db.execSQL(CREATE_MESSAGES);
        db.execSQL(CREATE_EXCHANGES);
        db.execSQL(CREATE_SESSION);
        db.execSQL(CREATE_WALLET);
        db.execSQL(CREATE_ADVERTISEMENTS);
        db.execSQL(CREATE_CONTACT_LIST_ID_INDEX);
        
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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // check versions for upgrade
        if(oldVersion < newVersion) {

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
    }
}
