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
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Michael Ritchie on 11/23/18.
 */
class WalletBalanceWorker (context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    lateinit var walletDao: WalletDao
    lateinit var preferences: Preferences
    lateinit var notificationUtils: NotificationUtils

    val disposable = CompositeDisposable()

    override fun doWork(): Result {
        Timber.d("dowork: ")
        //Timber.d("observable: ${observable}")
        /*val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(applicationContext, endpoint)
        val fetcher = LocalBitcoinsFetcher(applicationContext, api, preferences)
        if(observable == null) {
            observable = fetcher.walletBalance
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ walletRemote ->
                        Timber.d("Wallet remoteBalance: " + walletRemote.total.balance);
                    }, { error ->
                        Timber.e("Error getting wallet balance ${error.message}")
                    })
        }*/
       return Result.SUCCESS

           /* disposable.add(fetcher.walletBalance
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ walletRemote ->
                        if(walletLocal != null) {
                            walletLocal.receivingAddress = walletRemote.receivingAddress
                            walletLocal.total = walletRemote.total
                            walletDao.updateItem(walletLocal)
                            val remoteBalance = walletRemote.total.balance
                            val localBalance = walletLocal.total.balance
                            Timber.d("Wallet remoteBalance: " + remoteBalance);
                            Timber.d("Wallet localBalance: " + localBalance);
                            if (remoteBalance != null && localBalance != null) {
                                val remote = remoteBalance.toDouble()
                                val local = localBalance.toDouble()
                                if(remote > local) {
                                    val diff = Conversions.formatBitcoinAmount(remote - local)
                                    notificationUtils.balanceUpdateNotification(diff)
                                    //sending data to the caller
                                    val reportBalance = Data.Builder()
                                            .putString(WALLET_BALANCE, diff)
                                            .build()

                                    outputData = reportBalance
                                }
                            }
                        } else if (walletRemote != null) {
                            walletDao.updateItem(walletRemote)
                            val remoteBalance = walletRemote.total.balance
                            if(remoteBalance != null) {
                                notificationUtils.balanceCurrentNotification(remoteBalance)
                                val reportBalance = Data.Builder()
                                        .putString(WALLET_BALANCE, remoteBalance)
                                        .build()
                                outputData = reportBalance
                            }
                        }
                    }, { error ->
                        Timber.e("Error getting wallet balance ${error.message}")
                    }))*/

    }

    private fun getWallet(): Flowable<Wallet> {
        return walletDao.getItems()
                .filter { items -> items.isNotEmpty() }
                .map { items -> items[0] }
    }

    companion object {
        const val WALLET_BALANCE = "walletBalance"
    }
}