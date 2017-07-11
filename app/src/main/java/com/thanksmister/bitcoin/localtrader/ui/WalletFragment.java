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

import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.model.WalletAdapter;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeRateItem;
import com.thanksmister.bitcoin.localtrader.data.database.TransactionItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.ui.components.SectionRecycleViewAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.TransactionsAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class WalletFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, 
        View.OnClickListener
{
    public static final String EXTRA_WALLET_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";

    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.fab)
    FloatingActionButton fab;
    
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


    private TransactionsAdapter transactionsAdapter;

    private Handler handler;
    private String address;
    private SectionRecycleViewAdapter sectionRecycleViewAdapter;
    
    private List<TransactionItem> transactionItems;
    public ExchangeRateItem exchangeItem;
    public WalletItem walletItem;
    public Bitmap qrImage;
   

    public static WalletFragment newInstance()
    {
        return new WalletFragment();
    }

    public WalletFragment()
    {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_WALLET_ADDRESS)) {
                address = savedInstanceState.getString(EXTRA_WALLET_ADDRESS);
            }
        }

        handler = new Handler();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        if (address != null)
            outState.putString(EXTRA_WALLET_ADDRESS, address);

        super.onSaveInstanceState(outState);
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
        
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);
        recycleView.setHasFixedSize(true);
        transactionsAdapter = new TransactionsAdapter(getActivity());
        sectionRecycleViewAdapter = createAdapter();
        recycleView.setAdapter(sectionRecycleViewAdapter);

        toast("Refreshing data...");
        
        setupToolbar();
        setupFab();
        
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
        subscribeData();
        onRefreshStart();
        updateData();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        
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
        updateData();
        onRefreshStart();
    }

    public void onRefreshStart()
    {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 50);
    }

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run() {
            
            if(swipeLayout != null && !swipeLayout.isShown())
                swipeLayout.setRefreshing(true);
        }
    };

    protected void onRefreshStop()
    {
        if(handler != null)
            handler.removeCallbacks(refreshRunnable);
        
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab && isAdded()) {
            ((MainActivity) getActivity()).launchScanner();
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

    protected void subscribeData()
    {
        //dbManager.clearWallet();
        dbManager.walletQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Wallet subscription safely unsubscribed");
                    }
                })
                .compose(this.<WalletItem>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<WalletItem>()
                {
                    @Override
                    public void call(WalletItem item)
                    {
                        walletItem = item;

                        if (walletItem != null) {
                            setWallet(walletItem);
                        }

                        if (exchangeItem != null && walletItem != null) {
                            setAppBarText(exchangeItem.rate(), walletItem.balance(), exchangeItem.exchange());
                        }

                        setupList(walletItem, qrImage, transactionItems);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        dbManager.transactionsQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Transactions subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<TransactionItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<TransactionItem>>()
                {
                    @Override
                    public void call(List<TransactionItem> items)
                    {
                        transactionItems = items;
                        setupList(walletItem, qrImage, transactionItems);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        dbManager.exchangeQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<ExchangeRateItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<ExchangeRateItem>>() {
                    @Override
                    public void call(List<ExchangeRateItem> exchanges) {
                        if (!exchanges.isEmpty()) {
                            String currency = exchangeService.getExchangeCurrency();
                            for (ExchangeRateItem rateItem : exchanges) {
                                if(rateItem.currency().equals(currency)) {
                                    exchangeItem = rateItem;
                                    break;
                                }
                            }
                        }
                        Timber.d("subscribeData exchange: " + exchangeItem);
                        if (exchangeItem != null && walletItem != null) {
                            setAppBarText(exchangeItem.rate(), walletItem.balance(), exchangeItem.exchange());
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });
    }

    protected void updateData()
    {
        Timber.d("updateData");

        if (!NetworkUtils.isNetworkConnected(getActivity())) {
            handleError(new NetworkConnectionException(), true);
            return;
        }

        if(exchangeService.needToRefreshExchanges()) {
            exchangeService.getSpotPrice()
                    .doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            Timber.i("Exchange update subscription safely unsubscribed");
                        }
                    })
                    .compose(this.<ExchangeRate>bindUntilEvent(FragmentEvent.PAUSE))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<ExchangeRate>() {
                        @Override
                        public void call(ExchangeRate exchange) {
                            dbManager.updateExchange(exchange);
                            exchangeService.setExchangeExpireTime();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(final Throwable throwable) {
                            exchangeService.setExchangeExpireTime();
                        }
                    });
        }
        
        dataService.getWallet(true)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Wallet update subscription safely unsubscribed");
                    }
                })
                .compose(this.<Wallet>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Wallet>() {
                    @Override
                    public void call(final Wallet wallet) {
                        Timber.d("updateData wallet: " + wallet.balance);
                        dbManager.updateWallet(wallet);
                        dbManager.updateTransactions(wallet.getTransactions());
                        onRefreshStop();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        //onRefreshStop();
                        //handleError(throwable, true);
                    }
                });
    }

    public void setWallet(final WalletItem item)
    {
        Observable.defer(new Func0<Observable<Bitmap>>()
        {
            @Override
            public Observable<Bitmap> call()
            {
                try {
                    Bitmap qrCode = (BitmapFactory.decodeByteArray(item.qrcode(), 0, item.qrcode().length));
                    return Observable.just(qrCode);
                } catch (Exception e) {
                    Timber.e("Error reading wallet QR Code data: " + e.getLocalizedMessage());
                    return null;
                }
            }
        })
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Bitmap subscription safely unsubscribed");
                    }
                })
                .compose(this.<Bitmap>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>()
                {
                    @Override
                    public void onCompleted()
                    {
                    }

                    @Override
                    public void onError(final Throwable e)
                    {
                        if(getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    setupList(walletItem, qrImage, transactionItems);
                                    reportError(e);  
                                }
                            });
                        }
                    }

                    @Override
                    public void onNext(final Bitmap bitmap)
                    {
                        if(getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    qrImage = bitmap;
                                    setupList(walletItem, qrImage, transactionItems);
                                }
                            });
                        }
                    }
                });
    }

    private void setupList(final WalletItem walletItem, final Bitmap qrImage, final List<TransactionItem> transactionItems)
    {
        // provide combined data
        ArrayList<Object> items = new ArrayList<>();

        WalletAdapter walletAdapterData = new WalletAdapter();
        
        if (walletItem != null) {
            address = walletItem.address(); // save address for sharing
        }

        if(qrImage != null && walletItem != null) {
            walletAdapterData.address = walletItem.address();
            walletAdapterData.qrImage = qrImage;
        }

        items.add(walletAdapterData);

        TransactionsAdapter itemAdapter = getAdapter();
        itemAdapter.replaceWith(items);

        if (transactionItems != null && transactionItems.size() > 0) {

            items.addAll(transactionItems);
            
            List<SectionRecycleViewAdapter.Section> sections = new ArrayList<>();
            sections.add(new SectionRecycleViewAdapter.Section(1, getString(R.string.wallet_recent_activity_header)));

            if (!sectionRecycleViewAdapter.hasSections()) {
                addAdapterSection(sections);
            }
        }

        sectionRecycleViewAdapter.updateBaseAdapter(itemAdapter);
    }

    private void addAdapterSection(List<SectionRecycleViewAdapter.Section> sections)
    {
        try {
            SectionRecycleViewAdapter.Section[] section = new SectionRecycleViewAdapter.Section[sections.size()];
            sectionRecycleViewAdapter.setSections(sections.toArray(section));
            sectionRecycleViewAdapter.notifyDataSetChanged();
        } catch (IllegalStateException e) {
            Timber.e(e.getLocalizedMessage());
        }
    }

    private SectionRecycleViewAdapter createAdapter()
    {
        TransactionsAdapter itemAdapter = getAdapter();
        return new SectionRecycleViewAdapter(getActivity(), R.layout.section, R.id.section_text, itemAdapter);
    }

    private TransactionsAdapter getAdapter()
    {
        return transactionsAdapter;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    protected void setAddressOnClipboard()
    {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.wallet_address_clipboard_title), address);
        clipboard.setPrimaryClip(clip);
        toast(getString(R.string.wallet_address_copied_toast));
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
        
        /*
        String bitcoinUrl = (Strings.isBlank(amount))? WalletUtils.generateBitCoinURI(address, amount): WalletUtils.generateBitCoinURI(address, amount);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bitcoinUrl)));
        } catch (ActivityNotFoundException ex) {
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bitcoin_request_clipboard_title));
                sendIntent.putExtra(Intent.EXTRA_TEXT, bitcoinUrl);
                startActivity(Intent.createChooser(sendIntent, "Share using:"));
            } catch (AndroidRuntimeException e) {
                Timber.e(e.getMessage());
            }
        }
         */
    }

    protected void setAppBarText(String rate, String balance, String exchange)
    {
        Timber.d("Rate: " + rate);
        Timber.d("Balance: " + balance);

        String currency = exchangeService.getExchangeCurrency();
        
        String btcValue = Calculations.computedValueOfBitcoin(rate, balance);
        String btcAmount = Conversions.formatBitcoinAmount(balance) + " " + getString(R.string.btc);
        bitcoinPrice.setText(btcAmount);
        bitcoinTitle.setText(R.string.wallet_account_balance);
        bitcoinValue.setText("≈ " + btcValue + " " + currency + " (" + exchange + ")");
    }
}
