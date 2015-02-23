package com.thanksmister.bitcoin.localtrader.ui.wallet;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.ui.main.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.misc.AutoResizeTextView;
import com.thanksmister.bitcoin.localtrader.ui.misc.TransactionsAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class WalletFragment extends BaseFragment implements WalletView, SwipeRefreshLayout.OnRefreshListener
{
    private static final String ARG_SECTION_NUMBER = "section_number";

    @Inject
    WalletPresenter presenter;

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

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

    @InjectView(R.id.retry)
    View retry;

    @InjectView(R.id.retryTextView)
    TextView retryTextView;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @OnClick(R.id.emptyRetryButton)
    public void emptyButtonClicked()
    {
        presenter.onResume();
    }

    @OnClick(R.id.walletFloatingButton)
    public void scanButtonClicked()
    {
        presenter.scanQrCode();
    }

    private TransactionsAdapter transactionsAdapter;
    private Wallet wallet;

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
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.view_wallet, container, false);
        return view;
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

        ButterKnife.inject(this, getActivity());

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        View headerView = getLayoutInflater(savedInstanceState).inflate(R.layout.view_wallet_header, null, false);
        list.addHeaderView(headerView, null, false);

        qrImage = (ImageView) headerView.findViewById(R.id.codeImage);
        qrImage.setOnClickListener(view -> {
            presenter.setAddressOnClipboard();
        });

        addressButton = (AutoResizeTextView) headerView.findViewById(R.id.walletAddressButton);
        addressButton.setOnClickListener(view -> {
            presenter.setAddressOnClipboard();
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
                presenter.shareAddress();
                return true;
            case R.id.action_copy:
                presenter.setAddressOnClipboard();
                return true;
            case R.id.action_blockchain:
                presenter.viewBlockChain();
            case R.id.action_address:
                presenter.newWalletAddress();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new WalletModule(this));
    }

    @Override
    public Context getContext()
    {
        return getActivity();
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
        
        presenter.onResume();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);
        
        presenter.onDestroy();
    }

    @Override
    public void onRefresh()
    {
        presenter.onResume();
    }

    @Override
    public void onRefreshStop()
    {
        swipeLayout.setRefreshing(false);
    }

    @Override
    public void onRetry(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        retry.setVisibility(View.VISIBLE);
        retryTextView.setText(message);
    }

    @Override
    public void onError(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }

    @Override
    public void showProgress()
    {
        empty.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress()
    {
        empty.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    @Override
    public void setWallet(Wallet wallet)
    { 
        addressButton.setText(wallet.address.address);
        qrImage.setImageBitmap(wallet.qrImage);
        bitcoinBalance.setText(Conversions.formatBitcoinAmount(wallet.total.balance) + " " + getString(R.string.btc));

        bitcoinValue.setText("≈ $" + wallet.getBitcoinValue() + " " + getString(R.string.usd) + " (" + wallet.exchange.name + ")");

        getAdapter().replaceWith(wallet.getTransactions());
    }

    private void setAdapter(TransactionsAdapter adapter)
    {
        list.setAdapter(adapter);
    }

    private TransactionsAdapter getAdapter()
    {
        return transactionsAdapter;
    }
}
