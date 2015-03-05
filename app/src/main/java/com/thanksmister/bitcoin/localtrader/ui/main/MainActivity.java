package com.thanksmister.bitcoin.localtrader.ui.main;


import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.zxing.android.IntentIntegrator;
import com.google.zxing.android.IntentResult;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.about.AboutFragment;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.dashboard.DashboardFragment;
import com.thanksmister.bitcoin.localtrader.ui.promo.PromoActivity;
import com.thanksmister.bitcoin.localtrader.ui.request.RequestFragment;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchFragment;
import com.thanksmister.bitcoin.localtrader.ui.wallet.WalletFragment;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

@BaseActivity.RequiresAuthentication
public class MainActivity extends BaseActivity implements MainView, NavigationDrawerFragment.NavigationDrawerCallbacks
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
    MainPresenter presenter;

    @Inject
    DataService service;

    @Inject
    Bus bus;

    private MaterialDialog alertDialog;
    private MaterialDialog progressDialog;
    private String contactId;
    
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
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
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            contactId = getIntent().getStringExtra(EXTRA_CONTACT);
        } else {
            contactId = savedInstanceState.getParcelable(EXTRA_CONTACT);
        }

        String bitcoinUri = getIntent().getStringExtra(BITCOIN_URI);
        if(bitcoinUri != null) {
            handleBitcoinUri(bitcoinUri);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        mTitle = "";

        if(service.isLoggedIn()) {
            // Set up the drawer.
            navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
            navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
           // SyncUtils.CreateSyncAccount(getApplicationContext());
           // SyncUtils.TriggerRefresh(getApplicationContext()); 
        } else {
            SyncUtils.ClearSyncAccount(getApplicationContext());
        }
    }

    /*@Override
    public void onRefresh()
    {
        empty.setVisibility(View.GONE);
        
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                if (getSupportFragmentManager().findFragmentByTag(SEND_RECEIVE_FRAGMENT) != null) {
                    Fragment  fragment = getSupportFragmentManager().findFragmentByTag(SEND_RECEIVE_FRAGMENT);
                    fragment.onResume(); // refresh
                } else if (getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT) != null) {
                    Fragment  fragment = getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT);
                    fragment.onResume(); // refresh
                } else if (getSupportFragmentManager().findFragmentByTag(WALLET_FRAGMENT) != null) {
                    Fragment  fragment = getSupportFragmentManager().findFragmentByTag(WALLET_FRAGMENT);
                    fragment.onResume(); // refresh
                } else if (getSupportFragmentManager().findFragmentByTag(DASHBOARD_FRAGMENT) != null) {
                    Fragment  fragment = getSupportFragmentManager().findFragmentByTag(DASHBOARD_FRAGMENT);
                    fragment.onResume(); // refresh
                }
            }
        }, 2500);

    }*/
    
    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();

        if(((Object) this).getClass().isAnnotationPresent(RequiresAuthentication.class)) {
            if(!service.isLoggedIn()) {
                Intent intent = new Intent(this, PromoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position)
    {
        clearActionBar();

        if(service.isLoggedIn()) {

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
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new MainModule(this));
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        int type = extras.getInt(EXTRA_TYPE, 0);
        if(type == NotificationUtils.NOTIFICATION_TYPE_CONTACT) {
            contactId = extras.getString(EXTRA_CONTACT);
            assert  contactId != null;
            
            Intent contactIntent = ContactActivity.createStartIntent(this, contactId);
            startActivity(contactIntent);
  
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            navigationDrawerFragment.selectItem(DRAWER_WALLET);
        }
    }
}
