/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.bitcoin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class QRCodeActivity extends Activity
{
    public static final String EXTRA_QR_BITMAP = "EXTRA_QR_BITMAP";
    public static final String EXTRA_QR_ADDRESS = "EXTRA_QR_ADDRESS";
    public static final String EXTRA_QR_AMOUNT = "EXTRA_QR_AMOUNT";
    
    private Bitmap bitmap;
    private String address;
    private String amount;
    private Subscriber<Bitmap> subscription;

    @InjectView(R.id.headerText)
    TextView headerText;

    @InjectView(R.id.image)
    ImageView image;

    @OnClick(R.id.cancelButton)
    public void cancelButtonClicked()
    {
        finish();
    }

    @OnClick(R.id.copyButton)
    public void copyButtonClicked()
    {
        setRequestOnClipboard();
    }

    @OnClick(R.id.shareButton)
    public void shareButtonClicked()
    {
        shareBitcoinRequest();
    }

    public static Intent createStartIntent(Context context, String address, String amount)
    {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra(EXTRA_QR_ADDRESS, address);
        intent.putExtra(EXTRA_QR_AMOUNT, amount);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.dialog_qr_code);
        
        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(EXTRA_QR_BITMAP)) 
                bitmap = savedInstanceState.getParcelable(EXTRA_QR_BITMAP);
            
            address = savedInstanceState.getString(EXTRA_QR_ADDRESS);
            amount = savedInstanceState.getString(EXTRA_QR_AMOUNT);
        } else {
            address = getIntent().getStringExtra(EXTRA_QR_ADDRESS);
            amount = getIntent().getStringExtra(EXTRA_QR_AMOUNT);
        }

        ButterKnife.inject(this);
        
        if(bitmap == null) {
            generateQrCodeImage(address, amount);
        } else {
            image.setImageBitmap(bitmap);
        }  
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        ButterKnife.reset(this);
        subscription.unsubscribe();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putParcelable(EXTRA_QR_BITMAP, bitmap);
        outState.putString(EXTRA_QR_ADDRESS, address);
        outState.putString(EXTRA_QR_AMOUNT, amount);
        super.onSaveInstanceState(outState);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setRequestOnClipboard()
    {
        String bitcoinUrl = (Strings.isBlank(amount))? WalletUtils.generateBitCoinURI(address, amount): WalletUtils.generateBitCoinURI(address, amount);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.bitcoin_request_clipboard_title), bitcoinUrl);
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(bitcoinUrl);
        }

        Toast.makeText(this, getString(R.string.bitcoin_request_copied_toast), Toast.LENGTH_LONG).show();
    }
    
    private void shareBitcoinRequest()
    {
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
    }

    public void generateQrCodeImage(final String address, final String amount)
    {
        subscription = new Subscriber<Bitmap>() {
            @Override
            public void onNext(Bitmap data) { 
                bitmap = data;
                image.setImageBitmap(bitmap);
            }

            @Override
            public void onCompleted() { 
                
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e.getMessage());
                Toast.makeText(getApplicationContext(), "Error generating QR code", Toast.LENGTH_LONG).show();
            }
        };

        generateBitmap(address, amount)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscription);
    }

    private Observable<Bitmap> generateBitmap(final String address, final String amount)
    {
        return Observable.create((Subscriber<? super Bitmap> subscriber) -> {
            try {
                subscriber.onNext(WalletUtils.encodeAsBitmap(address, amount, getApplicationContext()));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
