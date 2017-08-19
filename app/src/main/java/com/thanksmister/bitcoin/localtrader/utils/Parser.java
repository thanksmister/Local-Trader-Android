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

package com.thanksmister.bitcoin.localtrader.utils;

import android.net.Uri;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.data.api.model.Place;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.data.api.model.TransactionType;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import retrofit.client.Response;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils.CODE_MINUS_ONE;

public class Parser
{
    public static Authorization parseAuthorization(String response)
    {
        JSONObject jsonObject;
        Authorization authorization = new Authorization();
        try {
            jsonObject = new JSONObject(response);
            authorization.access_token = jsonObject.getString("access_token");
            authorization.refresh_token = jsonObject.getString("refresh_token");
            authorization.expires_in = jsonObject.getString("expires_in");
            return authorization;

        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }
    }

    //{"data": {"message": "Ad deleted successfully!"}}
    public static String parseJSONResponse(JSONObject jsonObject)
    {
        String message = "";

        try {
            if (jsonObject.has("data")) {
                JSONObject data = jsonObject.getJSONObject("data");
                message = data.getString("message");
            }
            return message;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        return null;
    }
    
    public static String parseRetrofitResponse(Response response) throws RetroError
    {
        if(response == null || response.getBody() == null)
            throw new RetroError("Error connecting to service.", CODE_MINUS_ONE);
        
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(response.getBody().in()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        return sb.toString();
    }

    public static boolean containsError(JSONObject jsonObject)
    {
        String response = jsonObject.toString();
        return containsError(response);
    }

    private static boolean containsError(String response)
    {
        if (response.contains("error_code") && response.contains("error")) {
            return true;
        }
        return false;
    }
    
    public static JSONObject parseResponseToJsonObject(Response response)
    {
        //Try to get response body
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(response.getBody().in()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(sb.toString());
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new JSONObject();
    }

    public static RetroError parseError(JSONObject jsonObject)
    {
        try {
            JSONObject errorObj = jsonObject.getJSONObject("error");
            int error_code = errorObj.getInt("error_code");
            String error_message = errorObj.getString("message"); // TODO this is too generic
            
            if(errorObj.has("errors")) {
                error_message = "";
                JSONObject errors = errorObj.getJSONObject("errors");
                Iterator<?> keys = errors.keys();
                while( keys.hasNext() ) {
                    String key = (String) keys.next();
                    String message = errors.getString(key);
                    error_message += Strings.convertCamelCase(key) + " " + message + " ";
                }
            }
            
            return new RetroError(error_message, error_code);

        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return parseInvalidGrantError(jsonObject.toString());
        }
    }

    public static RetroError parseError(String response)
    {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject errorObj = jsonObject.getJSONObject("error");
            int error_code = errorObj.getInt("error_code");
            String error_message = errorObj.getString("message");
            return new RetroError(error_message, error_code);

        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return parseInvalidGrantError(response);
        }
    }

    /**
     * {
     "error_description":"* error\n  * i\n  * n\n  * v\n  * a\n  * l\n  * i\n  * d\n  * _\n  * g\n  * r\n  * a\n  * n\n  * t",
     "error":"invalid_grant"
     }
     * @param response
     * @return
     */
    private static RetroError parseInvalidGrantError(String response)
    {
        if(response.contains("invalid_grant")) {
            return new RetroError("Invalid refresh token, access denied.", 403);
        } else {
            return new RetroError("Unknown error has occurred. If this continues try logging in again.", 400);
        }
    }

    public static User parseUser(String response)
    {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }

        try {
            User user = new User();
            JSONObject object = jsonObject.getJSONObject("data");
            user.username = (object.getString("username"));
            user.age_text = (object.getString("age_text"));
            user.feedback_count = (Integer.parseInt(object.getString("feedback_count")));
            user.has_common_trades = ((object.getString("has_common_trades").equals("true")));
            user.confirmed_trade_count_text = (object.getString("confirmed_trade_count_text"));
            user.trade_volume_text = (object.getString("trade_volume_text"));
            user.blocked_count = (Integer.parseInt(object.getString("blocked_count")));
            user.feedback_score = (object.getString("feedback_score"));
            user.feedbacks_unconfirmed_count = (Integer.parseInt(object.getString("feedbacks_unconfirmed_count")));
            user.trading_partners_count = (Integer.parseInt(object.getString("trading_partners_count")));
            user.trusted_count = (Integer.parseInt(object.getString("trusted_count")));
            user.url = (object.getString("url"));
            user.created_at = (object.getString("created_at"));
            return user;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        return null;
    }
    
    public static List<Contact> parseContacts(String response)
    {
        JSONObject jsonObject;
        List<Contact> items = new ArrayList<Contact>();
        
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return items;
        }

        try {
            JSONObject dataObject = jsonObject.getJSONObject("data");
            JSONArray contactListObject = dataObject.getJSONArray("contact_list");
            
            for (int i = 0; i < contactListObject.length(); i++) {
                JSONObject item = contactListObject.getJSONObject(i);
                Contact contact = createContact(item); // you are selling, they are buying
                if (contact != null)
                    items.add(contact);
            }

        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        return items;
    }
    
    public static Contact parseContact(String response)
    {
        JSONObject jsonObject;
        
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }
        
        return createContact(jsonObject);
    }
    
    private static Contact createContact(JSONObject object)
    {
        Contact item = new Contact();

        try {
            JSONObject data = object.getJSONObject("data");

            JSONObject buyer = data.getJSONObject("buyer");
            item.buyer.username = (buyer.getString("username"));
            item.buyer.feedback_score = (buyer.getString("feedback_score"));
            item.buyer.last_online = (buyer.getString("last_online"));
            item.buyer.trade_count = (buyer.getString("trade_count"));
            item.buyer.name = (buyer.getString("name"));

            JSONObject seller = data.getJSONObject("seller");
            item.seller.username = (seller.getString("username"));
            item.seller.feedback_score = (seller.getString("feedback_score"));
            item.seller.last_online = (seller.getString("last_online"));
            item.seller.trade_count = (seller.getString("trade_count"));
            item.seller.name = (seller.getString("name"));

            item.is_buying = (data.getString("is_buying").equals("true"));
            item.is_selling = (data.getString("is_selling").equals("true"));

            item.amount = (data.getString("amount"));
            item.contact_id = (data.getString("contact_id"));
            item.amount_btc = (data.getString("amount_btc"));
            item.created_at = (data.getString("created_at"));

            if (!data.isNull("released_at"))
                item.released_at = (data.getString("released_at"));

            if (!data.isNull("disputed_at"))
                item.disputed_at = (data.getString("disputed_at"));

            if (!data.isNull("closed_at"))
                item.closed_at = (data.getString("closed_at"));

            if (!data.isNull("escrowed_at"))
                item.escrowed_at = (data.getString("escrowed_at"));

            if (!data.isNull("canceled_at"))
                item.canceled_at = (data.getString("canceled_at"));

            if (!data.isNull("funded_at")) {
                item.funded_at = (data.getString("funded_at"));
                item.is_funded = true;
            }

            if (data.has("payment_completed_at") && !data.isNull("payment_completed_at")) {
                item.payment_completed_at = (data.getString("payment_completed_at"));
            }
            
            if (data.has("currency")){
                item.currency = data.getString("currency");
            }
            
            if (data.has("exchange_rate_updated_at")) {
                item.exchange_rate_updated_at = (data.getString("exchange_rate_updated_at"));
            }
            if (data.has("reference_code")) {
                item.reference_code = (data.getString("reference_code"));
            }

            if (!data.isNull("account_details") && data.has("account_details")) {
                
                JSONObject account_details = data.getJSONObject("account_details");
                
                if (account_details.has("receiver_name")) {
                    item.account_details.receiver_name = account_details.getString("receiver_name");
                }
                
                if (account_details.has("receiver_email")) {
                    item.account_details.receiver_email = account_details.getString("receiver_email");
                }
                
                if (account_details.has("iban")) {
                    item.account_details.iban = account_details.getString("iban");
                }
                
                if (account_details.has("swift_bic")) {
                    item.account_details.swift_bic = account_details.getString("swift_bic");
                }
                
                if (account_details.has("reference")) {
                    item.account_details.reference = account_details.getString("reference");
                }

                if (account_details.has("ethereum_address")) {
                    item.account_details.ethereum_address = account_details.getString("ethereum_address");
                }

                if (account_details.has("phone_number")) {
                    item.account_details.phone_number = account_details.getString("phone_number");
                }
                
                if (account_details.has("bsb")) {
                    item.account_details.bsb = account_details.getString("bsb");
                }
                
                if (account_details.has("biller_code")) {
                    item.account_details.biller_code = account_details.getString("biller_code");
                }
                
                if (account_details.has("account_number")) {
                    item.account_details.account_number = account_details.getString("account_number");
                }
                
                if (account_details.has("sort_code")) {
                    item.account_details.sort_code = account_details.getString("sort_code");
                }
            }

            JSONObject advertisement = data.getJSONObject("advertisement");
            if (advertisement.has("id")) {
                item.advertisement.id = (advertisement.getString("id"));
            }
            
            if (advertisement.has("payment_method")) {
                item.advertisement.payment_method = (advertisement.getString("payment_method"));
            }
            
            if (advertisement.has("trade_type")) {
                String trade_type = advertisement.getString("trade_type");
                if(trade_type.equals(TradeType.LOCAL_BUY.name())
                        || trade_type.equals(TradeType.LOCAL_SELL.name())
                        || trade_type.equals(TradeType.ONLINE_BUY.name())
                        || trade_type.equals(TradeType.ONLINE_SELL.name())) {
                    item.advertisement.trade_type = (TradeType.valueOf(trade_type));
                } else {
                    if(BuildConfig.DEBUG) {
                        Crashlytics.setString("contact_data_key", data.toString());
                        Crashlytics.logException(new Throwable("Found invalid trade type for contact: " + trade_type));
                    }
                    item.advertisement.trade_type = TradeType.NONE; 
                }
            }

            JSONObject advertiser = advertisement.getJSONObject("advertiser");
            item.advertisement.advertiser.username = (advertiser.getString("username"));
            item.advertisement.advertiser.feedback_score = (advertiser.getString("feedback_score"));
            item.advertisement.advertiser.last_online = (advertiser.getString("last_online"));
            item.advertisement.advertiser.trade_count = (advertiser.getString("trade_count"));
            item.advertisement.advertiser.name = (advertiser.getString("name"));

            if (data.has("is_funded")) { //Boolean signaling if the escrow is enabled and not funded.
                item.is_funded = (data.getBoolean("is_funded"));
            }

            JSONObject actions = object.getJSONObject("actions");

            if (actions.has("release_url")) {
                item.actions.release_url = (actions.getString("release_url"));
            }

            if (actions.has("cancel_url")) {
                item.actions.cancel_url = (actions.getString("cancel_url"));
            }

            if (actions.has("mark_as_paid_url")) {
                item.actions.mark_as_paid_url = (actions.getString("mark_as_paid_url"));
            }

            if (actions.has("dispute_url")) {
                item.actions.dispute_url = (actions.getString("dispute_url"));
            }

            if (actions.has("fund_url")) {
                item.actions.fund_url = (actions.getString("fund_url"));
            }

            if (actions.has("advertisement_public_view")) {
                item.actions.advertisement_public_view = actions.getString("advertisement_public_view");
            }

            if (actions.has("messages_url")) {
                item.actions.messages_url = actions.getString("messages_url");
            }

            if (actions.has("message_post_url")) {
                item.actions.message_post_url = actions.getString("message_post_url");
            }

            return item;

        
        } catch (JSONException e) {
            Timber.e("Error Parsing Contact: " + e.getMessage());
            Timber.e("Error Parsing Contact: " + object.toString());
            if(!BuildConfig.DEBUG) {
                Crashlytics.setString("Message", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing contact: " + object.toString()));
            }
        }
        
    return null;
    }

    public static ArrayList<Notification> parseNotifications(String response)
    {
        JSONObject jsonObject;
        ArrayList<Notification> results = new ArrayList<Notification>();
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return results;
        }

        try {
            JSONArray notifications_list = jsonObject.getJSONArray("data");

            for (int i = 0; i < notifications_list.length(); i++) {
                JSONObject notificationObj = notifications_list.getJSONObject(i);
                Notification notification = parseNotification(notificationObj);
                if (notification != null) results.add(notification);
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }
        
        return results;
    }
    
    private static Notification parseNotification(JSONObject jsonObject)
    {
        Notification notification = new Notification();
        try {
            if (jsonObject.has("url")) notification.url = (jsonObject.getString("url"));
            if (jsonObject.has("created_at")) notification.created_at = (jsonObject.getString("created_at"));
            if (jsonObject.has("contact_id")) notification.contact_id = (jsonObject.getString("contact_id"));
            if (jsonObject.has("advertisement_id")) notification.advertisement_id = (jsonObject.getString("advertisement_id"));
            if (jsonObject.has("read")) notification.read = (jsonObject.getBoolean("read"));
            if (jsonObject.has("msg")) notification.msg = jsonObject.getString("msg");
            if (jsonObject.has("id")) notification.notification_id = (jsonObject.getString("id"));
            return notification;
        } catch (JSONException e) {
            Timber.e("Error Parsing Notification: " + e.getMessage());
        }

        return null;
    }
    
    public static ArrayList<Message> parseMessages(String response)
    {
        JSONObject jsonObject;
        ArrayList<Message> results = new ArrayList<Message>();
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return results;
        }

        try {
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray message_list = data.getJSONArray("message_list");

            for (int i = 0; i < message_list.length(); i++) {
                JSONObject messageObj = message_list.getJSONObject(i);
                Message message = parseMessage(messageObj);
                if (message != null) results.add(message);
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        Collections.reverse(results);
        //Collections.sort(results, new MessagesComparator());
        
        return results;
    }

    public static Message parseMessage(JSONObject messageObj)
    {
        Message message = new Message();
        
        try {
            
            JSONObject sender = messageObj.getJSONObject("sender");
            if (sender.has("username")) message.sender.username = (sender.getString("username"));
            if (sender.has("name")) message.sender.name = (sender.getString("name"));
            if (sender.has("trade_count")) message.sender.trade_count = (sender.getString("trade_count"));
            if (sender.has("last_online")) message.sender.last_seen_on = (sender.getString("last_online"));
            if (messageObj.has("contact_id")) message.contact_id = (messageObj.getString("contact_id"));
            if (messageObj.has("created_at")) message.created_at = (messageObj.getString("created_at"));
            if (messageObj.has("msg")) message.msg = (Uri.decode(messageObj.getString("msg")));
            if (messageObj.has("is_admin")) message.is_admin = (Boolean.valueOf(messageObj.getString("is_admin")));
            if (messageObj.has("attachment_name")) message.attachment_name = (messageObj.getString("attachment_name"));
            if (messageObj.has("attachment_type")) message.attachment_type = (messageObj.getString("attachment_type"));
            if (messageObj.has("attachment_url")) message.attachment_url = (messageObj.getString("attachment_url"));

            return message;

        } catch (JSONException e) {
            
            Timber.e("Error Parsing: " + e.getMessage());
            if(!BuildConfig.DEBUG) {
                Crashlytics.setString("Message", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing advertisement: " + messageObj.toString()));
            }
           
        }

        return null;
    }

    public static Wallet parseWallet(String response)
    {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }

        Wallet wallet = new Wallet();

        try {

            JSONObject data = jsonObject.getJSONObject("data");

            wallet.message = (data.getString("message"));
            wallet.address = data.getString("receiving_address");
            
            JSONObject total = data.getJSONObject("total");
            
            wallet.balance = (total.getString("balance"));
            wallet.sendable = (total.getString("sendable"));
            
            JSONArray sent_transactions = data.getJSONArray("sent_transactions_30d");

            ArrayList<Transaction> sentTransactions = new ArrayList<>();
            for (int i = 0; i < sent_transactions.length(); i++) {

                JSONObject object = (JSONObject) sent_transactions.get(i);

                Transaction transaction = new Transaction();
                transaction.type = (TransactionType.SENT);
                transaction.created_at = (object.getString("created_at"));

                if (object.has("tx_type") && !object.isNull("tx_type"))
                    transaction.tx_type = object.getString("tx_type");

                if (object.has("txid") && !object.isNull("txid")) {
                    transaction.txid = (object.getString("txid"));
                } else {
                    transaction.txid = transaction.created_at;
                }
                
                if (object.has("amount"))
                    transaction.amount = (object.getString("amount"));

                if (object.has("description"))
                    transaction.description = (object.getString("description"));

                
                if (transaction.description != null && transaction.description.toLowerCase().contains("fee")) {
                    transaction.type = (TransactionType.FEE);
                } else if (transaction.description != null && (transaction.description.toLowerCase().contains("contact")
                        || transaction.description.toLowerCase().contains("bitcoin sell"))) {
                    transaction.type = (TransactionType.CONTACT_SENT);

                } else if (transaction.description != null && transaction.description.toLowerCase().contains("internal")) {
                    transaction.type = (TransactionType.INTERNAL);
                } else if (transaction.description != null && transaction.description.toLowerCase().contains("reserve")) {
                    transaction.type = (TransactionType.SENT);
                } 

                sentTransactions.add(transaction);
            }

            wallet.sent_transactions = sentTransactions;

            JSONArray received_transactions = data.getJSONArray("received_transactions_30d");
            ArrayList<Transaction> receivedTransactions = new ArrayList<>();
            for (int i = 0; i < received_transactions.length(); i++) {

                JSONObject object = (JSONObject) received_transactions.get(i);

                Transaction transaction = new Transaction();
                transaction.type = (TransactionType.RECEIVED);
                transaction.created_at = (object.getString("created_at"));

                
                if (object.has("tx_type") && !object.isNull("tx_type"))
                    transaction.tx_type = object.getString("tx_type");

                
                if (object.has("txid") && !object.isNull("txid")) {
                    transaction.txid = (object.getString("txid"));
                } else {
                    transaction.txid = transaction.created_at;
                }
                
                if (object.has("amount"))
                    transaction.amount = (object.getString("amount"));

                if (object.has("description"))
                    transaction.description = (object.getString("description"));
                
                if (transaction.description != null && transaction.description.toLowerCase().contains("contact")) {
                    transaction.type = (TransactionType.CONTACT_RECEIVE);
                } else if (transaction.description != null && transaction.description.toLowerCase().contains("internal")) {
                    transaction.type = (TransactionType.INTERNAL);
                } else if (transaction.description != null && transaction.description.toLowerCase().contains("reserve")) {
                    transaction.type = (TransactionType.RESERVE);
                } else if (transaction.description != null && transaction.description.toLowerCase().contains("affiliate")) {
                    transaction.type = (TransactionType.AFFILIATE);
                }

                receivedTransactions.add(transaction);
            }

            wallet.receiving_transactions = receivedTransactions;

            // just get the first address
            /*JSONArray receiving_address_list = data.getJSONArray("receiving_address_list");
            JSONObject object = (JSONObject) receiving_address_list.get(receiving_address_list.length() - 1);
            wallet.address = (object.getString("address"));
            wallet.received = (object.getString("received"))*/;

            return wallet;

        } catch (Exception e) {
           
            Timber.e("Error Parsing: " + e.getMessage());
            if(!BuildConfig.DEBUG) {
                Crashlytics.setString("Wallet", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing wallet: " + e.getMessage()));
                //Timber.e("Error Parsing: " + e.getMessage());
            }
        }

        return null;
    }

    public static Wallet parseWalletBalance(String response)
    {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }

        Wallet wallet = new Wallet();

        try {

            JSONObject data = jsonObject.getJSONObject("data");
            wallet.message = (data.getString("message"));
            wallet.address = (data.getString("receiving_address"));
            
            JSONObject total = data.getJSONObject("total");
            
            wallet.balance = (total.getString("balance"));
            wallet.sendable = (total.getString("sendable"));

            return wallet;

        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        return null;
    }

    public static List<Advertisement> parseAdvertisements(String response)
    {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }

        ArrayList<Advertisement> items = new ArrayList<Advertisement>();
        String nextUrl = null;
        try {
            JSONObject object = jsonObject.getJSONObject("data");
            if (object.has("pagination")) {
                JSONObject pagination = object.getJSONObject("pagination");
                if (pagination.has("next")) {
                    nextUrl = pagination.getString("next");
                }
            }

            JSONArray ad_list = object.getJSONArray("ad_list");
            for (int i = 0; i < ad_list.length(); i++) {
                JSONObject obj = ad_list.getJSONObject(i);
                Advertisement item = parseAdvertisement(obj, nextUrl);
                if (item != null) items.add(item);
            }
        } catch (JSONException e) {
            Timber.e("Error Parsing Ads: " + e.getMessage());
        }
        
        //Collections.sort(items, new AdvertisementNameComparator());
        return items;
    }

    private static class AdvertisementNameComparator implements Comparator<Advertisement>
    {
        @Override
        public int compare(Advertisement o1, Advertisement o2) {
            return o1.distance.toLowerCase().compareTo(o2.distance.toLowerCase());
        }
    }

    public static List<Method> parseMethods(String response)
    {
        JSONObject jsonObject;
        JSONObject dataObject;
        ArrayList<Method> methods = new ArrayList<>();
        try {
            jsonObject = new JSONObject(response);
            dataObject = jsonObject.getJSONObject("data");
            
            JSONObject methodsObject = dataObject.getJSONObject("methods");
            Iterator<?> keys = methodsObject.keys();
            while( keys.hasNext() ){
                Method method = new Method();
                String key = (String) keys.next();
                try {
                    if( methodsObject.get(key) instanceof JSONObject ) {
                        method.key = key;
                        JSONObject obj = (JSONObject) methodsObject.get(key);
                        if(obj.has("code")) method.code = (obj.getString("code"));
                        if(obj.has("name")) method.name = (obj.getString("name"));
                        methods.add(method);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }

        Collections.sort(methods, new MethodNameComparator());
        
        return methods;
    }

    private static class MethodNameComparator implements Comparator<Method>
    {
        @Override
        public int compare(Method o1, Method o2) {
            return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
        }
    }

    private static class MessagesComparator implements Comparator<Message>
    {
        @Override
        public int compare(Message o1, Message o2) {

            Date d1 = null;
            Date d2 = null;
            d1 = Dates.parseLocalDate(o1.created_at);
            d2 = Dates.parseLocalDate(o2.created_at);
            return (d1.getTime() < d2.getTime() ? -1 : 1);     //descending
        }
    }

    public static Advertisement parseAdvertisement(String response)
    {
        JSONObject jsonObject;
        JSONObject object = null;
        try {
            jsonObject = new JSONObject(response);
            object = jsonObject.getJSONObject("data");
            
            JSONArray ad_list = object.getJSONArray("ad_list");
            JSONObject obj = ad_list.getJSONObject(0);
            return parseAdvertisement(obj, null);
            
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            
            if(!BuildConfig.DEBUG) {
                Crashlytics.setString("Advertisement", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing advertisement: " + object));
            }
            return null;
        }
    }

    public static Advertisement parseAdvertisement(JSONObject object, String nextUrl)
    {
        Advertisement item = new Advertisement();

        try {
            JSONObject data = object.getJSONObject("data");
            JSONObject actions = object.getJSONObject("actions");
            
            //Timber.d("Advertisement Data: " + data.toString());

            item.ad_id = (data.getString("ad_id"));
            item.created_at = (data.getString("created_at"));
            
            if (data.has("atm_model") && !data.isNull("atm_model")) {
                String atm = (data.getString("atm_model"));
                item.atm_model = atm;
            }

            item.visible = (data.getBoolean("visible"));
            item.temp_price = ((data.getString("temp_price")));
            item.temp_price_usd = ((data.getString("temp_price_usd")));
            
            item.temp_price_usd = ((data.getString("temp_price_usd")));
            item.temp_price_usd = ((data.getString("temp_price_usd")));

            if (data.has("require_feedback_score"))
                item.require_feedback_score = ((data.getString("require_feedback_score")));

            if (data.has("require_trade_volume"))
                item.require_trade_volume = data.getString("require_trade_volume");

            if (data.has("first_time_limit_btc") && !data.getString("first_time_limit_btc").equals("null"))
                item.first_time_limit_btc = (data.getString("first_time_limit_btc"));

            if (data.has("require_identification"))
                item.require_identification = (data.getBoolean("require_identification"));
            
            item.email = (data.getString("email"));
            item.location = (data.getString("location_string"));
            
            item.country_code = (data.getString("countrycode"));
            Timber.d("Country Code: " + item.country_code);
            
            item.city = (data.getString("city"));

            String trade_type = data.getString("trade_type");
            item.trade_type = (TradeType.valueOf(trade_type));
            item.online_provider = (data.getString("online_provider"));
            
            if (data.has("price_equation"))
                item.price_equation = (data.getString("price_equation"));
            
            if (data.has("reference_type"))
                item.reference_type = (data.getString("reference_type"));
            
            if (data.has("track_max_amount")) item.track_max_amount = (data.getBoolean("track_max_amount"));
            if (data.has("trusted_required")) item.trusted_required = (data.getBoolean("trusted_required"));
            if (data.has("sms_verification_required")) item.sms_verification_required = (data.getBoolean("sms_verification_required"));

            item.currency = (data.getString("currency"));
            
            if (data.has("account_info")) {
                item.account_info = data.getString("account_info");
                //Timber.d("Account Info: " + data.getString("account_info"));
            }
            
            item.lat = (Float.parseFloat(data.getString("lat")));
            item.lon = (Float.parseFloat(data.getString("lon")));

            if (data.has("distance")) 
                item.distance = (data.getString("distance")); // for public searches only

            if (data.has("bank_name") && !data.isNull("bank_name")) {
                item.bank_name = (data.getString("bank_name"));
            }

            if (data.has("msg") && !data.isNull("msg")) {
                String message = (data.getString("msg"));
                //message = message.replace("\n", "").replace("\r", "<br>");
                item.message = message;
            }

            if (data.has("min_amount") && !data.isNull("min_amount")) {
                String min_amount = data.getString("min_amount");
                String min[] = min_amount.split(".");
                item.min_amount = ((min.length > 0) ? min[0] : data.getString("min_amount"));
            } 

            if (data.has("max_amount") && !data.isNull("max_amount")) {
                String max_amount = data.getString("max_amount");
                String max[] = max_amount.split(".");
                    item.max_amount = ((max.length > 0) ? max[0] : data.getString("max_amount"));
            } 
            if (data.has("max_amount_available") && !data.isNull("max_amount_available")) {
                item.max_amount_available = data.getString("max_amount_available");
            }

            if (actions.has("public_view")) {
                item.actions.public_view = (actions.getString("public_view"));
            }

            if (data.has("profile")) {
                JSONObject profile = data.getJSONObject("profile");
                if (profile.has("last_online")) item.profile.last_online = (profile.getString("last_online"));
                item.profile.name = (profile.getString("name"));
                item.profile.username = (profile.getString("username"));
                if (profile.has("feedback_score")) item.profile.feedback_score = (profile.getString("feedback_score"));
                if (profile.has("trade_count")) item.profile.trade_count = (profile.getString("trade_count"));
            }

            if(nextUrl != null)
                item.nextUrl = nextUrl;

            return item;

        } catch (JSONException e) {
            Timber.e("Error Parsing: " + e.getMessage());
            if(!BuildConfig.DEBUG) {
                Crashlytics.setString("Advertisement", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing advertisement: " + object.toString()));
            }
        }

        return null;
    }

    public static Place parsePlace(String responseString)
    {
        Place place = new Place();

        try {
            JSONObject jsonObject = new JSONObject(responseString);
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray places = data.getJSONArray("places");
            if (places.length() > 0) {
                JSONObject placeObject = (JSONObject) places.get(0);
                place.location_string = (placeObject.getString("location_string"));
                place.buy_local_url = (placeObject.getString("buy_local_url"));
                place.sell_local_url = (placeObject.getString("sell_local_url"));
                place.url = (placeObject.getString("url"));
                place.lon = (placeObject.getString("lon"));
                place.lat = (placeObject.getString("lat"));
                return place;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ContactRequest parseContactRequest(String response)
    {
        ContactRequest contactRequest = new ContactRequest();

        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject data = jsonObject.getJSONObject("data");
            contactRequest.contact_id = (data.getString("contact_id"));
            //contactRequest.setType(ContactType.BUY);
            return contactRequest;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
    "ALL": {
    "ask": 38655.83,
    "bid": 38619.71,
    "last": 38651.92,
    "timestamp": "Sat, 08 Nov 2014 17:25:27 -0000",
    "volume_btc": 0.0,
    "volume_percent": 0.0
  }
     */
    public static List<Currency> parseCurrencies(String response)
    {
        JSONObject jsonObject;

        String date = Dates.getLocalDateMilitaryTime();

        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        ArrayList<Currency> currencies = new ArrayList<Currency>();
        ArrayList<String> keyList = new ArrayList<String>();

        Iterator<?> keys = jsonObject.keys();
        while( keys.hasNext() ) {
            Currency currency = new Currency();
            String key = (String) keys.next();
            keyList.add(key);

            try {
                if( jsonObject.get(key) instanceof JSONObject ) {
                    
                    JSONObject obj = (JSONObject) jsonObject.get(key);
                    currency.ticker = key;
                    currency.date = date;
                    
                    if(obj.has("last")) currency.last =(obj.getString("last"));
                    if(obj.has("volume_btc"))currency.volume_btc =(Float.parseFloat(obj.getString("volume_btc")));
                    
                    currencies.add(currency);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Currency currency = new Currency();
        currency.ticker = "XAR";
        currencies.add(currency);
        
        Collections.sort(currencies, new ExchangeNameComparator());
        return currencies;
    }

    public static class ExchangeNameComparator implements Comparator<Currency>
    {
        @Override
        public int compare(Currency o1, Currency o2) {
            return o1.ticker.toLowerCase().compareTo(o2.ticker.toLowerCase());
        }
    }

    public static List<ExchangeCurrency> parseExchangeCurrencies(String response) {
        
        JSONObject jsonObject;
        ArrayList<ExchangeCurrency> currencies = new ArrayList<>();

        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            JSONObject currenciesObject = dataObject.getJSONObject("currencies");
            Iterator<?> keys = currenciesObject.keys();
            while( keys.hasNext() ){
                String key = (String) keys.next();
                ExchangeCurrency currency = new ExchangeCurrency(key);
                currencies.add(currency);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        Collections.sort(currencies, new CurrencyComparator());
        Timber.d("Currencies: " + currencies);
        return currencies;
    }

    public static class CurrencyComparator implements Comparator<ExchangeCurrency>
    {
        @Override
        public int compare(ExchangeCurrency o1, ExchangeCurrency o2) {
            return o1.getCurrency().toLowerCase().compareTo(o2.getCurrency().toLowerCase());
        }
    }

    public static List<ExchangeCurrency> parseCoinbaseCurrencies(String response) {
        
        ArrayList<ExchangeCurrency> currencies = new ArrayList<>();
        JSONObject jsonObject;
        Timber.d("Response" + response);

        try {
            jsonObject = new JSONObject(response);
            JSONArray dataObject = jsonObject.getJSONArray("data");
            for (int i = 0; i < dataObject.length(); i++) {
                JSONObject obj = dataObject.getJSONObject(i);
                ExchangeCurrency exchange = new ExchangeCurrency(obj.getString("id"));
                currencies.add(exchange);
            }
            return currencies;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    //{"data":{"amount":"2664.95","currency":"USD"}}
    public static ExchangeRate parseExchangeRate(String response) {
        JSONObject jsonObject;
        Timber.d("Response" + response);
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            String exchangeName = "Coinbase";
            return new ExchangeRate(exchangeName, dataObject.getString("amount"), dataObject.getString("currency"));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public static Exchange parseMarket(String response)
    {
        JSONObject jsonObject;
        Timber.d("Calculations" + response);

        try {
            jsonObject = new JSONObject(response);

            String ask = "";
            String bid = "";
            String last = "";
            String display_name = "BitcoinAverage";
            String source = "http://www.bitcoinaverage.com";
            String created_at = "";

            ask = jsonObject.getString("ask");
            bid = jsonObject.getString("bid");
            last = jsonObject.getString("last");
            created_at = jsonObject.getString("timestamp");

            return new Exchange(display_name, ask, bid, last, source, created_at);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}