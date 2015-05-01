package com.thanksmister.bitcoin.localtrader;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.zxing.android.IntentIntegrator;
import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.PromoActivity;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import butterknife.ButterKnife;
import rx.functions.Action0;
import timber.log.Timber;

/** Base activity which sets up a per-activity object graph and performs injection. */
public abstract class BaseActivity extends ActionBarActivity 
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

    private MaterialDialog progressDialog;

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

        getApplicationContext().unregisterReceiver(connReceiver);
    }

    @Override
    public void onResume() {

        super.onResume();
        
        bus.register(this);

        getApplicationContext().registerReceiver(connReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
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
        View dialogView = inflater.inflate(R.layout.dialog_progress, null);
        TextView progressDialogMessage = (TextView) dialogView.findViewById(R.id.progressDialogMessage);
        progressDialogMessage.setText(event.message);

        progressDialog = new MaterialDialog.Builder(this)
                .customView(dialogView)
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
        new MaterialDialog.Builder(this)
                .title(event.title)
                .content(Html.fromHtml(event.message))
                .neutralText(getString(android.R.string.ok))
                .show();
    }

    public void logOutConfirmation()
    {
        ConfirmationDialogEvent event = new ConfirmationDialogEvent(getString(R.string.dialog_logout_title),
                getString(R.string.dialog_logout_message),
                getString(R.string.button_ok),
                getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                logOut();
            }
        });

        showConfirmationDialog(event);
    }

    public void logOut()
    {
        dbManager.clearDbManager();
        dataService.reset();
        
        Intent intent = PromoActivity.createStartIntent(BaseActivity.this);
        startActivity(intent);
        finish();
    }

    public void showConfirmationDialog(ConfirmationDialogEvent event)
    {
        new MaterialDialog.Builder(this)
                .callback(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        event.action.call(); // call function 
                    }
                })
                .title(event.title)
                .content(event.message)
                .positiveText(event.positive)
                .negativeText(event.negative)
                .show();
    }

    // TODO replace with RxAndroid
    private BroadcastReceiver connReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
            NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(currentNetworkInfo != null && currentNetworkInfo.isConnected()) {
                bus.post(NetworkEvent.CONNECTED);
            } else {
                bus.post(NetworkEvent.DISCONNECTED);
            }
        }
    };

    protected void toast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void handleError(Throwable throwable)
    {
        Timber.e(throwable.getLocalizedMessage());

        if(DataServiceUtils.isHttp403Error(throwable)) {
            toast(R.string.error_authentication);
            logOut();
        } else if(DataServiceUtils.isHttp401Error(throwable)) {
            toast(R.string.error_no_internet);
        } else if(DataServiceUtils.isHttp400Error(throwable)) {
            toast(R.string.error_service_error);
        } else if(DataServiceUtils.isHttp500Error(throwable)) {
            toast(R.string.error_service_error);
        } else {
            toast(R.string.error_generic_error);
        }
    }

    protected void toast(int messageId)
    {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
    
}
