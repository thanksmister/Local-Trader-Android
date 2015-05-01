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

import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;

import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class Contact
{
    public long id;
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
        public String messages_url;
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
        public String email;
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
    
    public Contact convertContentItemToContact(ContactItem contactItem)
    {
        Contact contact = new Contact();
        contact.id = contactItem.id();
        contact.contact_id = contactItem.contact_id();
        contact.reference_code = contactItem.reference_code();
        contact.currency = contactItem.currency();
        contact.amount = contactItem.amount();
        contact.amount_btc = contactItem.amount_btc();
        
        contact.is_funded = contactItem.is_funded();
        contact.is_selling = contactItem.is_selling();
        contact.is_buying = contactItem.is_buying();
        
        contact.created_at = contactItem.created_at();
        contact.closed_at = contactItem.closed_at();
        contact.disputed_at = contactItem.disputed_at();
        contact.funded_at = contactItem.funded_at();
        contact.escrowed_at = contactItem.escrowed_at();
        contact.canceled_at = contactItem.canceled_at();
        contact.released_at = contactItem.released_at();
        contact.payment_completed_at = contactItem.payment_completed_at();
        contact.exchange_rate_updated_at = contactItem.exchange_rate_updated_at();
        
        contact.actions.release_url = contactItem.release_url();
        contact.actions.advertisement_public_view = contactItem.advertisement_public_view();
        contact.actions.messages_url = contactItem.message_url();
        contact.actions.message_post_url = contactItem.message_post_url();
        contact.actions.mark_as_paid_url = contactItem.mark_as_paid_url();
        contact.actions.dispute_url = contactItem.dispute_url();
        contact.actions.cancel_url = contactItem.cancel_url();
        contact.actions.fund_url = contactItem.fund_url();
        
        contact.account_details.receiver_name = contactItem.account_receiver_name();
        contact.account_details.iban = contactItem.account_iban();
        contact.account_details.swift_bic = contactItem.account_swift_bic();
        contact.account_details.reference = contactItem.account_reference();
        contact.account_details.email = contactItem.account_receiver_email();
        
        contact.advertisement.id = contactItem.advertisement_id();
        contact.advertisement.payment_method = contactItem.advertisement_payment_method();
        contact.advertisement.trade_type = TradeType.valueOf(contactItem.advertisement_trade_type());
       
        contact.advertisement.advertiser.username = contactItem.advertiser_username();
        contact.advertisement.advertiser.feedback_score = contactItem.advertiser_feedback_score();
        contact.advertisement.advertiser.trade_count = contactItem.advertiser_trade_count();
        contact.advertisement.advertiser.last_online = contactItem.advertiser_last_online();
        contact.advertisement.advertiser.name = contactItem.advertiser_name();
        
        return contact;
    }
}
