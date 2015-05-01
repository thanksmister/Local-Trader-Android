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

package com.thanksmister.bitcoin.localtrader.data.api.model;

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
}
