/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.data.CrashlyticsTree;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;
import timber.log.Timber;

public class BaseApplication extends Application
{
    private ObjectGraph applicationGraph;
    
    @Override
    public void onCreate() 
    {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            //ButterKnife.setDebug(BuildConfig.DEBUG);
        } else {
            Crashlytics.start(this);
            Timber.plant(new CrashlyticsTree());
        }

        applicationGraph = ObjectGraph.create(getModules().toArray());
    }

    /**
     * A list of modules to use for the application graph. Subclasses can override this method to
     * provide additional modules provided they call {@code super.getModules()}.
     */
    protected List<Object> getModules() {
        return Arrays.<Object>asList(new ApplicationModule(this));
    }

    public boolean isConnected()
    {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public boolean connectedOnWiFi()
    {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public ObjectGraph createScopedGraph(Object... modules) 
    {
        return applicationGraph.plus(modules);
    }

    public void inject(Object o) 
    {
        applicationGraph.inject(o);
    }

    public static BaseApplication get(Context context) 
    {
        return (BaseApplication) context.getApplicationContext();
    }
}
