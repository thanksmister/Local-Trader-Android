package com.thanksmister.bitcoin.localtrader.ui.advertise;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;

public class AdvertiserActivity extends BaseActivity implements AdvertiserView
{
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";

    @Inject
    AdvertiserPresenter presenter;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(R.id.advertiserContent)
    ScrollView content;

    @InjectView(android.R.id.empty)
    View empty;
    
    @InjectView(R.id.buttonLayout)
    View buttonLayout;
    
    @InjectView(R.id.priceLayout)
    View priceLayout;

    @InjectView(R.id.buttonLayoutDivider)
    View buttonLayoutDivider;

    @InjectView(R.id.priceLayoutDivider)
    View priceLayoutDivider;

    @InjectView(R.id.retryTextView)
    TextView emptyTextView;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.tradePrice)
    TextView tradePrice;

    @InjectView(R.id.traderName)
    TextView traderName;

    @InjectView(R.id.tradeLimit)
    TextView tradeLimit;

    @InjectView(R.id.tradeTerms)
    TextView tradeTerms;

    @InjectView(R.id.tradeFeedback)
    TextView tradeFeedback;

    @InjectView(R.id.tradeCount)
    TextView tradeCount;

    @Optional
    @InjectView(R.id.dateText)
    TextView dateText;

    @InjectView(R.id.noteTextAdvertiser)
    TextView noteTextAdvertiser;

    @InjectView(R.id.lastSeenIcon)
    View lastSeenIcon;
    
    @OnClick(R.id.requestButton)
    public void requestButtonClicked()
    {
        presenter.showTradeRequest();
    }
    
    private String adId;

    public static Intent createStartIntent(Context context, String adId)
    {
        Intent intent = new Intent(context, AdvertiserActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_advertiser);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            adId = getIntent().getStringExtra(EXTRA_AD_ID);
        } else {
            adId = savedInstanceState.getString(EXTRA_AD_ID);
        }
        
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            setToolBarMenu(toolbar);
        }

        presenter.getAdvertisement(adId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putSerializable(EXTRA_AD_ID, adId);
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
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new AdvertiserModule(this));
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

        presenter.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    public void onRefreshStop()
    {
        // TODO implement refresh
    }

    @Override
    public void onError(String message)
    {
        // TODO implement
    }

    @Override
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.action_profile:
                        presenter.showProfile();
                        return true;
                    case R.id.action_location:
                        presenter.showAdvertisementOnMap();
                        return true;
                    case R.id.action_website:
                        presenter.showPublicAdvertisement();
                        return true;
                }
                return false;
            }
        });
    }

    @Override 
    public Context getContext() 
    {
        return this;
    }

    @Override
    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }

    @Override
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    public void setAdvertisement(Advertisement advertisement, Method method)
    {
        String location = (TradeUtils.isLocalTrade(advertisement))? advertisement.location:advertisement.location + " (" + advertisement.distance + " km)";
        String provider = TradeUtils.getPaymentMethod(advertisement, method);
        
        switch (advertisement.trade_type) {
            case ONLINE_SELL:
                noteTextAdvertiser.setText(Html.fromHtml(getContext().getString(R.string.advertiser_notes_text_online, "sell", advertisement.currency, provider)));
                break;
            case LOCAL_SELL:
                noteTextAdvertiser.setText(Html.fromHtml(getContext().getString(R.string.advertiser_notes_text_locally, "sell", advertisement.currency, location)));
                break;
            case ONLINE_BUY:
                noteTextAdvertiser.setText(Html.fromHtml(getContext().getString(R.string.advertiser_notes_text_online, "buy your", advertisement.currency, provider)));
                break;
            case LOCAL_BUY:
                noteTextAdvertiser.setText(Html.fromHtml(getContext().getString(R.string.advertiser_notes_text_locally, "buy your", advertisement.currency, location)));
                break;
        }

        if(advertisement.isATM()) {
            priceLayout.setVisibility(View.GONE);
            buttonLayout.setVisibility(View.GONE);
            priceLayoutDivider.setVisibility(View.GONE);
            buttonLayoutDivider.setVisibility(View.GONE);
            tradePrice.setText("ATM");
            noteTextAdvertiser.setText(Html.fromHtml(getContext().getString(R.string.advertiser_notes_text_atm, advertisement.currency, location)));
        } else {
            tradePrice.setText(getContext().getString(R.string.trade_price, advertisement.price, advertisement.currency));
        }
        
        traderName.setText(advertisement.profile.username);
        
        if(advertisement.isATM()) {
            tradeLimit.setText("");
        } else if(advertisement.min_amount == null) {
            tradeLimit.setText("");
        } else if(advertisement.max_amount == null) {
            tradeLimit.setText(getContext().getString(R.string.trade_limit_min, advertisement.min_amount, advertisement.currency));
        } else { // no maximum set
            tradeLimit.setText(getContext().getString(R.string.trade_limit, advertisement.min_amount, advertisement.max_amount_available, advertisement.currency));
        }

        if(advertisement.msg != null && !advertisement.msg.isEmpty()){
            tradeTerms.setText(Html.fromHtml(advertisement.msg.trim()));
            tradeTerms.setMovementMethod(LinkMovementMethod.getInstance());
        }

        tradeFeedback.setText(advertisement.profile.feedback_score);
        tradeCount.setText(advertisement.profile.trade_count);
        lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(advertisement.profile.last_online));
        String date = Dates.parseLocalDateStringAbbreviatedTime(advertisement.profile.last_online);
        dateText.setText("Last Seen - " + date);
    }

    @Override
    public void setHeader(TradeType tradeType)
    {
        String header = "";
        switch (tradeType) {
            case ONLINE_SELL:
            case ONLINE_BUY:
                header = (tradeType == TradeType.ONLINE_SELL) ? "Online Seller" : "Online Buyer";
                break;
            case LOCAL_SELL:
            case LOCAL_BUY:
                header = (tradeType == TradeType.LOCAL_SELL) ? "Local Seller" : "Local Buyer";
                break;
        }

        toolbar.setTitle(header);
    }

}
