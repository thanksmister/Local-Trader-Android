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
 *
 */

package com.thanksmister.bitcoin.localtrader

import android.content.Context
import android.support.multidex.MultiDex
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.Stetho
import com.thanksmister.bitcoin.localtrader.di.DaggerApplicationComponent
import com.thanksmister.bitcoin.localtrader.network.sync.SyncUtils
import com.thanksmister.bitcoin.localtrader.utils.CrashlyticsTree
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import javax.inject.Inject

class BaseApplication : DaggerApplication() {

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
            Timber.plant(CrashlyticsTree())
        }

        // set up sync
        /*try {
            val account = SyncUtils.getSyncAccount(this)
            if (account != null) {
                ContentResolver.setIsSyncable(account, SyncProvider.CONTENT_AUTHORITY, 1)
                ContentResolver.setSyncAutomatically(account, SyncProvider.CONTENT_AUTHORITY, true)
                ContentResolver.addPeriodicSync(account, SyncProvider.CONTENT_AUTHORITY, Bundle.EMPTY, SyncUtils.SYNC_FREQUENCY)
            }
        } catch (e: Exception) {
            Timber.e(e.message)
            if (!BuildConfig.DEBUG) {
                Crashlytics.log(1, "Sync Error", e.message)
            }
        }*/

        configureWorkManager()

        SyncUtils.createSyncAccount(applicationContext)
        SyncUtils.requestSyncNow(applicationContext)
    }

    @Inject
    lateinit var workerFactory: WorkerFactory

    private fun configureWorkManager() {
        val config = Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

        WorkManager.initialize(this, config)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}