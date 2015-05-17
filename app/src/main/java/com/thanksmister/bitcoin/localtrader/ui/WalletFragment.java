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
import android.app.Activity;
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
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
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

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.misc.AutoResizeTextView;
import com.thanksmister.bitcoin.localtrader.ui.misc.TransactionsAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.w3c.dom.Text;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class WalletFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    private static final String ARG_SECTION_NUMBER = "section_number";

    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;
    
    @InjectView(android.R.id.list)
    ListView list;
    
    @InjectView(R.id.bitcoinBalance)
    TextView bitcoinBalance;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;
    
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @OnClick(R.id.walletFloatingButton)
    public void scanButtonClicked()
    {
        scanQrCode();
    }

    View progress;
    ImageView qrImage;
    AutoResizeTextView addressButton;
    TextView recentTextView;
    View noActivityTextView;

    private CompositeSubscription subscriptions;
    private Subscription subscription = Subscriptions.empty();
    private Subscription updateSubscription = Subscriptions.empty();
    
    private TransactionsAdapter transactionsAdapter;
    private Observable<WalletItem> walletObservable;
    private Observable<Wallet> walletUpdateObservable;
    private Observable<ExchangeItem> exchangeObservable;
    
    private class WalletData {
        public WalletItem wallet;
        public ExchangeItem exchange;
    }

    private WalletData walletData;

    public static WalletFragment newInstance(int sectionNumber)
    {
        WalletFragment fragment = new WalletFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
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

        walletData = new WalletData();
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

        progress = headerView.findViewById(R.id.walletProgress);
        recentTextView = (TextView) headerView.findViewById(R.id.recentTextView);
        noActivityTextView = headerView.findViewById(R.id.noActivityTextView);
        
        qrImage = (ImageView) headerView.findViewById(R.id.codeImage);
        qrImage.setOnClickListener(view -> {
            setAddressOnClipboard();
        });

        addressButton = (AutoResizeTextView) headerView.findViewById(R.id.walletAddressButton);
        addressButton.setOnClickListener(view -> {
            setAddressOnClipboard();
        });

        transactionsAdapter = new TransactionsAdapter(getActivity());
        setAdapter(transactionsAdapter);
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
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onResume() 
    {
        super.onResume();
        
        showProgress();

        subscribeData();
        
        updateData();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscriptions.unsubscribe();
        subscription.unsubscribe();
        updateSubscription.unsubscribe();
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
        updateData();
    }

    public void onRefreshStop()
    {
        hideProgress();
        swipeLayout.setRefreshing(false);
    }
    
    public void showActivity(Boolean show)
    {
        noActivityTextView.setVisibility(show?View.GONE:View.VISIBLE);
        recentTextView.setVisibility(show?View.VISIBLE:View.GONE);
    }
    
    public void showProgress()
    {
        noActivityTextView.setVisibility(View.GONE);
        recentTextView.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
    }

    protected void subscribeData()
    {
        subscriptions = new CompositeSubscription();
        
        subscriptions.add(Observable.combineLatest(walletObservable, exchangeObservable, new Func2<WalletItem, ExchangeItem, WalletData>()
        {
            @Override
            public WalletData call(WalletItem wallet, ExchangeItem exchange)
            {
                walletData.wallet = wallet;
                walletData.exchange = exchange;
                return walletData;
            }
        }).subscribe(new Action1<WalletData>()
        {
            @Override
            public void call(WalletData data)
            {
                setWallet(data.wallet, data.exchange);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                handleError(throwable);
            }
        }));
    }

    protected void updateData()
    {
        subscription = walletUpdateObservable.subscribe(new Action1<Wallet>()
        {
            @Override
            public void call(Wallet wallet)
            {
                updateWalletBalance(wallet);
                
                setTransactions(wallet.getTransactions());
                
                onRefreshStop();
                
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                handleError(throwable);
                
                onRefreshStop();

                showActivity(false);
            }
        });
    }
    
    private void updateWalletBalance(Wallet wallet)
    {
        updateSubscription = walletObservable.subscribe(new Action1<WalletItem>() {
            @Override
            public void call(WalletItem walletItem)
            {
                if(walletItem != null) {
                    double oldBalance = Doubles.convertToDouble(walletItem.balance());
                    double newBalance = Doubles.convertToDouble(wallet.total.balance);
                    String address = walletItem.address();

                    if (oldBalance != newBalance || !address.equals(wallet.address.address)) {
                        updateWallet(wallet);
                    }
                    
                    if (newBalance > oldBalance) {
                        String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                        toast("Received " + diff + " BTC");
                    }
                } else {
                    updateWallet(wallet);
                }
            }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable)
                {
                    reportError(throwable);
                }
        });
    }

    private void updateWallet(Wallet wallet)
    {
        dbManager.updateWallet(wallet);
    }
    
    public void setWallet(WalletItem wallet, ExchangeItem exchange)
    {
        try {
            
            Bitmap qrCode = (BitmapFactory.decodeByteArray(wallet.qrcode(), 0, wallet.qrcode().length));
            qrImage.setImageBitmap(qrCode);
            
        } catch (NullPointerException e){
            
            Timber.e("Null Wallet QRCode");
        }
        
        if(exchange != null) {

            Timber.e("Exchange: " + exchange.exchange());
            
            String btcValue = Calculations.computedValueOfBitcoin(exchange.bid(), exchange.ask(), wallet.balance());
            String btcAmount = Conversions.formatBitcoinAmount(wallet.balance()) + " " + getString(R.string.btc);

            addressButton.setText(wallet.address());
            bitcoinBalance.setText(btcAmount);
            bitcoinValue.setText("≈ $" + btcValue + " " + getString(R.string.usd) + " (" + exchange.exchange() + ")"); 
        }
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
    
    public void scanQrCode()
    {
       //launchScanner();
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setAddressOnClipboard()
    {
        String address = walletData.wallet.address();
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
    
    public void viewBlockChain()
    {
        Intent blockChainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BLOCKCHAIN_INFO_ADDRESS + walletData.wallet.address()));
        startActivity(blockChainIntent);
    }
    
    public void shareAddress()
    {
        Intent sendIntent;
        String address = walletData.wallet.address();
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
}
