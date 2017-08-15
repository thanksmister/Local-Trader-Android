/*
 * Copyright (c) 2017 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertiseAdapter;
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class SearchResultsActivity extends BaseActivity {
  
    @Inject
    DbManager dbManager;

    @Inject
    GeoLocationService geoLocationService;

    @InjectView(R.id.resultsList)
    ListView content;

    @InjectView(R.id.resultsProgress)
    View progress;

    @InjectView(R.id.emptyLayout)
    View emptyLayout;

    @InjectView(R.id.emptyText)
    TextView emptyText;
    
    @InjectView(R.id.searchResultsToolBar)
    Toolbar toolbar;
    
    private AdvertiseAdapter adapter;
    private TradeType tradeType = TradeType.NONE;
    

    public static Intent createStartIntent(Context context) {
        return new Intent(context, SearchResultsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_search_results);

        ButterKnife.inject(this);
        
        tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences));
        
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if(getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(getHeader(tradeType)); 
            }
        }
        
        content.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Advertisement advertisement = (Advertisement) adapterView.getAdapter().getItem(i);
                showAdvertiser(advertisement);
            }
        });
        
        adapter = new AdvertiseAdapter(this);
        setAdapter(adapter);
        updateData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbar != null)
            toolbar.inflateMenu(R.menu.searchresults);

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void handleRefresh() {
        updateData();
    }
    

    public void showContent() {
        if(content != null && progress != null && emptyLayout != null) {
            content.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            progress.setVisibility(View.GONE); 
        }
    }

    public void showEmpty() {
        if(content != null && progress != null && emptyLayout != null) {
            content.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            emptyText.setText(R.string.text_no_advertisers);
        }
    }

    public void showProgress() {
        if(content != null && progress != null && emptyLayout != null) {
            content.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
        }
    }

    protected void updateData() {
        
        Timber.d("Update Data");

        final String currency = SearchUtils.getSearchCurrency(sharedPreferences);
        final String paymentMethod = SearchUtils.getSearchPaymentMethod(sharedPreferences);
        final String country = SearchUtils.getSearchCountryName(sharedPreferences);
        final String code = SearchUtils.getSearchCountryCode(sharedPreferences);
        final double latitude = SearchUtils.getSearchLatitude(sharedPreferences);
        final double longitude = SearchUtils.getSearchLongitude(sharedPreferences);

        Timber.d("tradeType: " + tradeType.name());
        Timber.d("currency: " + currency);
        Timber.d("method: " + paymentMethod);
        Timber.d("country: " + country);
        Timber.d("code: " + code);
        Timber.d("latitude: " + latitude);
        Timber.d("longitude: " + longitude);

        toast(getString(R.string.toast_searching));
        showProgress();
        
        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
           geoLocationService.getLocalAdvertisements(latitude, longitude, tradeType)
                    .subscribeOn(Schedulers.newThread())
                    .compose(this.<List<Advertisement>>bindUntilEvent(ActivityEvent.PAUSE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Advertisement>>() {
                        @Override
                        public void call(final List<Advertisement> advertisements) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setData(advertisements, null);
                                }
                            });
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(final Throwable throwable) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showEmpty();
                                }
                            });
                        }
                    });
        } else {
            dbManager.methodQuery().cache()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(this.<List<MethodItem>>bindUntilEvent(ActivityEvent.PAUSE))
                    .subscribe(new Action1<List<MethodItem>>() {
                        @Override
                        public void call(final List<MethodItem> methodItems) {
                            String method = TradeUtils.getPaymentMethod(paymentMethod, methodItems);
                            Timber.i("Payment Method Util: " + method);
                            geoLocationService.getOnlineAdvertisements(tradeType, country, code, currency, method)
                                    .subscribe(new Action1<List<Advertisement>>() {
                                        @Override
                                        public void call(final List<Advertisement> advertisements) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setData(advertisements, methodItems);
                                                }
                                            });
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(final Throwable throwable) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    toast(throwable.getMessage());
                                                    showEmpty();
                                                }
                                            });
                                        }
                                    });
                        }
                    });
        }
    }

    private void setData(List<Advertisement> advertisements, List<MethodItem> methodItems) {
        if (advertisements.isEmpty()) {
            showEmpty();
        } else if ((tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL) && methodItems == null) {
            showEmpty();
            toast(getString(R.string.toast_error_advertisers));
        } else {
            showContent();
            getAdapter().replaceWith(advertisements, methodItems);
        }
    }

    private void setAdapter(AdvertiseAdapter adapter) {
        content.setAdapter(adapter);
    }

    private AdvertiseAdapter getAdapter() {
        return adapter;
    }

    private String getHeader(TradeType tradeType) {
        String header = "";
        if (tradeType == null || tradeType == TradeType.NONE) {
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

        header = getString(R.string.text_results_for, header);
        return header;
    }

    public void showAdvertiser(Advertisement advertisement) {
        Intent intent = AdvertiserActivity.createStartIntent(this, advertisement.ad_id);
        startActivity(intent);
    }
}
