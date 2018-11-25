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

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.util.Log
import androidx.work.*
import androidx.work.PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS
import androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS

import java.util.concurrent.TimeUnit

import timber.log.Timber

/**
 * Created by Michael Ritchie on 11/24/18.
 */
object WalletBalanceScheduler {

    fun refreshWalletBalanceWorkOnce(lifecycleOwner: LifecycleOwner) {

        //worker input
        val source = Data.Builder()
                .putString("workType", "OneTime")
                .build()

        //One time work request
        val refreshCpnWork = OneTimeWorkRequest.Builder(WalletBalanceWorker::class.java)
                .setInputData(source)
                .build()

        //enqueue the work request
        WorkManager.getInstance().enqueue(refreshCpnWork)

        //listen to status and data from worker
        // TODO we could add a listener here to show progress
        WorkManager.getInstance().getStatusByIdLiveData(refreshCpnWork.id)
                .observe(lifecycleOwner, Observer { status ->
                    if (status != null && status.state.isFinished) {
                        val refreshTime = status.outputData.getString(WalletBalanceWorker.WALLET_BALANCE)
                        Timber.d("wallet balance: " + refreshTime)
                    }
                })
    }

    fun refreshWalletBalanceWorkPeriodically() {

        Timber.d("refreshWalletBalanceWorkPeriodically")
        //define constraints
        //    .setRequiresDeviceIdle(false) API 23+
        val myConstraints = Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

        val source = Data.Builder()
                .putString("workType", "PeriodicTime")
                .build()

        val refreshCpnWork = PeriodicWorkRequest.Builder(WalletBalanceWorker::class.java, MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MINUTES)
                .setConstraints(myConstraints)
                .setInputData(source)
                .build()

        WorkManager.getInstance().enqueueUniquePeriodicWork("walletBalanceWork", ExistingPeriodicWorkPolicy.REPLACE, refreshCpnWork)
    }

    fun cancelUniqueWalletBalanceWork() {
        WorkManager.getInstance().cancelUniqueWork("walletBalanceWork")
    }

    fun cancelRefreshWalletBalanceWork() {
        WorkManager.getInstance().cancelAllWork()
    }
}