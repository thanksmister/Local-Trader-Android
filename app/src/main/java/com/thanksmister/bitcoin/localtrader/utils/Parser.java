/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.utils;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.network.api.model.Actions;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.network.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.network.api.model.Message;
import com.thanksmister.bitcoin.localtrader.network.api.model.Method;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.network.api.model.Place;
import com.thanksmister.bitcoin.localtrader.network.api.model.Profile;
import com.thanksmister.bitcoin.localtrader.network.api.model.Total;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.network.api.model.User;
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes.NO_ERROR_CODE;


public class Parser {

   /* public static Authorization parseAuthorization(String response) {
        JSONObject jsonObject;
        Authorization authorization = new Authorization();
        try {
            jsonObject = new JSONObject(response);
            authorization.accessToken =  jsonObject.getString("accessToken");
            authorization.refreshToken = jsonObject.getString("refreshToken");
            authorization.expiresInn = jsonObject.getString("expiresInn");
            return authorization;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }
    }*/


    public static ExchangeRate parseExchangeRate(String response) {
        JSONObject jsonObject;
        Timber.d("Response" + response);
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            String exchangeName = "Coinbase";

            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setCurrency(dataObject.getString("currency"));
            exchangeRate.setRate(dataObject.getString("amount"));
            exchangeRate.setName(exchangeName);
            return exchangeRate;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean containsError(String response) {
        if (response.contains("error_code") && response.contains("error")) {
            return true;
        }
        return false;
    }

    public static String parseDataMessage(String response, String defaultResponse) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObj = jsonObject.getJSONObject("data");
            if (dataObj.has("message")) {
                return dataObj.getString("message");
            } else {
                return defaultResponse;
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return defaultResponse;
        }
    }

    public static NetworkException parseError(String response) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject errorObj = jsonObject.getJSONObject("error");

            int error_code = INSTANCE.getNO_ERROR_CODE();
            if (errorObj.has("error_code")) {
                error_code = errorObj.getInt("error_code");
            }

            StringBuilder error_message = new StringBuilder(errorObj.getString("message"));

