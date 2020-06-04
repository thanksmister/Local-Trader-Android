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

package com.thanksmister.bitcoin.localtrader.network.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet
import com.thanksmister.bitcoin.localtrader.persistence.NotificationsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.applySchedulersIo
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import timber.log.BuildConfig
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.get
import kotlin.collections.isNotEmpty
import kotlin.collections.remove
import kotlin.collections.set

@Singleton
class SyncAdapter : AbstractThreadedSyncAdapter {

    private var preferences: Preferences? = null
    private var walletDao: WalletDao? = null
    private var notificationsDao: NotificationsDao? = null
    private var notificationUtils: NotificationUtils? = null
    private var fetcher: LocalBitcoinsFetcher? = null
    private val disposable = CompositeDisposable()

    @Inject
    constructor(context: Context,
                okHttpClient: OkHttpClient,
                walletDao: WalletDao,
                notificationsDao: NotificationsDao,
                notificationUtils: NotificationUtils,
                preferences: Preferences) : this(context, true) {

        this.preferences = preferences
        this.walletDao = walletDao
        this.notificationsDao = notificationsDao
        this.notificationUtils = notificationUtils
        val api = LocalBitcoinsApi(okHttpClient, preferences.getServiceEndpoint())
        fetcher = LocalBitcoinsFetcher(context, api, preferences)
        Timber.d("SyncAdapter")
    }

    constructor(context: Context, autoInitialize: Boolean) : super(context, autoInitialize) {
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    constructor(
            context: Context,
            autoInitialize: Boolean,
            allowParallelSyncs: Boolean) : super(context, autoInitialize, allowParallelSyncs) {
    }

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult) {

        Timber.d("onPerformSync")
        // TODO let's slow down balance calls
        updateWalletBalance()
        updateNotifications()
    }

    private fun updateNotifications() {
        disposable.add(getNotifications()
                .applySchedulersIo()
                .subscribe({ localNotifications ->
                    fetcher!!.notifications()
                            .applySchedulers()
                            .subscribe({ remoteNotifications ->
                                if(!localNotifications.isEmpty() && !remoteNotifications.isEmpty()) {
                                    val entryMap = HashMap<String, Notification>();
                                    for (notification in remoteNotifications) {
                                        if(notification.notificationId != null) {
                                            entryMap[notification.notificationId!!] = notification
                                        }
                                    }
                                    for (localNotification in localNotifications) {
                                        val notificationId = localNotification.notificationId
                                        val notificationRead = localNotification.read
                                        val url = localNotification.url
                                        val match = entryMap[notificationId]
                                        if (match != null) {
                                            entryMap.remove(notificationId);
                                            if (match.read != notificationRead || !match.url.equals(url)) {
                                                localNotification.read = notificationRead
                                                localNotification.url = url
                                                updateNotifications(localNotification)
                                            }
                                        } else {
                                            deleteNotifications(localNotification)
                                        }
                                    }
                                    val newNotifications = ArrayList<Notification>();
                                    if (!entryMap.isEmpty()) {
                                        for (notification in entryMap.values) {
                                            newNotifications.add(notification);
                                        }
                                        insertNotifications(newNotifications)
                                    }
                                    Timber.d("New Notifications: ${newNotifications.size}");
                                    if (!newNotifications.isEmpty()) {
                                        notificationUtils!!.createNotifications(newNotifications);
                                    }
                                } else if (localNotifications.isEmpty() &&!remoteNotifications.isEmpty()) {
                                    notificationUtils!!.createNotifications(remoteNotifications);
                                    insertNotifications(remoteNotifications)
                                }
                            }, { error ->
                                Timber.e("Error update notifications ${error.message}")
                            })
                }, { error ->
                    Timber.e("Error update notifications ${error.message}")
                    if(!BuildConfig.DEBUG) {
                        Crashlytics.setString("sync_error", "update notification error")
                        Crashlytics.logException(error)
                    }
                }))
    }

    private fun updateWalletBalance() {
        disposable.add(getWallet()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ walletLocal ->
                    fetcher!!.walletBalance()
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ walletRemote ->
                                if(walletLocal != null) {
                                    val remoteBalance = walletRemote.total.balance
                                    val localBalance = walletLocal.total.balance
                                    Timber.d("Wallet remoteBalance: " + remoteBalance);
                                    Timber.d("Wallet localBalance: " + localBalance);
                                    if (remoteBalance != null && localBalance != null) {
                                        val remote = remoteBalance.toDouble()
                                        val local = localBalance.toDouble()
                                        if(remote > local) {
                                            val diff = Conversions.formatBitcoinAmount(remote - local)
                                            notificationUtils!!.balanceUpdateNotification(diff)
                                            walletLocal.receivingAddress = walletRemote.receivingAddress
                                            walletLocal.total = walletRemote.total
                                            insertWallet(walletLocal)
                                        }
                                    }
                                } else if (walletRemote != null) {
                                    val remoteBalance = walletRemote.total.balance
                                    if(remoteBalance != null) {
                                        notificationUtils!!.balanceCurrentNotification(remoteBalance)
                                        insertWallet(walletRemote)
                                    }
                                }
                            }, { error ->
                                Timber.e("Error getting wallet balance ${error.message}")
                            })
                }, { error ->
                    Timber.e("Error getting wallet balance ${error.message}")
                    if(!BuildConfig.DEBUG) {
                        Crashlytics.setString("sync_error", "update wallet balance error")
                        Crashlytics.logException(error)
                    }
                }))
    }

    private fun getNotifications():Flowable<List<Notification>> {
        return notificationsDao!!.getItems()
                .filter { items -> items.isNotEmpty() }
    }

    private fun getWallet(): Flowable<Wallet> {
        return walletDao!!.getItems()
                .filter { items -> items.isNotEmpty() }
                .map { items -> items[0] }
    }

    private fun insertWallet(item: Wallet) {
        Timber.d("insertWallet")
        disposable.add(Completable.fromAction {
            walletDao!!.updateItem(item)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Wallet insert error" + error.message) }))
    }

    private fun deleteNotifications(item: Notification) {
        disposable.add(Completable.fromAction {
            notificationsDao!!.deleteItem(item.notificationId!!)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification upate error" + error.message)}))
    }

    private fun updateNotifications(item: Notification) {
        disposable.add(Completable.fromAction {
            notificationsDao!!.updateItem(item)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification upate error" + error.message)}))
    }

    private fun insertNotifications(items: List<Notification>) {
        disposable.add(Completable.fromAction {
            notificationsDao!!.insertItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification insert error" + error.message)}))
    }

    override fun onSyncCanceled() {
        super.onSyncCanceled()
        disposable.clear()
    }
}