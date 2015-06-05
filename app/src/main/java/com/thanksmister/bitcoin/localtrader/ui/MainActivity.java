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

package com.thanksmister.bitcoin.localtrader.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.Toast;

import com.google.zxing.android.IntentIntegrator;
import com.google.zxing.android.IntentResult;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.drawer.NavigationDrawerFragment;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchFragment;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

@BaseActivity.RequiresAuthentication
public class MainActivity extends BaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks
{
    private static final String BITCOIN_URI = "com.thanksmister.extra.BITCOIN_URI";
    private static final String DASHBOARD_FRAGMENT = "com.thanksmister.fragment.DASHBOARD_FRAGMENT";
    private static final String ABOUT_FRAGMENT = "com.thanksmister.fragment.ACCOUNT_FRAGMENT";
    private static final String SEND_RECEIVE_FRAGMENT = "com.thanksmister.fragment.SEND_RECEIVE_FRAGMENT";
    private static final String SEARCH_FRAGMENT = "com.thanksmister.fragment.SEARCH_FRAGMENT";
    private static final String WALLET_FRAGMENT = "com.thanksmister.fragment.WALLET_FRAGMENT";
    
    public static String EXTRA_CONTACT = "extra_contact";
    public static String EXTRA_TYPE = "extra_type";
    
    private static final int DRAWER_DASHBOARD = 0;
    private static final int DRAWER_SEARCH = 1;
    private static final int DRAWER_WALLET = 3;
    private static final int DRAWER_SEND = 2;
    private static final int DRAWER_ABOUT = 4;
    
    private static final int REQUEST_SCAN = 49374;
    
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;
    
    private CharSequence mTitle = "";
    private Toolbar toolbar;

    public static Intent createStartIntent(Context context, String bitcoinUri)
    {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(BITCOIN_URI, bitcoinUri);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
        
        String bitcoinUri = getIntent().getStringExtra(BITCOIN_URI);
        if(bitcoinUri != null) {
            handleBitcoinUri(bitcoinUri);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        dbManager.isLoggedIn()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Boolean>()
        {
            @Override
            public void call(Boolean isLoggedIn)
            {
                Timber.d("isLoggedIn " + isLoggedIn);

                if (isLoggedIn) {
                    navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
                    navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
                    SyncUtils.CreateSyncAccount(getApplicationContext());
                    SyncUtils.TriggerRefresh(getApplicationContext());
                }
            }
        });
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();

        if(((Object) this).getClass().isAnnotationPresent(RequiresAuthentication.class)) {
            dbManager.isLoggedIn()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Boolean>()
            {
                @Override
                public void call(Boolean isLoggedIn)
                {
                    if (!isLoggedIn) {
                        Intent intent = new Intent(MainActivity.this, PromoActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(final int position)
    {
        clearActionBar();

        Observable<Boolean> observable = dbManager.isLoggedIn();
        observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>()
                {
                    @Override
                    public void call(Boolean isLoggedIn)
                    {
                        Timber.d("isLoggedIn " + isLoggedIn);

                        if (isLoggedIn) {
                            if (position == DRAWER_WALLET) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, WalletFragment.newInstance(position), WALLET_FRAGMENT)
                                        .commit();
                            } else if (position == DRAWER_SEARCH) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, SearchFragment.newInstance(position), SEARCH_FRAGMENT)
                                        .commit();
                            } else if (position == DRAWER_SEND) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, RequestFragment.newInstance(position, RequestFragment.WalletTransactionType.SEND), SEND_RECEIVE_FRAGMENT)
                                        .commit();
                            } else if (position == DRAWER_DASHBOARD) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, DashboardFragment.newInstance(position), DASHBOARD_FRAGMENT)
                                        .commit();
                            } else if (position == DRAWER_ABOUT) {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, AboutFragment.newInstance(position), ABOUT_FRAGMENT)
                                        .commit();
                            }

                            onSectionAttached(position);
                        }
                    }
                });
    }

    private void startSendRequestFragment(String bitcoinAddress, String bitcoinAmount)
    {
        //if(getSupportFragmentManager().findFragmentByTag(SEND_RECEIVE_FRAGMENT) == null) {
            clearActionBar();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, RequestFragment.newInstance(DRAWER_SEND, bitcoinAddress, bitcoinAmount), SEND_RECEIVE_FRAGMENT)
                    .commit();
        //}
    }

    @Subscribe
    public void onNavigateEvent (NavigateEvent event)
    {
        if(event == NavigateEvent.SEND) {
            navigationDrawerFragment.selectItem(DRAWER_SEND);
        } else if (event == NavigateEvent.SEARCH) {
            navigationDrawerFragment.selectItem(DRAWER_SEARCH);
        } else if (event == NavigateEvent.WALLET) {
            navigationDrawerFragment.selectItem(DRAWER_WALLET);
        } else if (event == NavigateEvent.LOGOUT_CONFIRM) {
            logOutConfirmation();
        } else if (event == NavigateEvent.LOGOUT) {
            logOut();
        } else if (event == NavigateEvent.QRCODE) {
            launchScanner();
        }
    }

    public void onSectionAttached(int number)
    {
        switch (number) {
            case DRAWER_DASHBOARD:
                mTitle = "";
                break;
            case DRAWER_SEARCH:
                mTitle = getString(R.string.view_title_buy_sell);
                break;
            case DRAWER_SEND:
                mTitle = getString(R.string.view_title_request);
                break;
            case DRAWER_WALLET:
                mTitle = "";
                break;
            case DRAWER_ABOUT:
                mTitle = getString(R.string.app_name);
                break;
        }
    }
    
    public void clearActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("");
    }

    public void restoreActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (navigationDrawerFragment != null && !navigationDrawerFragment.isDrawerOpen()) {
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if(requestCode == REQUEST_SCAN) {
            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanningResult != null) {
                handleBitcoinUri(scanningResult.getContents());
            } else {
                Toast.makeText(this, getString(R.string.toast_scan_canceled), Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void handleBitcoinUri(String bitcoinUri)
    {
        String bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri);
        String bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri);

        if(bitcoinAddress == null) {
            Toast.makeText(this, getString(R.string.toast_scan_canceled), Toast.LENGTH_SHORT).show();
            return;
        } else if(!WalletUtils.validBitcoinAddress(bitcoinAddress)) {
            Toast.makeText(this, getString(R.string.toast_invalid_address), Toast.LENGTH_SHORT).show();
            return;
        }

        if(bitcoinAmount != null && !WalletUtils.validAmount(bitcoinAmount)) {
            Toast.makeText(this, getString(R.string.toast_invalid_btc_amount), Toast.LENGTH_SHORT).show();
            bitcoinAmount = null; // set it to null and show toast
        }

        startSendRequestFragment(bitcoinAddress, bitcoinAmount);
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        int type = extras.getInt(EXTRA_TYPE, 0);
        
        if(type == NotificationUtils.NOTIFICATION_TYPE_CONTACT || type == NotificationUtils.NOTIFICATION_TYPE_MESSAGE ) {
            
           String contactId = extras.getString(EXTRA_CONTACT);
            if(contactId != null) {
                Intent contactIntent = ContactActivity.createStartIntent(this, contactId, DashboardType.ACTIVE);
                startActivity(contactIntent);
            }
            
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            navigationDrawerFragment.selectItem(DRAWER_WALLET);
        }
    }
}
