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


import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.android.IntentIntegrator;
import com.google.zxing.android.IntentResult;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchFragment;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindActivity;

@BaseActivity.RequiresAuthentication
public class MainActivity extends BaseActivity implements View.OnClickListener, AppBarLayout.OnOffsetChangedListener
{
    private static final String BITCOIN_URI = "com.thanksmister.extra.BITCOIN_URI";
    private static final String DASHBOARD_FRAGMENT = "com.thanksmister.fragment.DASHBOARD_FRAGMENT";
    private static final String ABOUT_FRAGMENT = "com.thanksmister.fragment.ACCOUNT_FRAGMENT";
    private static final String SEND_RECEIVE_FRAGMENT = "com.thanksmister.fragment.SEND_RECEIVE_FRAGMENT";
    private static final String SEARCH_FRAGMENT = "com.thanksmister.fragment.SEARCH_FRAGMENT";
    private static final String WALLET_FRAGMENT = "com.thanksmister.fragment.WALLET_FRAGMENT";
    
    public static String EXTRA_CONTACT = "extra_contact";
    public static String EXTRA_TYPE = "extra_type";
    public static String EXTRA_FRAGMENT = "extra_fragment";
    
    private static final int DRAWER_DASHBOARD = 0;
    private static final int DRAWER_SEARCH = 1;
    private static final int DRAWER_WALLET = 3;
    private static final int DRAWER_SEND = 2;
    private static final int DRAWER_ABOUT = 4;
    
    private static final int REQUEST_SCAN = 49374;

    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;
    
    /*@InjectView(R.id.fab)
    FloatingActionButton fab;*/

    /*@InjectView(R.id.toolbar)
    Toolbar toolbar;*/
    
    @InjectView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @InjectView(R.id.navigation)
    NavigationView navigationView;

   /* @InjectView(R.id.bitcoinTitle)
    TextView bitcoinTitle;
    
    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;
    
    @InjectView(R.id.bitcoinLayout)
    View bitcoinLayout;*/

    /*@InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;
*/
    /*@InjectView(R.id.appBarLayout)
    AppBarLayout appBarLayout;*/
    
    CharSequence mTitle = "";
    Handler handler;
    Fragment fragment;
    int position = DRAWER_DASHBOARD;
    Observable<ExchangeItem> exchangeObservable;
    Observable<WalletItem> walletObservable;
    Subscription subscription = Subscriptions.empty();
    CompositeSubscription subscriptions = new CompositeSubscription();

    private class WalletData {
        public WalletItem wallet;
        public ExchangeItem exchange;
    }

    WalletData walletData;
    
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
        
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
        
        String bitcoinUri = getIntent().getStringExtra(BITCOIN_URI);
        if(bitcoinUri != null) {
            handleBitcoinUri(bitcoinUri);
        }
        
        if(savedInstanceState != null) {
            position = savedInstanceState.getInt(EXTRA_FRAGMENT);
        }
        
