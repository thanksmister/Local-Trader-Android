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

public class ContactSync
{
    public Long _id;
    public String contact_id;
    public String created_at;
    public String currency;
    public String amount;
    public String amount_btc;
   
    public boolean is_funded; //contacts with escrow enabled and funded
    public boolean is_selling; // you are selling
    public boolean is_buying; // you are buying

   /* public String closed_at;
    public String disputed_at;
    public String funded_at;
    public String escrowed_at;
    public String canceled_at;
    public String released_at;
    public String payment_completed_at;*/

    public String seller_name;
    public String buyer_name;

    public String advertisement_id;
    public String advertisement_payment_method;
    public String advertisement_trade_type;
    public String advertiser_name;

    public int messageCount;
    public String lastMessageText;
    public String lastMessageSender;
}
