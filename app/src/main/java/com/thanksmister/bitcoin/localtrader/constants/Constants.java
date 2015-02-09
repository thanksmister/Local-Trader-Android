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

package com.thanksmister.bitcoin.localtrader.constants;

public class Constants
{
    public static final String CONTENT_AUTHORITY = "com.thanksmister.bitcoin.localtrader.provider";
    public static final String AUTHORITY = "com.thanksmister.bitcoin.localtrader.provider";
    public static final String ACCOUNT_TYPE = "com.thanksmister.bitcoin.localtrader.sync";
    public static final String ACCOUNT_NAME = "LocalBitcoins";
    
    public static String WEB_USER_URL = "https://localbitcoins.com/p/";
    public static final String BASE_URL = "https://localbitcoins.com/";

    public static final String DEFAULT_PRICE_EQUATION = "bitstampusd";
    public static final String DEFAULT_CURRENCY = "USD";
    public static final String DEFAULT_MARGIN = "1";

    public static int MAXIMUM_BTC_DECIMALS = 8;
    public static int MINIMUM_BTC_DECIMALS = 1;

    public static final String OAUTH_URL = "https://localbitcoins.com/oauth2/authorize/?ch=2hbo&client_id=" + Constants.CLIENT_ID + "&response_type=code&scope=read+write+money_pin";
    public static final String GOOGLE_PLUS_COMMUNITY = "https://plus.google.com/u/0/communities/114531451627808630329";
    public static final String GOOGLE_PLAY_RATING = "com.thanksmister.bitcoin.localtrader";
    public static final String BITCOIN_ADDRESS = "18M4NaYiCrtWhrifTuZY4uPMudcHLXCdNX";
    public static final String BITCOIN_URL = "https://blockchain.info/payment_request?address=18M4NaYiCrtWhrifTuZY4uPMudcHLXCdNX&amount=0.01#";
    public static final String EMAIL_ADDRESS = "mister@thanksmister.com";
    
    public static final String CLIENT_ID = "c37c50b0b7f4e7ad40c2"; 
    public static final String CLIENT_SECRET = "ba99c41345eb1a34e5501ae482ccdec2c462a83c";
    
    public static final String BLOCKCHAIN_INFO_ADDRESS = "https://blockchain.info/address/";
    
    public static final String REGISTRATION_URL = "http://goo.gl/xgUVP8";
    
    //public static final String CHANGE_TIP_URL = "https://www.changetip.com/tipme/thanksmister";
    
    public static Boolean USE_MOCK_DATA = false;
}
