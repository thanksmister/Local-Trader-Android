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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.zxing.android.IntentIntegrator;
import com.google.zxing.android.IntentResult;
import com.kobakei.ratethisapp.RateThisApp;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeRateItem;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.network.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.network.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter;
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.fragments.AboutFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.DashboardFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.RequestFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.SearchFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.SendFragment;
import com.thanksmister.bitcoin.localtrader.ui.fragments.WalletFragment;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;
import com.trello.rxlifecycle.ActivityEvent;

import java.io.InterruptedIOException;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

@BaseActivity.RequiresAuthentication
public class MainActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static IntentFilter syncIntentFilter = new IntentFilter(SyncAdapter.ACTION_SYNC);

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
    public static final int DRAWER_RECEIVE = 3;
    public static final int DRAWER_WALLET = 4;
    public static final int DRAWER_ABOUT = 5;

    private static final int REQUEST_SCAN = 49374;

    @Inject
    ExchangeService exchangeService;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView(R.id.navigation)
    NavigationView navigationView;

    @BindView(R.id.bitcoinTitle)
    TextView bitcoinTitle;

    @BindView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @BindView(R.id.bitcoinValue)
    TextView bitcoinValue;

    @BindView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private Fragment fragment;
    private int position = DRAWER_DASHBOARD;
    private int lastMenuItemId = R.id.navigationItemDashboard;
    TextView userName;
    TextView feedbackScore;
    TextView tradeCount;

    public static Intent createStartIntent(Context context, String bitcoinUri) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(BITCOIN_URI, bitcoinUri);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
        } catch (NoClassDefFoundError e) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_device_title), getString(R.string.error_device_softare_description)), new Action0() {
                @Override
                public void call() {
                    finish();
                }
            });
            return;
        }

        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            position = savedInstanceState.getInt(EXTRA_FRAGMENT);
        }

        setupNavigationView();

        final String bitcoinUri = getIntent().getStringExtra(BITCOIN_URI);
        boolean authenticated = AuthUtils.hasCredentials(preference, sharedPreferences);

        if (authenticated) {
            if (bitcoinUri != null) {
                final String bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri);
                final String bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri);
                startSendFragment(bitcoinAddress, bitcoinAmount);
            } else {
                setContentFragment(position);
            }
        }

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));
        swipeLayout.setProgressViewOffset(false, 48, 186);
        swipeLayout.setDistanceToTriggerSync(250);

        if (!AuthUtils.isFirstTime(preference)) {
            toast(R.string.toast_refreshing_data);
            //AuthUtils.setForceUpdate(preference, false);
            SyncUtils.requestSyncNow(MainActivity.this);
        }

        // Application rating dialog
        // Set custom criteria (optional)
        RateThisApp.Config config = new RateThisApp.Config(7, 10);
        RateThisApp.init(config);

        // Monitor launch times and interval from installation
        RateThisApp.onCreate(this);

        // If the condition is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this, R.style.DialogTheme);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
        outState.putInt(EXTRA_FRAGMENT, position);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (((Object) this).getClass().isAnnotationPresent(RequiresAuthentication.class)) {
            boolean authenticated = AuthUtils.hasCredentials(preference, sharedPreferences);
            if (!authenticated) {
                launchPromoScreen();
                return;
            } else if (AuthUtils.showUpgradedMessage(getApplicationContext(), preference)) {
                //String title = getString(R.string.text_whats_new) + AuthUtils.getCurrentVersionName(getApplicationContext());
                //showAlertDialogLinks(new AlertDialogEvent(title, getString(R.string.whats_new_message)));
                AuthUtils.setUpgradeVersion(getApplicationContext(), preference);
            }
        }

        subscribeData();
        updateData();
        navigationView.getMenu().findItem(lastMenuItemId).setChecked(true);
        registerReceiver(syncBroadcastReceiver, syncIntentFilter);
    }

    @Override
    protected void handleNetworkDisconnect() {
        boolean retry = (position == DRAWER_DASHBOARD || position == DRAWER_WALLET);
        snack(getString(R.string.error_no_internet), retry);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(syncBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Timber.e(e.getMessage());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (drawerLayout != null)
                    drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        //AuthUtils.setForceUpdate(preference, true);
        SyncUtils.requestSyncNow(MainActivity.this);
        //handleRefresh();
        updateData();
    }

    private void onRefreshStart() {
        if (swipeLayout != null) {
            swipeLayout.setRefreshing(true);
        }
    }

    private void onRefreshStop() {
        if (swipeLayout != null) {
            swipeLayout.setRefreshing(false);
        }
    }

    @Override
    public void launchScanner() {
        onRefreshStop();
        super.launchScanner();
    }

    private void updateData() {
        exchangeService.getSpotPrice()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Update Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<ExchangeRate>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ExchangeRate>() {
                    @Override
                    public void call(final ExchangeRate exchange) {
                        if (exchange != null) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setHeaderItem(exchange.getRate(), exchange.getCurrency(), exchange.getDisplay_name());
                                }
                            });
                            dbManager.updateExchange(exchange);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if(throwable instanceof NetworkConnectionException) {
                            onRefreshStop();
                        } else {
                            snackError(getString(R.string.error_update_exchange_rate));
                        }

                    }
                });
    }

    private void subscribeData() {
        Timber.d("subscribeData");
        dbManager.exchangeQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<ExchangeRateItem>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ExchangeRateItem>() {
                    @Override
                    public void call(final ExchangeRateItem exchange) {
                        if (exchange != null) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setHeaderItem(exchange.rate(), exchange.currency(), exchange.exchange());
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        reportError(throwable);
                    }
                });
    }

    private void launchPromoScreen() {
        Intent intent = new Intent(MainActivity.this, PromoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupNavigationView() {

        final View headerView = navigationView.getHeaderView(0);
        userName = (TextView) headerView.findViewById(R.id.userName);
        tradeCount = (TextView) headerView.findViewById(R.id.userTradeCount);
        feedbackScore = (TextView) headerView.findViewById(R.id.userTradeFeedback);

        userName.setText(AuthUtils.getUsername(preference, sharedPreferences));
        tradeCount.setText(AuthUtils.getUsername(preference, sharedPreferences));
        feedbackScore.setText(AuthUtils.getTrades(preference, sharedPreferences));

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                hideSoftKeyboard(MainActivity.this);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                selectDrawerItem(menuItem);
                return true;
            }
        });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.navigationItemSearch:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_SEARCH);
                onRefreshStop();
                break;
            case R.id.navigationItemSend:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_SEND);
                onRefreshStop();
                break;
            case R.id.navigationItemReceive:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_RECEIVE);
                onRefreshStop();
                break;
            case R.id.navigationItemWallet:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_WALLET);
                onRefreshStop();
                break;
            case R.id.navigationItemAbout:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_ABOUT);
                onRefreshStop();
                break;
            case R.id.navigationItemSettings:
                Intent intent = SettingsActivity.createStartIntent(this);
                startActivity(intent);
                onRefreshStop();
                break;
            default:
                lastMenuItemId = menuItem.getItemId();
                setContentFragment(DRAWER_DASHBOARD);
                break;
        }

        // Highlight the selected item, update the title, and close the drawer
        menuItem.setChecked(true);
        drawerLayout.closeDrawers();
    }

    public void setContentFragment(int position) {

        this.position = position;

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.w("Error closing keyboard");
        }

        if (!isFinishing()) {
            if (position == DRAWER_WALLET) {
                swipeLayout.setEnabled(true);
                fragment = WalletFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, WALLET_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_SEARCH) {
                swipeLayout.setEnabled(false);
                fragment = SearchFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, SEARCH_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_SEND) {
                swipeLayout.setEnabled(false);
                fragment = SendFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, SEND_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_RECEIVE) {
                swipeLayout.setEnabled(false);
                fragment = RequestFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, RECEIVE_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_DASHBOARD) {
                swipeLayout.setEnabled(true);
                fragment = DashboardFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, DASHBOARD_FRAGMENT)
                        .commitAllowingStateLoss();
            } else if (position == DRAWER_ABOUT) {
                swipeLayout.setEnabled(false);
                fragment = AboutFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment, ABOUT_FRAGMENT)
                        .commitAllowingStateLoss();
            }
        }
    }

    private void startSendFragment(String bitcoinAddress, String bitcoinAmount) {
        fragment = SendFragment.newInstance(bitcoinAddress, bitcoinAmount);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, SEND_FRAGMENT)
                .commitAllowingStateLoss();

        navigationView.getMenu().findItem(R.id.navigationItemSend).setChecked(true);
        position = DRAWER_SEND;
    }

    @Override
    public void handleRefresh() {
        if (fragment != null && fragment.getTag() != null) {
            switch (fragment.getTag()) {
                case WALLET_FRAGMENT:
                    ((WalletFragment) fragment).onRefresh();
                    break;
                case DASHBOARD_FRAGMENT:
                    ((DashboardFragment) fragment).onRefresh();
                    break;
            }
        }
    }

    public void navigateDashboardViewAndRefresh() {
        setContentFragment(DRAWER_DASHBOARD);
        navigationView.getMenu().findItem(R.id.navigationItemDashboard).setChecked(true);
        onRefresh();
    }

    public void navigateSendView() {
        setContentFragment(DRAWER_SEND);
        navigationView.getMenu().findItem(R.id.navigationItemSend).setChecked(true);
    }

    public void navigateDashboardView() {
        setContentFragment(DRAWER_DASHBOARD);
        navigationView.getMenu().findItem(R.id.navigationItemDashboard).setChecked(true);
    }

    public void navigateSearchView() {
        setContentFragment(DRAWER_SEARCH);
    }

    public void restoreActionBar() {
        /*ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle(mTitle);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (drawerLayout != null && !drawerLayout.isDrawerOpen(GravityCompat.START)) {
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_SCAN) {
            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanningResult != null) {
                final String bitcoinAddress = WalletUtils.parseBitcoinAddress(scanningResult.getContents());
                final String bitcoinAmount = WalletUtils.parseBitcoinAmount(scanningResult.getContents());
                startSendFragment(bitcoinAddress, bitcoinAmount);
            } else {
                toast(getString(R.string.toast_scan_canceled));
            }
        } else if (requestCode == EditAdvertisementActivity.REQUEST_CODE) {
            if (resultCode == EditAdvertisementActivity.RESULT_UPDATED) {
                onRefresh();
            }
        } else if (requestCode == AdvertisementActivity.REQUEST_CODE) {
            if (resultCode == AdvertisementActivity.RESULT_DELETED) {
                onRefresh();
            }
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent == null || intent.getExtras() == null)
            return;

        Bundle extras = intent.getExtras();
        int type = extras.getInt(EXTRA_NOTIFICATION_TYPE, 0);

        if (type == NotificationUtils.NOTIFICATION_TYPE_CONTACT) {
            String id = extras.getString(EXTRA_NOTIFICATION_ID);
            if (id != null) {
                Intent launchIntent = ContactActivity.createStartIntent(this, id);
                startActivity(launchIntent);
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT) {
            String id = extras.getString(EXTRA_NOTIFICATION_ID);
            if (TextUtils.isEmpty(id)) {
                showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), getString(R.string.error_no_advertisement)));
            } else {
                Intent launchIntent = AdvertisementActivity.createStartIntent(this, id);
                startActivity(launchIntent);
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            setContentFragment(DRAWER_WALLET);
            navigationView.getMenu().findItem(R.id.navigationItemWallet).setChecked(true);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setHeaderItem(String rate, String currency, String exchange) {
        bitcoinTitle.setText(R.string.text_title_market_price);
        bitcoinPrice.setText(rate + " " + currency + "/" + getString(R.string.btc));
        bitcoinValue.setText(exchange + " (" + currency + ")");
    }

    protected void handleSyncEvent(String syncActionType, String extraErrorMessage, int extraErrorCode) {
        switch (syncActionType) {
            case SyncAdapter.ACTION_TYPE_START:
                break;
            case SyncAdapter.ACTION_TYPE_REFRESH:
                handleRefresh();
            case SyncAdapter.ACTION_TYPE_COMPLETE:
                onRefreshStop();
                AuthUtils.setFirstTime(preference, false);
                break;
            case SyncAdapter.ACTION_TYPE_CANCELED:
                onRefreshStop();
                AuthUtils.setFirstTime(preference, false);
                break;
            case SyncAdapter.ACTION_TYPE_ERROR:
                Timber.e("Sync error: " + extraErrorMessage + "code: " + extraErrorCode);
                if(extraErrorCode == DataServiceUtils.STATUS_403) {
                    showAlertDialog(new AlertDialogEvent(getString(R.string.alert_token_expired_title), getString(R.string.error_bad_token)), new Action0() {
                        @Override
                        public void call() {
                            logOut();
                        }
                    });
                }
                onRefreshStop();
                AuthUtils.setFirstTime(preference, false);
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

            handleSyncEvent(syncActionType, extraErrorMessage, extraErrorCode);
        }
    };
}