package com.thanksmister.bitcoin.localtrader.ui.advertisement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditActivity;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AdvertisementActivity extends BaseActivity implements AdvertisementView
{
    public static final String EXTRA_AD = "com.thanksmister.extras.EXTRA_AD";
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";
    public static final String EXTRA_TRADE_TYPE = "com.thanksmister.extras.EXTRA_TRADE_TYPE";
    public static final String EXTRA_METHOD = "com.thanksmister.extras.EXTRA_METHOD";

    @Inject
    AdvertisementPresenter presenter;

    @InjectView(R.id.tradePrice)
    TextView tradePrice;

    @InjectView(R.id.noteTextAdvertisement)
    TextView noteTextAdvertisement;

    @InjectView(R.id.tradeLimit)
    TextView tradeLimit;

    @InjectView(R.id.tradeTerms)
    TextView tradeTerms;

    @InjectView(R.id.priceEquation)
    TextView priceEquation;

    @InjectView(R.id.onlineProvider)
    TextView onlineProvider;

    @InjectView(R.id.paymentDetails)
    TextView paymentDetails;

    @InjectView(R.id.noteText)
    TextView noteText;

    @InjectView(R.id.noteLayout)
    View noteLayout;

    @InjectView(R.id.onlinePaymentLayout)
    View onlinePaymentLayout;

    @InjectView(R.id.advertisementProgress)
    View progress;

    @InjectView(R.id.advertisementEmpty)
    View empty;

    @InjectView(R.id.retryTextView)
    TextView errorTextView;

    @InjectView(R.id.advertisementMainView)
    View content;

    @InjectView(R.id.advertisementToolBar)
    Toolbar toolbar;
    
    private String adId;
    private Menu menu;

    public static Intent createStartIntent(Context context, String adId, TradeType tradeType, String methodKey)
    {
        Intent intent = new Intent(context, AdvertisementActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        intent.putExtra(EXTRA_TRADE_TYPE, tradeType);
        intent.putExtra(EXTRA_METHOD, methodKey);
        return intent;
    }

    public static Intent createStartIntent(Context context, String adId)
    {
        Intent intent = new Intent(context, AdvertisementActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_advertisement);

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
        outState.putString(EXTRA_AD_ID, adId);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == EditActivity.REQUEST_CODE) {
            if (resultCode == EditActivity.RESULT_UPDATED) {
                presenter.getAdvertisement(adId); // refresh advertisement
            }
        }
    }

    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new AdvertisementModule(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.advertisement);

        this.menu = menu;

        return true;
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
                switch (menuItem.getItemId()){
                    case R.id.action_edit:
                        presenter.editAdvertisement();
                        return true;
                    case R.id.action_share:
                        presenter.shareAdvertisement();
                        return true;
                    case R.id.action_delete:
                        presenter.deleteAdvertisement();
                        return true;
                    case R.id.action_visible:
                        presenter.updateAdvertisementVisibility();
                        return true;
                    case R.id.action_website:
                        presenter.viewOnlineAdvertisement();
                        return true;
                    case R.id.action_location:
                        presenter.showAdvertisementOnMap();
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
        errorTextView.setText(message);
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

    @Override
    public void setAdvertisement(Advertisement advertisement, @Nullable Method method)
    {
        tradePrice.setText(getString(R.string.trade_price, advertisement.price, advertisement.currency));

        String price = advertisement.currency;
        String date = Dates.parseLocalDateStringAbbreviatedDate(advertisement.created_at);

        String title = "";
        switch (advertisement.trade_type) {
            case LOCAL_BUY:
                title = "Local buy";
                break;
            case LOCAL_SELL:
                title = "Local sale";
                break;
            case ONLINE_BUY:
                title = "Online buy";
                break;
            case ONLINE_SELL:
                title = "Online sale";
                break;
        }

        String location = advertisement.location;
        if (TradeUtils.isLocalTrade(advertisement)) {
            noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_locally, title, price, location)));
        } else {
            String paymentMethod = TradeUtils.getPaymentMethod(advertisement, method);
            noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_online, title, price, paymentMethod, advertisement.city)));
        }

        if(advertisement.trade_type == TradeType.LOCAL_SELL || advertisement.trade_type == TradeType.LOCAL_BUY) {
            onlinePaymentLayout.setVisibility(View.GONE);
        }

        if(advertisement.max_amount == null) {
            tradeLimit.setText(getString(R.string.trade_limit_min, advertisement.min_amount, advertisement.currency));
        } else { // no maximum set
            tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount, advertisement.max_amount, advertisement.currency));
        }

        priceEquation.setText(advertisement.price_equation);

        if(advertisement.msg != null && !advertisement.msg.isEmpty()){
            tradeTerms.setText(Html.fromHtml(advertisement.msg.trim()));
            tradeTerms.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if(advertisement.trade_type == TradeType.ONLINE_SELL) {
            String paymentMethod = TradeUtils.getPaymentMethodName(advertisement, method);
            onlineProvider.setText(paymentMethod);
            paymentDetails.setText(advertisement.account_info);
        }

        updateAdvertisement(advertisement);
    }

    @Override
    public void updateAdvertisement(Advertisement advertisement)
    {
        noteLayout.setVisibility(advertisement.visible?View.GONE:View.VISIBLE);
        noteText.setText(getString(R.string.advertisement_invisible_warning));
        setMenuVisibilityIcon(advertisement.visible);
    }

    private void setMenuVisibilityIcon(boolean show)
    {
        int icon;
        if(show) {
            icon = R.drawable.ic_action_visibility;
        } else {
            icon = R.drawable.ic_action_visibility_off;
        }

        if(menu != null)
            menu.getItem(0).setIcon(icon);
    }
}
