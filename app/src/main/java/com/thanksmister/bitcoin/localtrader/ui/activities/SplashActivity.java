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

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter;
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;

import butterknife.ButterKnife;
import rx.functions.Action0;
import timber.log.Timber;

@BaseActivity.RequiresAuthentication
public class SplashActivity extends BaseActivity {

    private static IntentFilter syncIntentFilter = new IntentFilter(SyncAdapter.ACTION_SYNC);

    public static Intent createStartIntent(Context context) {
        return new Intent(context, SplashActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_splash);

        ButterKnife.bind(this);

        if (!AuthUtils.hasCredentials(preference, sharedPreferences)) {
            Intent intent = new Intent(this, PromoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (AuthUtils.isFirstTime(preference)) {
            AuthUtils.setForceUpdate(preference, false);
            SyncUtils.requestSyncNow(this);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(syncBroadcastReceiver, syncIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(syncBroadcastReceiver);
    }

    @Override
    protected void handleNetworkDisconnect() {
        showAlertDialog(new AlertDialogEvent(getString(R.string.error_generic_error), getString(R.string.error_no_internet)));
    }

    protected void handleStartSync(String syncActionType, String extraErrorMessage, int extraErrorCode) {
        Timber.d("handleSyncEvent: " + syncActionType);
        switch (syncActionType) {
            case SyncAdapter.ACTION_TYPE_START:
                break;
            case SyncAdapter.ACTION_TYPE_COMPLETE:
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            case SyncAdapter.ACTION_TYPE_CANCELED:
                break;
            case SyncAdapter.ACTION_TYPE_ERROR:
                Timber.e("Sync error: " + extraErrorMessage + "code: " + extraErrorCode);
                showAlertDialog(new AlertDialogEvent(getString(R.string.error_generic_error), getString(R.string.error_sync)), new Action0() {
                    @Override
                    public void call() {
                        finish();
                    }
                });
                break;
        }
    }

    private BroadcastReceiver syncBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String syncActionType = intent.getStringExtra(SyncAdapter.EXTRA_ACTION_TYPE);
            assert syncActionType != null; // this should never be null

            String extraErrorMessage = "";
            int extraErrorCode = SyncAdapter.SYNC_ERROR_CODE;

            if (intent.hasExtra(SyncAdapter.EXTRA_ERROR_MESSAGE)) {
                extraErrorMessage = intent.getStringExtra(SyncAdapter.EXTRA_ERROR_MESSAGE);
            }
            if (intent.hasExtra(SyncAdapter.EXTRA_ERROR_CODE)) {
                extraErrorCode = intent.getIntExtra(SyncAdapter.EXTRA_ERROR_CODE, SyncAdapter.SYNC_ERROR_CODE);
            }

            handleStartSync(syncActionType, extraErrorMessage, extraErrorCode);
        }
    };
}