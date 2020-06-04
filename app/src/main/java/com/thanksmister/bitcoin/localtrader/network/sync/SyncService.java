/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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