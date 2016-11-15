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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.data.services.NotificationService;
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class DashboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    NotificationService notificationService;

    @Inject
    DbManager dbManager;

    @Inject
    BriteDatabase db;

    @Inject
    Bus bus;

    @InjectView(R.id.fab)
    FloatingActionButton fab;
    
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.bitcoinTitle)
    TextView bitcoinTitle;

    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @InjectView(R.id.pagerTabStrip)
    PagerTabStrip mPagerTabStrip;

    @InjectView(R.id.mainViewPager)
    ViewPager mViewPager;
    
    @Inject
    protected SharedPreferences sharedPreferences;
    
    private static String[] mTabNames;
    private Handler handler;
    private int pagerPosition = 0;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DashboardFragment newInstance()
    {
        DashboardFragment dashboardFragment = new DashboardFragment();
        Bundle args = new Bundle();
        args.putInt("pagePosition", 0);
        dashboardFragment.setArguments(args);
        return new DashboardFragment();
    }
    

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            pagerPosition = getArguments().getInt("pagePosition", 0); 
        }
        
        // refresh handler
        handler = new Handler();
        
        mTabNames = getResources().getStringArray(R.array.tab_items);
        
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));
        swipeLayout.setProgressViewOffset(false, 48, 186);
        
        mPagerTabStrip.setTabIndicatorColorResource(R.color.white);
        mPagerTabStrip.setDrawFullUnderline(true);
        mPagerTabStrip.setTextSpacing(5);

        DashboardPagerAdapter adapterViewPager = new DashboardPagerAdapter(getChildFragmentManager());
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setCurrentItem(pagerPosition);
        mViewPager.setAdapter(adapterViewPager);
        mViewPager.setOnPageChangeListener( new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled( int position, float v, int i1 ) {
            }

            @Override
            public void onPageSelected( int position ) {
                pagerPosition = position;
            }

            @Override
            public void onPageScrollStateChanged( int state )
            {
                enableDisableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE );
            }
        } );

        setupToolbar();
        setupFab();
    }

    private void setupFab()
    {
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bus.post(NavigateEvent.QRCODE);
            }
        });
    }

    private void setupToolbar()
    {
        if(!isAdded()) return;
        
        try {
            ((MainActivity) getActivity()).setSupportActionBar(toolbar);
        } catch (NoClassDefFoundError e) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_device_title),
                    getString(R.string.error_device_softare_description)), new Action0()
            {
                @Override
                public void call()
                {
                    getActivity().finish();
                }
            });
            return;
        }


        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle("");
        ab.setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_dashboard, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt("pagePosition", 0);
        }
        
        mViewPager.setCurrentItem(pagerPosition);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == EditActivity.REQUEST_CODE) {
            if (resultCode == EditActivity.RESULT_CREATED || resultCode == EditActivity.RESULT_UPDATED) {
                updateData();
            }
        } else if (requestCode == AdvertisementActivity.REQUEST_CODE) {
            if ( resultCode == AdvertisementActivity.RESULT_DELETED) {
                updateData();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.dashboard, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_search:
                showSearchScreen();
                return false;
            case R.id.action_send:
                showSendScreen();
                return true;
            case R.id.action_trades:
                showTradesScreen();
                return true;
            case R.id.action_advertise:
                createAdvertisementScreen();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pagePosition", pagerPosition);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        subscribeData();
        onRefreshStart();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);

        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void onRefresh()
    {
        String userName = AuthUtils.getUsername(sharedPreferences);
        SyncUtils.TriggerRefresh(getContext().getApplicationContext(), userName);
        updateData();
    }

    public void onRefreshStart()
    {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 1000);
    }

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(swipeLayout != null)
                swipeLayout.setRefreshing(true);
            
            updateData();
        }
    };

    protected void onRefreshStop()
    {
        if(handler != null)
            handler.removeCallbacks(refreshRunnable);

        if (swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    protected void subscribeData()
    {
        Timber.d("subscribeData");

        //dbManager.clearDashboard();

        dbManager.exchangeQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<ExchangeItem>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<ExchangeItem>()
                {
                    @Override
                    public void call(ExchangeItem exchangeItem)
                    {
                        if (exchangeItem != null) {
                            setHeaderItem(exchangeItem);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });
    }

    protected void updateData()
    {
        Timber.d("UpdateData");

        CompositeSubscription updateSubscriptions = new CompositeSubscription();

        updateSubscriptions.add(exchangeService.getMarket()
                .timeout(20, TimeUnit.SECONDS)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Update Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<Exchange>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Exchange>()
                {
                    @Override
                    public void call(Exchange exchange)
                    {
                        Timber.d("exchangeUpdateObservable");
                        dbManager.updateExchange(exchange);
                        onRefreshStop();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        snackError("Unable to update currency rate...");
                        onRefreshStop();
                    }
                }));
    }

    public static class DashboardPagerAdapter extends FragmentPagerAdapter
    {
        private static int NUM_ITEMS = 3;

        public DashboardPagerAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount()
        {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position)
        {
            switch (position) {
                case 0:
                    return AdvertisementsFragment.newInstance();
                case 1:
                    return ContactsFragment.newInstance();
                case 2:
                    return MessagesFragment.newInstance();
                default:
                    return null;
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position)
        {
            return mTabNames[position];
        }
    }
    
    protected void showSendScreen()
    {
        bus.post(NavigateEvent.SEND);
    }

    protected void showSearchScreen()
    {
        bus.post(NavigateEvent.SEARCH);
    }
    
    protected void showTradesScreen()
    {
        Intent intent = ContactsActivity.createStartIntent(getActivity(), DashboardType.RELEASED);
        intent.setClass(getActivity(), ContactsActivity.class);
        startActivity(intent);
    }
    
    protected void createAdvertisementScreen()
    {
        Intent intent = EditActivity.createStartIntent(getActivity(), true, null);
        intent.setClass(getActivity(), EditActivity.class);
        startActivityForResult(intent, EditActivity.REQUEST_CODE);
    }

    protected void setHeaderItem(ExchangeItem exchange)
    {
        String currency = exchangeService.getExchangeCurrency();
        String value = Calculations.calculateAverageBidAskFormatted(exchange.bid(), exchange.ask());
        bitcoinTitle.setText("MARKET PRICE");
        bitcoinPrice.setText("$" + value + " / BTC");
        bitcoinValue.setText("Source " + exchange.exchange() + " (" + currency + ")");
    }

    private void enableDisableSwipeRefresh(Boolean enableSwipeRefresh)
    {
        swipeLayout.setEnabled(enableSwipeRefresh);
    }
}