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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class AdvertiserActivity extends BaseActivity {

    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";

    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @BindView(R.id.advertiserProgress)
    View progress;

    @BindView(R.id.advertiserContent)
    ScrollView content;

    @BindView(R.id.priceLayout)
    View priceLayout;

    @BindView(R.id.priceLayoutDivider)
    View priceLayoutDivider;

    @BindView(R.id.advertiserToolbar)
    Toolbar toolbar;

    @BindView(R.id.tradePrice)
    TextView tradePrice;

    @BindView(R.id.traderName)
    TextView traderName;

    @BindView(R.id.tradeLimit)
    TextView tradeLimit;

    @BindView(R.id.tradeTerms)
    TextView tradeTerms;

    @BindView(R.id.tradeFeedback)
    TextView tradeFeedback;

    @BindView(R.id.tradeCount)
    TextView tradeCount;

    @Nullable
    @BindView(R.id.dateText)
    TextView dateText;

    @BindView(R.id.noteTextAdvertiser)
    TextView noteTextAdvertiser;

    @BindView(R.id.lastSeenIcon)
    View lastSeenIcon;

    @BindView(R.id.requirementsLayout)
    View requirementsLayout;

    @BindView(R.id.trustedTextView)
    TextView trustedTextView;

    @BindView(R.id.identifiedTextView)
    TextView identifiedTextView;

    @BindView(R.id.smsTextView)
    TextView smsTextView;

    @BindView(R.id.feedbackText)
    TextView feedbackText;

    @BindView(R.id.limitText)
    TextView limitText;

    @BindView(R.id.volumeText)
    TextView volumeText;

    @BindView(R.id.requestButton)
    Button requestButton;

    @OnClick(R.id.requestButton)
    public void requestButtonClicked() {
        showTradeRequest();
    }

    private String adId;

    private AdvertisementData advertisementData;

    private class AdvertisementData {
        public Advertisement advertisement;
        public MethodItem method;
    }

    public static Intent createStartIntent(Context context, String adId) {
        Intent intent = new Intent(context, AdvertiserActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_advertiser);

        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            adId = getIntent().getStringExtra(EXTRA_AD_ID);
        } else {
            adId = savedInstanceState.getString(EXTRA_AD_ID);
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }
            setToolBarMenu(toolbar);
        }

        requestButton.setEnabled(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_AD_ID, adId);
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
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.advertiser);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void setToolBarMenu(Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
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

    public void showContent(final boolean show) {
        if (progress == null || content == null) {
            return;
        }
        progress.setVisibility(show ? View.GONE : View.VISIBLE);
        content.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    protected void subscribeData() {
        Observable.combineLatest(
                dbManager.methodQuery(),
                dataService.getAdvertisement(adId),
                new Func2<List<MethodItem>, Advertisement, AdvertisementData>() {
                    @Override
                    public AdvertisementData call(List<MethodItem> methods, Advertisement advertisement) {
                        MethodItem method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
                        advertisementData = new AdvertisementData();
                        advertisementData.method = method;
                        advertisementData.advertisement = advertisement;
                        return advertisementData;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Advertisement and method subscription safely unsubscribed");
                    }
                })
                .compose(this.<AdvertisementData>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AdvertisementData>() {
                    @Override
                    public void call(AdvertisementData advertisementData) {
                        showContent(true);
                        if (TradeUtils.isOnlineTrade(advertisementData.advertisement)) {
                            setAdvertisement(advertisementData.advertisement, advertisementData.method);
                        } else {
                            setAdvertisement(advertisementData.advertisement, null);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (!BuildConfig.DEBUG) {
                            Crashlytics.setString("advertiser_id", adId);
                            Crashlytics.logException(throwable);
                        }
                        handleError(throwable);
                        //toast(R.string.toast_error_advertisement_data);
                    }
                });
    }

    public void setAdvertisement(Advertisement advertisement, MethodItem method) {

        requestButton.setEnabled(true);
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

        if (advertisement.isATM()) {
            priceLayout.setVisibility(View.GONE);
            priceLayoutDivider.setVisibility(View.GONE);
            tradePrice.setText("ATM");
            noteTextAdvertiser.setText(Html.fromHtml(getString(R.string.advertiser_notes_text_atm, advertisement.currency, location)));
        } else {
            tradePrice.setText(getString(R.string.trade_price, advertisement.temp_price, advertisement.currency));
        }

        traderName.setText(advertisement.profile.username);

        if (advertisement.isATM()) {
            tradeLimit.setText("");
        } else {
            if (advertisement.max_amount != null && advertisement.min_amount != null) {
                tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount, advertisement.max_amount, advertisement.currency));
            }

            if (advertisement.max_amount == null && advertisement.min_amount != null) {
                tradeLimit.setText(getString(R.string.trade_limit_min, advertisement.min_amount, advertisement.currency));
            }

            if (advertisement.max_amount_available != null && advertisement.min_amount != null) { // no maximum set
                tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount, advertisement.max_amount_available, advertisement.currency));
            } else if (advertisement.max_amount_available != null) {
                tradeLimit.setText(getString(R.string.trade_limit_max, advertisement.max_amount_available, advertisement.currency));
            }
        }

        if (!TextUtils.isEmpty(advertisement.message)) {
            tradeTerms.setText(advertisement.message.trim());
        }

        tradeFeedback.setText(advertisement.profile.feedback_score);
        tradeCount.setText(advertisement.profile.trade_count);
        lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(advertisement.profile.last_online));
        String date = Dates.parseLocalDateStringAbbreviatedTime(advertisement.profile.last_online);
        dateText.setText(getString(R.string.text_last_seen, date));

        setTradeRequirements(advertisement);
    }

    /**
     * Toggles the trader requirements and options visibility
     *
     * @param advertisement <code>Advertisement</code>
     */
    public void setTradeRequirements(Advertisement advertisement) {
        boolean showLayout = false;
        if (advertisement.trusted_required
                || advertisement.sms_verification_required
                || advertisement.require_identification) {
            showLayout = true;
        }

        trustedTextView.setVisibility(advertisement.trusted_required ? View.VISIBLE : View.GONE);
        identifiedTextView.setVisibility(advertisement.require_identification ? View.VISIBLE : View.GONE);
        smsTextView.setVisibility(advertisement.sms_verification_required ? View.VISIBLE : View.GONE);

        if (!Strings.isBlank(advertisement.require_feedback_score) && TradeUtils.isOnlineTrade(advertisement)) {
            feedbackText.setVisibility(View.VISIBLE);
            feedbackText.setText(Html.fromHtml(getString(R.string.trade_request_minimum_feedback_score, advertisement.require_feedback_score)));
            showLayout = true;
        } else {
            feedbackText.setVisibility(View.GONE);
        }

        if (!Strings.isBlank(advertisement.require_trade_volume) && TradeUtils.isOnlineTrade(advertisement)) {
            volumeText.setVisibility(View.VISIBLE);
            volumeText.setText(Html.fromHtml(getString(R.string.trade_request_minimum_volume, advertisement.require_trade_volume)));
            showLayout = true;
        } else {
            volumeText.setVisibility(View.GONE);
        }

        if (!Strings.isBlank(advertisement.first_time_limit_btc) && TradeUtils.isOnlineTrade(advertisement)) {
            limitText.setVisibility(View.VISIBLE);
            limitText.setText(Html.fromHtml(getString(R.string.trade_request_new_buyer_limit, advertisement.first_time_limit_btc)));
            showLayout = true;
        } else {
            limitText.setVisibility(View.GONE);
        }

        requirementsLayout.setVisibility(showLayout ? View.VISIBLE : View.GONE);
    }

    public void setHeader(TradeType tradeType) {
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

        if (toolbar != null) {
            toolbar.setTitle(header);
        }
    }

    public void showTradeRequest() {
        if (advertisementData == null || advertisementData.advertisement == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        final TradeType tradeType = advertisement.trade_type;
        if (tradeType == null || TradeType.NONE.name().equals(tradeType.name())) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_title), getString(R.string.error_invalid_trade_type)), new Action0() {
                @Override
                public void call() {
                    if (!BuildConfig.DEBUG) {
                        Crashlytics.log("advertisement_data: " + advertisementData.advertisement.toString());
                        Crashlytics.logException(new Throwable("Bad trade type for requested trade: " + tradeType + " advertisement Id: " + adId));
                    }
                }
            });
            return;
        }

        Intent intent = TradeRequestActivity.createStartIntent(this, advertisement.ad_id,
                advertisement.trade_type, advertisement.country_code, advertisement.online_provider,
                advertisement.temp_price, advertisement.min_amount,
                advertisement.max_amount_available, advertisement.currency,
                advertisement.profile.username);
        startActivity(intent);
    }

    public void showPublicAdvertisement() {
        if (advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.actions.public_view));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException exception) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
    }

    public void showProfile() {
        if (advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        String url = "https://localbitcoins.com/accounts/profile/" + advertisement.profile.username + "/";
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException exception) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
    }

    public void showAdvertisementOnMap() {
        if (advertisementData == null) return;
        Advertisement advertisement = advertisementData.advertisement;
        String geoUri = "";
        if (advertisement.trade_type == TradeType.LOCAL_BUY || advertisement.trade_type == TradeType.LOCAL_SELL) {
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