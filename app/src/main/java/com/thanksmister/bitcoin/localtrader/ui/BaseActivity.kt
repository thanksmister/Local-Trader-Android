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

import android.content.Context
import android.os.NetworkOnMainThreadException
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.android.IntentIntegrator
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent
import com.thanksmister.bitcoin.localtrader.network.NetworkException
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.activities.PromoActivity
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import timber.log.Timber
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var preferences: Preferences
    val disposable = CompositeDisposable()
    private var snackBar: Snackbar? = null
    private val hasNetwork = AtomicBoolean(true)

    /**
     * This activity requires authentication
     */
    @Retention(RetentionPolicy.RUNTIME)
    annotation class RequiresAuthentication

    override fun onDestroy() {
        super.onDestroy()
        if (snackBar != null && snackBar!!.isShownOrQueued) {
            snackBar!!.dismiss()
            snackBar = null
        }
    }


    open fun handleNetworkDisconnect() {
        hasNetwork.set(false)
    }

    open fun handleNetworkConnect() {
        if (snackBar != null && snackBar!!.isShown) {
            snackBar!!.dismiss()
            snackBar = null
        }
        hasNetwork.set(true)
    }

    fun launchScanner() {
        val scanIntegrator = IntentIntegrator(this@BaseActivity)
        scanIntegrator.initiateScan(IntentIntegrator.QR_CODE_TYPES)
    }

    @JvmOverloads
    fun showProgressDialog(event: ProgressDialogEvent, cancelable: Boolean = false) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_progress, null, false)
        val progressDialogMessage = dialogView.findViewById<View>(R.id.progressDialogMessage) as TextView
        progressDialogMessage.text = event.message

        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setCancelable(cancelable)
                .setView(dialogView)
                .show()
    }

    @Deprecated("No longer using progress")
    fun hideProgressDialog() {

    }

    @Deprecated("No longer used")
    fun showAlertDialog(message: String) {
        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    @Deprecated("No longer used")
    fun showAlertDialog(event: AlertDialogEvent) {

        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setTitle(event.title)
                .setMessage(Html.fromHtml(event.message))
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(event.cancelable)
                .show()
    }

    @Deprecated("No longer used")
    fun showAlertDialogLinks(message: String) {

        val view = View.inflate(this@BaseActivity, R.layout.dialog_about, null)
        val textView = view.findViewById<View>(R.id.message) as TextView
        textView.text = Html.fromHtml(message)
        textView.movementMethod = LinkMovementMethod.getInstance()
        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    @Deprecated("No longer used")
    fun showAlertDialog(event: AlertDialogEvent, actionToTake: Action) {

        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setTitle(event.title)
                .setMessage(Html.fromHtml(event.message))
                .setCancelable(event.cancelable)
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    try {
                        actionToTake.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .show()
    }

    @Deprecated("No longer used")
    fun showAlertDialog(message: String, actionToTake: Action) {
        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    try {
                        actionToTake.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .show()
    }

    @Deprecated("No longer used")
    fun showAlertDialog(message: String, actionToTake: Action, cancelActionToTake: Action) {
       AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setCancelable(false)
                .setMessage(Html.fromHtml(message))
                .setNegativeButton(android.R.string.cancel) { dialogInterface, i ->
                    try {
                        cancelActionToTake.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    try {
                        actionToTake.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .show()
    }

    @Deprecated("No longer used")
    fun showAlertDialog(event: AlertDialogEvent, actionToTake: Action, cancelActionToTake: Action) {
       AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setTitle(event.title)
                .setCancelable(false)
                .setMessage(Html.fromHtml(event.message))
                .setCancelable(event.cancelable)
                .setNegativeButton(android.R.string.cancel) { dialogInterface, i ->
                    try {
                        cancelActionToTake.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    try {
                        actionToTake.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .show()
    }

    fun logOutConfirmation() {
        AlertDialog.Builder(this@BaseActivity, R.style.DialogTheme)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok) { dialogInterface, i -> onLoggedOut() }
                .show()
    }

    @Deprecated("No longer used")
    fun logOut() {
        onLoggedOut()
    }

    open fun onLoggedOut() {
        preferences.reset()
        SyncUtils.cancelSync(this)
        val intent = PromoActivity.createStartIntent(this@BaseActivity)
        startActivity(intent)
        finish()
    }

    fun hasNetworkConnectivity(): Boolean {
        return hasNetwork.get()
    }

    open fun toast(messageId: Int) {
        toast(getString(messageId))
    }

    open fun toast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM, 0, 180)
        toast.show()
    }

    fun reportError(throwable: Throwable?) {
        if (throwable is SSLHandshakeException) {
            Timber.e(throwable.message)
            return
        }
        if (throwable is UnknownHostException) {
            Timber.e(throwable.message)
            toast(getString(R.string.error_no_internet))
            return
        }
        if (throwable != null && throwable is NetworkOnMainThreadException) {
            val exception = throwable as NetworkOnMainThreadException?
            Timber.e(exception!!.message)
        } else if (throwable != null) {
            Timber.e(throwable.message)
            throwable.printStackTrace()
        }
    }

    // FIXME when we move this over to new Retrofit implement new error handling across all network calls
    @Deprecated("")
    @JvmOverloads
    fun handleError(throwable: Throwable, retry: Boolean = false) {

        // Handle NetworkConnectionException
        /*if (throwable instanceof ConnectionException) {
            ConnectionException networkConnectionException = (ConnectionException) throwable;
            snack(networkConnectionException.getMessage(), retry);
            return;
        }*/

        // Handle NetworkException
        if (throwable is NetworkException) {
            val networkException = throwable

            /*if (networkException.getStatus() == 403) {
                showAlertDialog(new AlertDialogEvent(getString(R.string.alert_token_expired_title), getString(R.string.error_bad_token)), new Action0() {
                    @Override
                    public void call() {
                        logOut();
                    }
                });
                return;
            } else if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                // refreshing token and will return 403 if refresh token invalid
                return;
            } else {
                // let's just let the throwable pass through
                throwable = networkException.getCause();
            }*/
        }

        // Handle Throwable exception
        /*if (DataServiceUtils.isConnectionError(throwable)) {
            Timber.i("Connection Error");
            snack(getString(R.string.error_service_unreachable_error), retry);
        } else if (DataServiceUtils.isTimeoutError(throwable)) {
            Timber.i("Timeout Error");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if (DataServiceUtils.isNetworkError(throwable)) {
            Timber.i("Data Error: " + "Code 503");
            snack(getString(R.string.error_service_unreachable_error), retry);
        } else if (DataServiceUtils.isHttp504Error(throwable)) {
            Timber.i("Data Error: " + "Code 504");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if (DataServiceUtils.isHttp502Error(throwable)) {
            Timber.i("Data Error: " + "Code 502");
            snack(getString(R.string.error_service_error), retry);
        } else if (DataServiceUtils.isConnectionError(throwable)) {
            Timber.e("Connection Error: " + "Code ???");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if (DataServiceUtils.isHttp403Error(throwable)) {
            Timber.i("Data Error: " + "Code 403");
            toast(getString(R.string.error_authentication));
        } else if (DataServiceUtils.isHttp401Error(throwable)) {
            Timber.i("Data Error: " + "Code 401");
            snack(getString(R.string.error_no_internet), retry);
        } else if (DataServiceUtils.isHttp500Error(throwable)) {
            Timber.i("Data Error: " + "Code 500");
            snack(getString(R.string.error_service_error), retry);
        } else if (DataServiceUtils.isHttp404Error(throwable)) {
            Timber.i("Data Error: " + "Code 404");
            snack(getString(R.string.error_service_error), retry);
        } else if (DataServiceUtils.isHttp400Error(throwable)) {
            Timber.e("Data Error: " + "Code 400");
            RetroError error = DataServiceUtils.createRetroError(throwable);
            if (error.getCode() == 403) {
                toast(getString(R.string.error_bad_token));
                showAlertDialog(new AlertDialogEvent(getString(R.string.alert_token_expired_title), getString(R.string.error_bad_token)), new Action0() {
                    @Override
                    public void call() {
                        logOut();
                    }
                });
            } else {
                Timber.e("Data Error Message: " + error.getMessage());
                snack(error.getMessage(), retry);
            }
        } else if (throwable != null && throwable.getMessage() != null) {
            Timber.i("Data Error: " + throwable.getMessage());
            snack(throwable.getMessage(), retry);
        } else {
            snack(R.string.error_unknown_error, retry);
        }*/
    }

    protected fun snack(message: Int, retry: Boolean) {
        snack(getString(message), retry)
    }

    protected fun snackError(message: String) {
        if (snackBar != null && snackBar!!.isShownOrQueued) {
            snackBar!!.dismiss()
        }

        try {
            // TODO check for null pointer
            val view = findViewById<View>(R.id.coordinatorLayout)
            if (view != null) {
                snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                val textView = snackBar!!.view.findViewById<View>(android.support.design.R.id.snackbar_text) as TextView
                textView.setTextColor(resources.getColor(R.color.white))
                snackBar!!.show()
            }
        } catch (e: NullPointerException) {
            Timber.e(e.message)
        }
    }

    open fun handleRefresh() {
        // TODO do we need to implement this any longer
    }

    protected fun snack(message: String, retry: Boolean) {
        if (snackBar != null && snackBar!!.isShownOrQueued) {
            snackBar!!.dismiss()
        }
        try {
            val view = findViewById<View>(R.id.coordinatorLayout)
            if (view != null) {
                if (retry) {
                    snackBar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.button_retry) { handleRefresh() }
                    val textView = snackBar!!.view.findViewById<View>(android.support.design.R.id.snackbar_text) as TextView
                    textView.setTextColor(resources.getColor(R.color.white))
                    snackBar!!.show()
                } else {
                    snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    val textView = snackBar!!.view.findViewById<View>(android.support.design.R.id.snackbar_text) as TextView
                    textView.setTextColor(resources.getColor(R.color.white))
                    snackBar!!.show()
                }
            }
        } catch (e: NullPointerException) {
            Timber.e(e.message)
        }
    }
}