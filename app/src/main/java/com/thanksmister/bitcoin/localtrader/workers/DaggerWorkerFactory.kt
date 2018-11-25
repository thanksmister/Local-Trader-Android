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
 */

package com.thanksmister.bitcoin.localtrader.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils

/**
 * Created by Michael Ritchie on 11/24/18.
 */
class DaggerWorkerFactory(private val walletDao: WalletDao, private val preferences: Preferences,
                          private val notificationUtils: NotificationUtils) : WorkerFactory() {

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {

        val workerKlass = Class.forName(workerClassName).asSubclass(Worker::class.java)
        val constructor = workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is WalletBalanceWorker -> {
                instance.walletDao = walletDao
                instance.preferences = preferences
                instance.notificationUtils = notificationUtils
            }
            // optionally, handle other workers
        }

        return instance
    }
}