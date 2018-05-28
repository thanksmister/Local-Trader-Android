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
 */

package com.thanksmister.bitcoin.localtrader.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.LocationManager;
import android.view.LayoutInflater;

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter;
import com.thanksmister.bitcoin.localtrader.network.services.SyncService;
import com.thanksmister.bitcoin.localtrader.persistence.CurrencyDao;
import com.thanksmister.bitcoin.localtrader.persistence.MethodDao;
import com.thanksmister.bitcoin.localtrader.persistence.NotificationDao;
import com.thanksmister.bitcoin.localtrader.persistence.Preferences;
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao;
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils;

import net.grandcentrix.tray.AppPreferences;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

@Module
class ActivityModule {

    @Provides
    static DialogUtils providesDialogUtils(Application application) {
        return new DialogUtils(application);
    }

    @Provides
    static Resources providesResources(Application application) {
        return application.getResources();
    }

    @Provides
    static LayoutInflater providesInflater(Application application) {
        return (LayoutInflater) application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Provides
    static LocationManager providesLocationManager(Application application) {
        return (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
    }

    @Provides
    SharedPreferences provideSharedPreferences(BaseApplication app) {
        return app.getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
    }

    @Provides
    static LocationManager provideLocationManager(Application application) {
        return (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
    }

    @Provides
    static AppPreferences provideAppPreferences(Application application) {
        return new AppPreferences(application);
    }

    @Provides
    static Preferences provideConfiguration(AppPreferences appPreferences) {
        return new Preferences(appPreferences);
    }

    @Provides
    static SyncAdapter provideSyncAdapter(BaseApplication context, Preferences preferences, NotificationDao notifications,
                                          WalletDao wallet, CurrencyDao currency, MethodDao methods) {
        return new SyncAdapter(context, true, preferences, notifications, wallet, currency, methods);
    }
}