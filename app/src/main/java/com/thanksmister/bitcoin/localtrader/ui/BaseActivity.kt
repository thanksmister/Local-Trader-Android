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

package com.thanksmister.bitcoin.localtrader.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.sync.SyncUtils
import com.thanksmister.bitcoin.localtrader.persistence.LocalTraderDatabase
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.activities.*
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import com.thanksmister.bitcoin.localtrader.utils.disposeProper
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

import javax.inject.Inject

/**
 * Base activity which sets up a per-activity object graph and performs injection.
 */
abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var preferences: Preferences
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var dialogUtils: DialogUtils
    @Inject
    lateinit var localBitcoinsDatabase: LocalTraderDatabase

    val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(dialogUtils)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.disposeProper()
    }

    // TODO this is not working from the notification?
    /*override fun onNewIntent(intent: Intent?) {
        Timber.d("onNewIntent")
        super.onNewIntent(intent)
        if (intent == null || intent.extras == null) {
            return
        }

        val extras = intent.extras
        val type = extras.getInt(MainActivity.EXTRA_NOTIFICATION_TYPE, 0)
        val id = extras.getInt(MainActivity.EXTRA_NOTIFICATION_ID, 0)

        Timber.d("type: $type")
        Timber.d("id: ${id}")

        if (type == NotificationUtils.NOTIFICATION_TYPE_CONTACT && id > 0) {
            //onRefreshStop()
            val launchIntent = ContactActivity.createStartIntent(this, id)
            startActivity(launchIntent)
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT  && id > 0) {
            //onRefreshStop()
            val launchIntent = AdvertisementActivity.createStartIntent(this, id)
            startActivity(launchIntent)
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            //onRefreshStop()
            startActivity(WalletActivity.createStartIntent(this@BaseActivity))
        }
    }*/

    open fun launchScanner() {
        startActivity(ScanQrCodeActivity.createStartIntent(this@BaseActivity))
    }

    fun logOutConfirmation() {
        AlertDialog.Builder(this@BaseActivity, R.style.CustomAlertDialog)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok) { dialogInterface, i -> logOut() }
                .show()
    }

    fun logOut() {
        dialogUtils.showProgressDialog(this@BaseActivity, getString(R.string.text_logging_out))
        clearAllTables()
    }

    private fun onLoggedOut() {
        SyncUtils.cancelSync(applicationContext)
        sharedPreferences.edit().clear().apply()
        preferences.reset()
        dialogUtils.hideProgressDialog()
        val intent = PromoActivity.createStartIntent(this@BaseActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun clearAllTables() {
        disposable.add(Completable.fromAction {
            localBitcoinsDatabase.clearAllTables()
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onLoggedOut()
                }, { error -> Timber.e("Database clear error" + error.message) }))
    }

    @Deprecated("use dialog utils")
    fun toast(messageId: Int) {
        dialogUtils.toast(messageId)
    }

    @Deprecated("use dialog utils")
    fun toast(message: String) {
        dialogUtils.toast(message)
    }

    companion object {
        fun hideSoftKeyboard(activity: Activity) {
            val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            if (activity.currentFocus != null && activity.currentFocus!!.windowToken != null) {
                inputMethodManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
            }
        }
    }
}