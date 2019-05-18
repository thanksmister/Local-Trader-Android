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