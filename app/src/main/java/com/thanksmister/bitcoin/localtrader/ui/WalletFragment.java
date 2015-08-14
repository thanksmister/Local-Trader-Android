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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.model.WalletData;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.components.SectionRecycleViewAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.TransactionsAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class WalletFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener, AppBarLayout.OnOffsetChangedListener
{
    public static final String EXTRA_WALLET_DATA = "com.thanksmister.extra.EXTRA_WALLET_DATA";
    
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

    @InjectView(R.id.appBarLayout)
    AppBarLayout appBarLayout;

    @InjectView(R.id.recycleView)
    RecyclerView recycleView;
    
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
    

    CompositeSubscription subscriptions = new CompositeSubscription();
    Subscription subscription = Subscriptions.empty();
    Subscription walletUpdateSubscription = Subscriptions.empty();
    Subscription updateSubscription = Subscriptions.empty();
    Subscription bitmapSubscription = Subscriptions.empty();
    
    TransactionsAdapter transactionsAdapter;
    Observable<Wallet> walletUpdateObservable;
    Observable<ExchangeItem> exchangeObservable;
    Observable<WalletItem> walletObservable;
    Observable<Bitmap> bitmaptObservable;
   
    private Handler handler;
    private WalletData walletData;
    
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

        if(savedInstanceState != null && savedInstanceState.containsKey(EXTRA_WALLET_DATA))
            walletData = savedInstanceState.getParcelable(EXTRA_WALLET_DATA);
        
        handler = new Handler();
     
        walletObservable = bindFragment(this, dbManager.walletQuery());
        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
        walletUpdateObservable = bindFragment(this, dataService.getWallet());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        if(walletData != null)
            outState.putParcelable(EXTRA_WALLET_DATA, walletData);
        
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i)
    {
        if (i == 0) {
            swipeLayout.setEnabled(true);
        } else {
            swipeLayout.setEnabled(false);
        }
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

        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);
        recycleView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                super.onScrolled(recyclerView, dx, dy);
                int topRowVerticalPosition = (recycleView == null || recycleView.getChildCount() == 0) ? 0 : recycleView.getChildAt(0).getTop();
                swipeLayout.setEnabled(topRowVerticalPosition >= 0);
            }
        });

        transactionsAdapter = new TransactionsAdapter(getActivity());
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

        appBarLayout.addOnOffsetChangedListener(this);

        onRefreshStart();
        
        subscribeData();
        
        updateData();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        appBarLayout.removeOnOffsetChangedListener(this);

        bitmapSubscription.unsubscribe();
        subscription.unsubscribe();
        subscriptions.unsubscribe();
        walletUpdateSubscription.unsubscribe();
        updateSubscription.unsubscribe();
        handler.removeCallbacks(refreshRunnable);
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

    protected void subscribeData()
    {
        Timber.d("SubscribeData");
        
        subscriptions = new CompositeSubscription();

        subscriptions.add(Observable.zip(walletObservable, exchangeObservable, new Func2<WalletItem, ExchangeItem, WalletItem>()
        {
            @Override
            public WalletItem call(WalletItem walletItem, ExchangeItem exchangeItem)
            {
                // setup our bar 
                setAppBarText(exchangeItem.bid(), exchangeItem.ask(), walletItem.balance(), exchangeItem.exchange());
                
                return walletItem;
            }
        }).subscribe(new Action1<WalletItem>()
        {
            @Override
            public void call(WalletItem dataItem)
            {
                setWallet(dataItem);
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
        walletUpdateSubscription = walletUpdateObservable.subscribe(new Action1<Wallet>()
        {
            @Override
            public void call(Wallet wallet)
            {
                updateWalletBalance(wallet);
                setupList(walletData, wallet.getTransactions());
            }
            
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();
                handleError(throwable, true);
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
                    double newBalance = Doubles.convertToDouble(wallet.balance);
                  
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
    
    public void setWallet(final WalletItem walletItem)
    {
        bitmaptObservable = bindFragment(this, Observable.defer(new Func0<Observable<Bitmap>>()
        {
            @Override
            public Observable<Bitmap> call()
            {
                try {
                    Bitmap qrCode = (BitmapFactory.decodeByteArray(walletItem.qrcode(), 0, walletItem.qrcode().length));
                    return Observable.just(qrCode);
                } catch (Exception e) {
                    Timber.e("Error reading wallet QR Code data: " + e.getLocalizedMessage());
                    return null;
                }
            }
        }));
        
        bitmapSubscription = bitmaptObservable
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Bitmap>()
                {
                    @Override
                    public void call(final Bitmap bitmap)
                    {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run(){

                                walletData = new WalletData();
                                walletData.setBalance(walletItem.balance());
                                walletData.setAddress(walletItem.address());
                                walletData.setImage(bitmap);
                                
                                setupList(walletData, Collections.EMPTY_LIST);
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        toast("Unable to generate QRCode");
                        reportError(throwable);
                    }
                });
    }

    private void setupList(WalletData walletData, List<Transaction> transactions)
    {
        // provide combined data
        ArrayList<Object> items = new ArrayList<>();
        
        items.add(walletData);
        items.addAll(transactions);
        
        TransactionsAdapter itemAdapter = getAdapter();
        itemAdapter.replaceWith(items);

        //This is the code to provide a sectioned list
        List<SectionRecycleViewAdapter.Section> sections = new ArrayList<SectionRecycleViewAdapter.Section>();
        if(transactions.size() > 0) {
            sections.add(new SectionRecycleViewAdapter.Section(1, getString(R.string.wallet_recent_activity_header)));
        }
        
        //Add your adapter to the sectionAdapter
        SectionRecycleViewAdapter.Section[] section = new SectionRecycleViewAdapter.Section[sections.size()];
        SectionRecycleViewAdapter sectionRecycleViewAdapter = new SectionRecycleViewAdapter(getActivity(), R.layout.section, R.id.section_text, itemAdapter);
        sectionRecycleViewAdapter.setSections(sections.toArray(section));

        //Apply this adapter to the RecyclerView
        recycleView.setAdapter(sectionRecycleViewAdapter);
    }
    
    private TransactionsAdapter getAdapter()
    {
        return transactionsAdapter;
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    protected void setAddressOnClipboard()
    {
        if(walletData == null) return;
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.wallet_address_clipboard_title), walletData.getAddress());
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(walletData.getAddress());
        }

        toast(getString(R.string.wallet_address_copied_toast));
    }

    protected void viewBlockChain()
    {
        if(walletData == null) return;
        
        Intent blockChainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BLOCKCHAIN_INFO_ADDRESS + walletData.getAddress()));
        startActivity(blockChainIntent);
    }

    protected void shareAddress()
    {
        if(walletData == null) return;
        
        Intent sendIntent;
        try {
            sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WalletUtils.generateBitCoinURI(walletData.getAddress())));
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sendIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.wallet_my_address_share));
                sendIntent.putExtra(Intent.EXTRA_TEXT, walletData.getAddress());
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_using)));
            } catch (AndroidRuntimeException e) {
                Timber.e(e.getMessage());
            }
        }
    }

    protected void setAppBarText(String bid, String ask, String balance, String exchange)
    {
        String btcValue = Calculations.computedValueOfBitcoin(bid, ask, balance);
        String btcAmount = Conversions.formatBitcoinAmount(balance) + " " + getString(R.string.btc);
        bitcoinPrice.setText(btcAmount);
        bitcoinTitle.setText("ACCOUNT BALANCE");
        bitcoinValue.setText("≈ $" + btcValue + " " + getString(R.string.usd) + " (" + exchange + ")");
    }
}
