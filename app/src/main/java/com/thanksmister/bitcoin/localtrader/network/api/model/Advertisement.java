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

package com.thanksmister.bitcoin.localtrader.network.api.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

public class Advertisement implements Parcelable
{
    public String ad_id;
    public String created_at;
    public boolean visible = true;
    public String email;

    @SerializedName("location_string")
    @Expose
    public String location;

    @SerializedName("countrycode")
    @Expose
    public String country_code;
    public String city;
    public TradeType trade_type = TradeType.LOCAL_SELL;
    public String min_amount;
    public String max_amount;
    public String max_amount_available;
    public String price_equation;
    public String currency = "USD";
    public String account_info;
    public String message;
    public double lat;
    public double lon;
  
    public String temp_price;
    public String temp_price_usd;
    public String bank_name;
    public String nextUrl;
    public String atm_model;

    public boolean track_max_amount = false;
    public boolean sms_verification_required = false;
    public boolean trusted_required = false;
    public boolean require_identification = false;
    
    public String online_provider = TradeUtils.NATIONAL_BANK;
    public Profile profile = new Profile();
    public Actions actions = new Actions();
    public String distance;
    public String require_trade_volume;
    public String first_time_limit_btc;
    public String require_feedback_score;
    public String reference_type;
    public String phone_number;

    // "opening_hours": "null" or "[[sun_start, sun_end], [mon_start, mon_end], [tue_start, tue_end], [wed_start, wed_end], [thu_start, thu_end], [fri_start, fri_end], [sat_start, sat_end]"
    public String opening_hours;
    
    // TODO not implemented yet
    public boolean require_trusted_by_advertiser = false;
    public boolean is_local_office = false;
    public boolean hidden_by_opening_hours = false;
    public boolean is_low_risk = false;
    public int payment_window_minutes;
    public String age_days_coefficient_limit;
    public String volume_coefficient_btc;
    public boolean floating;
    public boolean display_reference;
    
    public Advertisement() {
    
    }

    protected Advertisement(Parcel in) {
        ad_id = in.readString();
        created_at = in.readString();
        visible = in.readByte() != 0;
        email = in.readString();
        location = in.readString();
        country_code = in.readString();
        city = in.readString();
        min_amount = in.readString();
        max_amount = in.readString();
        max_amount_available = in.readString();
        price_equation = in.readString();
        currency = in.readString();
        account_info = in.readString();
        message = in.readString();
        lat = in.readDouble();
        lon = in.readDouble();
        temp_price = in.readString();
        temp_price_usd = in.readString();
        bank_name = in.readString();
        nextUrl = in.readString();
        atm_model = in.readString();
        track_max_amount = in.readByte() != 0;
        sms_verification_required = in.readByte() != 0;
        trusted_required = in.readByte() != 0;
        require_identification = in.readByte() != 0;
        require_trusted_by_advertiser = in.readByte() != 0;
        is_local_office = in.readByte() != 0;
        hidden_by_opening_hours = in.readByte() != 0;
        is_low_risk = in.readByte() != 0;
        online_provider = in.readString();
        distance = in.readString();
        require_trade_volume = in.readString();
        first_time_limit_btc = in.readString();
        require_feedback_score = in.readString();
        reference_type = in.readString();
        phone_number = in.readString();
        payment_window_minutes = in.readInt();
        age_days_coefficient_limit = in.readString();
        volume_coefficient_btc = in.readString();
        floating = in.readByte() != 0;
        display_reference = in.readByte() != 0;
        opening_hours = in.readString();
    }

    public static final Creator<Advertisement> CREATOR = new Creator<Advertisement>() {
        @Override
        public Advertisement createFromParcel(Parcel in) {
            return new Advertisement(in);
        }

        @Override
        public Advertisement[] newArray(int size) {
            return new Advertisement[size];
        }
    };

