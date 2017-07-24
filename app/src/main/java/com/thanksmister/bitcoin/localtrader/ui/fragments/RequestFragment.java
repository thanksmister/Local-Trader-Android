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

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeRateItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.QRCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.AutoResizeTextView;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class RequestFragment extends BaseFragment {

    public static final String EXTRA_WALLET = "com.thanksmister.extra.EXTRA_WALLET";
    public static final String EXTRA_EXCHANGE = "com.thanksmister.extra.EXTRA_EXCHANGE";

    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.amountText)
    TextView amountText;

    @InjectView(R.id.currencyText)
    TextView currencyText;

    @InjectView(R.id.fiatEditText)
    TextView fiatEditText;

    @OnClick(R.id.qrButton)
    public void qrButtonClicked() {
        validateForm(walletItem);
    }

    @InjectView(R.id.codeImage)
    ImageView qrCodeImage;

    @InjectView(R.id.walletAddressButton)
    AutoResizeTextView addressButton;

    @OnClick(R.id.codeImage)
    public void codeButtonClicked() {
        setAddressOnClipboard(addressButton.getText().toString());
    }

    @OnClick(R.id.walletAddressButton)
    public void addressButtonClicked() {
        setAddressOnClipboard(addressButton.getText().toString());
    }

    private WalletItem walletItem;
    private ExchangeRateItem exchangeItem;
    private Bitmap qrImage;

    CompositeSubscription subscriptions = new CompositeSubscription();
    CompositeSubscription updateSubscriptions = new CompositeSubscription();


    public static RequestFragment newInstance() {
        return new RequestFragment();
    }

    public RequestFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            walletItem = savedInstanceState.getParcelable(EXTRA_WALLET);
            exchangeItem = savedInstanceState.getParcelable(EXTRA_EXCHANGE);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_WALLET, walletItem);
        outState.putParcelable(EXTRA_EXCHANGE, exchangeItem);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.request, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_paste:
                setAmountFromClipboard();
                return true;
            case R.id.action_scan:
                ((BaseActivity) getActivity()).launchScanner();
                return true;
            case R.id.action_share:
                if(walletItem != null) {
                    shareAddress(walletItem.address());
                }
                return true;
            case R.id.action_copy:
                if(walletItem != null) {
                    setAddressOnClipboard(walletItem.address());
                }
                return true;
            case R.id.action_blockchain:
                viewBlockChain(walletItem.address());
            default:
                break;
        }
        return false;
    }

    private void setCurrency() {
        String currency = exchangeService.getExchangeCurrency();
        if (currencyText != null) {
            currencyText.setText(currency);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_request, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        amountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (amountText != null && amountText.hasFocus()) {
                    String bitcoin = charSequence.toString();
                    calculateCurrencyAmount(bitcoin);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        fiatEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (fiatEditText != null && fiatEditText.hasFocus()) {
                    calculateBitcoinAmount(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        setupToolbar();
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
        setCurrency();
    }

    @Override
    public void onPause() {
        super.onPause();
        subscriptions.unsubscribe();
        updateSubscriptions.unsubscribe();
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

    private void setupToolbar() {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
            ab.setTitle(getString(R.string.view_title_request));
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void subscribeData() {

        dbManager.exchangeQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<List<ExchangeRateItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Exchange Rate subscription safely unsubscribed");
                    }
                })
                .subscribe(new Action1<List<ExchangeRateItem>>() {
                    @Override
                    public void call(List<ExchangeRateItem> results) {
                        String currency = exchangeService.getExchangeCurrency();
                        for (ExchangeRateItem rateItem : results) {
                            if (rateItem.currency().equals(currency)) {
                                exchangeItem = rateItem;
                                setCurrencyAmount();
                                break;
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        reportError(throwable);
                    }
                });

        dbManager.walletQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<WalletItem>bindUntilEvent(FragmentEvent.PAUSE))
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Wallet subscription safely unsubscribed");
                    }
                })
                .subscribe(new Action1<WalletItem>() {
                    @Override
                    public void call(WalletItem item) {
                        if(item != null) {
                            walletItem = item;
                            setWallet(walletItem);   
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        reportError(throwable);
                    }
                });
    }

    public void setWallet(final WalletItem item) {
        Observable.defer(new Func0<Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call() {
                        try {
                            Bitmap qrCode = (BitmapFactory.decodeByteArray(item.qrcode(), 0, item.qrcode().length));
                            return Observable.just(qrCode);
                        } catch (Exception e) {
                            Timber.e("Error reading wallet QR Code data: " + e.getLocalizedMessage());
                            return null;
                        }
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Bitmap subscription safely unsubscribed");
                    }
                })
                .compose(this.<Bitmap>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onCompleted() {
                    }
                    @Override
                    public void onError(final Throwable e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setupWallet(walletItem, qrImage);
                                    reportError(e);
                                }
                            });
                        }
                    }
                    @Override
                    public void onNext(final Bitmap bitmap) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    qrImage = bitmap;
                                    setupWallet(walletItem, qrImage);
                                }
                            });
                        }
                    }
                });
    }

    private void setupWallet(WalletItem walletItem, Bitmap qrImage) {
        if (walletItem.address() != null) {
            addressButton.setText(walletItem.address());
        }

        if (qrImage != null) {
            qrCodeImage.setImageBitmap(qrImage);
        }
    }

    private void showGeneratedQrCodeActivity(String bitcoinAddress, String bitcoinAmount) {
        assert bitcoinAddress != null;
        Intent intent = QRCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivity(intent);
    }

    public void setAmountFromClipboard() {
        
        String clipText = getClipboardText();
        if (Strings.isBlank(clipText)) {
            toast(R.string.toast_clipboard_empty);
            return;
        }

        if (WalletUtils.validAmount(clipText)) {
            setAmount(WalletUtils.parseBitcoinAmount(clipText));
        } else {
            toast(getString(R.string.toast_invalid_clipboard_contents));
        }
    }

    private String getClipboardText() {
        try {
            String clipText = "";
            ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = clipboardManager.getPrimaryClip();
            if (clip != null) {
                ClipData.Item item = clip.getItemAt(0);
                if (item.getText() != null)
                    clipText = item.getText().toString();
            }

            return clipText;
        } catch (Exception e) {
            reportError(e);
        }

        return "";
    }

    public void setAmount(String bitcoinAmount) {
        if (!Strings.isBlank(bitcoinAmount)) {
            amountText.setText(bitcoinAmount);
            calculateCurrencyAmount(bitcoinAmount);
        }
    }

    public void setCurrencyAmount() {
        if (Strings.isBlank(amountText.getText())) {
            calculateCurrencyAmount("0.00");
        } else {
            calculateCurrencyAmount(amountText.getText().toString());
        }
    }

    protected void validateForm(WalletItem walletItem) {
        if (walletItem == null) {
            toast(getString(R.string.toast_no_valid_address_bitcoin));
            return;
        }

        String amount = "";
        if (amountText != null) {
            amount = amountText.getText().toString();
            if (Strings.isBlank(amount)) {
                toast(getString(R.string.error_missing_amount));
                return;
            }
        }
        String bitcoinAmount = Conversions.formatBitcoinAmount(amount);
        showGeneratedQrCodeActivity(walletItem.address(), bitcoinAmount);
    }

    // TODO move to unit tests and utility classes
    private void calculateBitcoinAmount(String requestAmount) {

        if (exchangeItem == null) {
            return;
        }

        if (Doubles.convertToDouble(requestAmount) == 0) {
            if (amountText != null)
                amountText.setText("");
            return;
        }

        String rate = exchangeItem.rate();
        double btc = Math.abs(Doubles.convertToDouble(requestAmount) / Doubles.convertToDouble(rate));
        String amount = Conversions.formatBitcoinAmount(btc);
        if (amountText != null)
            amountText.setText(amount);
    }

    private void calculateCurrencyAmount(String bitcoin) {
        if (exchangeItem == null) {
            return;
        }

        if (Doubles.convertToDouble(bitcoin) == 0) {
            if (fiatEditText != null)
                fiatEditText.setText("");
            return;
        }

        String rate = exchangeItem.rate();
        String value = Calculations.computedValueOfBitcoin(rate, bitcoin);
        if (fiatEditText != null)
            fiatEditText.setText(value);
    }

    private void setAddressOnClipboard(@NonNull String address) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getActivity().getString(R.string.wallet_address_clipboard_title), address);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getActivity(), getActivity().getString(R.string.wallet_address_copied_toast), Toast.LENGTH_SHORT).show();
    }

    protected void viewBlockChain(@NonNull String address) {
        Intent blockChainIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BLOCKCHAIN_INFO_ADDRESS + address));
        startActivity(blockChainIntent);
    }

    protected void shareAddress(@NonNull String address) {
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
}