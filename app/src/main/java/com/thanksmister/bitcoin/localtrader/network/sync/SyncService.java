/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.sync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.android.AndroidInjection;

public class SyncService extends Service {
    
    @Inject
    Lazy<SyncAdapter> sSyncAdapter;
    
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.get().getSyncAdapterBinder();
    }
    
    public static void requestSyncNow(Context context) {
        SyncUtils.requestSyncNow(context);
    }

    public static void requestSyncLater(final Context context, long delay) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                SyncService.requestSyncNow(context);
            }
        }, delay);
    }
}