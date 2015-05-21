/*
 * Copyright 2007 ZXing authors
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

package com.thanksmister.bitcoin.localtrader.data.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseBitfinexToExchange;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAd;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToCurrencyList;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToJSONObject;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMethod;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToUser;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.client.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;


@Singleton
public class NotificationService
{
    private final SharedPreferences sharedPreferences;
    private final Context context;
    private StringPreference stringPreference;
    
    @Inject
    public NotificationService(Context context, SharedPreferences sharedPreferences)
    {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        stringPreference = new StringPreference(sharedPreferences, DataService.PREFS_USER);
    }
    
    public void messageNotifications(List<Message> messages)
    {
        List<Message> newMessages = new ArrayList<Message>();
        for (Message message : messages) {
            boolean isAccountUser = message.sender.username.toLowerCase().equals(stringPreference.get());
            if (!isAccountUser) {
                newMessages.add(message);
            }
        }

        if (newMessages.size() > 1) { // if just single arrived
            NotificationUtils.createMessageNotification(context.getApplicationContext(), "New Messages", "You have new messages!", "You have " + newMessages.size() + " new trade messages.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else {
            if (newMessages.size() > 0) {
                Message message = newMessages.get(0);
                String username = message.sender.username;
                NotificationUtils.createMessageNotification(context.getApplicationContext(), "New message from " + username, "New message from " + username, message.msg, NotificationUtils.NOTIFICATION_TYPE_MESSAGE, message.contact_id);
            }
        }
    }

    public void contactNewNotification(List<Contact> contacts)
    {
        Timber.e("new contacts size: " + contacts.size());
        
        if (contacts.size() > 1) {
            NotificationUtils.createNotification(context.getApplicationContext(), "New Trades", "You have new trades to buy or sell bitcoin!", "You have " + contacts.size() + " new trades to buy or sell bitcoins.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            String username = TradeUtils.getContactName(contact);
            String type = (contact.is_buying) ? "sell" : "buy";
            String location = (TradeUtils.isLocalTrade(contact)) ? "local" : "online";
            NotificationUtils.createNotification(context.getApplicationContext(), "New trade with " + username, "New " + location + " trade with " + username, "Trade to " + type + " " + contact.amount + " " + contact.currency + " (" + context.getString(R.string.btc_symbol) + contact.amount_btc + ")", NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }
    }

    public void contactUpdateNotification(List<Contact> contacts)
    {
        Timber.e("updated contacts size: " + contacts.size());
        if (contacts.size() > 1) {
            NotificationUtils.createNotification(context.getApplicationContext(), "Trade Updates", "Trade status updates..", "Two or more of your trades have been updated.", NotificationUtils.NOTIFICATION_TYPE_CONTACT, null);
        } else if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling) ? " with buyer " : " with seller ";
            NotificationUtils.createNotification(context.getApplicationContext(), "Trade Updated", ("The trade with" + contactName + " updated."), ("Trade #" + contact.contact_id + saleType + contactName + " has been updated."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }
    }

    public void contactDeleteNotification(List<Contact> contacts)
    {
        Timber.e("delete contacts size: " + contacts.size());
        
        if (contacts.size() > 1) {

            List<Contact> canceled = new ArrayList<Contact>();
            List<Contact> released = new ArrayList<Contact>();
            for (Contact contact : contacts) {
                if(TradeUtils.isCanceledTrade(contact)) {
                    canceled.add(contact);
                } else if (TradeUtils.isReleased(contact)){
                    released.add(contact);
                }
            }

            if(canceled.size() > 1 && released.size() > 1) {
                NotificationUtils.createNotification(context, "Trades canceled or released", "Trades canceled or released...", "Two or more of your trades have been canceled or released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
            } else if (canceled.size() > 1) {
                NotificationUtils.createNotification(context, "Trades canceled", "Trades canceled...", "Two or more of your trades have been canceled.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
            } else if (canceled.size() > 1) {
                NotificationUtils.createNotification(context, "Trades released", "Trades released...", "Two or more of your trades have been released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
            }
        } else {
            Contact contact = contacts.get(0);
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling) ? " with buyer " : " with seller ";
            if (TradeUtils.isCanceledTrade(contact)) {
                NotificationUtils.createNotification(context, "Trade Canceled", ("Trade with" + contactName + " canceled."), ("Trade #" + contact.contact_id + saleType + contactName + " has been canceled."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
            } else if (TradeUtils.isReleased(contact)) {
                NotificationUtils.createNotification(context, "Trade Released", ("Trade with" + contactName + " released."), ("Trade #" + contact.contact_id + saleType + contactName + " has been released."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
            }
        }
    }
}
