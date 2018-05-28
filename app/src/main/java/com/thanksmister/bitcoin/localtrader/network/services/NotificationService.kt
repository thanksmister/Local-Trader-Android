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

package com.thanksmister.bitcoin.localtrader.network.services

import android.content.Context
import android.text.TextUtils

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.persistence.Notification
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject
constructor(context: Context) {

    private val notificationUtils: NotificationUtils = NotificationUtils(context)

    fun createNotifications(notifications: List<Notification>) {
        if (notifications.isEmpty()) {
            return
        }

        val notificationList = ArrayList<Notification>()
        for (notification in notifications) {
            val read = notification.read
            if (!TextUtils.isEmpty(notification.url)) {
                val support = notification.url!!.contains("support")
                val feedback = notification.url!!.contains("feedback")
                if (!read && !feedback && !support) {
                    notificationList.add(notification)
                }
            }
        }

        if (notificationList.size > 1) {
            notificationUtils.createNotification(notificationUtils.getString(R.string.notifications_notification_title), notificationUtils.getString(R.string.notifications_description, notificationList.size))
        } else if (notificationList.size == 1) {
            val notification = notificationList[0]
            if (notification.contactId != null) {
                notificationUtils.createNotification(notificationUtils.getString(R.string.notifications_trade_title), notification.msg)
            } else if (notification.advertisementId != null) {
                notificationUtils.createNotification(notificationUtils.getString(R.string.notifications_advertisement_title), notification.msg)
            } else {
                notificationUtils.createNotification(notificationUtils.getString(R.string.notification_title), notification.msg)
            }
        }
    }

    fun createNotification(title: String, message: String) {
        notificationUtils.createNotification(title, message)
    }
}