    public boolean isATM()
    {
        return atm_model != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ad_id);
        dest.writeString(created_at);
        dest.writeByte((byte) (visible ? 1 : 0));
        dest.writeString(email);
        dest.writeString(location);
        dest.writeString(country_code);
        dest.writeString(city);
        dest.writeString(min_amount);
        dest.writeString(max_amount);
        dest.writeString(max_amount_available);
        dest.writeString(price_equation);
        dest.writeString(currency);
        dest.writeString(account_info);
        dest.writeString(message);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeString(temp_price);
        dest.writeString(temp_price_usd);
        dest.writeString(bank_name);
        dest.writeString(nextUrl);
        dest.writeString(atm_model);
        dest.writeByte((byte) (track_max_amount ? 1 : 0));
        dest.writeByte((byte) (sms_verification_required ? 1 : 0));
        dest.writeByte((byte) (trusted_required ? 1 : 0));
        dest.writeByte((byte) (require_identification ? 1 : 0));
        dest.writeByte((byte) (require_trusted_by_advertiser ? 1 : 0));
        dest.writeByte((byte) (is_local_office ? 1 : 0));
        dest.writeByte((byte) (hidden_by_opening_hours ? 1 : 0));
        dest.writeByte((byte) (is_low_risk ? 1 : 0));
        dest.writeString(online_provider);
        dest.writeString(distance);
        dest.writeString(require_trade_volume);
        dest.writeString(first_time_limit_btc);
        dest.writeString(require_feedback_score);
        dest.writeString(reference_type);
        dest.writeString(phone_number);
        dest.writeInt(payment_window_minutes);
        dest.writeString(age_days_coefficient_limit);
        dest.writeString(volume_coefficient_btc);
        dest.writeByte((byte) (floating ? 1 : 0));
        dest.writeByte((byte) (display_reference ? 1 : 0));
        dest.writeString(opening_hours);
    }

    public class Profile {
        public String name;
        public String trade_count;
        public String feedback_score;
        public String username;
        public String last_online;
    }

    public class Actions {
        public String public_view;
    }

    public Advertisement convertAdvertisementItemToAdvertisement(AdvertisementItem item) {
        Advertisement advertisement = new Advertisement();
        advertisement.ad_id = item.ad_id();
        advertisement.created_at = item.created_at();
        advertisement.visible = item.visible();
        advertisement.email = item.email();
        advertisement.location = item.location_string();
        advertisement.country_code = item.country_code();
        advertisement.city = item.city();
        advertisement.trade_type = TradeType.valueOf(item.trade_type());
        advertisement.min_amount = item.min_amount();
        advertisement.max_amount = item.max_amount();
        advertisement.max_amount_available = item.max_amount_available();
        advertisement.price_equation = item.price_equation();
        advertisement.currency = item.currency();
        advertisement.account_info = item.account_info();
        advertisement.message = item.message();
        advertisement.lat = item.lat();
        advertisement.lon = item.lon();
        advertisement.temp_price = item.temp_price();
        advertisement.temp_price_usd = item.temp_price_usd();
        advertisement.bank_name = item.bank_name();
        advertisement.atm_model = item.atm_model();
        advertisement.track_max_amount = item.track_max_amount();
        advertisement.sms_verification_required = item.sms_verification_required();
        advertisement.trusted_required = item.trusted_required();
        advertisement.online_provider = item.online_provider();
        advertisement.require_trade_volume = item.require_trade_volume();
        advertisement.require_feedback_score = item.require_feedback_score();
        advertisement.reference_type = item.reference_type();
        advertisement.phone_number = item.phone_number();
        advertisement.opening_hours = item.opening_hours();
        
        // profile
        advertisement.profile.name = item.profile_name();
        advertisement.profile.username = item.profile_username();
        advertisement.profile.last_online = item.profile_last_online();
        advertisement.profile.trade_count = item.profile_trade_count();
        advertisement.profile.feedback_score = item.profile_feedback_score();
        
        // actions
        advertisement.actions.public_view = item.action_public_view();
                
        return advertisement;
    }
}
