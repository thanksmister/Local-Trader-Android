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

import java.util.ArrayList;
import java.util.List;

import auto.parcel.AutoParcel;
import rx.functions.Func1;

import static com.squareup.sqlbrite.SqlBrite.Query;

/**
 * https://github.com/square/sqlbrite/
 */
@AutoParcel
public abstract class AdvertisementItem
{
    public static final String TABLE = "advertisement_item";

    public static final String ID = "_id";
    
    public static final String AD_ID = "ad_id";
    public static final String CREATED_AT = "created_at";
    public static final String VISIBLE = "visible";
    public static final String EMAIL= "email";
    public static final String LOCATION_STRING = "location";
    public static final String COUNTRY_CODE = "country_code";
    public static final String CITY = "city";
    public static final String TRADE_TYPE= "trade_type";
    public static final String ONLINE_PROVIDER = "online_provider";
    public static final String TEMP_PRICE = "temp_price";
    public static final String TEMP_PRICE_USD = "temp_price_usd";
    public static final String PRICE_EQUATION = "price_equation";
    public static final String REFERENCE_TYPE = "reference_type";
    public static final String ATM_MODEL = "atm_model";
    public static final String CURRENCY = "currency";
    public static final String ACCOUNT_INFO = "account_info";
    public static final String LAT = "lat";
    public static final String LON= "lon";
    public static final String MIN_AMOUNT = "min_amount";
    public static final String MAX_AMOUNT = "max_amount";
    public static final String MAX_AMOUNT_AVAILABLE = "max_amount_available";
    public static final String REQUIRE_FEEDBACK_SCORE = "require_feedback_score";
    public static final String REQUIRE_TRADE_VOLUME = "require_trade_volume";
    public static final String ACTION_PUBLIC_VIEW = "action_public_view";
    public static final String PROFILE_NAME = "profile_name";
    public static final String PROFILE_USERNAME = "profile_username";
    public static final String PROFILE_TRADE_COUNT = "profile_trade_count";
    public static final String PROFILE_LAST_ONLINE = "profile_last_online";
    public static final String PROFILE_FEEDBACK_SCORE = "profile_feedback_score";
    public static final String BANK_NAME = "bank_name";
    public static final String MESSAGE = "message";
    public static final String TRUSTED_REQUIRED = "trusted_required";
    public static final String TRACK_MAX_AMOUNT = "track_max_amount";
    public static final String SMS_VERIFICATION_REQUIRED = "sms_verification_required";
//33
    
    public static final String QUERY = "SELECT * FROM " 
            + AdvertisementItem.TABLE;

    public static final String QUERY_ITEM = "SELECT * FROM "
            + AdvertisementItem.TABLE
            + " WHERE "
            + AdvertisementItem.AD_ID
            + " = ? ORDER BY "
            + AdvertisementItem.CREATED_AT
            + " ASC";

    public static final String QUERY_DELETE_ITEM = "DELETE * FROM "
            + AdvertisementItem.TABLE
            + " WHERE "
            + AdvertisementItem.AD_ID
            + " = ? ";
    

    public abstract long id();
    
    public abstract String ad_id();
    public abstract String created_at();
    public abstract boolean visible();
    public abstract String email();
    public abstract String location_string();
    @Nullable public abstract String country_code();
    @Nullable public abstract String city();
    public abstract String trade_type();
    public abstract String online_provider();
    public abstract String temp_price();
    public abstract String temp_price_usd();
    public abstract String price_equation();
    @Nullable public abstract String reference_type();
    @Nullable public abstract String atm_model();
    public abstract String currency();
    public abstract String account_info();
    public abstract double lat();
    public abstract double lon();
    public abstract String min_amount();
    public abstract String max_amount();
    public abstract String max_amount_available();
    public abstract String require_trade_volume();
    public abstract String require_feedback_score();
    public abstract String action_public_view();
    public abstract String profile_name();
    public abstract String profile_username();
    public abstract String profile_trade_count();
    public abstract String profile_last_online();
    public abstract String profile_feedback_score();
    public abstract String bank_name();
    @Nullable public abstract String message();
    public abstract boolean sms_verification_required();
    public abstract boolean track_max_amount();
    public abstract boolean trusted_required();
// 33
    
