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
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;
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

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.list)
    ListView list;

    ImageView qrImage;
    AutoResizeTextView addressButton;

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
        walletUpdateObservable = bindFragment(this, dbManager.getWallet());
        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
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

        showProgress();

        subscribeData();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        View headerView = getLayoutInflater(savedInstanceState).inflate(R.layout.view_wallet_header, null, false);
        list.addHeaderView(headerView, null, false);

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

        updateData();
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

    protected void onRefreshStart()
    {
        swipeLayout.setRefreshing(true);
    }

    public void onRefreshStop()
    {
        swipeLayout.setRefreshing(false);
    }
    
    public void onError()
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
    }
    
    public void showProgress()
    {
        list.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    protected void subscribeData()
    {
        Observable.combineLatest(walletObservable, exchangeObservable, new Func2<WalletItem, ExchangeItem, WalletData>()
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
                hideProgress();
                setWallet(data.wallet, data.exchange);
            }
        });
    }

    protected void updateData()
    {
        onRefreshStart();

        walletUpdateObservable.subscribe(new Action1<Wallet>()
        {
            @Override
            public void call(Wallet wallet)
            {
                dbManager.updateWallet(wallet);
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
            }
        });
    }
    
    public void setWallet(WalletItem wallet, ExchangeItem exchange)
    {
        // TODO make sure this doesn't blow up
        if(wallet.qrcode() != null) {
            Bitmap qrCode = (BitmapFactory.decodeByteArray(wallet.qrcode(), 0, wallet.qrcode().length));
            qrImage.setImageBitmap(qrCode);
        }
        
        if(exchange != null) {
            String btcValue = Calculations.computedValueOfBitcoin(exchange.bid(), exchange.ask(), wallet.balance());
            String btcAmount = Conversions.formatBitcoinAmount(wallet.balance()) + " " + getString(R.string.btc);

            addressButton.setText(wallet.address());
            bitcoinBalance.setText(btcAmount);
            bitcoinValue.setText("≈ $" + btcValue + " " + getString(R.string.usd) + " (" + exchange.exchange() + ")"); 
        }
    }
    
    public void setTransactions(List<Transaction> transactions)
    {
        Timber.d("Transactions: " + transactions.size());
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
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "My Bitcoin Address");
                sendIntent.putExtra(Intent.EXTRA_TEXT, address);
                startActivity(Intent.createChooser(sendIntent, "Share using:"));
            } catch (AndroidRuntimeException e) {
                Timber.e(e.getMessage());
            }
        }
    }
}
