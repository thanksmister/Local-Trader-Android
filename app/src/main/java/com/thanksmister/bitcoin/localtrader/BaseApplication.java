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

package com.thanksmister.bitcoin.localtrader;

import android.content.ContentResolver;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.thanksmister.bitcoin.localtrader.data.CrashlyticsTree;
import com.thanksmister.bitcoin.localtrader.network.services.SyncProvider;
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class BaseApplication extends MultiDexApplication
{
    @Override
    public void onCreate() 
    {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            
            Timber.plant(new Timber.DebugTree());
            //ButterKnife.setDebug(BuildConfig.DEBUG);
            //LeakCanary.install(this);
            //refWatcher = LeakCanary.install(this);

           Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build());

        } else {
            
            Fabric.with(this, new Crashlytics());
            /*RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler()
            {
                @Override
                public void handleError(Throwable e)
                {
                    Timber.e("RXJava Error", e);
                    Crashlytics.logException(e);
                }
            });
            */
            Timber.plant(new CrashlyticsTree());
        }
        
        Injector.init(this);

        // set up sync
        ContentResolver.setIsSyncable(SyncUtils.getSyncAccount(this), SyncProvider.CONTENT_AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(SyncUtils.getSyncAccount(this), SyncProvider.CONTENT_AUTHORITY, true);
        ContentResolver.addPeriodicSync(SyncUtils.getSyncAccount(this), SyncProvider.CONTENT_AUTHORITY, Bundle.EMPTY, SyncUtils.SYNC_FREQUENCY);
    }
}
