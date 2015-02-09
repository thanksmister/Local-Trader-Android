package com.thanksmister.bitcoin.localtrader.ui.request;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ScannerEvent;
import com.thanksmister.bitcoin.localtrader.ui.qrcode.QRCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.release.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import retrofit.RetrofitError;
import rx.Observer;
import rx.Subscription;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class RequestPresenterImpl implements RequestPresenter
{
    private RequestView view;
    private DataService service;
    private Subscription subscription;
    private Subscription sendSubscription;
    private Bus bus;
    private Wallet wallet;

    public RequestPresenterImpl(RequestView view, DataService service, Bus bus) 
    {
        this.view = view;
        this.service = service;
        this.bus = bus;
    }

    @Override
    public void onResume()
    {
        getWalletBalance();
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();

        if(sendSubscription != null)
            sendSubscription.unsubscribe();
    }

    protected void getWalletBalance()
    {
        subscription = service.getWalletBalance(new Observer<Wallet>(){
            @Override
            public void onCompleted() {
                getView().hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e.getMessage());
                getView().showError(e.getMessage());
            }

            @Override
            public void onNext(Wallet result) {
                wallet = result;
                getView().setWallet(result);
                getView().hideProgress();
            }
        });
    }

    

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setAddressFromClipboard()
    {
        String clipText = getClipboardText();
        if(Strings.isBlank(clipText)) {
            Toast.makeText(getView().getContext(), getView().getContext().getString(R.string.toast_clipboard_empty), Toast.LENGTH_LONG).show();
            return;
        }

        String btcAddress = "";
        if (clipText.toLowerCase().contains("bitcoin:")) {
            btcAddress = WalletUtils.parseBitcoinAddress(clipText);
        } else if (WalletUtils.validBitcoinAddress(clipText)) {
            btcAddress = clipText;
        }

        String btcAmount = "";
        if(WalletUtils.validAmount(btcAmount)) {
            btcAmount = WalletUtils.parseBitcoinAmount(clipText);
        }

        if (!Strings.isBlank(btcAddress)) {
            getView().setBitcoinAddress(btcAddress);
            if(!Strings.isBlank(btcAmount)) {
                getView().setAmount(btcAmount);
            }
        } else if (!Strings.isBlank(btcAmount)) {
            getView().setAmount(btcAmount);
        } else {
            Toast.makeText(getView().getContext(), getView().getContext().getString(R.string.toast_invalid_address), Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setAmountFromClipboard()
    {
        String clipText = getClipboardText();
        if(Strings.isBlank(clipText)) {
            Toast.makeText(getView().getContext(), getView().getContext().getString(R.string.toast_clipboard_empty), Toast.LENGTH_LONG).show();
            return;
        }

        if(WalletUtils.validAmount(clipText)) {
            getView().setAmount(WalletUtils.parseBitcoinAmount(clipText));
        } else {
            Toast.makeText(getView().getContext(), getView().getContext().getString(R.string.toast_invalid_amount), Toast.LENGTH_LONG).show();
        }
    }

    private String getClipboardText()
    {
        String clipText = "";
        ClipboardManager clipboardManager = (ClipboardManager) getView().getContext().getSystemService(Context.CLIPBOARD_SERVICE);;
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

    @Override
    public void scanQrCode()
    {
        //bus.post(ScannerEvent.SCAN);
        ((BaseActivity) getView().getContext()).launchScanner();
    }

    @Override
    public void pinCodeEvent(String pinCode, String address, String amount)
    {
        Timber.d("PinCodeEvent pin: " + pinCode);
        Timber.d("PinCodeEvent address: " + address);
        Timber.d("PinCodeEvent amount: " + amount);

        ((BaseActivity) getView().getContext()).showProgressDialog(new ProgressDialogEvent("Sending bitcoin..."));

        sendSubscription = service.sendPinCodeMoney(new Observer<String>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable throwable) {

                ((BaseActivity) getView().getContext()).hideProgressDialog();
                
                if (throwable instanceof RetrofitError) {
                    RetroError retroError = DataServiceUtils.convertRetroError(throwable, getView().getContext());
                    Toast.makeText(getView().getContext(), retroError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNext(String response) {
                
                ((BaseActivity) getView().getContext()).hideProgressDialog();
                
                Timber.d("Response: " + response);
                
                getWalletBalance(); // refresh wallet balance
                getView().resetWallet();
                Toast.makeText(getView().getContext(), getView().getContext().getString(R.string.toast_transaction_success), Toast.LENGTH_SHORT).show();
            }
        }, pinCode, address, amount);
    }

    private RequestView getView()
    {
        return view;
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        Timber.d("onNetworkEvent: " + event.name());

        if(event == NetworkEvent.DISCONNECTED) {
            //cancelCheck(); // stop checking we have no network
        } else  {
            //startCheck();
        }
    }
}
