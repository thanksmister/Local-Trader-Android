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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.zxing.android.IntentIntegrator;
import com.google.zxing.android.IntentResult;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchFragment;
import com.thanksmister.bitcoin.localtrader.ui.settings.SettingsActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action0;
import timber.log.Timber;

@BaseActivity.RequiresAuthentication
public class MainActivity extends BaseActivity implements View.OnClickListener
{
    private static final String BITCOIN_URI = "com.thanksmister.extra.BITCOIN_URI";
    private static final String DASHBOARD_FRAGMENT = "com.thanksmister.fragment.DASHBOARD_FRAGMENT";
    private static final String ABOUT_FRAGMENT = "com.thanksmister.fragment.ACCOUNT_FRAGMENT";
    private static final String RECEIVE_FRAGMENT = "com.thanksmister.fragment.RECEIVE_FRAGMENT";
    private static final String SEND_FRAGMENT = "com.thanksmister.fragment.SEND_FRAGMENT";
    private static final String SEARCH_FRAGMENT = "com.thanksmister.fragment.SEARCH_FRAGMENT";
    private static final String WALLET_FRAGMENT = "com.thanksmister.fragment.WALLET_FRAGMENT";
    
    public static String EXTRA_CONTACT = "extra_contact";
    public static String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static String EXTRA_NOTIFICATION_TYPE = "extra_notification_type";
    public static String EXTRA_FRAGMENT = "extra_fragment";

    public static final int DRAWER_DASHBOARD = 0;
    public static final int DRAWER_SEARCH = 1;
    public static final int DRAWER_SEND = 2;
    public static final int DRAWER_RECEIVE= 3;
    public static final int DRAWER_WALLET = 4;
    public static final int DRAWER_ABOUT = 5;
    
    private static final int REQUEST_SCAN = 49374;

    @InjectView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @InjectView(R.id.navigation)
    NavigationView navigationView;

    private Fragment fragment;
    private int position = DRAWER_DASHBOARD;
    private int lastMenuItemId = R.id.navigationItemDashboard;
    TextView userName;
    TextView feedbackScore;
    TextView tradeCount;
   
