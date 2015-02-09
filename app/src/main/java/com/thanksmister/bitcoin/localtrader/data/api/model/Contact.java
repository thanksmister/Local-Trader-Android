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

import java.util.Collections;
import java.util.List;

public class Contact
{
    public String reference_code;
    public String currency;
    public String amount;
    public String amount_btc;
    public String contact_id;

    public boolean is_funded; //contacts with escrow enabled and funded
    public boolean is_selling; // you are selling
    public boolean is_buying; // you are buying

    public String created_at;
    public String closed_at;
    public String disputed_at;
    public String funded_at;
    public String escrowed_at;
    public String canceled_at;
    public String released_at;
    public String payment_completed_at;
    public String exchange_rate_updated_at;

    public Seller seller = new Seller();
    public Buyer buyer = new Buyer();
    public Advertisement advertisement = new Advertisement();
    public Actions actions = new Actions();
    public Account_Details account_details = new Account_Details();

    public List<Message> messages = Collections.emptyList();
  
    public class Seller {
        public String username;
        public String feedback_score;
        public String trade_count;
        public String last_online;
        public String name;
    }

    public class Buyer {
        public String username;
        public String feedback_score;
        public String trade_count;
        public String last_online;
        public String name;
    }

    public class Actions {
        public String release_url; // ONLINE_SELL escrows only
        public String advertisement_public_view;
        public String message_url;
        public String message_post_url;
        public String mark_as_paid_url; // ONLINE_BUY
        public String dispute_url; // if eligible for dispute
        public String cancel_url; //  if eligible for canceling
        public String fund_url; //  contacts with escrow enabled but not funded
    }

    public class Account_Details {
        public String receiver_name;
        public String iban;
        public String swift_bic;
        public String reference;
    }

    public class Advertisement {
        public String id;
        public String payment_method;
        public TradeType trade_type;
        public Advertiser advertiser = new Advertiser();

        public class Advertiser {
            public String username;
            public String feedback_score;
            public String trade_count;
            public String last_online;
            public String name;
        }
    }
}
