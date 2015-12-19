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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.ui.bitcoin.QRCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.lang.reflect.Field;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class RequestFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_WALLET = "com.thanksmister.extra.EXTRA_WALLET";
    public static final String EXTRA_EXCHANGE = "com.thanksmister.extra.EXTRA_EXCHANGE";
  
    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;
    
    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    
    @InjectView(R.id.amountText)
    TextView amountText;

    @InjectView(R.id.currencyText)
    TextView currencyText;

    @InjectView(R.id.fiatEditText)
    TextView fiatEditText;
    
    @OnClick(R.id.qrButton)
    public void qrButtonClicked()
    {
        validateForm(walletItem);
    }
    
    private WalletItem walletItem;
    private ExchangeItem exchangeItem;

    CompositeSubscription subscriptions = new CompositeSubscription();
    CompositeSubscription updateSubscriptions = new CompositeSubscription();

    private Handler handler;

    public static RequestFragment newInstance()
    {
        return new RequestFragment();
    }

    public RequestFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            
            if(savedInstanceState.containsKey(EXTRA_WALLET)) {
                walletItem = savedInstanceState.getParcelable(EXTRA_WALLET);
            }

            if(savedInstanceState.containsKey(EXTRA_EXCHANGE)) {
                exchangeItem = savedInstanceState.getParcelable(EXTRA_EXCHANGE);
            }
        }
        
        // refresh handler
        handler = new Handler();
        
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        if(walletItem != null)
            outState.putParcelable(EXTRA_WALLET, walletItem);

        if(exchangeItem != null)
            outState.putParcelable(EXTRA_EXCHANGE, exchangeItem);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.request, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_paste:
                setAmountFromClipboard();
                return true;
            case R.id.action_scan:
                ((BaseActivity) getActivity()).launchScanner();
                return true;
            default:
                break;
        }

        return false;
    }

    private void setCurrency()
    {
        String currency = exchangeService.getExchangeCurrency();

        if(currencyText != null)
            currencyText.setText(currency);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_request, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        amountText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                if (amountText.hasFocus()) {
                    String bitcoin = editable.toString();
                    calculateCurrencyAmount(bitcoin);
                }
            }
        });

        fiatEditText.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3){
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3){
            }
            @Override
            public void afterTextChanged(Editable editable){

                if(fiatEditText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });
        
        setupToolbar();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        subscribeData();
        setCurrency();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();

        handler.removeCallbacks(refreshRunnable);
        subscriptions.unsubscribe();
        updateSubscriptions.unsubscribe();
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
        onRefreshStart();
        updateData();
    }

    public void onRefreshStart()
    {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 1000);
    }

    protected void onRefreshStop()
    {
        handler.removeCallbacks(refreshRunnable);
        swipeLayout.setRefreshing(false);
    }

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run() {
            swipeLayout.setRefreshing(true);
        }
    };

    private void setupToolbar()
    {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);

        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle(getString(R.string.view_title_request));
        ab.setDisplayHomeAsUpEnabled(true);
    }

    protected void subscribeData()
    {
        subscriptions = new CompositeSubscription();
        
        subscriptions.add(dbManager.exchangeQuery().subscribe(new Action1<ExchangeItem>()
        {
            @Override
            public void call(ExchangeItem results)
            {
                exchangeItem = results;
                setWallet(exchangeItem);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                reportError(throwable);
            }
        }));
        
        subscriptions.add(dbManager.walletQuery().subscribe(new Action1<WalletItem>()
        {
            @Override
            public void call(WalletItem results)
            {
                walletItem = results;
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                reportError(throwable);
            }
        }));
    }
    
    private void updateData()
    {
        updateSubscriptions = new CompositeSubscription();
        updateSubscriptions.add(dataService.getWalletBalance()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Wallet>()
                {
                    @Override
                    public void call(Wallet wallet)
                    {
                        onRefreshStop();
                        dbManager.updateWallet(wallet);
                    }

                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        onRefreshStop();
                        handleError(throwable, true);
                    }
                }));

        updateSubscriptions.add(exchangeService.getMarket(true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Exchange>()
                {
                    @Override
                    public void call(Exchange exchange)
                    {
                        dbManager.updateExchange(exchange);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        onRefreshStop();
                        reportError(throwable);
                    }
                }));
    }
    
    private void showGeneratedQrCodeActivity(String bitcoinAddress, String bitcoinAmount)
    {
        assert bitcoinAddress != null;
        Intent intent = QRCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivity(intent);
    }
    
    public void setAmountFromClipboard()
    {
        String clipText = getClipboardText();
        if(Strings.isBlank(clipText)) {
            toast(R.string.toast_clipboard_empty);
            return;
        }

        if(WalletUtils.validAmount(clipText)) {
            setAmount(WalletUtils.parseBitcoinAmount(clipText), exchangeItem);
        } else {
            toast(R.string.toast_invalid_amount);
        }
    }

    private String getClipboardText()
    {
        String clipText = "";
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipData clip = clipboardManager.getPrimaryClip();
            if(clip != null) {
                ClipData.Item item = clip.getItemAt(0);
                if(item.getText() != null)
                    clipText = item.getText().toString();
            }
        } else {
            clipText = clipboardManager.getText().toString();
        }

        return  clipText;
    }
    
    public void setAmount(String bitcoinAmount, ExchangeItem exchangeItem)
    {
        if(!Strings.isBlank(bitcoinAmount)) {
            amountText.setText(bitcoinAmount);
            calculateCurrencyAmount(bitcoinAmount);
        }
    }
    
    public void setWallet(ExchangeItem exchangeItem)
    {
        if(Strings.isBlank(amountText.getText())) {
            calculateCurrencyAmount("0.00");
        } else {
            calculateCurrencyAmount(amountText.getText().toString());
        }
    }

    protected void validateForm(WalletItem walletItem)
    {
        if(walletItem == null) {
            toast("No valid address to receive bitcoin...");
            return;
        }
        
        if(Strings.isBlank(amountText.getText())) {
            toast(getString(R.string.error_missing_amount));
            return;
        }

        String bitcoinAmount = Conversions.formatBitcoinAmount(amountText.getText().toString());
        showGeneratedQrCodeActivity(walletItem.address(), bitcoinAmount);
    }

    private void calculateBitcoinAmount(String requestAmount)
    {
        if(exchangeItem == null) {
            return;
        }

        Timber.d("calculateBitcoinAmount: " + requestAmount);
        
        try {
            if(Doubles.convertToDouble(requestAmount) == 0) {
                amountText.setText("");
                return;
            }
        } catch (Exception e) {
            reportError(e);
            return;
        }
    
        String rate = Calculations.calculateAverageBidAskFormatted(exchangeItem.ask(), exchangeItem.bid());

        double btc = Math.abs(Doubles.convertToDouble(requestAmount) / Doubles.convertToDouble(rate));

        Timber.d("BTC: " + btc);

        String amount = Conversions.formatBitcoinAmount(btc);

        Timber.d("Bitcoin amount: " + amount);

        amountText.setText(amount);
    }

    private void calculateCurrencyAmount(String bitcoin)
    {
        if(exchangeItem == null) {
            return;
        }

        try {
            if( Doubles.convertToDouble(bitcoin) == 0) {
                fiatEditText.setText("");
                return;
            }
        } catch (Exception e) {
            reportError(e);
            return;
        }
        
        String rate = Calculations.calculateAverageBidAskFormatted(exchangeItem.ask(), exchangeItem.bid());
        String value = Calculations.computedValueOfBitcoin(rate, bitcoin);
        fiatEditText.setText(value);
    }
}