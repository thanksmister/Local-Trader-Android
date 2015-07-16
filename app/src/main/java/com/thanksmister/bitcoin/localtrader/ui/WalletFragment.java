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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.misc.AutoResizeTextView;
import com.thanksmister.bitcoin.localtrader.ui.misc.TransactionsAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class WalletFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener
{
    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.fab)
    FloatingActionButton fab;
    
    @InjectView(R.id.walletList)
    ListView list;
    
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @InjectView(R.id.bitcoinTitle)
    TextView bitcoinTitle;

    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;

    @InjectView(R.id.bitcoinLayout)
    View bitcoinLayout;
   
    ImageView qrImage;
    AutoResizeTextView addressButton;
    TextView recentTextView;
    View noActivityTextView;

    CompositeSubscription subscriptions = new CompositeSubscription();
    Subscription subscription = Subscriptions.empty();
    Subscription walletSubscription = Subscriptions.empty();
    Subscription updateSubscription = Subscriptions.empty();
    
    TransactionsAdapter transactionsAdapter;
    Observable<Wallet> walletUpdateObservable;
    Observable<ExchangeItem> exchangeObservable;
    Observable<WalletItem> walletObservable;
   
    private Handler handler;
    private String address;

    private class WalletData {
        public WalletItem wallet;
        public ExchangeItem exchange;
    }

    public static WalletFragment newInstance()
    {
        return new WalletFragment();
    }

    public WalletFragment()
    {
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        handler = new Handler();
     
        walletObservable = bindFragment(this, dbManager.walletQuery());
        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
        walletUpdateObservable = bindFragment(this, dataService.getWallet());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_wallet, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));
        
        View headerView = getLayoutInflater(savedInstanceState).inflate(R.layout.view_wallet_header, null, false);
        list.addHeaderView(headerView, null, false);

        recentTextView = (TextView) headerView.findViewById(R.id.recentTextView);
        noActivityTextView = headerView.findViewById(R.id.noActivityTextView);
        
        qrImage = (ImageView) headerView.findViewById(R.id.codeImage);
        
        qrImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setAddressOnClipboard();
            }
        });
     
        addressButton = (AutoResizeTextView) headerView.findViewById(R.id.walletAddressButton);
        addressButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setAddressOnClipboard();
            }
        });

        transactionsAdapter = new TransactionsAdapter(getActivity());
        
        setAdapter(transactionsAdapter);
        setupToolbar();
        setupFab();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.wallet, menu);
        
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_share:
                shareAddress();
                return true;
            case R.id.action_copy:
                setAddressOnClipboard();
                return true;
            case R.id.action_blockchain:
                viewBlockChain();
            case R.id.action_address:
                //newWalletAddress();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onResume() 
    {
        super.onResume();

        onRefreshStart();
        
        subscribeData();
        
        updateData();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscription.unsubscribe();
        subscriptions.unsubscribe();
        walletSubscription.unsubscribe();
        updateSubscription.unsubscribe();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);
    }

    @Override
    public void onRefresh()
    {
        onRefreshStart();
        updateData();
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.fab) {
            bus.post(NavigateEvent.QRCODE);
        }
    }

    private void setupFab()
    {
        fab.setOnClickListener(this);
    }

    private void setupToolbar()
    {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);

        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle("");
        ab.setDisplayHomeAsUpEnabled(true);
    }

    public void onRefreshStart()
    {
        handler.postDelayed(refreshRunnable, 1000);
    }

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run() {
            swipeLayout.setRefreshing(true);
        }
    };

    public void onRefreshStop()
    {
        handler.removeCallbacks(refreshRunnable);
        swipeLayout.setRefreshing(false);
    }
    
    protected void showActivity(Boolean show)
    {
        // TODO we need to store these in database
        if(transactionsAdapter.getCount() == 0) {
            noActivityTextView.setVisibility(show?View.GONE:View.VISIBLE);
            recentTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    protected void subscribeData()
    {
        subscriptions = new CompositeSubscription();

        subscriptions.add(Observable.combineLatest(walletObservable, exchangeObservable, new Func2<WalletItem, ExchangeItem, WalletData>()
        {
            @Override
            public WalletData call(WalletItem wallet, ExchangeItem exchange)
            {
                WalletData walletData = new WalletData();
                walletData.wallet = wallet;
                walletData.exchange = exchange;
                return walletData;
                
            }
        }).subscribe(new Action1<WalletData>()
        {
            @Override
            public void call(WalletData walletData)
            {
                if(walletData.wallet != null && walletData.exchange != null) {
                    setWallet(walletData.wallet);
                    setAppBarText(walletData); 
                }
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

    protected void updateData()
    {
        walletSubscription = walletUpdateObservable.subscribe(new Action1<Wallet>()
        {
            @Override
            public void call(Wallet wallet)
            {
                updateWalletBalance(wallet);
                setTransactions(wallet.getTransactions());
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();
                handleError(throwable, true);
                showActivity(false);
            }
        }, new Action0()
        {
            @Override
            public void call()
            {
                onRefreshStop();
            }
        });
    }
    
    private void updateWalletBalance(final Wallet wallet)
    {
        updateSubscription = walletObservable.subscribe(new Action1<WalletItem>() {
            @Override
            public void call(WalletItem walletItem)
            {
                onRefreshStop();
                
                if(walletItem != null) {
                    
                    double oldBalance = Doubles.convertToDouble(walletItem.balance());
                    double newBalance = Doubles.convertToDouble(wallet.total.balance);
                    String address = walletItem.address();
                    
                    Timber.d("Wallet Address: " + address);

                   /* if (oldBalance != newBalance || !address.equals(wallet.address.address)) {
                        dbManager.updateWallet(wallet);
                    }
                    */
                    if (newBalance > oldBalance) {
                        String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                        snack("Received " + diff + " BTC");
                    }
                } 
                
                dbManager.updateWallet(wallet);
            }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable)
                {
                    onRefreshStop();
                    reportError(throwable);
                }
        });
    }
    
    public void setWallet(WalletItem wallet)
    {
        try {
            Bitmap qrCode = (BitmapFactory.decodeByteArray(wallet.qrcode(), 0, wallet.qrcode().length));
            qrImage.setImageBitmap(qrCode);
        } catch (NullPointerException e){
            Timber.e("Error reading wallet qrcode data: " + e.getLocalizedMessage());
        }

        address = wallet.address(); // save for later use
        addressButton.setText(wallet.address());
    }
    
    public void setTransactions(List<Transaction> transactions)
    {
        showActivity(!transactions.isEmpty());
        
        getAdapter().replaceWith(transactions);
    }

    private void setAdapter(TransactionsAdapter adapter)
    {
        list.setAdapter(adapter);
    }

    private TransactionsAdapter getAdapter()
    {
        return transactionsAdapter;
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    protected void setAddressOnClipboard()
    {
        if (address != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.wallet_address_clipboard_title), address);
                clipboard.setPrimaryClip(clip);
            } else {
                android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setText(address);
            }

            toast(getString(R.string.wallet_address_copied_toast));
        }
    }

    protected void viewBlockChain()
    {
        Intent blockChainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BLOCKCHAIN_INFO_ADDRESS + address));
        startActivity(blockChainIntent);
    }

    protected void shareAddress()
    {
        Intent sendIntent;
        try {
            sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WalletUtils.generateBitCoinURI(address)));
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sendIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.wallet_my_address_share));
                sendIntent.putExtra(Intent.EXTRA_TEXT, address);
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_using)));
            } catch (AndroidRuntimeException e) {
                Timber.e(e.getMessage());
            }
        }
    }

    protected void setAppBarText(WalletData data)
    {
        String btcValue = Calculations.computedValueOfBitcoin(data.exchange.bid(), data.exchange.ask(), data.wallet.balance());
        String btcAmount = Conversions.formatBitcoinAmount(data.wallet.balance()) + " " + getString(R.string.btc);
        bitcoinPrice.setText(btcAmount);
        bitcoinTitle.setText("ACCOUNT BALANCE");
        bitcoinValue.setText("≈ $" + btcValue + " " + getString(R.string.usd) + " (" + data.exchange.exchange() + ")");
    }
}
