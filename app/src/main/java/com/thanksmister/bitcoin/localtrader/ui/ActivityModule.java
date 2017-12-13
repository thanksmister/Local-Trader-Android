/*
 * Copyright (c) 2017 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui;

import android.content.SharedPreferences;

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.DataModule;
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.database.DbModule;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.network.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.network.services.NotificationService;
import com.thanksmister.bitcoin.localtrader.domain.DomainModule;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.AboutFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.AdvertisementsFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.ContactsFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.DashboardFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.LoginActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.NotificationsFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.PromoActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.RequestFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.SendFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.SplashActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.WalletFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertiserActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.EditAdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditInfoFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditMoreInfoFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditOnlineFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditSecurityFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.EditTypeFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.BitcoinHandler;
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.MessageActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.SearchFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchResultsActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.TradeRequestActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.SettingsActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.SettingsFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dpreference.DPreference;

@Module(
        injects = {MainActivity.class, PromoActivity.class, LoginActivity.class, AdvertisementActivity.class, 
                SplashActivity.class, EditAdvertisementActivity.class, 
                AdvertisementsFragment.class, ContactsFragment.class, 
                EditTypeFragment.class, EditInfoFragment.class, EditMoreInfoFragment.class, EditOnlineFragment.class, EditSecurityFragment.class,
                EditActivity.class, DashboardFragment.class, WalletFragment.class, ContactActivity.class, SearchFragment.class,
                PinCodeActivity.class, SearchResultsActivity.class, AdvertiserActivity.class, AboutFragment.class, SendFragment.class, RequestFragment.class,
                TradeRequestActivity.class, ContactsActivity.class, BitcoinHandler.class, SettingsActivity.class, SettingsFragment.class, 
                MessageActivity.class, NotificationsFragment.class},
        includes = {DataModule.class, DomainModule.class, DbModule.class},
        complete = false,
        library = true
)
public final class ActivityModule
{
    @Provides
    @Singleton
    DataService provideDataService(BaseApplication app, DPreference preferences, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins) {
        return new DataService(app, preferences, sharedPreferences, localBitcoins);
    }

    @Provides
    @Singleton
    NotificationService provideNotificationService(BaseApplication app, DPreference preferences, SharedPreferences sharedPreferences) {
        return new NotificationService(app, preferences, sharedPreferences);
    }

    @Provides
    @Singleton
    GeoLocationService provideGeoLocationService(BaseApplication app, LocalBitcoins localBitcoins) {
        return new GeoLocationService(app, localBitcoins);
    }
}