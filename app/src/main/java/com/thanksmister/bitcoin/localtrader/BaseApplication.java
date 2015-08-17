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

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.thanksmister.bitcoin.localtrader.data.CrashlyticsTree;

import butterknife.ButterKnife;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;
import timber.log.Timber;

public class BaseApplication extends Application
{
    @Override
    public void onCreate() 
    {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            //ButterKnife.setDebug(BuildConfig.DEBUG);
            LeakCanary.install(this);
            //refWatcher = LeakCanary.install(this);
        } else {
            Fabric.with(this, new Crashlytics());
            RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler()
            {
                @Override
                public void handleError(Throwable e)
                {
                    Timber.e("RXJava Error", e);
                    Crashlytics.logException(e);
                }
            });
            
            Timber.plant(new CrashlyticsTree());
        }
        
        Injector.init(this);
    }

    public static RefWatcher getRefWatcher(Context context) {
        BaseApplication application = (BaseApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    private RefWatcher refWatcher;
}
