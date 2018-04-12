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

package com.thanksmister.bitcoin.localtrader.network.services;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.network.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class NotificationService {

    private final NotificationUtils notificationUtils;

    @Inject
    public NotificationService(Context context) {
        this.notificationUtils = new NotificationUtils(context);
    }

    public void createNotifications(List<Notification> notifications) {
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
            notificationUtils.createNotification("New notifications", "You have " + notificationList.size() + " new notifications.");
        } else if (notificationList.size() == 1) {
            Notification notification = notificationList.get(0);
            if (notification.contact_id != null) {
                notificationUtils.createNotification("Trade notification", notification.msg);
            } else if (notification.advertisement_id != null) {
                notificationUtils.createNotification("Advertisement notification", notification.msg);
            } else {
                notificationUtils.createNotification("Notification", notification.msg);
            }
        }
    }

    public void balanceUpdateNotification(String title, String ticker, String message) {
        notificationUtils.createNotification(title, message);
    }
}
