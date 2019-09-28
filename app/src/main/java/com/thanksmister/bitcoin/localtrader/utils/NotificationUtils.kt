/*
 * Copyright (c) 2019 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.content.ContextCompat
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.activities.*
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity.Companion.EXTRA_NOTIFICATION_ID
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity.Companion.EXTRA_NOTIFICATION_TYPE
import dagger.Reusable
import timber.log.Timber
import java.util.*

@Reusable
class NotificationUtils(context: Context) : ContextWrapper(context) {

    private var notificationManager: NotificationManager? = null
    // private val pendingIntent: PendingIntent
    // private val notificationIntent: Intent

    private val manager: NotificationManager
        get() {
            if (notificationManager == null) {
                notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            return notificationManager!!
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels()
        }
    }

    fun createNotifications(notifications: List<com.thanksmister.bitcoin.localtrader.network.api.model.Notification>) {
        if (notifications.isEmpty())
            return

        val notificationList = ArrayList<com.thanksmister.bitcoin.localtrader.network.api.model.Notification>()
        for (notification in notifications) {
            val read = notification.read
            val support = notification.url!!.contains("support")
            val feedback = notification.url!!.contains("feedback")
            if (!read && !feedback && !support) {
                notificationList.add(notification)
            }
        }

        try {
            if (notificationList.size > 1) {
                createNotification(getString(R.string.notification_new_notifications_title), getString(R.string.notification_new_notifications, notificationList.size.toString()))
            } else if (notificationList.size == 1) {
                val notification = notificationList[0]
                if (notification.contactId != null && notification.message != null) {
                    createContactNotification(getString(R.string.notification_trade_notification_title), notification.message!!, notification.contactId!!)
                } else if (notification.advertisementId != null && notification.message != null) {
                    createAdvertisementNotification(getString(R.string.notification_ad_notification_title), notification.message!!, notification.advertisementId!!)
                } else if (notification.message != null) {
                    createNotification(getString(R.string.notification_title), notification.message!!)
                }
            }
        } catch (e: Exception) {
            Timber.e(" Notification error ${e.message}")
        }
    }

    fun balanceUpdateNotification(diff: String) {
        createWalletNotification(getString(R.string.notification_btc_received), getString(R.string.notification_btc_received_message, diff))
    }

    fun balanceCurrentNotification(balance: String) {
        createWalletNotification(getString(R.string.notification_btc_balance), getString(R.string.notification_btc_balance_message, balance));
    }

    fun createWalletNotification(title: String, message: String) {
        Timber.d("createWalletNotification")

        val notificationIntent = Intent(applicationContext, WalletActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationIntent.putExtra(EXTRA_NOTIFICATION_ID, 0)
        notificationIntent.putExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_BALANCE)

        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        val pendingIntent = PendingIntent.getActivity(applicationContext, WALLET_NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nb = getAndroidChannelNotification(title, message, pendingIntent)
            manager.notify(WALLET_NOTIFICATION_ID, nb.build())
        } else {
            val nb = getAndroidNotification(title, message)
            nb.setContentIntent(pendingIntent)
            manager.notify(WALLET_NOTIFICATION_ID, nb.build())
        }
    }

    // TODO open up contact directly when receiving new contact or contact message
    fun createContactNotification(title: String, message: String, contactId: Int) {
        val notificationIntent = Intent(applicationContext, ContactActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationIntent.putExtra(EXTRA_NOTIFICATION_ID, contactId)
        notificationIntent.putExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_CONTACT)
        notificationIntent.action = Intent.ACTION_MAIN;
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        val pendingIntent = PendingIntent.getActivity(applicationContext, CONTACT_NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nb = getAndroidChannelNotification(title, message, pendingIntent)
            manager.notify(CONTACT_NOTIFICATION_ID, nb.build())
        } else {
            val nb = getAndroidNotification(title, message)
            nb.setContentIntent(pendingIntent)
            manager.notify(CONTACT_NOTIFICATION_ID, nb.build())
        }
    }

    fun createAdvertisementNotification(title: String, message: String, adId: Int) {
        val notificationIntent = Intent(applicationContext, AdvertisementActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationIntent.putExtra(EXTRA_NOTIFICATION_ID, adId)
        notificationIntent.putExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_ADVERTISEMENT)
        notificationIntent.action = Intent.ACTION_MAIN;
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        val pendingIntent = PendingIntent.getActivity(applicationContext, ADVERTISEMENT_NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nb = getAndroidChannelNotification(title, message, pendingIntent)
            manager.notify(ADVERTISEMENT_NOTIFICATION_ID, nb.build())
        } else {
            val nb = getAndroidNotification(title, message)
            nb.setContentIntent(pendingIntent)
            manager.notify(ADVERTISEMENT_NOTIFICATION_ID, nb.build())
        }
    }

    private fun createNotification(title: String, message: String) {
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nb = getAndroidChannelNotification(title, message, pendingIntent)
            manager.notify(NOTIFICATION_ID, nb.build())
        } else {
            val nb = getAndroidNotification(title, message)
            nb.setContentIntent(pendingIntent)
            manager.notify(NOTIFICATION_ID, nb.build())
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createChannels() {
        val description = getString(R.string.notification_lbc_notice)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val mChannel = NotificationChannel(ANDROID_CHANNEL_ID, ANDROID_CHANNEL_NAME, importance)
        mChannel.description = description
        mChannel.enableLights(true)
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(true)
        mChannel.lockscreenVisibility = VISIBILITY_PUBLIC
        mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        manager.createNotificationChannel(mChannel)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getAndroidChannelNotification(title: String, body: String, pendingIntent: PendingIntent): Notification.Builder {
        val color = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        val builder = Notification.Builder(applicationContext, ANDROID_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(color)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setAutoCancel(true)

        builder.setContentIntent(pendingIntent)
        return builder
    }

    private fun getAndroidNotification(title: String, body: String): NotificationCompat.Builder {
        val color = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        return NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setPriority(PRIORITY_MAX)
                .setVisibility(VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setColor(color)
                .setAutoCancel(true)
    }

    fun clearNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    companion object {
        const val NOTIFICATION_ID = 1538
        const val WALLET_NOTIFICATION_ID = 1540
        const val CONTACT_NOTIFICATION_ID = 1541
        const val ADVERTISEMENT_NOTIFICATION_ID = 1542
        const val NOTIFICATION_TYPE_BALANCE = 4
        const val NOTIFICATION_TYPE_CONTACT = 5
        const val NOTIFICATION_TYPE_ADVERTISEMENT = 6
        const val NOTIFICATION_TYPE_NOTIFICATION = 8
        const val ANDROID_CHANNEL_ID = "com.thanksmister.bitcoin.localtrader.ANDROID"
        const val ANDROID_CHANNEL_NAME = "LocalBitcoins Notification"
    }
}
