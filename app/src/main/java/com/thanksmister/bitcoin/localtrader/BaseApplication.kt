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

package com.thanksmister.bitcoin.localtrader

import android.content.Context
import androidx.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.facebook.stetho.Stetho
import com.google.firebase.analytics.FirebaseAnalytics
import com.thanksmister.bitcoin.localtrader.di.DaggerApplicationComponent
import com.thanksmister.bitcoin.localtrader.utils.CrashlyticsTree
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import io.fabric.sdk.android.Fabric
import timber.log.Timber

class BaseApplication : DaggerApplication() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.builder().create(this);
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build())
            Timber.plant(CrashlyticsTree())
        } else {
            Fabric.with(this, Crashlytics())
            Fabric.with(this, Answers())
            Timber.plant(CrashlyticsTree())
            // Obtain the FirebaseAnalytics instance.
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}