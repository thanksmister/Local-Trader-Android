/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import android.view.LayoutInflater
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
    fun provideSharedPreferences(app: Application): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(app.applicationContext)
    }

    @Provides
    fun notificationUtils(application: Application): NotificationUtils {
        return NotificationUtils(application)
    }
}