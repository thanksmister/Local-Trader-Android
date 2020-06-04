/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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