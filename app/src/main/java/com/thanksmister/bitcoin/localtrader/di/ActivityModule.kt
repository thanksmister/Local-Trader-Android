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

package com.thanksmister.bitcoin.localtrader.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.location.LocationManager
import android.preference.PreferenceManager
import android.view.LayoutInflater
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import dagger.Module
import dagger.Provides
import net.grandcentrix.tray.AppPreferences
import javax.inject.Singleton

@Module
class ActivityModule {

    @Provides
    fun providesDialogUtils(application: Application): DialogUtils {
        return DialogUtils(application)
    }

    @Provides
    fun provideAppPreferences(application: Application): AppPreferences {
        return AppPreferences(application)
    }

    @Provides
    fun providesResources(application: Application): Resources {
        return application.resources
    }

    @Provides
    fun providesInflater(application: Application): LayoutInflater {
        return application.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    @Provides
    fun provideLocationManager(application: Application): LocationManager {
        return application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(app: Application): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(app.applicationContext)
    }

    @Singleton
    @Provides
    fun notificationUtils(application: Application): NotificationUtils {
        return NotificationUtils(application)
    }

    @Singleton
    @Provides
    fun providesRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance()
    }

    /*@Provides
    internal fun provideGeoLocationService(app: BaseApplication, localBitcoins: LocalBitcoinsService): GeoLocationService {
        return GeoLocationService(app, localBitcoins)
    }
    */
}