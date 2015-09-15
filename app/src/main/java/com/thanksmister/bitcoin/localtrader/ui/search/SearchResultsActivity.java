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

package com.thanksmister.bitcoin.localtrader.ui.search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertiserActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertiseAdapter;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class SearchResultsActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_TRADE_TYPE = "com.thanksmister.extras.EXTRA_TRADE_TYPE";
    public static final String EXTRA_ADDRESS = "com.thanksmister.extras.EXTRA_ADDRESS";
    public static final String EXTRA_METHOD = "com.thanksmister.extras.EXTRA_METHOD";

    @Inject
    DbManager dbManager;
    
    @Inject
    GeoLocationService geoLocationService;
    
    @InjectView(R.id.resultsList)
    ListView content;

    @InjectView(R.id.resultsProgress)
    View progress;
    
    @InjectView(R.id.searchResultsToolBar)
    Toolbar toolbar;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;
    
    private AdvertiseAdapter adapter;
    private String paymentMethod;
    private TradeType tradeType = TradeType.NONE;
    private Address address;
    
    public static Intent createStartIntent(Context context, @NonNull TradeType tradeType, @NonNull Address address, @Nullable String paymentMethod)
    {
        Intent intent = new Intent(context, SearchResultsActivity.class);
        intent.putExtra(EXTRA_TRADE_TYPE, tradeType);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_METHOD, paymentMethod);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_search_results);

        ButterKnife.inject(this);

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        if (savedInstanceState == null) {
            tradeType = (TradeType) getIntent().getSerializableExtra(EXTRA_TRADE_TYPE);
            address = getIntent().getParcelableExtra(EXTRA_ADDRESS);
            paymentMethod = getIntent().getStringExtra(EXTRA_METHOD);
        } else {
            tradeType = (TradeType) savedInstanceState.getSerializable(EXTRA_TRADE_TYPE);
            address = savedInstanceState.getParcelable(EXTRA_ADDRESS);
            paymentMethod = savedInstanceState.getString(EXTRA_METHOD);
        }
        
        if(tradeType == null) { // this is funky but maybe 
            Timber.e("Trade type null");
            tradeType = TradeType.NONE;
        }
        
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getHeader(tradeType));
        }

        content.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Advertisement advertisement = (Advertisement) adapterView.getAdapter().getItem(i);
                showAdvertiser(advertisement);
            }
        });

        content.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                int topRowVerticalPosition = (content == null || content.getChildCount() == 0) ? 0 : content.getChildAt(0).getTop();
                swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });
  
        adapter = new AdvertiseAdapter(this);
        setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_TRADE_TYPE, tradeType);
        outState.putParcelable(EXTRA_ADDRESS, address);
        outState.putString(EXTRA_METHOD, paymentMethod);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.searchresults);

        return true;
    }
    
    @Override
    public void onPause()
    {
        super.onPause();

        if(progress != null)
            progress.clearAnimation();

        if(content != null)
            content.clearAnimation();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        updateData();
    }

    @Override
    public void onRefresh()
    {
        updateData();
    }

    @Subscribe
    public void onRefreshEvent(RefreshEvent event)
    {
        if (event == RefreshEvent.REFRESH) {
            updateData();
        }
    }
    
    public void onRefreshStop()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    public void showContent(final boolean show)
    {
        if (progress == null || content == null) return;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progress.setVisibility(show ? View.GONE : View.VISIBLE);
        progress.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if(progress != null)
                    progress.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        content.setVisibility(show ? View.VISIBLE : View.GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if(content != null)
                    content.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    protected void updateData()
    {
        Timber.d("Update Data");
     
        if(tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {

            Observable<List<Advertisement>> advertisementsObservable = geoLocationService.getLocalAdvertisements(address.getLatitude(), address.getLongitude(), tradeType);
            advertisementsObservable
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Advertisement>>()
                    {
                        @Override
                        public void call(final List<Advertisement> advertisements)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    onRefreshStop();
                                    showContent(true);
                                    setData(advertisements, null);
                                }
                            });
                        }
                    }, new Action1<Throwable>()
                    {
                        @Override
                        public void call(final Throwable throwable)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    onRefreshStop();
                                    reportError(throwable);
                                    handleError(throwable, true);
                                }
                            });
                        }
                    });
        } else {

            Observable<List<MethodItem>> methodObservable = dbManager.methodQuery().cache();
            methodObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<MethodItem>>()
                    {
                        @Override
                        public void call(final List<MethodItem> methodItems)
                        {
                            String method = TradeUtils.getPaymentMethod(paymentMethod, methodItems);

                            Timber.i("Payment Method Util: " + method);

                            Observable<List<Advertisement>> advertisementsObservable = geoLocationService.getOnlineAdvertisements(address.getCountryCode(), address.getCountryName(), tradeType, method);
                            advertisementsObservable.subscribe(new Action1<List<Advertisement>>()
                            {
                                @Override
                                public void call(final List<Advertisement> advertisements)
                                {
                                    Timber.i("Payment Methods: " + methodItems.size());
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            onRefreshStop();
                                            showContent(true);
                                            setData(advertisements, methodItems);
                                        }
                                    });
                                }
                            }, new Action1<Throwable>()
                            {
                                @Override
                                public void call(final Throwable throwable)
                                {
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            onRefreshStop();
                                            handleError(throwable, true);
                                        }
                                    });
                                }
                            });
                        }
                    });
        }
    }

    private void setData(List<Advertisement> advertisements, List<MethodItem> methodItems)
    {
        Timber.d("setData");
        Timber.d("setData Payment Methods: " + methodItems);
        
        if(methodItems != null)
            Timber.d("setData Payment Methods: " + methodItems.size());
        
        Timber.d("setData Advertisement: " + advertisements.size());
    
        if(advertisements.isEmpty()) {
            snackError("No advertisers located.");
        } else if ( (tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL) && methodItems == null ) {
            snackError("Unable to load online advertisers.");
        } else {
            getAdapter().replaceWith(advertisements, methodItems);
        }
    }

    private void setAdapter(AdvertiseAdapter adapter)
    {
        content.setAdapter(adapter);
    }

    private AdvertiseAdapter getAdapter()
    {
        return adapter;
    }

    private String getHeader(TradeType tradeType)
    {
        String header = ""; 
        if(tradeType == null || tradeType == TradeType.NONE) {
            return header;
        }
        switch (tradeType) {
            case LOCAL_BUY:
                header = getString(R.string.search_local_sellers_header);
                break;
            case LOCAL_SELL:
                header = getString(R.string.search_local_buyers_header);
                break;
            case ONLINE_BUY:
                header = getString(R.string.search_online_sellers_header);
                break;
            case ONLINE_SELL:
                header = getString(R.string.search_online_buyers_header);
                break;
        }

        header = "Results for " + header;
        return header;
    }

    public void showAdvertiser(Advertisement advertisement)
    {
        Intent intent = AdvertiserActivity.createStartIntent(this, advertisement.ad_id);
        startActivity(intent);
    }
}
