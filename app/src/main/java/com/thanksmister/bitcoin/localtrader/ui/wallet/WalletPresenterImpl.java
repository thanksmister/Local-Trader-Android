package com.thanksmister.bitcoin.localtrader.ui.wallet;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AndroidRuntimeException;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ProgressEvent;
import com.thanksmister.bitcoin.localtrader.events.ScannerEvent;
import com.thanksmister.bitcoin.localtrader.ui.main.MainPresenter;
import com.thanksmister.bitcoin.localtrader.ui.wallet.WalletView;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import javax.inject.Inject;

import retrofit.RetrofitError;
import rx.Observer;
import rx.Subscription;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class WalletPresenterImpl implements WalletPresenter
{
    private WalletView view;
    private Bus bus;
    private DataService dataService;
    private Subscription subscription;
    private Wallet wallet;

    public WalletPresenterImpl(WalletView view, DataService dataService, Bus bus) 
    {
        this.view = view;
        this.dataService = dataService;
        this.bus = bus;
    }

    @Override
    public void onResume()
    {
        getWallet();
        
       /* if(wallet == null) {
            getWallet();
        } else {
            getView().setWallet(wallet);
            getView().hideProgress();
        }*/
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();
    }

    @Override
    public void scanQrCode()
    {
        ((BaseActivity) getView().getContext()).launchScanner();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setAddressOnClipboard()
    {
        String address = wallet.address.address;
            if (address != null) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    ClipboardManager clipboard = (ClipboardManager) getView().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(getView().getContext().getString(R.string.wallet_address_clipboard_title), address);
                    clipboard.setPrimaryClip(clip);
                } else {
                    android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getView().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(address);
                }

                Toast.makeText(getView().getContext(), getView().getContext().getString(R.string.wallet_address_copied_toast), Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void viewBlockChain()
    {
        Intent blockChainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BLOCKCHAIN_INFO_ADDRESS + wallet.address.address));
        getView().getContext().startActivity(blockChainIntent);
    }

    @Override
    public void shareAddress()
    {
        Intent sendIntent;
        String address = wallet.address.address;
        try {
            sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WalletUtils.generateBitCoinURI(address)));
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getView().getContext().startActivity(sendIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "My Bitcoin Address");
                sendIntent.putExtra(Intent.EXTRA_TEXT, address);
                getView().getContext().startActivity(Intent.createChooser(sendIntent, "Share using:"));
            } catch (AndroidRuntimeException e) {
                Timber.e(e.getMessage());
            }
        }
    }

    @Override
    public void newWalletAddress()
    {
        // TODO implement new wallet address request
    }

    public void getWallet()
    {
        subscription = dataService.getWallet(new Observer<Wallet>() {
            @Override
            public void onCompleted() {
                view.hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                RetroError retroError = DataServiceUtils.convertRetroError(e, getView().getContext());
                if(retroError.isAuthenticationError()) {
                    Toast.makeText(getContext(), retroError.getMessage(), Toast.LENGTH_SHORT).show();
                    ((BaseActivity) getContext()).logOut();
                } else {
                    getView().showError(retroError.getMessage());
                }

                ((BaseActivity) getContext()).onRefreshStop();
            }

            @Override
            public void onNext(Wallet result) {
                wallet = result; // set data
                view.setWallet(wallet);

                ((BaseActivity) getContext()).onRefreshStop();
            }
        });
    }

    private Context getContext()
    {
        return getView().getContext();
    }
    
    private WalletView getView()
    {
        return view;
    }
}