    public static final Func1<Query, List<AdvertisementItem>> MAP = new Func1<Query, List<AdvertisementItem>>() {
        @Override
        public List<AdvertisementItem> call(Query query) {
            Cursor cursor = query.run();
            try {
                List<AdvertisementItem> values = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    long id = Db.getLong(cursor, ID);
                    String ad_id = Db.getString(cursor, AD_ID);
                    String created_at = Db.getString(cursor, CREATED_AT);
                    boolean visible = Db.getBoolean(cursor, VISIBLE);
                    String email = Db.getString(cursor, EMAIL);
                    String location_string = Db.getString(cursor, LOCATION_STRING);
                    String country_code = Db.getString(cursor, COUNTRY_CODE);
                    String city = Db.getString(cursor, CITY);
                    String trade_type = Db.getString(cursor, TRADE_TYPE);
                    String online_provider = Db.getString(cursor, ONLINE_PROVIDER);
                    String temp_price = Db.getString(cursor, TEMP_PRICE);
                    String temp_price_usd = Db.getString(cursor, TEMP_PRICE_USD);
                    String price_equation = Db.getString(cursor, PRICE_EQUATION);
                    String reference_type = Db.getString(cursor, REFERENCE_TYPE);
                    String atm_model = Db.getString(cursor, ATM_MODEL);
                    String currency = Db.getString(cursor, CURRENCY);
                    String account_info = Db.getString(cursor, ACCOUNT_INFO);
                    double lat = Db.getDouble(cursor, LAT);
                    double lon = Db.getDouble(cursor, LON);
                    String min_amount = Db.getString(cursor, MIN_AMOUNT);
                    String max_amount = Db.getString(cursor, MAX_AMOUNT);
                    String max_amount_available = Db.getString(cursor, MAX_AMOUNT_AVAILABLE);
                    String require_trade_volume = Db.getString(cursor, REQUIRE_TRADE_VOLUME);
                    String require_feedback_score = Db.getString(cursor, REQUIRE_FEEDBACK_SCORE);
                    String action_public_view = Db.getString(cursor, ACTION_PUBLIC_VIEW);
                    String profile_name = Db.getString(cursor, PROFILE_NAME);
                    String profile_username = Db.getString(cursor, PROFILE_USERNAME);
                    String profile_trade_count = Db.getString(cursor, PROFILE_TRADE_COUNT);
                    String profile_last_online = Db.getString(cursor, PROFILE_LAST_ONLINE);
                    String profile_feedback_score = Db.getString(cursor, PROFILE_FEEDBACK_SCORE);
                    String bank_name = Db.getString(cursor, BANK_NAME);
                    String message = Db.getString(cursor, MESSAGE);
                    boolean sms_verification_required = Db.getBoolean(cursor, SMS_VERIFICATION_REQUIRED);
                    boolean track_max_amount = Db.getBoolean(cursor, TRACK_MAX_AMOUNT);
                    boolean trusted_required = Db.getBoolean(cursor, TRUSTED_REQUIRED);
                    
                    values.add(new AutoParcel_AdvertisementItem(id, ad_id, created_at, visible, email, location_string, country_code, city,
                            trade_type, online_provider, temp_price, temp_price_usd, price_equation, reference_type, atm_model,
                            currency, account_info, lat, lon, min_amount, max_amount, max_amount_available, require_trade_volume,
                            require_feedback_score, action_public_view, profile_name, profile_username, profile_trade_count, profile_last_online,
                            profile_feedback_score, bank_name, message, sms_verification_required, track_max_amount, trusted_required));
                }
                return values;
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
        
        public Builder ad_id(String value) {
            values.put(AD_ID, value);
            return this;
        }

        public Builder created_at(String value) {
            values.put(CREATED_AT, value);
            return this;
        }

        public Builder visible(boolean value) {
            values.put(VISIBLE, value);
            return this;
        }

        public Builder email(String value) {
            values.put(EMAIL, value);
            return this;
        }

        public Builder location_string(String value) {
            values.put(LOCATION_STRING, value);
            return this;
        }

        public Builder country_code(String value) {
            values.put(COUNTRY_CODE, value);
            return this;
        }

        public Builder city(String value) {
            values.put(CITY, value);
            return this;
        }

        public Builder trade_type(String value) {
            values.put(TRADE_TYPE, value);
            return this;
        }

        public Builder online_provider(String value) {
            values.put(ONLINE_PROVIDER, value);
            return this;
        }

        public Builder temp_price(String value) {
            values.put(TEMP_PRICE, value);
            return this;
        }

        public Builder temp_price_usd(String value) {
            values.put(TEMP_PRICE_USD, value);
            return this;
        }

        public Builder price_equation(String value) {
            values.put(PRICE_EQUATION, value);
            return this;
        }

        public Builder reference_type(String value) {
            values.put(REFERENCE_TYPE, value);
            return this;
        }

        public Builder atm_model(String value) {
            values.put(ATM_MODEL, value);
            return this;
        }
        
        public Builder currency(String value) {
            values.put(CURRENCY, value);
            return this;
        }

        public Builder account_info(String value) {
            values.put(ACCOUNT_INFO, value);
            return this;
        }

        public Builder lat(double value) {
            values.put(LAT, value);
            return this;
        }

        public Builder lon(double value) {
            values.put(LON, value);
            return this;
        }

        public Builder min_amount(String value) {
            values.put(MIN_AMOUNT, value);
            return this;
        }

        public Builder max_amount(String value) {
            values.put(MAX_AMOUNT, value);
            return this;
        }

        public Builder max_amount_available(String value) {
            values.put(MAX_AMOUNT_AVAILABLE, value);
            return this;
        }

        public Builder require_trade_volume(String value) {
            values.put(REQUIRE_TRADE_VOLUME, value);
            return this;
        }

        public Builder require_feedback_score(String value) {
            values.put(REQUIRE_FEEDBACK_SCORE, value);
            return this;
        }

        public Builder action_public_view(String value) {
            values.put(ACTION_PUBLIC_VIEW, value);
            return this;
        }

        public Builder profile_name(String value) {
            values.put(PROFILE_NAME, value);
            return this;
        }

        public Builder profile_username(String value) {
            values.put(PROFILE_USERNAME, value);
            return this;
        }

        public Builder profile_trade_count(String value) {
            values.put(PROFILE_TRADE_COUNT, value);
            return this;
        }

        public Builder profile_last_online(String value) {
            values.put(PROFILE_LAST_ONLINE, value);
            return this;
        }
        
        public Builder profile_feedback_score(String value) {
            values.put(PROFILE_FEEDBACK_SCORE, value);
            return this;
        }

        public Builder bank_name(String value) {
            values.put(BANK_NAME, value);
            return this;
        }

        public Builder message(String value) {
            values.put(MESSAGE, value);
            return this;
        }

        public Builder sms_verification_required(boolean value) {
            values.put(SMS_VERIFICATION_REQUIRED, value);
            return this;
        }

        public Builder track_max_amount(boolean value) {
            values.put(TRACK_MAX_AMOUNT, value);
            return this;
        }

        public Builder trusted_required(boolean value) {
            values.put(TRUSTED_REQUIRED, value);
            return this;
        }
        
        public ContentValues build() {
            return values; 
        }
    }
}