            if (errorObj.has("errors")) {
                error_message = new StringBuilder();
                JSONObject errors = errorObj.getJSONObject("errors");
                Iterator<?> keys = errors.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String message = errors.getString(key);
                    error_message.append(StringUtils.convertCamelCase(key)).append(" ").append(message).append(" ");
                }
            }

            return new NetworkException(error_message.toString(), error_code);

        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return new NetworkException(e.getMessage(), INSTANCE.getNO_ERROR_CODE());
        }
    }

    public static User parseUser(String response) {
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
            user.setUsername((object.getString("username")));
            user.setAgeText((object.getString("age_text")));
            user.setFeedbackCount((Integer.parseInt(object.getString("feedback_count"))));
            user.setHasCommonTrades(((object.getString("has_common_trades").equals("true"))));
            user.setConfirmedTradeCountText((object.getString("confirmed_trade_count_text")));
            user.setTradeVolumeText((object.getString("trade_volume_text")));
            user.setBlockedCount((Integer.parseInt(object.getString("blocked_count"))));
            user.setFeedbackScore((object.getString("feedback_score")));
            user.setFeedbacksUnconfirmedCount((Integer.parseInt(object.getString("feedbacks_unconfirmed_count"))));
            user.setTradingPartnersCount((Integer.parseInt(object.getString("trading_partners_count"))));
            user.setTrustedCount((Integer.parseInt(object.getString("trusted_count"))));
            user.setUrl((object.getString("url")));
            user.setCreatedAt((object.getString("created_at")));
            return user;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        return null;
    }

    public static List<Contact> parseContacts(String response) {
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

    @Deprecated
    public static Contact parseContact(String response) {
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return null;
        }

        return createContact(jsonObject);
    }

    @Deprecated
    private static Contact createContact(JSONObject object) {
        Contact item = new Contact();

        try {
            JSONObject data = object.getJSONObject("data");

            JSONObject buyer = data.getJSONObject("buyer");
            item.getBuyer().setUsername(buyer.getString("username"));
            item.getBuyer().setFeedbackScore(buyer.getInt("feedback_score"));
            item.getBuyer().setLastOnline(buyer.getString("last_online"));
            item.getBuyer().setTradeCount(buyer.getString("trade_count"));
            item.getBuyer().setName(buyer.getString("name"));

            JSONObject seller = data.getJSONObject("seller");
            item.getSeller().setUsername(seller.getString("username"));
            item.getSeller().setFeedbackScore(seller.getInt("feedback_score"));
            item.getSeller().setLastOnline(seller.getString("last_online"));
            item.getSeller().setTradeCount(seller.getString("trade_count"));
            item.getSeller().setName(seller.getString("name"));

            item.setBuying(data.getString("is_buying").equals("true"));
            item.setSelling(data.getString("is_selling").equals("true"));

            item.setAmount(data.getString("amount"));
            item.setContactId(data.getInt("contact_id"));
            item.setAmountBtc(data.getString("amount_btc"));
            item.setCreatedAt(data.getString("created_at"));

            if (!data.isNull("released_at"))
                item.setReleasedAt(data.getString("released_at"));

            if (!data.isNull("disputed_at"))
                item.setDisputedAt(data.getString("disputed_at"));

            if (!data.isNull("closed_at"))
                item.setClosedAt(data.getString("closed_at"));

            if (!data.isNull("escrowed_at"))
                item.setEscrowedAt(data.getString("escrowed_at"));

            if (!data.isNull("canceled_at"))
                item.setCanceledAt(data.getString("canceled_at"));

            if (!data.isNull("funded_at")) {
                item.setFundedAt(data.getString("funded_at"));
                item.setFunded(true);
            }

            if (data.has("payment_completed_at") && !data.isNull("payment_completed_at")) {
                item.setPaymentCompletedAt(data.getString("payment_completed_at"));
            }

            if (data.has("currency")) {
                item.setCurrency(data.getString("currency"));
            }

            if (data.has("exchange_rate_updated_at")) {
                item.setExchangeRateUpdatedAt(data.getString("exchange_rate_updated_at"));
            }
            if (data.has("reference_code")) {
                item.setReferenceCode(data.getString("reference_code"));
            }

            if (!data.isNull("account_details") && data.has("account_details")) {

                JSONObject account_details = data.getJSONObject("account_details");

                if (account_details.has("receiver_name")) {
                    item.getAccountDetails().setReceiverName(account_details.getString("receiver_name"));
                }

                if (account_details.has("receiver_email")) {
                    item.getAccountDetails().setReceiverEmail(account_details.getString("receiver_email"));
                }

                if (account_details.has("iban")) {
                    item.getAccountDetails().setIban(account_details.getString("iban"));
                }

                if (account_details.has("swift_bic")) {
                    item.getAccountDetails().setSwiftBic(account_details.getString("swift_bic"));
                }

                if (account_details.has("reference")) {
                    item.getAccountDetails().setReference(account_details.getString("reference"));
                }

                if (account_details.has("ethereum_address")) {
                    item.getAccountDetails().setEthereumAddress(account_details.getString("ethereum_address"));
                }

                if (account_details.has("phone_number")) {
                    item.getAccountDetails().setPhoneNumber(account_details.getString("phone_number"));
                }

                if (account_details.has("bsb")) {
                    item.getAccountDetails().setBsb(account_details.getString("bsb"));
                }

                if (account_details.has("biller_code")) {
                    item.getAccountDetails().setBillerCode(account_details.getString("biller_code"));
                }

                if (account_details.has("account_number")) {
                    item.getAccountDetails().setAccountNumber( account_details.getString("account_number"));
                }

                if (account_details.has("sort_code")) {
                    item.getAccountDetails().setSortCode(account_details.getString("sort_code"));
                }
            }

            JSONObject advertisement = data.getJSONObject("advertisement");
            if (advertisement.has("id")) {
                item.getAdvertisement().setId(advertisement.getInt("id"));
            }

            if (advertisement.has("payment_method")) {
                item.getAdvertisement().setPaymentMethod(advertisement.getString("payment_method"));
            }

            if (advertisement.has("trade_type")) {
                String trade_type = advertisement.getString("trade_type");
                if (trade_type.equals(TradeType.LOCAL_BUY.name())
                        || trade_type.equals(TradeType.LOCAL_SELL.name())
                        || trade_type.equals(TradeType.ONLINE_BUY.name())
                        || trade_type.equals(TradeType.ONLINE_SELL.name())) {
                    item.getAdvertisement().setTradeType(trade_type);
                } else {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("contact_data_key", data.toString());
                        Crashlytics.logException(new Throwable("Found invalid trade type for contact: " + trade_type));
                    }
                    item.getAdvertisement().setTradeType(TradeType.NONE.name());
                }
            }

            JSONObject advertiser = advertisement.getJSONObject("advertiser");
            item.getAdvertisement().getAdvertiser().setUsername(advertiser.getString("username"));
            item.getAdvertisement().getAdvertiser().setFeedbackScore(advertiser.getInt("feedback_score"));
            item.getAdvertisement().getAdvertiser().setLastOnline(advertiser.getString("last_online"));
            item.getAdvertisement().getAdvertiser().setTradeCount(advertiser.getString("trade_count"));
            item.getAdvertisement().getAdvertiser().setName(advertiser.getString("name"));

            if (data.has("is_funded")) { //Boolean signaling if the escrow is enabled and not funded.
                item.setFunded(data.getBoolean("is_funded"));
            }

            JSONObject actions = object.getJSONObject("actions");
            Actions actionsList = new Actions();

            if (actions.has("release_url")) {
                actionsList.setReleaseUrl(actions.getString("release_url"));
            }

            if (actions.has("cancel_url")) {
                actionsList.setCancelUrl(actions.getString("cancel_url"));
            }

            if (actions.has("mark_as_paid_url")) {
                actionsList.setMarkAsPaidUrl(actions.getString("mark_as_paid_url"));
            }

            if (actions.has("dispute_url")) {
                actionsList.setDisputeUrl(actions.getString("dispute_url"));
            }

            if (actions.has("fund_url")) {
                actionsList.setFundUrl(actions.getString("fund_url"));
            }

            if (actions.has("advertisement_public_view")) {
                actionsList.setAdvertisementPublicView(actions.getString("advertisement_public_view"));
            }

            if (actions.has("messages_url")) {
                actionsList.setMessagesUrl(actions.getString("messages_url"));
            }

            if (actions.has("message_post_url")) {
                actionsList.setMessagePostUrl(actions.getString("message_post_url"));
            }

            item.setActions(actionsList);
            return item;

        } catch (JSONException e) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("Message", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing contact: " + object.toString()));
            }
        }

        return null;
    }

    public static ArrayList<Notification> parseNotifications(String response) {
        Timber.d("Notification json: " + response);
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

    private static Notification parseNotification(JSONObject jsonObject) {
        Notification notification = new Notification();
        try {
            if (jsonObject.has("url")) notification.setUrl((jsonObject.getString("url")));
            if (jsonObject.has("created_at"))
                notification.setCreatedAt((jsonObject.getString("created_at")));
            if (jsonObject.has("contact_id"))
                notification.setContactId((jsonObject.getInt("contact_id")));
            if (jsonObject.has("advertisement_id"))
                notification.setAdvertisementId((jsonObject.getInt("advertisement_id")));
            if (jsonObject.has("read")) notification.setRead((jsonObject.getBoolean("read")));
            if (jsonObject.has("msg")) notification.setMessage(jsonObject.getString("msg"));
            if (jsonObject.has("id")) notification.setNotificationId((jsonObject.getString("id")));
            return notification;
        } catch (JSONException e) {
            Timber.e("Error Parsing Notification: " + e.getMessage());
        }

        return null;
    }

    @Deprecated
    public static ArrayList<Message> parseMessages(String response) {
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
        return results;
    }

    @Deprecated
    private static Message parseMessage(JSONObject messageObj) {
        Message message = new Message();
        try {
            JSONObject sender = messageObj.getJSONObject("sender");
            if (sender.has("username")) message.getSender().setUsername(sender.getString("username"));
            if (sender.has("name")) message.getSender().setName(sender.getString("name"));
            if (sender.has("trade_count"))
                message.getSender().setTradeCount(sender.getString("trade_count"));
            if (sender.has("last_online"))
                message.getSender().setLastOnline(sender.getString("last_online"));
            if (messageObj.has("contact_id"))
                message.setContactId((messageObj.getInt("contact_id")));
            if (messageObj.has("created_at"))
                message.setCreatedAt((messageObj.getString("created_at")));
            if (messageObj.has("msg")) message.setMessage(Uri.decode(messageObj.getString("msg")));
            if (messageObj.has("is_admin"))
                message.setAdmin((Boolean.valueOf(messageObj.getString("is_admin"))));
            if (messageObj.has("attachment_name"))
                message.setAttachmentName((messageObj.getString("attachment_name")));
            if (messageObj.has("attachment_type"))
                message.setAttachmentType((messageObj.getString("attachment_type")));
            if (messageObj.has("attachment_url"))
                message.setAttachmentUrl((messageObj.getString("attachment_url")));
            return message;
        } catch (JSONException e) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("Message", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing advertisement: " + messageObj.toString()));
            }
        }
        return null;
    }

    public static Wallet parseWallet(String response) {
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
            wallet.setMessage((data.getString("message")));
            wallet.setReceivingAddress(data.getString("receiving_address"));
            JSONObject total = data.getJSONObject("total");
            Total walletTotal = new Total();
            walletTotal.setBalance(total.getString("balance"));
            walletTotal.setSendable(total.getString("sendable"));
            if (TextUtils.isEmpty(walletTotal.getBalance()) || walletTotal.getBalance().equals("0E-8")) {
                walletTotal.setBalance("0");
            }
            wallet.setTotal(walletTotal);

            JSONArray sent_transactions = data.getJSONArray("sent_transactions_30d");

            ArrayList<Transaction> sentTransactions = new ArrayList<>();
            for (int i = 0; i < sent_transactions.length(); i++) {
                JSONObject object = (JSONObject) sent_transactions.get(i);
                Transaction transaction = new Transaction();
                //transaction.setType((TransactionType.SENT));
                transaction.setCreatedAt((object.getString("created_at")));
                if (object.has("tx_type") && !object.isNull("tx_type"))
                    transaction.setTxType(object.getString("tx_type"));
                if (object.has("txid") && !object.isNull("txid")) {
                    transaction.setTxid((object.getString("txid")));
                } else {
                    transaction.setTxid(transaction.getCreatedAt());
                }
                if (object.has("amount"))
                    transaction.setAmount((object.getString("amount")));
                if (object.has("description"))
                    transaction.setDescription((object.getString("description")));

                /*if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("fee")) {
                    transaction.setType((TransactionType.FEE));
                } else if (transaction.getDescription() != null && (transaction.getDescription().toLowerCase().contains("contact")
                        || transaction.getDescription().toLowerCase().contains("bitcoin sell"))) {
                    transaction.setType((TransactionType.CONTACT_SENT));

                } else if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("internal")) {
                    transaction.setType((TransactionType.INTERNAL));
                } else if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("reserve")) {
                    transaction.setType((TransactionType.SENT));
                }*/
                sentTransactions.add(transaction);
            }

            wallet.setSentTransactions(sentTransactions);

            JSONArray received_transactions = data.getJSONArray("received_transactions_30d");
            ArrayList<Transaction> receivedTransactions = new ArrayList<>();
            for (int i = 0; i < received_transactions.length(); i++) {
                JSONObject object = (JSONObject) received_transactions.get(i);
                Transaction transaction = new Transaction();
                transaction.setCreatedAt((object.getString("created_at")));
                if (object.has("tx_type") && !object.isNull("tx_type"))
                    transaction.setTxType(object.getString("tx_type"));
                if (object.has("txid") && !object.isNull("txid")) {
                    transaction.setTxid((object.getString("txid")));
                } else {
                    transaction.setTxid(transaction.getCreatedAt());
                }

                if (object.has("amount"))
                    transaction.setAmount((object.getString("amount")));

                if (object.has("description"))
                    transaction.setDescription((object.getString("description")));

                /*if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("contact")) {
                    transaction.setType((TransactionType.CONTACT_RECEIVE));
                } else if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("internal")) {
                    transaction.setType((TransactionType.INTERNAL));
                } else if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("reserve")) {
                    transaction.setType((TransactionType.RESERVE));
                } else if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains("affiliate")) {
                    transaction.setType((TransactionType.AFFILIATE));
                }*/

                receivedTransactions.add(transaction);
            }

            wallet.setReceivingTransactions(receivedTransactions);

            // just get the first address
            /*JSONArray receiving_address_list = data.getJSONArray("receiving_address_list");
            JSONObject object = (JSONObject) receiving_address_list.get(receiving_address_list.length() - 1);
            wallet.address = (object.getString("address"));
            wallet.received = (object.getString("received"))*/
            ;

            return wallet;

        } catch (Exception e) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("Wallet", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing wallet: " + e.getMessage()));
            }
        }

        return null;
    }

    public static Wallet parseWalletBalance(String response) {
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
            wallet.setMessage((data.getString("message")));
            wallet.setReceivingAddress((data.getString("receiving_address")));
            JSONObject total = data.getJSONObject("total");
            Total walletTotal = new Total();
            walletTotal.setBalance(total.getString("balance"));
            walletTotal.setSendable(total.getString("sendable"));
            if (TextUtils.isEmpty(walletTotal.getBalance()) || walletTotal.getBalance().equals("0E-8")) {
                walletTotal.setBalance("0");
            }
            wallet.setTotal(walletTotal);
            return wallet;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }
        return null;
    }

    @Deprecated
    public static List<Advertisement> parseAdvertisements(String response) {
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
        return items;
    }

    public static List<Method> parseMethods(TreeMap<String, Object> treeMap) {
        ArrayList<Method> methods = new ArrayList<Method>();
        for (Object o : treeMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            LinkedTreeMap linkedTreeMap = (LinkedTreeMap) entry.getValue();
            Method method = new Method();
            method.setKey((String) entry.getKey());
            method.setCode((String) linkedTreeMap.get("code"));
            method.setName((String) linkedTreeMap.get("name"));
            method.setCurrencies((ArrayList<String>) linkedTreeMap.get("currencies"));
            methods.add(method);
        }
        Collections.sort(methods, new MethodNameComparator());
        return methods;
    }


    public static List<Method> parseMethods(String response) {
        JSONObject jsonObject;
        JSONObject dataObject;
        ArrayList<Method> methods = new ArrayList<>();
        try {
            jsonObject = new JSONObject(response);
            dataObject = jsonObject.getJSONObject("data");

            JSONObject methodsObject = dataObject.getJSONObject("methods");
            Iterator<?> keys = methodsObject.keys();
            while (keys.hasNext()) {
                Method method = new Method();
                String key = (String) keys.next();
                try {
                    if (methodsObject.get(key) instanceof JSONObject) {
                        method.setKey(key);
                        JSONObject obj = (JSONObject) methodsObject.get(key);
                        if (obj.has("code")) method.setCode((obj.getString("code")));
                        if (obj.has("name")) method.setName((obj.getString("name")));
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

    private static class MethodNameComparator implements Comparator<Method> {
        @Override
        public int compare(Method o1, Method o2) {
            return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }
    }

    public static Advertisement parseAdvertisement(String response) {
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

            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("Advertisement", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing advertisement: " + object));
            }
            return null;
        }
    }

    @Deprecated
    private static Advertisement parseAdvertisement(JSONObject object, String nextUrl) {
        Advertisement item = new Advertisement();

        try {
            JSONObject data = object.getJSONObject("data");
            JSONObject actions = object.getJSONObject("actions");

            item.setAdId(data.getInt("ad_id"));
            item.setCreatedAt(data.getString("created_at"));

            if (data.has("atm_model") && !data.isNull("atm_model")) {
                item.setAtmModel(data.getString("atm_model"));
            }

            item.setVisible(data.getBoolean("visible"));
            item.setTempPrice(data.getString("temp_price"));
            item.setTempPriceUsd(data.getString("temp_price_usd"));

            if (data.has("require_feedback_score"))
                item.setRequireFeedbackScore(data.getString("require_feedback_score"));

            if (data.has("require_trade_volume"))
                item.setRequireTradeVolume( data.getString("require_trade_volume"));

            if (data.has("first_time_limit_btc") && !data.getString("first_time_limit_btc").equals("null"))
                item.setFirstTimeLimitBtc(data.getString("first_time_limit_btc"));

            if (data.has("require_identification"))
                item.setRequireIdentification(data.getBoolean("require_identification"));

            if (data.has("email")) {
                item.setEmail(data.getString("email"));
            }

            item.setLocation(data.getString("locationString"));

            item.setCountryCode(data.getString("countrycode"));
            item.setCity(data.getString("city"));

            String trade_type = data.getString("trade_type");
            item.setTradeType(trade_type);
            item.setOnlineProvider(data.getString("online_provider"));

            if (data.has("price_equation"))
                item.setPriceEquation(data.getString("price_equation"));

            if (data.has("reference_type"))
                item.setReferenceType(data.getString("reference_type"));

            if (data.has("track_max_amount"))
                item.setTrackMaxAmount(data.getBoolean("track_max_amount"));
            if (data.has("trusted_required"))
                item.setTrustedRequired(data.getBoolean("trusted_required"));
            if (data.has("sms_verification_required"))
                item.setSmsVerificationRequired(data.getBoolean("sms_verification_required"));

            item.setCurrency(data.getString("currency"));

            if (data.has("account_info")) {
                item.setAccountInfo(data.getString("account_info"));
            }

            item.setLat(Float.parseFloat(data.getString("lat")));
            item.setLon(Float.parseFloat(data.getString("lon")));

            if (data.has("distance"))
                item.setDistance(data.getString("distance")); // for public searches only

            if (data.has("bank_name") && !data.isNull("bank_name")) {
                item.setBankName(data.getString("bank_name"));
            }

            if (data.has("msg") && !data.isNull("msg")) {
                //message = message.replace("\n", "").replace("\r", "<br>");
                item.setMessage(data.getString("msg"));
            }

            if (data.has("min_amount") && !data.isNull("min_amount")) {
                String min_amount = data.getString("min_amount");
                String min[] = min_amount.split(".");
                item.setMinAmount((min.length > 0) ? min[0] : data.getString("min_amount"));
            }

            if (data.has("max_amount") && !data.isNull("max_amount")) {
                String max_amount = data.getString("max_amount");
                String max[] = max_amount.split(".");
                item.setMaxAmount((max.length > 0) ? max[0] : data.getString("max_amount"));
            }
            if (data.has("max_amount_available") && !data.isNull("max_amount_available")) {
                item.setMaxAmountAvailable( data.getString("max_amount_available"));
            }

            if (actions.has("public_view")) {
                Actions actionsList = new Actions();
                actionsList.setAdvertisementPublicView(actions.getString("public_view"));
                item.setActions(actionsList);
            }

            if (data.has("profile")) {
                Profile profileItem = new Profile();
                JSONObject profile = data.getJSONObject("profile");
                if (profile.has("last_online"))
                    profileItem.setLastOnline((profile.getString("last_online")));
                profileItem.setName((profile.getString("name")));
                profileItem.setUsername((profile.getString("username")));
                if (profile.has("feedback_score"))
                    profileItem.setFeedbackScore(profile.getInt("feedback_score"));
                if (profile.has("trade_count"))
                    profileItem.setTradeCount(profile.getString("trade_count"));

                item.setProfile(profileItem);
            }
            if (nextUrl != null) {
                item.setNextUrl(nextUrl);
            }
            return item;
        } catch (JSONException e) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("Advertisement", "Parsing error discovered");
                Crashlytics.logException(new Throwable("Error parsing advertisement: " + object.toString()));
            }
        }
        return null;
    }

    /*public static Place parsePlace(String responseString) {
        Place place = new Place();
        try {
            JSONObject jsonObject = new JSONObject(responseString);
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray places = data.getJSONArray("places");
            if (places.length() > 0) {
                JSONObject placeObject = (JSONObject) places.get(0);
                place.locationString = (placeObject.getString("locationString"));
                place.buyLocalUrl = (placeObject.getString("buyLocalUrl"));
                place.sellLocalUrl = (placeObject.getString("sellLocalUrl"));
                place.url = (placeObject.getString("url"));
                place.lon = (placeObject.getString("lon"));
                place.lat = (placeObject.getString("lat"));
                return place;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }*/

    public static ContactRequest parseContactRequest(String response) {
        ContactRequest contactRequest = new ContactRequest();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject data = jsonObject.getJSONObject("data");
            contactRequest.contactId = data.getString("contact_id");
            return contactRequest;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Currency> parseCurrencies(TreeMap<String, Object> treeMap) {
        ArrayList<Currency> currencies = new ArrayList<Currency>();
        for (Object o : treeMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            LinkedTreeMap linkedTreeMap = (LinkedTreeMap) entry.getValue();
            Currency currency = new Currency();
            currency.setCode((String) entry.getKey());
            currency.setName((String) linkedTreeMap.get("name"));
            currency.setAltcoin((Boolean) linkedTreeMap.get("altcoin"));
            currencies.add(currency);
        }
        Collections.sort(currencies, new CurrencyComparator());
        return currencies;
    }

    public static List<Currency> parseCurrencies(String response) {
        JSONObject jsonObject;
        ArrayList<Currency> currencies = new ArrayList<>();
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            JSONObject currenciesObject = dataObject.getJSONObject("currencies");
            Iterator<?> keys = currenciesObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Currency currency = new Currency();
                if(currenciesObject.get("key") instanceof JSONObject) {
                    currency.setCode(key);
                    JSONObject obj = (JSONObject) currenciesObject.get(key);
                    if (obj.has("code")) currency.setAltcoin(obj.getBoolean("altcoin"));
                    if (obj.has("name")) currency.setName(obj.getString("name"));
                    currencies.add(currency);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        Collections.sort(currencies, new CurrencyComparator());
        return currencies;
    }

    public static class CurrencyComparator implements Comparator<Currency> {
        @Override
        public int compare(Currency o1, Currency o2) {
            return o1.getCode().toLowerCase().compareTo(o2.getCode().toLowerCase());
        }
    }

    public static List<ExchangeCurrency> parseCoinbaseCurrencies(String response) {
        ArrayList<ExchangeCurrency> currencies = new ArrayList<>();
        JSONObject jsonObject;
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

    public static ExchangeRate parseBitcoinAverageExchangeRate(String exchangeName, String currency, String result) {
        try {
            JSONObject jsonObject;
            jsonObject = new JSONObject(result);
            Iterator<?> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (jsonObject.get(key) instanceof JSONObject) {
                    JSONObject exchangeObj = (JSONObject) jsonObject.get(key);
                    JSONObject rateObject = (JSONObject) exchangeObj.get("rates");
                    String rate = rateObject.getString("last");
                    if (exchangeObj.has("avg_1h")) {
                        rate = exchangeObj.getString("avg_1h");
                    }
                    if (currency.equals(key)) {
                        ExchangeRate exchangeRate = new ExchangeRate();
                        exchangeRate.setCurrency(currency);
                        exchangeRate.setRate(rate);
                        exchangeRate.setName(exchangeName);
                        return exchangeRate;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("DefaultLocale")
    public static ExchangeRate parseBitfinexExchangeRate(String response) {
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(response);
            if (jsonArray.length() > 6) {
                String rate = jsonArray.get(6).toString();
                rate = String.format("%.2f", Doubles.convertToDouble(rate));
                ExchangeRate exchangeRate = new ExchangeRate();
                exchangeRate.setRate(rate);
                exchangeRate.setName("Bitfinex");
                return exchangeRate;
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static ExchangeRate parseCoinbaseExchangeRate(String response) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            String exchangeName = "Coinbase";
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setCurrency(dataObject.getString("currency"));
            exchangeRate.setRate(dataObject.getString("amount"));
            exchangeRate.setName(exchangeName);
            return exchangeRate;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public static Exchange parseMarket(String response) {
        JSONObject jsonObject;
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