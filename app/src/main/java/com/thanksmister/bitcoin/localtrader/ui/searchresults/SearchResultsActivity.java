package com.thanksmister.bitcoin.localtrader.ui.searchresults;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.ui.misc.AdvertiseAdapter;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SearchResultsActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_TRADE_TYPE = "com.thanksmister.extras.EXTRA_TRADE_TYPE";
    public static final String EXTRA_ADDRESS = "com.thanksmister.extras.EXTRA_ADDRESS";
    public static final String EXTRA_METHOD = "com.thanksmister.extras.EXTRA_METHOD";
    
    @InjectView(android.R.id.list)
    ListView list;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.content)
    View content;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

    @InjectView(R.id.contactToolBar)
    Toolbar toolbar;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @OnClick(R.id.emptyRetryButton)
    public void emptyButtonClicked()
    {
        getAdvertisements(tradeType, address, paymentMethod);
    }

    private AdvertiseAdapter adapter;
    private String paymentMethod;
    private TradeType tradeType;
    private Address address;

    public static Intent createStartIntent(Context context, TradeType tradeType, Address address, String paymentMethod)
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
        
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getHeader(tradeType));
        }

        list.setOnItemClickListener((adapterView, view, i, l) -> {
            Advertisement advertisement = (Advertisement) adapterView.getAdapter().getItem(i);
            showAdvertiser(advertisement);
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
    public void onResume()
    {
        super.onResume();

        getAdvertisements(tradeType, address, paymentMethod);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);
    }

    @Override
    public void onRefresh()
    {
        getAdvertisements(tradeType, address, paymentMethod);
    }
    
    public void onRefreshStop()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }
    
    public void onError(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);

        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }
    
    public void showProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }
    
    public void hideProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    public void setData(List<Advertisement> advertisements, List<Method> methods, TradeType tradeType, String address)
    {
        if(advertisements.isEmpty()) {
            onError("No advertisers.");
        } else {
            hideProgress();
            getAdapter().replaceWith(advertisements, methods);
        }
    }

    private void setAdapter(AdvertiseAdapter adapter)
    {
        list.setAdapter(adapter);
    }

    private AdvertiseAdapter getAdapter()
    {
        return adapter;
    }

    private String getHeader(TradeType tradeType)
    {
        String header = "";
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
}
