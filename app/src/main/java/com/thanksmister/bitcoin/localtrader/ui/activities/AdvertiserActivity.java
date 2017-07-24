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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.components.SelectableLinkMovementMethod;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class AdvertiserActivity extends BaseActivity
{
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";

    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;

    @InjectView(R.id.advertiserProgress)
    View progress;

    @InjectView(R.id.advertiserContent)
    View content;
    
    @InjectView(R.id.priceLayout)
    View priceLayout;

    @InjectView(R.id.priceLayoutDivider)
    View priceLayoutDivider;
    
    @InjectView(R.id.advertiserToolbar)
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
    
    @InjectView(R.id.requirementsLayout)
    View requirementsLayout;

    @InjectView(R.id.trustedTextView)
    TextView trustedTextView;

    @InjectView(R.id.identifiedTextView)
    TextView identifiedTextView;

    @InjectView(R.id.smsTextView)
    TextView smsTextView;

    @InjectView(R.id.feedbackText)
    TextView feedbackText;

    @InjectView(R.id.limitText)
    TextView limitText;

    @InjectView(R.id.volumeText)
    TextView volumeText;
    
    @OnClick(R.id.requestButton)
    public void requestButtonClicked()
    {
        showTradeRequest();
    }
    
    private String adId;
    
    private AdvertisementData advertisementData;

    private class AdvertisementData {
        public Advertisement advertisement;
        public MethodItem method;
    }

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
        
        subscribeData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(EXTRA_AD_ID, adId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.advertiser);

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(toolbar != null)
            subscribeData();
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
    
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId()) {
                    case R.id.action_profile:
                        showProfile();
                        return true;
                    case R.id.action_location:
                        showAdvertisementOnMap();
                        return true;
                    case R.id.action_website:
                        showPublicAdvertisement();
                        return true;
                }
                return false;
            }
        });
    }
    
    public void showContent(final boolean show)
    {
        if (progress == null || content == null)
            return;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progress.setVisibility(show ? View.GONE : View.VISIBLE);
        progress.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (progress != null)
                    progress.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        content.setVisibility(show ? View.VISIBLE : View.GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if (content != null)
                    content.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    protected void subscribeData()
    {
        Observable.combineLatest(dbManager.methodQuery().cache(), dataService.getAdvertisement(adId).cache(), new Func2<List<MethodItem>, Advertisement, AdvertisementData>()
        {
            @Override
            public AdvertisementData call(List<MethodItem> methods, Advertisement advertisement)
            {
                MethodItem method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
                advertisementData = new AdvertisementData();
                advertisementData.method = method;
                advertisementData.advertisement = advertisement;
                return advertisementData;
            }
        })
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Advertisement and method subscription safely unsubscribed");
                    }
                })
                .compose(this.<AdvertisementData>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AdvertisementData>()
                {
                    @Override
                    public void call(AdvertisementData advertisementData)
                    {
                        showContent(true);
                        if (TradeUtils.isOnlineTrade(advertisementData.advertisement)) {
                            setAdvertisement(advertisementData.advertisement, advertisementData.method);
                        } else {
                            setAdvertisement(advertisementData.advertisement, null);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                        toast("Unable to retrieve advertisement.");
                    }
                });
    }

    public void setAdvertisement(Advertisement advertisement, MethodItem method)
    {
        setHeader(advertisement.trade_type);
        String location = advertisement.location;
        String provider = TradeUtils.getPaymentMethod(advertisement, method);
        
        switch (advertisement.trade_type) {
            case ONLINE_SELL:
                noteTextAdvertiser.setText(Html.fromHtml(getString(R.string.advertiser_notes_text_online, "sell", advertisement.currency, provider)));
                break;
            case LOCAL_SELL:
                noteTextAdvertiser.setText(Html.fromHtml(getString(R.string.advertiser_notes_text_locally, "sell", advertisement.currency, location)));
                break;
            case ONLINE_BUY:
                noteTextAdvertiser.setText(Html.fromHtml(getString(R.string.advertiser_notes_text_online, "buy your", advertisement.currency, provider)));
                break;
            case LOCAL_BUY:
                noteTextAdvertiser.setText(Html.fromHtml(getString(R.string.advertiser_notes_text_locally, "buy your", advertisement.currency, location)));
                break;
        }

        if(advertisement.isATM()) {
            priceLayout.setVisibility(View.GONE);
            priceLayoutDivider.setVisibility(View.GONE);
            tradePrice.setText("ATM");
            noteTextAdvertiser.setText(Html.fromHtml(getString(R.string.advertiser_notes_text_atm, advertisement.currency, location)));
        } else {
            tradePrice.setText(getString(R.string.trade_price, advertisement.temp_price, advertisement.currency));
        }
        
        traderName.setText(advertisement.profile.username);
        
        if(advertisement.isATM()) {
            
            tradeLimit.setText("");
            
        } else {

            if(advertisement.max_amount != null && advertisement.min_amount != null) {
                tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount, advertisement.max_amount, advertisement.currency));
            }
            
            if(advertisement.max_amount == null && advertisement.min_amount != null) {
                tradeLimit.setText(getString(R.string.trade_limit_min, advertisement.min_amount, advertisement.currency));
            }

            if (advertisement.max_amount_available != null && advertisement.min_amount != null){ // no maximum set
                tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount, advertisement.max_amount_available, advertisement.currency));
            } else if (advertisement.max_amount_available != null) {
                tradeLimit.setText(getString(R.string.trade_limit_max, advertisement.max_amount_available, advertisement.currency));
            }
        }
        
        if(!Strings.isBlank(advertisement.message)){
            tradeTerms.setText(advertisement.message.trim());
            tradeTerms.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        }

        tradeFeedback.setText(advertisement.profile.feedback_score);
        tradeCount.setText(advertisement.profile.trade_count);
        lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(advertisement.profile.last_online));
        String date = Dates.parseLocalDateStringAbbreviatedTime(advertisement.profile.last_online);
        dateText.setText("Last Seen - " + date);

        setTradeRequirements(advertisement);
    }

    /**
     * Toggles the trader requirements and options visibility
     * @param advertisement <code>Advertisement</code>
     */
    public void setTradeRequirements(Advertisement advertisement)
    {
        boolean showLayout = false;
        if(advertisement.trusted_required 
                || advertisement.sms_verification_required
                || advertisement.require_identification ) {

            showLayout = true;
        } 

        trustedTextView.setVisibility(advertisement.trusted_required? View.VISIBLE:View.GONE);
        identifiedTextView.setVisibility(advertisement.require_identification ? View.VISIBLE:View.GONE);
        smsTextView.setVisibility(advertisement.sms_verification_required? View.VISIBLE:View.GONE);

        if(!Strings.isBlank(advertisement.require_feedback_score) && TradeUtils.isOnlineTrade(advertisement)) {
            feedbackText.setVisibility(View.VISIBLE);
            feedbackText.setText(Html.fromHtml(getString(R.string.trade_request_minimum_feedback_score, advertisement.require_feedback_score)));
            showLayout = true;
        } else {
            feedbackText.setVisibility(View.GONE);
        }

        if(!Strings.isBlank(advertisement.require_trade_volume) && TradeUtils.isOnlineTrade(advertisement)) {
            volumeText.setVisibility(View.VISIBLE);
            volumeText.setText(Html.fromHtml(getString(R.string.trade_request_minimum_volume, advertisement.require_trade_volume)));
            showLayout = true;
        } else {
            volumeText.setVisibility(View.GONE);
        }
        
        if(!Strings.isBlank(advertisement.first_time_limit_btc) && TradeUtils.isOnlineTrade(advertisement)) {
            limitText.setVisibility(View.VISIBLE);
            limitText.setText(Html.fromHtml(getString(R.string.trade_request_new_buyer_limit, advertisement.first_time_limit_btc)));
            showLayout = true;
        } else {
            limitText.setVisibility(View.GONE);
        }
        
        requirementsLayout.setVisibility(showLayout? View.VISIBLE:View.GONE);
    }

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

        if(toolbar != null)
            toolbar.setTitle(header);
    }
    
    public void showTradeRequest()
    {
        if(advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        Intent intent = TradeRequestActivity.createStartIntent(this, advertisement.ad_id, 
                advertisement.trade_type, advertisement.country_code, advertisement.online_provider,
                advertisement.temp_price, advertisement.min_amount, 
                advertisement.max_amount_available, advertisement.currency, 
                advertisement.profile.username);
        startActivity(intent);
    }
    
    public void showPublicAdvertisement()
    {
        if(advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.actions.public_view));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException exception) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
    }
    
    public void showProfile()
    {
        if(advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        String url = "https://localbitcoins.com/accounts/profile/" + advertisement.profile.username + "/";
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException exception) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
    }
    
    public void showAdvertisementOnMap()
    {
        if(advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        String geoUri = "";
        if(advertisement.trade_type == TradeType.LOCAL_BUY || advertisement.trade_type == TradeType.LOCAL_SELL) {
            geoUri = "http://maps.google.com/maps?q=loc:" + advertisement.lat + "," + advertisement.lon + " (" + advertisement.location + ")";
        } else {
            geoUri = "geo:0,0?q=" + advertisement.location;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
    }
}