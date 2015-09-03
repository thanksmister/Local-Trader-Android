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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.android.IntentIntegrator;
import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.PromoActivity;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import butterknife.ButterKnife;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

/** Base activity which sets up a per-activity object graph and performs injection. */
public abstract class BaseActivity extends AppCompatActivity 
{
    /** This activity requires authentication */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface RequiresAuthentication { }

    @Inject
    Bus bus;

    @Inject
    DbManager dbManager;
    
    @Inject
    DataService dataService;

    AlertDialog progressDialog;
    
    Subscription subscription = Subscriptions.empty();

    @Override 
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        Injector.inject(this);
    }

    @Override 
    protected void onDestroy() 
    {
        super.onDestroy();

        ButterKnife.reset(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        bus.unregister(this);

        unregisterReceiver(connReceiver);

        subscription.unsubscribe();
    }

    @Override
    public void onResume() {

        super.onResume();
        
        bus.register(this);

        registerReceiver(connReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }
    
    public void launchScanner()
    {
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    public void showProgressDialog(ProgressDialogEvent event)
    {
        if(progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
            return;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_progress, null, false);
        TextView progressDialogMessage = (TextView) dialogView.findViewById(R.id.progressDialogMessage);
        progressDialogMessage.setText(event.message);

        progressDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .show();
    }

    public void hideProgressDialog()
    {
        if(progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public void showAlertDialog(AlertDialogEvent event)
    {
         new AlertDialog.Builder(this)
                 .setTitle(event.title)
                 .setMessage(Html.fromHtml(event.message))
                 .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public void logOutConfirmation()
    {
        new AlertDialog.Builder(this)
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
        subscription = dataService.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>()
                {
                    @Override
                    public void call(JSONObject jsonObject)
                    {
                        Timber.d("Logged out: " + jsonObject.toString());
                        onLoggedOut();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                        onLoggedOut();
                    }
                });
    }
    
    private void  onLoggedOut()
    {
        dbManager.clearDbManager();
        
        // clear preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();
        
        Intent intent = PromoActivity.createStartIntent(BaseActivity.this);
        startActivity(intent);
        finish();
    }

    public void showConfirmationDialog(final ConfirmationDialogEvent event)
    {
        new AlertDialog.Builder(this)
                .setTitle(event.title)
                .setMessage(event.message)
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

    // TODO replace with RxAndroid
    private BroadcastReceiver connReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
            NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(currentNetworkInfo != null && currentNetworkInfo.isConnected()) {
                //bus.post(NetworkEvent.CONNECTED);
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
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    protected void handleError(Throwable throwable) 
    {
        handleError(throwable, false);
    }
    
    protected void handleError(Throwable throwable, boolean retry)
    {
        Timber.d("handleError");
        
        if(DataServiceUtils.isConnectionError(throwable)) {
            Timber.e("Connection Error");
            snack(getString(R.string.error_service_unreachable_error), retry);
        } else if(DataServiceUtils.isTimeoutError(throwable)) {
            Timber.e("Timeout Error");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if(DataServiceUtils.isNetworkError(throwable)) {
            Timber.e("Data Error: " + "Code 503");
            snack(getString(R.string.error_no_internet), retry);
        } else if(DataServiceUtils.isHttp502Error(throwable)) {
            Timber.e("Data Error: " + "Code 502");
            snack(getString(R.string.error_service_error), retry);
        } else if(DataServiceUtils.isConnectionError(throwable)) {
            Timber.e("Connection Error: " + "Code ???");
            snack(getString(R.string.error_service_timeout_error), retry);
        } else if(DataServiceUtils.isHttp403Error(throwable)) {
            Timber.e("Data Error: " + "Code 403");
            toast(getString(R.string.error_authentication));
            logOut();
        } else if(DataServiceUtils.isHttp401Error(throwable)) {
            Timber.e("Data Error: " + "Code 401");
            snack(getString(R.string.error_no_internet), retry);
        } else if(DataServiceUtils.isHttp500Error(throwable)) {
            Timber.e("Data Error: " + "Code 500");
            snack(getString(R.string.error_service_error), retry);
        } else if(DataServiceUtils.isHttp404Error(throwable)) {
            Timber.e("Data Error: " + "Code 404");
            snack(getString(R.string.error_service_error), retry);
        } else if(DataServiceUtils.isHttp400GrantError(throwable)) {
            Timber.e("Data Error: " + "Code 400 Grant Invalid");
            toast(getString(R.string.error_authentication));
            logOut();
        } else if(DataServiceUtils.isHttp400Error(throwable)) {
            snack(getString(R.string.error_service_error), retry);
        } else if(throwable != null && throwable.getLocalizedMessage() != null) {
            Timber.e("Data Error: " + throwable.getLocalizedMessage());
            snack(throwable.getLocalizedMessage(), retry);
        } else {
            snack(R.string.error_unknown_error, retry);
        }
        
        reportError(throwable);
    }

    protected void snack(int message, boolean retry)
    {
        snack(getString(message), retry);
    }

    protected void snackError(String message)
    {
        try {
            View view = findViewById(R.id.coordinatorLayout);
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Close", new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                //
                            }
                        })
                        .show();
            
        } catch (NullPointerException e) {
            // nothing
        }
    }
    
    // TODO add action to this
    protected void snack(String message, boolean retry)
    {
         try { 
                View view = findViewById(R.id.coordinatorLayout);
                if(retry){
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                            .setAction("Retry", new View.OnClickListener() {
                                @Override
                                public void onClick(View view)
                                {
                                    bus.post(RefreshEvent.RETRY);
                                }
                            })
                            .show();
                } else {
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                }
            } catch (NullPointerException e) {
                // nothing
            }
    }

    protected void toast(int messageId)
    {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}
