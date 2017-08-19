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

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dpreference.DPreference;
import timber.log.Timber;

@Singleton
public class NotificationService {
    
    private final DPreference preference;
    private final SharedPreferences sharedPreferences;
    private final Context context;

    @Inject
    public NotificationService(Context context, DPreference preference, SharedPreferences sharedPreferences) {
        this.context = context;
        this.preference = preference;
        this.sharedPreferences = sharedPreferences;
    }

    public void createNotifications(List<Notification> notifications) {
        Timber.d("createNotifications : " + notifications.size());
        if (notifications.isEmpty())
            return;

        List<Notification> notificationList = new ArrayList<Notification>();
        for (Notification notification : notifications) {
            boolean read = notification.read;
            boolean support = notification.url.contains("support");
            boolean feedback = notification.url.contains("feedback");
            if (!read && !feedback && !support) {
                notificationList.add(notification);
            }
        }

        if (notificationList.size() > 1) {
            NotificationUtils.createNotification(context.getApplicationContext(), "New Notifications", "Notifications", "You have " + notificationList.size() + " new notifications.", NotificationUtils.NOTIFICATION_TYPE_NOTIFICATION, null);
        } else if (notificationList.size() == 1) {
            Notification notification = notificationList.get(0);
            if (notification.contact_id != null) {
                NotificationUtils.createNotification(context.getApplicationContext(), "New Trade Notification", "Trade notification", notification.msg, NotificationUtils.NOTIFICATION_TYPE_CONTACT, notification.contact_id);
            } else if (notification.advertisement_id != null) {
                NotificationUtils.createNotification(context.getApplicationContext(), "New Advertisement Notification", "Advertisement notification", notification.msg, NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT, notification.advertisement_id);
            } else {
                NotificationUtils.createNotification(context.getApplicationContext(), "New Notification", "Notification", notification.msg, NotificationUtils.NOTIFICATION_TYPE_NOTIFICATION, null);
            }
        }
    }

    public void messageNotifications(List<Message> messages) {
        if (messages.isEmpty()) return;

        List<Message> newMessages = new ArrayList<Message>();
        for (Message message : messages) {
            boolean isAccountUser = message.sender.username.toLowerCase().equals(AuthUtils.getUsername(preference, sharedPreferences));
            if (!isAccountUser) {
                newMessages.add(message);
            }
        }

        if (newMessages.size() > 1) { // if just single arrived
            NotificationUtils.createMessageNotification(context.getApplicationContext(), "New Messages", "You have new messages!", "You have " + newMessages.size() + " new trade messages.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else if (newMessages.size() == 1) {
            Message message = newMessages.get(0);
            assert message.contact_id != null;
            String username = message.sender.username;
            NotificationUtils.createMessageNotification(context.getApplicationContext(), "New message from " + username, "New message from " + username, message.msg, NotificationUtils.NOTIFICATION_TYPE_MESSAGE, message.contact_id);
        }
    }

    public void balanceUpdateNotification(String title, String ticker, String message) {
        Timber.d("balanceUpdateNotification");
        NotificationUtils.createBalanceNotification(context.getApplicationContext(), title, ticker, message, NotificationUtils.NOTIFICATION_TYPE_BALANCE, null);
    }

    public void contactNewNotification(List<Contact> contacts) {
        Timber.d("new contacts size: " + contacts.size());

        if (contacts.size() > 1) {
            NotificationUtils.createNotification(context.getApplicationContext(), "New Trades", "You have new trades to buy or sell bitcoin!", "You have " + contacts.size() + " new trades to buy or sell bitcoins.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
        } else if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            assert contact.contact_id != null;
            String username = TradeUtils.getContactName(contact);
            String type = (contact.is_buying) ? "sell" : "buy";
            String location = (TradeUtils.isLocalTrade(contact)) ? "local" : "online";
            NotificationUtils.createNotification(context.getApplicationContext(), "New trade with " + username, "New " + location + " trade with " + username, "Trade to " + type + " " + contact.amount + " " + contact.currency + " (" + context.getString(R.string.btc_symbol) + contact.amount_btc + ")", NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }
    }

    public void contactUpdateNotification(List<Contact> contacts) {
        Timber.d("updated contacts size: " + contacts.size());

        if (contacts.size() > 1) {
            NotificationUtils.createNotification(context.getApplicationContext(), "Trade Updates", "Trade status updates..", "Two or more of your trades have been updated.", NotificationUtils.NOTIFICATION_TYPE_CONTACT, null);
        } else if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            assert contact.contact_id != null;
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling) ? " with buyer " : " with seller ";
            NotificationUtils.createNotification(context.getApplicationContext(), "Trade Updated", ("The trade with " + contactName + " updated."), ("Trade #" + contact.contact_id + saleType + contactName + " has been updated."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
        }
    }

    public void contactDeleteNotification(List<Contact> contacts) {
        Timber.d("Notify Deleted Contact Size: " + contacts.size());
        if (contacts.size() > 1) {
            List<Contact> canceled = new ArrayList<Contact>();
            List<Contact> released = new ArrayList<Contact>();
            for (Contact contact : contacts) {
                if (TradeUtils.isReleased(contact)) {
                    released.add(contact);
                } else {
                    canceled.add(contact);
                }
            }

            if (canceled.size() > 1 && released.size() > 1) {
                NotificationUtils.createNotification(context, "Trades canceled or released", "Trades canceled or released...", "Two or more of your trades have been canceled or released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
            } else if (canceled.size() > 1) {
                NotificationUtils.createNotification(context, "Trades canceled", "Trades canceled...", "Two or more of your trades have been canceled.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
            } else if (released.size() > 1) {
                NotificationUtils.createNotification(context, "Trades released", "Trades released...", "Two or more of your trades have been released.", NotificationUtils.NOTIFICATION_TYPE_MESSAGE, null);
            }

        } else if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            String contactName = TradeUtils.getContactName(contact);
            String saleType = (contact.is_selling) ? " with buyer " : " with seller ";
            if (TradeUtils.isReleased(contact)) {
                NotificationUtils.createNotification(context, "Trade Released", ("Trade with" + contactName + " released."), ("Trade #" + contact.contact_id + saleType + contactName + " has been released."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);
            } else {
                NotificationUtils.createNotification(context, "Trade Canceled", ("Trade with" + contactName + " canceled."), ("Trade #" + contact.contact_id + saleType + contactName + " has been canceled."), NotificationUtils.NOTIFICATION_TYPE_CONTACT, contact.contact_id);

            }
        }
    }
}
