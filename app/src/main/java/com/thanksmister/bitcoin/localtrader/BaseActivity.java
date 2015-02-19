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
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.zxing.android.IntentIntegrator;
import com.google.zxing.android.IntentResult;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.promo.PromoActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;
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
    DataService service;
    
    private ObjectGraph activityGraph;
    private MaterialDialog alertDialog;
    private MaterialDialog progressDialog;

    @Override 
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        activityGraph = ((BaseApplication) getApplication()).createScopedGraph(getModules().toArray());
        activityGraph.inject(this);
    }

    @Override 
    protected void onDestroy() 
    {
        super.onDestroy();
        activityGraph = null;
    }

    public abstract void onRefreshStop();

    public abstract void onError(String message);

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

    public void inject(Object object) 
    {
        activityGraph.inject(object);
    }
    
    protected abstract List<Object> getModules();

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
            return;
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
        ConfirmationDialogEvent event = new ConfirmationDialogEvent("Log Out",
                "Do you wish to logout?",
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
        service.logOut();
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

    protected void showToastMessage(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
