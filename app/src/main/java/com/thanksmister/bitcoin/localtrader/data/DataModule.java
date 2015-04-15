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

package com.thanksmister.bitcoin.localtrader.data;

import android.content.SharedPreferences;
import android.location.LocationManager;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.thanksmister.bitcoin.localtrader.ActivityModule;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.ApiModule;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.BitstampExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
//import com.thanksmister.bitcoin.localtrader.data.database.CupboardSQLiteOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

@Module(
        includes = {ApiModule.class},
        complete = false,
        library = true
)
public final class DataModule
{
    static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(BaseApplication app)
    {
        return app.getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
    }

    @Provides
    @Singleton
    DatabaseManager provideDatabaseManager()
    {
        return new DatabaseManager();
    }

    @Provides
    @Singleton
    DataModel provideDataModel()
    {
        return new DataModel();
    }

   
    /*@Provides
    @Singleton
    CupboardSQLiteOpenHelper provideCupboardSQLiteOpenHelper(BaseApplication app)
    {
        return new CupboardSQLiteOpenHelper(app);
    }*/

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient(BaseApplication app)
    {
        return createOkHttpClient(app);
    }

    static OkHttpClient createOkHttpClient(BaseApplication app)
    {
        OkHttpClient client = new OkHttpClient();

        // Install an HTTP cache in the application cache directory.
        try {
            File cacheDir = new File(app.getCacheDir(), "http");
            Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
            client.setCache(cache);
        } catch (IOException e) {
            Timber.e(e, "Unable to install disk cache.");
        }

        return client;
    }
}