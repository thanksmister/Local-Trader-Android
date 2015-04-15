package com.thanksmister.bitcoin.localtrader.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditActivity;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindActivity;

public class AdvertisementActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";

    @Inject
    DbManager dbManager;

    @Inject
    SqlBrite db;
    
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

    @InjectView(R.id.advertisementMainView)
    View content;

    @InjectView(R.id.advertisementToolBar)
    Toolbar toolbar;
    
    private AdvertisementItem advertisement;
    private String adId;
    private Menu menu;

    

    private class AdvertisementData {
       public AdvertisementItem advertisement;
       public MethodItem method;
    }

    private Subscription subscription = Subscriptions.empty();
    private Observable<AdvertisementItem> advertisementObservable;
    private Observable<List<MethodItem>> methodObservable;
    private Observable<Boolean> deleteObservable;
    private Observable<Boolean> updateObservable;

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

        methodObservable = bindActivity(this, dbManager.methodQuery().cache());
        advertisementObservable = bindActivity(this, dbManager.advertisementItemQuery(adId));
        deleteObservable = bindActivity(this, dbManager.deleteAdvertisement(adId));

        subscribeData();
    }
    

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_AD_ID, adId);
    }

    /*public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == EditActivity.REQUEST_CODE) {
            if (resultCode == EditActivity.RESULT_UPDATED) {
                //presenter.getAdvertisement(adId); // refresh advertisement
            }
        }
    }*/

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
    public void onRefresh()
    {
        // TODO load advertisement data manuall
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        // TODO load advertisement data manuall
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        
        subscription.unsubscribe();
    }
    
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId()) {
                    case R.id.action_edit:
                        editAdvertisement();
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
    
    public Context getContext()
    {
        return this;
    }

    public void showError()
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
    }
    
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }
    
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    protected void subscribeData()
    {
        subscription = Observable.combineLatest(methodObservable, advertisementObservable, new Func2<List<MethodItem>, AdvertisementItem, AdvertisementData>()
        {
            @Override
            public AdvertisementData call(List<MethodItem> methods, AdvertisementItem advertisement)
            {
                AdvertisementData advertisementData = new AdvertisementData();
                MethodItem method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
                advertisementData.method = method;
                advertisementData.advertisement = advertisement;
                return advertisementData;
            }
        }).subscribe(new Action1<AdvertisementData>()
        {
            @Override
            public void call(AdvertisementData advertisementData)
            {
                hideProgress();
                advertisement = advertisementData.advertisement; // store local advertisement
                setAdvertisement(advertisementData.advertisement, advertisementData.method);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                showError();
                toast("Unable to retrieve advertisement data.");
            }
        });
    }
    
    public void setAdvertisement(AdvertisementItem advertisement, @Nullable MethodItem method)
    {
        tradePrice.setText(getString(R.string.trade_price, advertisement.temp_price(), advertisement.currency()));

        String price = advertisement.currency();
        String date = Dates.parseLocalDateStringAbbreviatedDate(advertisement.created_at());

        TradeType tradeType = TradeType.valueOf(advertisement.trade_type());
        String title = "";
        switch (tradeType) {
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

        String location = advertisement.location_string();
        if (TradeUtils.isLocalTrade(advertisement)) {
            noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_locally, title, price, location)));
        } else {
            String paymentMethod = TradeUtils.getPaymentMethod(advertisement, method);
            noteTextAdvertisement.setText(Html.fromHtml(getString(R.string.advertisement_notes_text_online, title, price, paymentMethod, advertisement.location_string())));
        }

        if(TradeUtils.isLocalTrade(advertisement)) {
            onlinePaymentLayout.setVisibility(View.GONE);
        }

        if(advertisement.max_amount() == null) {
            tradeLimit.setText(getString(R.string.trade_limit_min, advertisement.min_amount(), advertisement.currency()));
        } else { // no maximum set
            tradeLimit.setText(getString(R.string.trade_limit, advertisement.min_amount(), advertisement.max_amount(), advertisement.currency()));
        }

        priceEquation.setText(advertisement.price_equation());
        
        /*if(!Strings.isBlank(advertisement.message())){
            String message = advertisement.message().trim();
            //message = message.replace("\n", "").replace("\r", "<br>");
            tradeTerms.setText(Html.fromHtml(message));
            tradeTerms.setMovementMethod(LinkMovementMethod.getInstance());
        }*/

        if(!Strings.isBlank(advertisement.message())){
            tradeTerms.setText(Html.fromHtml(advertisement.message().trim()));
            tradeTerms.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if(tradeType == TradeType.ONLINE_SELL) {
            String paymentMethod = TradeUtils.getPaymentMethodName(advertisement, method);
            onlineProvider.setText(paymentMethod);
            paymentDetails.setText(advertisement.account_info());
        }

        updateAdvertisement(advertisement);
    }
    
    public void updateAdvertisement(AdvertisementItem advertisement)
    {
        noteLayout.setVisibility(advertisement.visible()?View.GONE:View.VISIBLE);
        noteText.setText(getString(R.string.advertisement_invisible_warning));
        setMenuVisibilityIcon(advertisement.visible());
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

    public void viewOnlineAdvertisement()
    {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.action_public_view()));
        startActivity(intent);
    }

    public void deleteAdvertisement()
    {
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

    private void deleteAdvertisementConfirmed(final String adId)
    {
        deleteObservable = bindActivity(this, dbManager.deleteAdvertisement(adId));
        deleteObservable.subscribe(new Action1<Boolean>()
        {
            @Override
            public void call(Boolean aBoolean)
            {
                db.delete(AdvertisementItem.TABLE, AdvertisementItem.QUERY_DELETE_ITEM, adId);
                Toast.makeText(getContext(), "Advertisement deleted!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e("Error deleting: " + throwable.getMessage());
                Toast.makeText(getContext(), "Error deleting advertisement!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateAdvertisementVisibility()
    {
        boolean visible = !advertisement.visible();
        updateObservable = bindActivity(this, dbManager.updateAdvertisementVisibility(advertisement, visible));
        updateObservable.subscribe(new Action1<Boolean>()
        {
            @Override
            public void call(Boolean aBoolean)
            {
                db.update(AdvertisementItem.TABLE, new AdvertisementItem.Builder().visible(visible).build(), AdvertisementItem.AD_ID + " = ?", String.valueOf(adId));
                Toast.makeText(getContext(), "Visibility updated!", Toast.LENGTH_SHORT).show();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e("Error updating: " + throwable.getMessage());
                Toast.makeText(getContext(), "Error updating visibility!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void showAdvertisementOnMap()
    {
        String geoUri = "";
        if(TradeUtils.isLocalTrade(advertisement)) {
            geoUri = "http://maps.google.com/maps?q=loc:" + advertisement.lat() + "," + advertisement.lon() + " (" + advertisement.location_string() + ")";
        } else {
            geoUri = "geo:0,0?q=" + advertisement.location_string();
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        startActivity(intent);
    }

    public void shareAdvertisement()
    {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String message = "";
        String buyOrSell = (TradeUtils.isBuyTrade(advertisement))? "Buy":"Sell";
        String prep = (TradeUtils.isSellTrade(advertisement))? " from ":" to ";
        String onlineOrLocal = (TradeUtils.isLocalTrade(advertisement))? "locally":"online";

        if(TradeUtils.isLocalTrade(advertisement)) {
            message = buyOrSell + " bitcoins " + onlineOrLocal + " in " + advertisement.location_string() + prep + advertisement.profile_username()  + ": " + advertisement.action_public_view();
        } else {
            String provider = TradeUtils.parsePaymentServiceTitle(advertisement.online_provider());
            message = buyOrSell + " bitcoins " + onlineOrLocal + " in " + advertisement.location_string() + prep + advertisement.profile_username() + " via "  + provider + ": " + advertisement.action_public_view();
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, buyOrSell + " bitcoins in " + advertisement.location_string());
        startActivity(Intent.createChooser(shareIntent, "Share to:"));
    }

    public void editAdvertisement()
    {
        Intent intent = EditActivity.createStartIntent(getContext(), false, adId);
        startActivity(intent);
    }
}
