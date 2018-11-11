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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.activities.PromoActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.ScanQrCodeActivity
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.HashMap

import javax.inject.Inject

import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber

/**
 * Base activity which sets up a per-activity object graph and performs injection.
 */
abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogUtils: DialogUtils

    private var progressDialog: AlertDialog? = null
    private var alertDialog: AlertDialog? = null
    private var snackBar: Snackbar? = null
    private var syncMap = HashMap<String, Boolean>() // init sync map

    /**
     * Checks if any active syncs are going one
     */
    private val isSyncing: Boolean
        get() {
            Timber.d("isSyncing: " + syncMap.containsValue(true))
            return syncMap.containsValue(true)
        }

    private val connReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetworkInfo = connectivityManager.activeNetworkInfo
            if (currentNetworkInfo != null && currentNetworkInfo.isConnected) {
                if (snackBar != null && snackBar!!.isShown) {
                    snackBar!!.dismiss()
                    snackBar = null
                }
            } else {
                handleNetworkDisconnect()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(dialogUtils)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
        if (snackBar != null && snackBar!!.isShownOrQueued) {
            snackBar!!.dismiss()
            snackBar = null
        }
    }

    public override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(connReceiver)
        } catch (e: IllegalArgumentException) {
            Timber.e(e.message)
        }

    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(connReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    /**
     * Keep a map of all syncing calls to update sync status and
     * broadcast when no more syncs running
     */
    open fun updateSyncMap(key: String, value: Boolean) {
        Timber.d("updateSyncMap: $key value: $value")
        syncMap[key] = value
        if (!isSyncing) {
            resetSyncing()
        }
    }

    /**
     * Resets the syncing map
     */
    private fun resetSyncing() {
        syncMap = HashMap()
    }

    /**
     * Called when network is disconnected
     */
    protected open fun handleNetworkDisconnect() {
        // override to in views to handle network disconnect
    }

    protected open fun handleRefresh() {
        // override to in views to handle refresh
    }

    open fun launchScanner() {
        startActivity(ScanQrCodeActivity.createStartIntent(this@BaseActivity))
        //startActivity(new Intent(BaseActivity.this, BarcodeCaptureActivity.class));
    }

    @JvmOverloads
    fun showProgressDialog(message: String, cancelable: Boolean = false) {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
            return
        }
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var dialogView: View? = null
        dialogView = inflater.inflate(R.layout.dialog_progress, null, false)
        val progressDialogMessage: TextView
        if (dialogView != null) {
            progressDialogMessage = dialogView.findViewById(R.id.progressDialogMessage)
            progressDialogMessage.text = message
        }
        progressDialog = AlertDialog.Builder(this@BaseActivity, R.style.CustomAlertDialog)
                .setCancelable(cancelable)
                .setView(dialogView)
                .show()
    }

    fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    fun showAlertDialog(message: String) {
        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        alertDialog = AlertDialog.Builder(this@BaseActivity, R.style.CustomAlertDialog)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialog(title:String, message: String) {
        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        alertDialog = AlertDialog.Builder(this@BaseActivity, R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    fun showAlertDialogLinks(message: String) {
        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        val view = View.inflate(this@BaseActivity, R.layout.dialog_about, null)
        val textView = view.findViewById<TextView>(R.id.message)
        textView.text = Html.fromHtml(message)
        textView.movementMethod = LinkMovementMethod.getInstance()
        alertDialog = AlertDialog.Builder(this@BaseActivity, R.style.CustomAlertDialog)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    /*public void showAlertDialog(@NonNull AlertDialogEvent event, final Action0 actionToTake) {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.CustomAlertDialog)
                .setTitle(event.title)
                .setMessage(Html.fromHtml(event.message))
                .setCancelable(event.cancelable)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        actionToTake.call();
                    }
                })
                .show();
    }

    public void showAlertDialog(String message, final Action0 actionToTake) {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.CustomAlertDialog)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        actionToTake.call();
                    }
                })
                .show();
    }

    public void showAlertDialog(String message, final Action0 actionToTake, final Action0 cancelActionToTake) {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage(Html.fromHtml(message))
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        cancelActionToTake.call();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        actionToTake.call();
                    }
                })
                .show();
    }*/
    /*

    public void showAlertDialog(@NonNull AlertDialogEvent event, final Action0 actionToTake, final Action0 cancelActionToTake) {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.CustomAlertDialog)
                .setTitle(event.title)
                .setCancelable(false)
                .setMessage(Html.fromHtml(event.message))
                .setCancelable(event.cancelable)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        cancelActionToTake.call();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        actionToTake.call();
                    }
                })
                .show();
    }
*/

    fun logOutConfirmation() {
        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        alertDialog = AlertDialog.Builder(this@BaseActivity, R.style.CustomAlertDialog)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok) { dialogInterface, i -> logOut() }
                .show()
    }

    fun logOut() {
        showProgressDialog("Logging out...")
        onLoggedOut()
    }

    private fun onLoggedOut() {

        val settings = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val prefEditor = settings.edit()
        prefEditor.clear()
        prefEditor.apply()

        SyncUtils.cancelSync(this)

        hideProgressDialog()

        val intent = PromoActivity.createStartIntent(this@BaseActivity)
        startActivity(intent)
        finish()
    }


    fun toast(messageId: Int) {
        toast(getString(messageId))
    }

    fun toast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM, 0, 180)
        toast.show()
    }

    fun reportError(throwable: Throwable) {
        /*if (throwable instanceof RetrofitError) {
            if (DataServiceUtils.isHttp400Error(throwable)) {
                return;
            } else if (DataServiceUtils.isHttp500Error(throwable)) {
                return;
            }
        }
        if (throwable instanceof SSLHandshakeException) {
            Timber.e(throwable.getMessage());
            return;
        }
        if (throwable instanceof UnknownHostException) {
            Timber.e(throwable.getMessage());
            toast(getString(R.string.error_no_internet));
            return;
        }
        if (throwable instanceof NetworkOnMainThreadException) {
            NetworkOnMainThreadException exception = (NetworkOnMainThreadException) throwable;
            Timber.e(exception.getMessage());
        } else if (throwable != null) {
            Timber.e(throwable.getMessage());
            throwable.printStackTrace();
        }*/
    }

    @JvmOverloads
    fun handleError(throwable: Throwable, retry: Boolean = false) {

        /* // Handle NetworkConnectionException
        if (throwable instanceof NetworkConnectionException) {
            NetworkConnectionException networkConnectionException = (NetworkConnectionException) throwable;
            snack(networkConnectionException.getMessage(), retry);
            return;
        }

        // Handle NetworkException
        if(throwable instanceof NetworkException) {
            NetworkException networkException = (NetworkException) throwable;
            if (networkException.getStatus() == 403) {
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
            }
        }

        // Handle Throwable exception
        if (DataServiceUtils.isConnectionError(throwable)) {
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

    companion object {

        fun hideSoftKeyboard(activity: Activity) {
            val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            if (activity.currentFocus != null && activity.currentFocus!!.windowToken != null) {
                inputMethodManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
            }
        }
    }
}