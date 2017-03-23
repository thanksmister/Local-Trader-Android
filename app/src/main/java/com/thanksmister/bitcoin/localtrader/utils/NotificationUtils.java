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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;

import timber.log.Timber;

public class NotificationUtils
{
    public static int NOTIFICATION_ID = 1138;
    public static int BALANCE_NOTIFICATION_ID = 1975;
    public static int MESSAGE_NOTIFICATION_ID = 1976;
    public static int NOTIFICATION_ERROR_ID = 1925;
    
    public static int NOTIFICATION_TYPE_BALANCE = 4;
    public static int NOTIFICATION_TYPE_CONTACT = 5;
    public static int NOTIFICATION_TYPE_ADVERTISEMENT = 6;
    public static int NOTIFICATION_TYPE_MESSAGE = 7;
    public static int NOTIFICATION_TYPE_NOTIFICATION = 8;
    

    public static void createNotification(Context context, String ticker, String title, String message, int type, String notification_id)
    {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_TYPE, type);
        if(notification_id != null) {
            notificationIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_ID, notification_id);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setTicker(ticker);
        builder.setSmallIcon(R.drawable.ic_stat_notification);

        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS); // requires VIBRATE permission
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL| Notification.FLAG_SHOW_LIGHTS;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(MESSAGE_NOTIFICATION_ID, notification);
    }

    public static void createBalanceNotification(Context context, String ticker, String title, String message, int type, String contact_id)
    {
        Timber.d("Create Balance Notification!!");
        
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        notificationIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_TYPE, type);
        if(type == NOTIFICATION_TYPE_BALANCE && contact_id != null) {
            notificationIntent.putExtra(MainActivity.EXTRA_CONTACT, contact_id);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setTicker(ticker);
        builder.setSmallIcon(R.drawable.ic_stat_notification);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int resource =  R.mipmap.ic_launcher;
            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resource);
            Resources res = context.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            // bm = Bitmap.createScaledBitmap(bm, width, height, false);
            //builder.setLargeIcon(bm);
        }*/

        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS); // requires VIBRATE permission
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL| Notification.FLAG_SHOW_LIGHTS;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(BALANCE_NOTIFICATION_ID, notification);
    }

    public static void createMessageNotification(Context context, String ticker, String title, String message, int type, String contact_id)
    {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        notificationIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_TYPE, type);
        if(type == NOTIFICATION_TYPE_MESSAGE && contact_id != null) {
            notificationIntent.putExtra(MainActivity.EXTRA_CONTACT, contact_id);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setTicker(ticker);
        builder.setSmallIcon(R.drawable.ic_stat_notification);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int resource =  R.mipmap.ic_launcher;
            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resource);
            Resources res = context.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            //bm = Bitmap.createScaledBitmap(bm, width, height, false);
            //builder.setLargeIcon(bm);
        }*/
        
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS); // requires VIBRATE permission
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL| Notification.FLAG_SHOW_LIGHTS;
        
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(MESSAGE_NOTIFICATION_ID, notification);
    }

    public static void createErrorNotification(Context context, String title, String message)
    {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags( Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT );
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setSmallIcon(android.R.drawable.stat_sys_warning);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL| Notification.FLAG_SHOW_LIGHTS;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ERROR_ID, notification);
    }
}