        setupNavigationView();
        setupToolbar();
        setupSwipeRefresh();
        setupFab();

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
                    setContentFragment(position);
                    SyncUtils.CreateSyncAccount(getApplicationContext());
                    SyncUtils.TriggerRefresh(getApplicationContext());
                }
            }
        });
        
        walletObservable = bindActivity(this, dbManager.walletQuery());
        exchangeObservable = bindActivity(this, dbManager.exchangeQuery());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //No call for super(). Bug on API Level > 11.
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_FRAGMENT, position);
    }
    
    @Override
    public void onResume()
    {
        super.onResume();

        //appBarLayout.addOnOffsetChangedListener(this);
        
        if (((Object) this).getClass().isAnnotationPresent(RequiresAuthentication.class)) {
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

        subscribeData();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        //appBarLayout.removeOnOffsetChangedListener(this);
        subscriptions.unsubscribe();
    }

    /*@Override
    public void onRefresh()
    {
        switch (fragment.getTag()) {
            case DASHBOARD_FRAGMENT:
                ((DashboardFragment) fragment).onRefresh();
                break;
            case WALLET_FRAGMENT:
                ((WalletFragment) fragment).onRefresh();
                break;
            case SEND_RECEIVE_FRAGMENT:
                ((RequestFragment) fragment).onRefresh();
                break;
            default:
               // onRefreshEvent(RefreshEvent.STOP);
                break;
        }
    }*/

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i)
    {
        /*if (i == 0) {
            swipeLayout.setEnabled(true);
        } else {
            swipeLayout.setEnabled(false);
        }*/
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
    
    private void setupFab()
    {
        //fab.setOnClickListener(this);
    }

    private void setupNavigationView()
    {
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
            case R.id.navigationItemDashboard:
                setContentFragment(DRAWER_DASHBOARD);
                break;
            case R.id.navigationItemSearch:
                setContentFragment(DRAWER_SEARCH);
                break;
            case R.id.navigationItemSend:
                setContentFragment(DRAWER_SEND);
                break;
            case R.id.navigationItemWallet:
                setContentFragment(DRAWER_WALLET);
                break;
            case R.id.navigationItemAbout:
                setContentFragment(DRAWER_ABOUT);
                break;
            default:
                setContentFragment(DRAWER_DASHBOARD);
        }

        // Highlight the selected item, update the title, and close the drawer
        menuItem.setChecked(true);
        drawerLayout.closeDrawers();
    }
    
    private void setInitialFragment(int position)
    {
        FragmentManager fragmentManager = getFragmentManager();
        if(fragmentManager.findFragmentByTag(DASHBOARD_FRAGMENT) == null) {
            setContentFragment(DRAWER_DASHBOARD);
        } else {
            setContentFragment(position);
        }
    }
    
    private void setupSwipeRefresh()
    {
        handler = new Handler();
        /*swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));*/
    }

    private void setupToolbar()
    {
        //toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Show menu icon
       /* final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setDisplayHomeAsUpEnabled(true);*/
    }
    
    private void setContentFragment(int position)
    {
        this.position = position;
        
        if (position == DRAWER_WALLET) {
            fragment = WalletFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment, WALLET_FRAGMENT)
                    .commit();
        } else if (position == DRAWER_SEARCH) {
            fragment = SearchFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment, SEARCH_FRAGMENT)
                    .commit();
        } else if (position == DRAWER_SEND) {
            fragment = RequestFragment.newInstance(RequestFragment.WalletTransactionType.SEND);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment, SEND_RECEIVE_FRAGMENT)
                    .commit();
        } else if (position == DRAWER_DASHBOARD) {
            fragment = DashboardFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment, DASHBOARD_FRAGMENT)
                    .commit();
        } else if (position == DRAWER_ABOUT) {
            fragment = AboutFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment, ABOUT_FRAGMENT)
                    .commit();
        }

        setAppBarText(walletData);
        setViewTitle(position);
    }
    
    private void startSendRequestFragment(String bitcoinAddress, String bitcoinAmount)
    {
        fragment = RequestFragment.newInstance(bitcoinAddress, bitcoinAmount);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, SEND_RECEIVE_FRAGMENT)
                .commit();

        /*bitcoinLayout.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);*/
        setViewTitle(DRAWER_SEND);
    }

    @Subscribe
    public void onNavigateEvent (NavigateEvent event)
    {
        if(event == NavigateEvent.SEND) {
            setContentFragment(DRAWER_SEND);
        } else if (event == NavigateEvent.SEARCH) {
            setContentFragment(DRAWER_SEARCH);
        } else if (event == NavigateEvent.WALLET) {
            setContentFragment(DRAWER_WALLET);
        } else if (event == NavigateEvent.LOGOUT_CONFIRM) {
            logOutConfirmation();
        } else if (event == NavigateEvent.LOGOUT) {
            logOut();
        } else if (event == NavigateEvent.QRCODE) {
            launchScanner();
        }
    }

    public void setViewTitle(int number)
    {
        clearActionBar();
                
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
                mTitle = getString(R.string.view_title_about);
                break;
        }

        //toolbar.setTitle(mTitle);
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
            setContentFragment(DRAWER_WALLET);
        }
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.fab) {
            bus.post(NavigateEvent.QRCODE);
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
    public void onRefreshEvent(RefreshEvent event)
    {
        Timber.d("RefreshEvent: " + event.name());
        if (event == RefreshEvent.RETRY) {
            switch (fragment.getTag()) {
                case DASHBOARD_FRAGMENT:
                    ((DashboardFragment) fragment).onRefresh();
                    break;
                case WALLET_FRAGMENT:
                    ((WalletFragment) fragment).onRefresh();
                    break;
                case SEND_RECEIVE_FRAGMENT:
                    ((RequestFragment) fragment).onRefresh();
                    break;
                default:
                    // onRefreshEvent(RefreshEvent.STOP);
                    break;
            }
        } 
    }
    
    protected void setAppBarText(WalletData data)
    {
        if(data == null) return;
        
        /*if(position == DRAWER_DASHBOARD) {
            String value = Calculations.calculateAverageBidAskFormatted(data.exchange.bid(), data.exchange.ask());
            bitcoinTitle.setText("MARKET PRICE");
            bitcoinPrice.setText("$" + value + " / BTC");
            bitcoinValue.setText("Source " + data.exchange.exchange());
        } else if (position == DRAWER_WALLET) {
            String btcValue = Calculations.computedValueOfBitcoin(data.exchange.bid(), data.exchange.ask(), data.wallet.balance());
            String btcAmount = Conversions.formatBitcoinAmount(data.wallet.balance()) + " " + getString(R.string.btc);
            bitcoinPrice.setText(btcAmount);
            bitcoinTitle.setText("ACCOUNT BALANCE");
            bitcoinValue.setText("≈ $" + btcValue + " " + getString(R.string.usd) + " (" + data.exchange.exchange() + ")");
        }*/
    }
    
    protected void subscribeData()
    {
        subscriptions = new CompositeSubscription();
        subscriptions.add(Observable.combineLatest(walletObservable, exchangeObservable, new Func2<WalletItem, ExchangeItem, WalletData>()
        {
            @Override
            public WalletData call(WalletItem wallet, ExchangeItem exchange)
            {
                WalletData walletData = new WalletData();
                walletData.wallet = wallet;
                walletData.exchange = exchange;
                return walletData;
            }
        }).subscribe(new Action1<WalletData>()
        {
            @Override
            public void call(WalletData data)
            {
                walletData = data;
                setAppBarText(walletData);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                reportError(throwable);
            }
        }));
        
        /*subscription = exchangeObservable.subscribe(new Action1<ExchangeItem>()
            {
                @Override
                public void call(ExchangeItem exchange)
                {
                    if (exchange != null) {
                        String value = Calculations.calculateAverageBidAskFormatted(exchange.bid(), exchange.ask());
                        bitcoinPrice.setText("$" + value + " / BTC");
                        bitcoinValue.setText("Source " + exchange.exchange());
                    }
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    reportError(throwable);
                }
            });*/
    }
    
    /*private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run() {
            swipeLayout.setRefreshing(true);
        }
    };

    @Subscribe
    public void onRefreshEvent(RefreshEvent event)
    {
        Timber.d("RefreshEvent: " + event.name());
        
        if (event == RefreshEvent.REFRESH) {
            handler = new Handler();
            handler.postDelayed(refreshRunnable, 1000);
        } else if (event == RefreshEvent.STOP) {
            handler.removeCallbacks(refreshRunnable);
            swipeLayout.setRefreshing(false);
        } else if (event == RefreshEvent.RETRY) {
            onRefresh();
        }
    }*/
}
