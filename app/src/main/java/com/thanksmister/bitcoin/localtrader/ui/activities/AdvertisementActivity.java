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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.network.services.SyncProvider;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;


public class AdvertisementActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    // The loader's unique id. Loader ids are specific to the Activity or
    private static final int ADVERTISEMENT_LOADER_ID = 1;
    private static final int METHOD_LOADER_ID = 2;

    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";
    public static final int REQUEST_CODE = 10939;
    public static final int RESULT_DELETED = 837373;

    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @Inject
    BriteDatabase db;

    @BindView(R.id.tradePrice)
    TextView tradePrice;

    @BindView(R.id.noteTextAdvertisement)
    TextView noteTextAdvertisement;

    @BindView(R.id.tradeLimit)
    TextView tradeLimit;

    @BindView(R.id.tradeTerms)
    TextView tradeTerms;

    @BindView(R.id.priceEquation)
    TextView priceEquation;

    @BindView(R.id.onlineProvider)
    TextView onlineProvider;

    @BindView(R.id.bankName)
    TextView bankName;

    @BindView(R.id.paymentDetails)
    TextView paymentDetails;

    @BindView(R.id.noteText)
    TextView noteText;

    @BindView(R.id.advertisementId)
    TextView advertisementId;

    @BindView(R.id.noteLayout)
    View noteLayout;

    @BindView(R.id.bankNameLayout)
    View bankNameLayout;

    @BindView(R.id.termsLayout)
    View termsLayout;

    @BindView(R.id.paymentDetailsLayout)
    View paymentDetailsLayout;

    @BindView(R.id.onlinePaymentLayout)
    View onlinePaymentLayout;

    @BindView(R.id.advertisementContent)
    View content;

    @BindView(R.id.advertisementToolBar)
    Toolbar toolbar;

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

    @BindView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private String adId;
    private Menu menu;
    private List<MethodItem> methodItems;
    private AdvertisementItem advertisement;
    private Handler handler;

    public static Intent createStartIntent(Context context, @NonNull String adId) {
        Intent intent = new Intent(context, AdvertisementActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_advertisement);

        ButterKnife.bind(this);

        handler = new Handler();

        if (savedInstanceState == null && getIntent().hasExtra(EXTRA_AD_ID)) {
            adId = getIntent().getStringExtra(EXTRA_AD_ID);
        } else if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_AD_ID)) {
            adId = savedInstanceState.getString(EXTRA_AD_ID);
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
                setToolBarMenu(toolbar);
            }
        }

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        if (TextUtils.isEmpty(adId)) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), getString(R.string.error_no_advertisement)), new Action0() {
                @Override
                public void call() {
                    finish();
                }
            });
        }
    }

    // Bug: http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
        outState.putString(EXTRA_AD_ID, adId);
        super.onSaveInstanceState(outState);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == EditAdvertisementActivity.REQUEST_CODE) {
            if (resultCode == EditAdvertisementActivity.RESULT_UPDATED) {
                updateAdvertisement(); // update the new editAdvertisement
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.advertisement);
        }
        this.menu = menu;
        if (advertisement != null) {
            setMenuVisibilityIcon(advertisement.visible());
        }
        return true;
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
    public void onRefresh() {
        Timber.d("onRefresh");
        updateAdvertisement();
    }

    public void onRefreshStop() {
        handler.removeCallbacks(refreshRunnable);

        if (swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    public void onRefreshStart() {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 50);
    }

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (swipeLayout != null)
                swipeLayout.setRefreshing(true);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(METHOD_LOADER_ID, null, this);
        getSupportLoaderManager().restartLoader(ADVERTISEMENT_LOADER_ID, null, this);
    }

    @Override
    protected void handleNetworkDisconnect() {
        onRefreshStop();
        snack(getString(R.string.error_no_internet), true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getSupportLoaderManager().destroyLoader(METHOD_LOADER_ID);
        getSupportLoaderManager().destroyLoader(ADVERTISEMENT_LOADER_ID);
    }

    @Override
    public void handleRefresh() {
        onRefreshStart();
        updateAdvertisement();
    }

    public void setToolBarMenu(Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_edit:
                        editAdvertisement(advertisement);
                        return true;
                    case R.id.action_share:
                        shareAdvertisement();
                        return true;
                    case R.id.action_delete:
                        deleteAdvertisement();
                        return true;
                    case R.id.action_visible:
                        updateAdvertisementVisibility();
                        return true;
                    case R.id.action_website:
                        viewOnlineAdvertisement();
                        return true;
                    case R.id.action_location:
                        showAdvertisementOnMap();
                        return true;
                }
                return false;
            }
        });
    }

    private void updateAdvertisement() {
        toast(getString(R.string.toast_refreshing_data));
        dataService.getAdvertisement(adId)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Update editAdvertisement subscription safely unsubscribed");
                    }
                })
                .compose(this.<Advertisement>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Advertisement>() {
                    @Override
                    public void call(Advertisement advertisement) {
                        if (advertisement != null) {
                            dbManager.updateAdvertisement(advertisement);
                        }
                        onRefreshStop();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable, true);
                        onRefreshStop();
                    }
                });
    }

    public void setAdvertisement(AdvertisementItem advertisement, List<MethodItem> methods) {

        MethodItem method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
        tradePrice.setText(getString(R.string.trade_price, advertisement.temp_price(), advertisement.currency()));

        String price = advertisement.currency();
        TradeType tradeType = TradeType.valueOf(advertisement.trade_type());
        String title = "";
        switch (tradeType) {
            case LOCAL_BUY:
                title = getString(R.string.text_advertisement_local_buy);
                break;
            case LOCAL_SELL:
                title = getString(R.string.text_advertisement_local_sale);
                break;
            case ONLINE_BUY:
                title = getString(R.string.text_advertisement_online_buy);
                break;
            case ONLINE_SELL:
                title = getString(R.string.text_advertisement_online_sale);
                break;
        }

        String location = advertisement.location_string();
        if (TradeUtils.isLocalTrade(advertisement)) {
            noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_locally, title, price, location)));
        } else {
            String paymentMethod = TradeUtils.getPaymentMethod(advertisement, method);
            if (TextUtils.isEmpty(paymentMethod)) {
                noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_online_location, title, price, location)));
            } else {
                noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_online, title, price, paymentMethod, location)));
            }
        }


        if (advertisement.atm_model() != null) {
            tradeLimit.setText("");
        } else if (advertisement.min_amount() == null) {
            tradeLimit.setText("");
        } else if (advertisement.max_amount() == null) {
            tradeLimit.setText(getString(R.string.trade_limit_min, advertisement.min_amount(), advertisement.currency()));
        } else { // no maximum set
            tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount(), advertisement.max_amount(), advertisement.currency()));
        }

        priceEquation.setText(advertisement.price_equation());

        if (!TextUtils.isEmpty(advertisement.message())) {
            tradeTerms.setText(advertisement.message().trim());
        } else {
            termsLayout.setVisibility(View.GONE);
        }

        if (TradeUtils.isOnlineTrade(advertisement)) {

            String paymentMethod = TradeUtils.getPaymentMethodName(advertisement, method);
            onlineProvider.setText(paymentMethod);

            if (!TextUtils.isEmpty(advertisement.bank_name())) {
                bankName.setText(advertisement.bank_name());
            } else {
                bankNameLayout.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(advertisement.account_info())) {
                paymentDetails.setText(advertisement.account_info().trim());
            } else {
                paymentDetailsLayout.setVisibility(View.GONE);
            }
        } else {
            onlinePaymentLayout.setVisibility(View.GONE);
            paymentDetailsLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
        }

        advertisementId.setText(advertisement.ad_id());
        setTradeRequirements(advertisement);
        updateAdvertisementNote(advertisement);
    }

    /**
     * Toggles the trader requirements and options visibility
     *
     * @param advertisement <code>AdvertisementItem</code>
     */
    public void setTradeRequirements(AdvertisementItem advertisement) {
        boolean showLayout = false;
        if (advertisement.trusted_required()
                || advertisement.sms_verification_required()
                || advertisement.require_identification()) {

            showLayout = true;
        }

        trustedTextView.setVisibility(advertisement.trusted_required() ? View.VISIBLE : View.GONE);
        identifiedTextView.setVisibility(advertisement.require_identification() ? View.VISIBLE : View.GONE);
        smsTextView.setVisibility(advertisement.sms_verification_required() ? View.VISIBLE : View.GONE);

        if (!Strings.isBlank(advertisement.require_feedback_score()) && TradeUtils.isOnlineTrade(advertisement)) {
            feedbackText.setVisibility(View.VISIBLE);
            feedbackText.setText(Html.fromHtml(getString(R.string.trade_request_minimum_feedback_score, advertisement.require_feedback_score())));
            showLayout = true;
        } else {
            feedbackText.setVisibility(View.GONE);
        }

        if (!Strings.isBlank(advertisement.require_trade_volume()) && TradeUtils.isOnlineTrade(advertisement)) {
            volumeText.setVisibility(View.VISIBLE);
            volumeText.setText(Html.fromHtml(getString(R.string.trade_request_minimum_volume, advertisement.require_trade_volume())));
            showLayout = true;
        } else {
            volumeText.setVisibility(View.GONE);
        }

        if (!Strings.isBlank(advertisement.first_time_limit_btc()) && TradeUtils.isOnlineTrade(advertisement)) {
            limitText.setVisibility(View.VISIBLE);
            limitText.setText(Html.fromHtml(getString(R.string.trade_request_new_buyer_limit, advertisement.first_time_limit_btc())));
            showLayout = true;
        } else {
            limitText.setVisibility(View.GONE);
        }

        requirementsLayout.setVisibility(showLayout ? View.VISIBLE : View.GONE);
    }

    public void updateAdvertisementNote(AdvertisementItem advertisement) {
        noteLayout.setVisibility(advertisement.visible() ? View.GONE : View.VISIBLE);
        noteText.setText(getString(R.string.advertisement_invisible_warning));
        setMenuVisibilityIcon(advertisement.visible());
    }

    private void setMenuVisibilityIcon(boolean show) {
        int icon;
        if (show) {
            icon = R.drawable.ic_action_visibility;
        } else {
            icon = R.drawable.ic_action_visibility_off;
        }

        if (menu != null && menu.hasVisibleItems()) {
            MenuItem menuItem = menu.getItem(0);
            if (menuItem != null)
                menuItem.setIcon(icon);
        }

        onRefreshStop();
    }

    private void viewOnlineAdvertisement() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.action_public_view()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showAlertDialog(getString(R.string.toast_error_no_installed_ativity));
        }
    }

    private void deleteAdvertisement() {
        ConfirmationDialogEvent event = new ConfirmationDialogEvent("Delete Advertisement",
                getString(R.string.advertisement_delete_confirm),
                getString(R.string.button_delete),
                getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                deleteAdvertisementConfirmed(adId);
            }
        });

        showConfirmationDialog(event);
    }

    private void deleteAdvertisementConfirmed(final String adId) {
        showProgressDialog(new ProgressDialogEvent(getString(R.string.progress_deleting)));
        dataService.deleteAdvertisement(adId)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Delete editAdvertisement safely unsubscribed");
                    }
                })
                .compose(this.<Boolean>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(final Boolean deleted) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                if (deleted) {
                                    dbManager.deleteAdvertisement(adId);
                                    toast(getString(R.string.toast_advertisement_deleted));
                                    setResult(RESULT_DELETED);
                                    finish();
                                } else {
                                    showAlertDialog(getString(R.string.alert_error_deleting_advertisement));
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                showAlertDialog(getString(R.string.alert_error_deleting_advertisement));
                            }
                        });
                    }
                });
    }

    private void updateAdvertisementVisibility() {

        showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_updating_visibility)));

        Advertisement editAdvertisement = new Advertisement();
        editAdvertisement = editAdvertisement.convertAdvertisementItemToAdvertisement(advertisement);

        final String adId = editAdvertisement.ad_id;
        final boolean visible = !editAdvertisement.visible;

        dataService.updateAdvertisementVisibility(editAdvertisement, visible)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject jsonObject) {
                        hideProgressDialog();
                        dbManager.updateAdvertisementVisibility(adId, visible);
                        toast(getString(R.string.toast_update_visibility));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        showAlertDialog(throwable.getMessage());
                    }
                });
    }

    private void showAdvertisementOnMap() {
        String geoUri = "";
        if (TradeUtils.isLocalTrade(advertisement)) {
            geoUri = "http://maps.google.com/maps?q=loc:" + advertisement.lat() + "," + advertisement.lon() + " (" + advertisement.location_string() + ")";
        } else {
            geoUri = "geo:0,0?q=" + advertisement.location_string();
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            toast(getString(R.string.toast_no_activity_for_maps));
        }
    }

    private void shareAdvertisement() {

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String message = "";
        String buyOrSell = (TradeUtils.isBuyTrade(advertisement)) ? getString(R.string.text_buy) : getString(R.string.text_sell);
        String prep = (TradeUtils.isSellTrade(advertisement)) ? getString(R.string.text_from) : getString(R.string.text_to);
        String onlineOrLocal = (TradeUtils.isLocalTrade(advertisement)) ? getString(R.string.text_locally) : getString(R.string.text_online);

        if (TradeUtils.isLocalTrade(advertisement)) {
            message = getString(R.string.text_advertisement_message_short, buyOrSell, onlineOrLocal, (advertisement.location_string() + prep + advertisement.profile_username()), advertisement.action_public_view());
        } else {
            String provider = TradeUtils.parsePaymentServiceTitle(advertisement.online_provider());
            message = getString(R.string.text_advertisement_message, buyOrSell, onlineOrLocal, (advertisement.location_string() + prep + advertisement.profile_username()), provider, advertisement.action_public_view());
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.text_share_advertisement, buyOrSell, advertisement.location_string()));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.text_chooser_share_to)));
    }

    private void editAdvertisement(AdvertisementItem advertisement) {
        Intent intent = EditAdvertisementActivity.createStartIntent(AdvertisementActivity.this, advertisement.ad_id());
        startActivityForResult(intent, EditAdvertisementActivity.REQUEST_CODE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == ADVERTISEMENT_LOADER_ID && !TextUtils.isEmpty(adId)) {
            return new CursorLoader(AdvertisementActivity.this, SyncProvider.ADVERTISEMENT_TABLE_URI, null, AdvertisementItem.AD_ID + " = ?", new String[]{adId}, null);
        } else if (id == METHOD_LOADER_ID) {
            return new CursorLoader(AdvertisementActivity.this, SyncProvider.METHOD_TABLE_URI, null, null, null, MethodItem.KEY);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ADVERTISEMENT_LOADER_ID:
                advertisement = AdvertisementItem.getModel(cursor);
                if (advertisement != null && methodItems != null) {
                    setAdvertisement(advertisement, methodItems);
                } else if (advertisement == null) {
                    updateAdvertisement(); // TODO make sure this doesn't just randomly run
                }
                break;
            case METHOD_LOADER_ID:
                methodItems = MethodItem.getModelList(cursor);
                if (advertisement != null && !methodItems.isEmpty()) {
                    setAdvertisement(advertisement, methodItems);
                }
                break;
            default:
                throw new Error("Incorrect loader Id");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}