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

import android.content.SharedPreferences;
import android.location.LocationManager;

import com.thanksmister.bitcoin.localtrader.data.DataModule;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.BitstampExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseManager;
import com.thanksmister.bitcoin.localtrader.data.database.DbModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.domain.DomainModule;
import com.thanksmister.bitcoin.localtrader.ui.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.DashboardFragment;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.LoginActivity;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.PromoActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, PromoActivity.class, LoginActivity.class, AdvertisementActivity.class, 
                EditActivity.class, DashboardFragment.class, WalletFragment.class, ContactActivity.class},
        includes = {DataModule.class, DomainModule.class, DbModule.class},
        complete = false,
        library = true
)
public final class ActivityModule
{
    @Provides
    @Singleton
    DataService provideDataService(BaseApplication app, DatabaseManager databaseManager, SharedPreferences preferences, LocalBitcoins localBitcoins, BitstampExchange exchange, BitcoinAverage bitcoinAverage, BitfinexExchange bitfinexExchange)
    {
        return new DataService(app, databaseManager, preferences, localBitcoins, exchange, bitcoinAverage, bitfinexExchange);
    }

    @Provides
    @Singleton
    GeoLocationService provideGeoLocationService(BaseApplication app, SharedPreferences preferences, LocationManager locationManager, LocalBitcoins localBitcoins)
    {
        return new GeoLocationService(app, preferences, locationManager, localBitcoins);
    }
}