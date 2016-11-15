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

import android.content.SharedPreferences;

import com.thanksmister.bitcoin.localtrader.data.DataModule;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.DbModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.data.services.NotificationService;
import com.thanksmister.bitcoin.localtrader.domain.DomainModule;
import com.thanksmister.bitcoin.localtrader.ui.AdvertisementsFragment;
import com.thanksmister.bitcoin.localtrader.ui.ContactsFragment;
import com.thanksmister.bitcoin.localtrader.ui.MessagesFragment;
import com.thanksmister.bitcoin.localtrader.ui.SendFragment;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertiserActivity;
import com.thanksmister.bitcoin.localtrader.ui.bitcoin.BitcoinHandler;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.DashboardFragment;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.LoginActivity;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.PromoActivity;
import com.thanksmister.bitcoin.localtrader.ui.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchFragment;
import com.thanksmister.bitcoin.localtrader.ui.WalletFragment;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchResultsActivity;
import com.thanksmister.bitcoin.localtrader.ui.AboutFragment;
import com.thanksmister.bitcoin.localtrader.ui.RequestFragment;
import com.thanksmister.bitcoin.localtrader.ui.search.TradeRequestActivity;
import com.thanksmister.bitcoin.localtrader.ui.settings.SettingsActivity;
import com.thanksmister.bitcoin.localtrader.ui.settings.SettingsFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, PromoActivity.class, LoginActivity.class, AdvertisementActivity.class, 
                AdvertisementsFragment.class, MessagesFragment.class, ContactsFragment.class,
                EditActivity.class, DashboardFragment.class, WalletFragment.class, ContactActivity.class, SearchFragment.class,
                PinCodeActivity.class, SearchResultsActivity.class, AdvertiserActivity.class, AboutFragment.class, SendFragment.class, RequestFragment.class,
                TradeRequestActivity.class, ContactsActivity.class, BitcoinHandler.class, SettingsActivity.class, SettingsFragment.class},
        includes = {DataModule.class, DomainModule.class, DbModule.class},
        complete = false,
        library = true
)
public final class ActivityModule
{
    @Provides
    @Singleton
    DataService provideDataService(DbManager db, BaseApplication app, SharedPreferences preferences, LocalBitcoins localBitcoins, BitcoinAverage bitcoinAverage)
    {
        return new DataService(db, app, preferences, localBitcoins, bitcoinAverage);
    }

    @Provides
    @Singleton
    NotificationService provideNotificationService(BaseApplication app, SharedPreferences preferences)
    {
        return new NotificationService(app, preferences);
    }

    @Provides
    @Singleton
    GeoLocationService provideGeoLocationService(BaseApplication app, LocalBitcoins localBitcoins)
    {
        return new GeoLocationService(app, localBitcoins);
    }
}