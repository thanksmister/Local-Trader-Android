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

package com.thanksmister.bitcoin.localtrader.data.api.model;

import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;

public class Advertisement
{
    public String ad_id;
    public String created_at;
    public boolean visible = true;
    public String email;
    public String location;
    public String country_code;
    public String city;
    public TradeType trade_type = TradeType.LOCAL_SELL;
    public String min_amount;
    public String max_amount;
    public String max_amount_available;
    public String price_equation;
    public String currency;
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
    
    public String online_provider;
    public Profile profile = new Profile();
    public Actions actions = new Actions();
    public String distance;
    public String require_trade_volume;
    public String require_feedback_score;
    public String reference_type;

    public boolean isATM()
    {
        return atm_model != null;
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

    public Advertisement convertAdvertisementItemToAdvertisement(AdvertisementItem item)
    {
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