    public static Intent createStartIntent(Context context, String bitcoinUri)
    {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(BITCOIN_URI, bitcoinUri);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
        } catch (NoClassDefFoundError e) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_device_title),
                    getString(R.string.error_device_softare_description)), new Action0()
            {
                @Override
                public void call()
                {
                    finish();
                }
            });
            return;
        }

        ButterKnife.inject(this);

        if(savedInstanceState != null) {
            position = savedInstanceState.getInt(EXTRA_FRAGMENT);
        }
        
        final String bitcoinUri = getIntent().getStringExtra(BITCOIN_URI);

        setupNavigationView();
        
        boolean authenticated = AuthUtils.hasCredentials(sharedPreferences);
        if(authenticated) {
            if (bitcoinUri != null && validAddressOrAmount(bitcoinUri)) { // we have a uri request so override setting content
                handleBitcoinUri(bitcoinUri);
            } else {
                setContentFragment(position);
            }
            
            String userName = AuthUtils.getUsername(sharedPreferences);
            SyncUtils.CreateSyncAccount(MainActivity.this, userName);
            SyncUtils.TriggerRefresh(getApplicationContext(), userName);
            toast("Refreshing data...");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //No call for super(). Bug on API Level > 11.
        outState.putInt(EXTRA_FRAGMENT, position);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        
        if (((Object) this).getClass().isAnnotationPresent(RequiresAuthentication.class)) {
            boolean authenticated = AuthUtils.hasCredentials(sharedPreferences);
            if(!authenticated) {
                launchPromoScreen();
                return;
            } else if(AuthUtils.showUpgradedMessage(getApplicationContext(), sharedPreferences)) {
                String title = "What's new in " + AuthUtils.getCurrentVersionName(getApplicationContext(), sharedPreferences);
                showAlertDialogLinks(new AlertDialogEvent(title, getString(R.string.whats_new_message)));
                AuthUtils.setUpgradeVersion(getApplicationContext(), sharedPreferences);
            }
        }

        navigationView.getMenu().findItem(lastMenuItemId).setChecked(true);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(drawerLayout != null)
                    drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void launchPromoScreen()
    {
        Intent intent = new Intent(MainActivity.this, PromoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
  
    private void setupNavigationView()
    {
        final View headerView = navigationView.getHeaderView(0);
        userName = (TextView) headerView.findViewById(R.id.userName);
        tradeCount = (TextView) headerView.findViewById(R.id.userTradeCount);
        feedbackScore = (TextView) headerView.findViewById(R.id.userTradeFeedback);

        userName.setText(AuthUtils.getUsername(sharedPreferences));
        tradeCount.setText(AuthUtils.getUsername(sharedPreferences));
        feedbackScore.setText(AuthUtils.getTrades(sharedPreferences));
        
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                InputMethodManager inputMethodManager = (InputMethodManager)  MainActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), 0);
            }

            @Override
            public void onDrawerClosed(View drawerView)
            {
            }

            @Override
            public void onDrawerStateChanged(int newState)
            {
            }
        });
        
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem)
            {
                selectDrawerItem(menuItem);
                return true;
            }
        });
    }

    public void selectDrawerItem(MenuItem menuItem) 
    {
        switch(menuItem.getItemId()) {
           case R.id.navigationItemSearch:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_SEARCH);
                break;
            case R.id.navigationItemSend:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_SEND);
                break;
            case R.id.navigationItemReceive:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_RECEIVE);
                break;
            case R.id.navigationItemWallet:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_WALLET);
                break;
            case R.id.navigationItemAbout:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_ABOUT);
                break;
            case R.id.navigationItemSettings:
                Intent intent = SettingsActivity.createStartIntent(this);
                startActivity(intent);
                break;
            default:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_DASHBOARD);
        }

        // Highlight the selected item, update the title, and close the drawer
        menuItem.setChecked(true);
        drawerLayout.closeDrawers();
    }
    
    public void setContentFragment(int position)
    {
        this.position = position;

        try{
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.w("Error closing keyboard");
        }


        if(!isFinishing()) {
            if (position == DRAWER_WALLET) {
                fragment = WalletFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, WALLET_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_SEARCH) {
                fragment = SearchFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, SEARCH_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_SEND) {
                fragment = SendFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, SEND_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_RECEIVE) {
                fragment = RequestFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, RECEIVE_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_DASHBOARD) {
                fragment = DashboardFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, DASHBOARD_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_ABOUT) {
                fragment = AboutFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, ABOUT_FRAGMENT)
                        .commitAllowingStateLoss();
            }
        }
    }
    
    private void startSendFragment(String bitcoinAddress, String bitcoinAmount)
    {
        fragment = SendFragment.newInstance(bitcoinAddress, bitcoinAmount);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, SEND_FRAGMENT)
                .commitAllowingStateLoss();
        
        navigationView.getMenu().findItem(R.id.navigationItemSend).setChecked(true);
        position = DRAWER_SEND;
    }

    @Subscribe
    public void onRefreshEvent(RefreshEvent event)
    {
        switch (fragment.getTag()) {
            case DASHBOARD_FRAGMENT:
                ((DashboardFragment) fragment).onRefresh();
                break;
            case WALLET_FRAGMENT:
                ((WalletFragment) fragment).onRefresh();
                break;
            case SEARCH_FRAGMENT:
                ((SearchFragment) fragment).onRefresh();
                break;
            case SEND_FRAGMENT:
                ((SendFragment) fragment).onRefresh();
                break;
        }
    }
    
    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        if (event == NetworkEvent.DISCONNECTED) {
            boolean retry = (position == DRAWER_DASHBOARD || position == DRAWER_WALLET);
            snack(getString(R.string.error_no_internet), retry);
        }
    }

    @Subscribe
    public void onNavigateEvent (NavigateEvent event)
    {
        if(event == NavigateEvent.DASHBOARD) {
            setContentFragment(DRAWER_DASHBOARD);
            navigationView.getMenu().findItem(R.id.navigationItemDashboard).setChecked(true);
        } else if(event == NavigateEvent.SEND) {
            setContentFragment(DRAWER_SEND);
            navigationView.getMenu().findItem(R.id.navigationItemSend).setChecked(true);
        } else if (event == NavigateEvent.SEARCH) {
            setContentFragment(DRAWER_SEARCH);
            navigationView.getMenu().findItem(R.id.navigationItemSearch).setChecked(true);
        } else if (event == NavigateEvent.WALLET) {
            setContentFragment(DRAWER_WALLET);
            navigationView.getMenu().findItem(R.id.navigationItemWallet).setChecked(true);
        } else if (event == NavigateEvent.LOGOUT_CONFIRM) {
            logOutConfirmation();
        } else if (event == NavigateEvent.LOGOUT) {
            logOut();
        } else if (event == NavigateEvent.QRCODE) {
            launchScanner();
        }
    }
    
    public void restoreActionBar()
    {
        /*ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle(mTitle);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (drawerLayout != null && !drawerLayout.isDrawerOpen(GravityCompat.START)) {
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
                toast(getString(R.string.toast_scan_canceled));
            }
        }
    }
    
    protected boolean validAddressOrAmount(String bitcoinUri)
    {
        String bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri);
        String bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri);
        
        if(bitcoinAddress == null) {
            return false;
        } else if(!WalletUtils.validBitcoinAddress(bitcoinAddress)) {
            toast(getString(R.string.toast_invalid_address));
            return false;
        }

        if(bitcoinAmount != null && !WalletUtils.validAmount(bitcoinAmount)) {
            toast(getString(R.string.toast_invalid_btc_amount));
            return false;
        }
        
        return true;
    }

    protected void handleBitcoinUri(String bitcoinUri)
    {
        String bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri);
        String bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri);
        startSendFragment(bitcoinAddress, bitcoinAmount);
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        
        if(intent == null)
            return;
        
        Bundle extras = intent.getExtras();
        int type = extras.getInt(EXTRA_NOTIFICATION_TYPE, 0);
        
        if(type == NotificationUtils.NOTIFICATION_TYPE_CONTACT) {
            String id = extras.getString(EXTRA_NOTIFICATION_ID);
            if(id != null) {
                Intent launchIntent = ContactActivity.createStartIntent(this, id);
                startActivity(launchIntent);
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT) {
            String id = extras.getString(EXTRA_NOTIFICATION_ID);
            if(id != null) {
                Intent launchIntent = AdvertisementActivity.createStartIntent(this, id);
                startActivity(launchIntent);
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            setContentFragment(DRAWER_WALLET);
            navigationView.getMenu().findItem(R.id.navigationItemWallet).setChecked(true);
        }
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.fab) {
            launchScanner();
        }
    }
}