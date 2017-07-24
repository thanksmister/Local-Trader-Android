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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeRateItem;
import com.thanksmister.bitcoin.localtrader.data.database.TransactionItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.SectionRecycleViewAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.TransactionsAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class WalletFragment extends BaseFragment {

    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.recycleView)
    RecyclerView recycleView;
    
    @InjectView(R.id.bitcoinTitle)
    TextView bitcoinTitle;

    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;

    @InjectView(R.id.bitcoinLayout)
    View bitcoinLayout;

    private TransactionsAdapter transactionsAdapter;
    private SectionRecycleViewAdapter sectionRecycleViewAdapter;
    private List<TransactionItem> transactionItems;
    private ExchangeRateItem exchangeItem;
    private WalletItem walletItem;
    
    public static WalletFragment newInstance() {
        return new WalletFragment();
    }

    public WalletFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_wallet, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        
        super.onActivityCreated(savedInstanceState);
        
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        
        recycleView.setLayoutManager(linearLayoutManager);
        recycleView.setHasFixedSize(true);
        
        transactionsAdapter = new TransactionsAdapter(getActivity());
        sectionRecycleViewAdapter = createAdapter();
        recycleView.setAdapter(sectionRecycleViewAdapter);

        setupToolbar();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        toast(getString(R.string.toast_refreshing_data));
        subscribeData();
        updateData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
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
    
    public void onRefresh() {
        updateData();
    }
    
    private void setupToolbar() {
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        if(ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
            ab.setTitle(getString(R.string.view_title_wallet));
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void subscribeData() {
        //dbManager.clearWallet();
        dbManager.walletQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Wallet subscription safely unsubscribed");
                    }
                })
                .compose(this.<WalletItem>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<WalletItem>() {
                    @Override
                    public void call(WalletItem item) {
                        walletItem = item;
                        if (exchangeItem != null && walletItem != null) {
                            setAppBarText(exchangeItem.rate(), walletItem.balance(), exchangeItem.exchange());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        reportError(throwable);
                    }
                });

        dbManager.transactionsQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Transactions subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<TransactionItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<TransactionItem>>() {
                    @Override
                    public void call(List<TransactionItem> items) {
                        transactionItems = items;
                        setupList(transactionItems);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        reportError(throwable);
                    }
                });

        dbManager.exchangeQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<ExchangeRateItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<ExchangeRateItem>>() {
                    @Override
                    public void call(List<ExchangeRateItem> exchanges) {
                        if (!exchanges.isEmpty()) {
                            String currency = exchangeService.getExchangeCurrency();
                            for (ExchangeRateItem rateItem : exchanges) {
                                if (rateItem.currency().equals(currency)) {
                                    exchangeItem = rateItem;
                                    break;
                                }
                            }
                        }
                        if (exchangeItem != null && walletItem != null) {
                            setAppBarText(exchangeItem.rate(), walletItem.balance(), exchangeItem.exchange());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        reportError(throwable);
                    }
                });
    }

    private void updateData() {
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
                        dbManager.updateWallet(wallet);
                        dbManager.updateTransactions(wallet.getTransactions());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        Timber.e(throwable.getMessage());
                    }
                });
    }

    // TODO split list by date
    private void setupList(final List<TransactionItem> transactionItems) {
        TransactionsAdapter itemAdapter = getAdapter();
        itemAdapter.replaceWith(transactionItems);
        if (!transactionItems.isEmpty()) {
            List<SectionRecycleViewAdapter.Section> sections = new ArrayList<>();
            sections.add(new SectionRecycleViewAdapter.Section(1, getString(R.string.wallet_recent_activity_header)));
            if (!sectionRecycleViewAdapter.hasSections()) {
                addAdapterSection(sections);
            }
        }
        sectionRecycleViewAdapter.updateBaseAdapter(itemAdapter);
    }

    private void addAdapterSection(List<SectionRecycleViewAdapter.Section> sections) {
        try {
            SectionRecycleViewAdapter.Section[] section = new SectionRecycleViewAdapter.Section[sections.size()];
            sectionRecycleViewAdapter.setSections(sections.toArray(section));
            sectionRecycleViewAdapter.notifyDataSetChanged();
        } catch (IllegalStateException e) {
            Timber.e(e.getLocalizedMessage());
        }
    }

    private SectionRecycleViewAdapter createAdapter() {
        TransactionsAdapter itemAdapter = getAdapter();
        return new SectionRecycleViewAdapter(getActivity(), R.layout.section, R.id.section_text, itemAdapter);
    }

    private TransactionsAdapter getAdapter() {
        return transactionsAdapter;
    }

    private void setAppBarText(String rate, String balance, String exchange) {
        String currency = exchangeService.getExchangeCurrency();
        String btcValue = Calculations.computedValueOfBitcoin(rate, balance);
        String btcAmount = Conversions.formatBitcoinAmount(balance) + " " + getString(R.string.btc);
        bitcoinPrice.setText(btcAmount);
        bitcoinTitle.setText(R.string.wallet_account_balance);
        bitcoinValue.setText("≈ " + btcValue + " " + currency + " (" + exchange + ")");
    }
}