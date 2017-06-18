/*
 * Copyright (c) 2015 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.android.IntentIntegrator;
import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.data.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.PromoActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

import butterknife.ButterKnife;
import retrofit.RetrofitError;
import rx.functions.Action0;
import timber.log.Timber;

/**
 * Base activity which sets up a per-activity object graph and performs injection.
 */
public abstract class BaseActivity extends RxAppCompatActivity
{
    /**
     * This activity requires authentication
     */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface RequiresAuthentication
    {
    }

    @Inject
    protected Bus bus;

    @Inject
    protected DbManager dbManager;

    @Inject
    protected DataService dataService;

    @Inject
    protected SharedPreferences sharedPreferences;

    AlertDialog progressDialog;
    AlertDialog alertDialog;
    Snackbar snackBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        // force locale US for conversions
        setLocale("en", "US");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        ButterKnife.reset(this);
        
        if(alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
        }

        if(progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if(snackBar != null && snackBar.isShownOrQueued()) {
            snackBar.dismiss();
            snackBar = null;
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        bus.unregister(BaseActivity.this);

        try {
            unregisterReceiver(connReceiver);
        } catch (IllegalArgumentException e) {
            Timber.e(e.getMessage());
        }
    }

    @Override
    public void onResume()
    {

        super.onResume();
        bus.register(BaseActivity.this);
        registerReceiver(connReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void launchScanner()
    {
        IntentIntegrator scanIntegrator = new IntentIntegrator(BaseActivity.this);
        scanIntegrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    public void showProgressDialog(ProgressDialogEvent event)
    {
        showProgressDialog(event, false);
    }

    public void showProgressDialog(ProgressDialogEvent event, boolean modal)
    {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
            return;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_progress, null, false);
        TextView progressDialogMessage = (TextView) dialogView.findViewById(R.id.progressDialogMessage);
        progressDialogMessage.setText(event.message);

        progressDialog = new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setCancelable(modal)
                .setView(dialogView)
                .show();
    }

    public void hideProgressDialog()
    {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public void showAlertDialog(AlertDialogEvent event)
    {
        new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setTitle(event.title)
                .setMessage(Html.fromHtml(event.message))
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(event.cancelable)
                .show();
    }
    
    public void showAlertDialogLinks(AlertDialogEvent event)
    {
        View view = View.inflate(BaseActivity.this, R.layout.dialog_about, null);
        TextView textView = (TextView) view.findViewById(R.id.message);
        textView.setText(Html.fromHtml(event.message));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setTitle(event.title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(event.cancelable)
                .show();
    }

    public void showAlertDialog(@NonNull AlertDialogEvent event, final Action0 actionToTake)
    {
        new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setTitle(event.title)
                .setMessage(Html.fromHtml(event.message))
                .setCancelable(event.cancelable)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        actionToTake.call();
                    }
                })
                .show();
    }

    public void showAlertDialog(@NonNull AlertDialogEvent event, final Action0 actionToTake, final Action0 cancelActionToTake)
    {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
            return;
        }

        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setTitle(event.title)
                .setCancelable(false)
                .setMessage(Html.fromHtml(event.message))
                .setCancelable(event.cancelable)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        cancelActionToTake.call();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        actionToTake.call();
                    }
                })
                .show();
    }

    public void logOutConfirmation()
    {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
            return;
        }
        
        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        logOut();
                    }
                })
                .show();
    }

    public void logOut()
    {
        showProgressDialog(new ProgressDialogEvent("Logging out..."));
        onLoggedOut();
    }

    private void onLoggedOut()
    {
        dataService.logout();
        dbManager.clearDbManager();

        // clear preferences
        AuthUtils.resetCredentials(sharedPreferences);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.clear();
        prefEditor.apply();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        String userName = AuthUtils.getUsername(sharedPreferences);
        SyncUtils.CancelSync(userName);
        SyncUtils.ClearSyncAccount(getApplicationContext(), userName);

        hideProgressDialog();

        Intent intent = PromoActivity.createStartIntent(BaseActivity.this);
        startActivity(intent);
        finish();
    }

    public void showConfirmationDialog(final ConfirmationDialogEvent event)
    {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
            return;
        }

        alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.DialogTheme)
                .setTitle(event.title)
                .setMessage(Html.fromHtml(event.message))
                .setNegativeButton(event.negative, null)
                .setPositiveButton(event.positive, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        event.action.call();
                    }
                })
                .show();
    }
    
    private BroadcastReceiver connReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
            NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (currentNetworkInfo != null && currentNetworkInfo.isConnected()) {
                if(snackBar != null && snackBar.isShown()) {
                    snackBar.dismiss();
                    snackBar = null;
                }
            } else {
                bus.post(NetworkEvent.DISCONNECTED);
            }
        }
    };

    protected void toast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void reportError(Throwable throwable)
    {
        if(throwable instanceof RetrofitError) {
            if(DataServiceUtils.isHttp400Error(throwable)) {
                return;
            } else if (DataServiceUtils.isHttp500Error(throwable)) {
                return;
            }
        }
        
        if(throwable instanceof SSLHandshakeException) {
            Timber.e(throwable.getMessage());
            return;
        }
        
        if(throwable instanceof UnknownHostException) {
            //Timber.e(throwable.getMessage());
            toast(getString(R.string.error_no_internet));
            return;
        }
        
        if (throwable != null && throwable instanceof NetworkOnMainThreadException) {
            NetworkOnMainThreadException exception = (NetworkOnMainThreadException) throwable;
            Timber.e(exception.getMessage());
        } else if (throwable != null) {
            Timber.e(throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    protected void handleError(Throwable throwable)
    {
        handleError(throwable, false);
    }

    protected void handleError(Throwable throwable, boolean retry)
    {
        if (throwable instanceof NetworkConnectionException) {
            snack(getString(R.string.error_no_internet), retry);
            return;
        }
        
        if (DataServiceUtils.isConnectionError(throwable)) {
            Timber.i("Connection Error");
            snack(getString(R.string.error_service_unreachable_error), retry);
        } else if (DataServiceUtils.isTimeoutError(throwable)) {
            Timber.i("Timeout Error");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if (DataServiceUtils.isNetworkError(throwable)) {
            Timber.i("Data Error: " + "Code 503");
            snack(getString(R.string.error_no_internet), retry);
        } else if (DataServiceUtils.isHttp504Error(throwable)) {
            Timber.i("Data Error: " + "Code 504");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if (DataServiceUtils.isHttp42Error(throwable)) {
            Timber.i("Data Error: " + "Code 42");
            snack(getString(R.string.error_generic_error), retry);
        } else if (DataServiceUtils.isHttp41Error(throwable)) {
            Timber.i("Data Error: " + "Code 41");
            snack(getString(R.string.error_authentication), retry);
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
            Timber.e("Data Error Message: " + error.getMessage());
            snack(error.getMessage(), retry);
        } else if (throwable != null && throwable.getMessage() != null) {
            Timber.i("Data Error: " + throwable.getMessage());
            snack(throwable.getMessage(), retry);
        } else {
            snack(R.string.error_unknown_error, retry);
        }
    }

    protected void snack(int message, boolean retry)
    {
        snack(getString(message), retry);
    }

    protected void snackError(String message)
    {
        if(snackBar != null && snackBar.isShownOrQueued()) {
            snackBar.dismiss();
            snackBar = null;
            return;
        }

        try {
            View view = findViewById(R.id.coordinatorLayout);
            snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            TextView textView = (TextView) snackBar.getView().findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(getResources().getColor(R.color.white));
            snackBar.show();
        } catch (NullPointerException e) {
            // nothing
        }
    }
    
    protected void snack(String message, boolean retry)
    {
        if(snackBar != null && snackBar.isShownOrQueued()) {
            snackBar.dismiss();
            snackBar = null;
            return;
        }
        
        try {
            View view = findViewById(R.id.coordinatorLayout);
            
            if (retry) {
                snackBar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                bus.post(RefreshEvent.RETRY);
                            }
                        });
                TextView textView = (TextView) snackBar.getView().findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(getResources().getColor(R.color.white));
                snackBar.show();
            } else {
                snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
                TextView textView = (TextView) snackBar.getView().findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(getResources().getColor(R.color.white));
                snackBar.show();
            }
            
        } catch (NullPointerException e) {
            // nothing
        }
    }

    protected void toast(int messageId)
    {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }

    protected void toast(int messageId, boolean showLong)
    {
        if (showLong) {
            Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        } else {
            toast(messageId);
        }
    }

    protected void setLocale(String language, String country)
    {
        // create new local
        Locale locale = new Locale(language, country);

        // here we update locale for date formatters
        Locale.setDefault(locale);

        // here we update locale for app resources

        Resources res = getResources();

        Configuration config = res.getConfiguration();
        config.locale = locale;

        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
