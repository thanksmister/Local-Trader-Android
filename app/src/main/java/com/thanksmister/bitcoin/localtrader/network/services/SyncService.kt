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

package com.thanksmister.bitcoin.localtrader.network.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

import com.thanksmister.bitcoin.localtrader.BaseApplication

import javax.inject.Inject

import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DaggerService
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasServiceInjector

class SyncService : Service() {

    @Inject lateinit var sSyncAdapter: SyncAdapter

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter.syncAdapterBinder
    }

    companion object {

        fun requestSyncNow(context: Context) {
            SyncUtils.requestSyncNow(context)
        }

        fun requestSyncLater(context: Context, delay: Long) {
            Handler(Looper.getMainLooper()).postDelayed({ SyncService.requestSyncNow(context) }, delay)
        }
    }
}