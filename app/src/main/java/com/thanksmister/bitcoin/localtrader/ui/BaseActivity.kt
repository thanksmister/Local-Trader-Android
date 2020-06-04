/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.sync.SyncUtils
import com.thanksmister.bitcoin.localtrader.persistence.LocalTraderDatabase
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.activities.PromoActivity
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.disposeProper
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
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
                .applySchedulers()
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