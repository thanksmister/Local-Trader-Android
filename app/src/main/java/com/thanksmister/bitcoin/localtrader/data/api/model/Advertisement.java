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


/*
 /*
    {
   "bank_name":"Bank transfer Argentina",
   "countrycode":"AR",
   "visible":true,
   "price_equation":"bitstampusd*USD_in_ARS*.99",
   "account_info":"",
   "reference_type":"SHORT",
   "location_string":"Buenos Aires, Argentina",
   "city":"Buenos Aires",
   "age_days_coefficient_limit":"4.00",
   "currency":"ARS",
   "require_feedback_score":0,
   "max_amount_available":"5000",
   "sms_verification_required":false,
   "created_at":"2014-04-26T16:45:48+00:00",
   "first_time_limit_btc":null,
   "lat":0,
   "max_amount":"5000",
   "profile":{
      "feedback_score":100,
      "username":"thanksmister",
      "trade_count":"70+",
      "last_online":"2014-04-29T17:46:54+00:00",
      "name":"thanksmister (70+; 100%)"
   },
   "track_max_amount":false,
   "lon":0,
   "temp_price":"3542.81",
   "min_amount":"1000",
   "require_trade_volume":0,
   "trusted_required":false,
   "msg":"Contact hours: Lun-Ver 08:00-22:00\r\n\r\nYo necessito vos informaciones de Banco para transferencia.\r\n",
   "trade_type":"ONLINE_BUY",
   "online_provider":"NATIONAL_BANK",
   "ad_id":96345,
   "email":null,
   "atm_model":null,
   "volume_coefficient_btc":"1.50",
   "temp_price_usd":"442.63"
}
 */
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
    public String msg;
    public String min_amount;
    public String max_amount;
    public String max_amount_available;
    public String price_equation;
    public String currency;
    public String account_info;
    public double lat;
    public double lon;
  
    public String price;
    public String bank_name;
    public String nextUrl;
    public String atm_model;

    public boolean track_max_amount = false;
    public boolean sms_verification_required = false;
    public boolean trusted_required = false;
    
    public String online_provider;
    public String online_provider_name;
    public Profile profile = new Profile();
    public Actions actions = new Actions();
    public String distance;
    
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
