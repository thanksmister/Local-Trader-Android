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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.utils.Strings;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class PinCodeActivity extends BaseActivity
{
    public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";
    public static final String EXTRA_PIN_CODE = "EXTRA_PIN_CODE";
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    public static final String EXTRA_AMOUNT = "EXTRA_AMOUNT";

    public static final int MAX_PINCODE_LENGTH = 4;
    public static final int REQUEST_CODE = 648;
    public static final int RESULT_VERIFIED = 7652;
    public static final int RESULT_CANCELED = 7653;

    @Inject
    DataService dataService;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.pinCode1)
    ImageView pinCode1;

    @InjectView(R.id.pinCode2)
    ImageView pinCode2;

    @InjectView(R.id.pinCode3)
    ImageView pinCode3;

    @InjectView(R.id.pinCode4)
    ImageView pinCode4;

    @OnClick(R.id.button0)
    public void button0Clicked()
    {
        addPinCode("0");
    }

    @OnClick(R.id.button1)
    public void button1Clicked()
    {
        addPinCode("1");
    }

    @OnClick(R.id.button2)
    public void button2Clicked()
    {
        addPinCode("2");
    }

    @OnClick(R.id.button3)
    public void button3Clicked()
    {
        addPinCode("3");
    }

    @OnClick(R.id.button4)
    public void button4Clicked()
    {
        addPinCode("4");
    }

    @OnClick(R.id.button5)
    public void button5Clicked()
    {
        addPinCode("5");
    }

    @OnClick(R.id.button6)
    public void button6Clicked()
    {
        addPinCode("6");
    }

    @OnClick(R.id.button7)
    public void button7Clicked()
    {
        addPinCode("7");
    }

    @OnClick(R.id.button8)
    public void button8Clicked()
    {
        addPinCode("8");
    }

    @OnClick(R.id.button9)
    public void button9Clicked()
    {
        addPinCode("9");
    }

    @OnClick(R.id.buttonDel)
    public void buttonDelClicked()
    {
        removePinCode();
    }

    private boolean pinComplete = false;
    private String pinCode = "";
    private String address;
    private String amount;
    private Subscription subscription = Subscriptions.empty();

    public static Intent createStartIntent(Context context)
    {
        return new Intent(context, PinCodeActivity.class);
    }

    public static Intent createStartIntent(Context context, String address, String amount)
    {
        Intent intent = new Intent(context, PinCodeActivity.class);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_AMOUNT, amount);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_release);

        ButterKnife.inject(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // show keyboard

        if (savedInstanceState != null) {
            
            if (savedInstanceState.containsKey(EXTRA_AMOUNT))
                amount = savedInstanceState.getString(EXTRA_AMOUNT);

            if (savedInstanceState.containsKey(EXTRA_ADDRESS))
                address = savedInstanceState.getString(EXTRA_ADDRESS);

            if (savedInstanceState.containsKey(EXTRA_PIN_CODE))
                pinCode = savedInstanceState.getString(EXTRA_PIN_CODE);

        } else {

            if (getIntent().hasExtra(EXTRA_AMOUNT))
                amount = getIntent().getStringExtra(EXTRA_AMOUNT);

            if (getIntent().hasExtra(EXTRA_ADDRESS))
                address = getIntent().getStringExtra(EXTRA_ADDRESS);
        }

        setupToolbar();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscription.unsubscribe();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        if (address != null)
            outState.putString(EXTRA_ADDRESS, address);

        if (amount != null)
            outState.putString(EXTRA_AMOUNT, amount);

        if (pinCode != null)
            outState.putString(EXTRA_PIN_CODE, pinCode);
    }

    private void setupToolbar()
    {
        // Show menu icon
        setSupportActionBar(toolbar);
        final ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle("Enter PIN Code");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            toast(R.string.toast_pin_code_canceled);
            setResult(PinCodeActivity.RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        toast(R.string.toast_pin_code_canceled);
        setResult(PinCodeActivity.RESULT_CANCELED);
        finish();
    }

    private void invalidatePinCode()
    {
        pinCode = "";
        pinComplete = false;
        showFilledPins(0);
        toast(R.string.toast_pin_code_invalid, true);
    }

    private void addPinCode(String code)
    {
        if (pinComplete) return;

        pinCode += code;

        showFilledPins(pinCode.length());

        if (pinCode.length() == MAX_PINCODE_LENGTH) {
            pinComplete = true;
            onSetPinCodeClick(pinCode);
        }
    }

    private void removePinCode()
    {
        if (pinComplete) return;

        if (!Strings.isBlank(pinCode)) {
            pinCode = pinCode.substring(0, pinCode.length() - 1);
            showFilledPins(pinCode.length());
        }
    }

    private void onSetPinCodeClick(String pinCode)
    {
        validatePinCode(pinCode, address, amount);
    }

    private void showFilledPins(int pinsShown)
    {
        switch (pinsShown) {
            case 1:
                pinCode1.setImageResource(R.drawable.ic_pin_code_on);
                pinCode2.setImageResource(R.drawable.ic_pin_code_off);
                pinCode3.setImageResource(R.drawable.ic_pin_code_off);
                pinCode4.setImageResource(R.drawable.ic_pin_code_off);
                break;
            case 2:
                pinCode1.setImageResource(R.drawable.ic_pin_code_on);
                pinCode2.setImageResource(R.drawable.ic_pin_code_on);
                pinCode3.setImageResource(R.drawable.ic_pin_code_off);
                pinCode4.setImageResource(R.drawable.ic_pin_code_off);
                break;
            case 3:
                pinCode1.setImageResource(R.drawable.ic_pin_code_on);
                pinCode2.setImageResource(R.drawable.ic_pin_code_on);
                pinCode3.setImageResource(R.drawable.ic_pin_code_on);
                pinCode4.setImageResource(R.drawable.ic_pin_code_off);
                break;
            case 4:
                pinCode1.setImageResource(R.drawable.ic_pin_code_on);
                pinCode2.setImageResource(R.drawable.ic_pin_code_on);
                pinCode3.setImageResource(R.drawable.ic_pin_code_on);
                pinCode4.setImageResource(R.drawable.ic_pin_code_on);
                break;
            default:
                pinCode1.setImageResource(R.drawable.ic_pin_code_off);
                pinCode2.setImageResource(R.drawable.ic_pin_code_off);
                pinCode3.setImageResource(R.drawable.ic_pin_code_off);
                pinCode4.setImageResource(R.drawable.ic_pin_code_off);
                break;
        }
    }

    private void validatePinCode(final String pinCode, final String address, final String amount)
    {
        showProgressDialog(new ProgressDialogEvent("Verifying PIN..."), true);
        
        subscription = dataService.validatePinCode(pinCode)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>()
                {
                    @Override
                    public void call(JSONObject jsonObject)
                    {
                        hideProgressDialog();
                        
                        try {
                            JSONObject object = jsonObject.getJSONObject("data");
                            Boolean valid = (object.getString("pincode_ok").equals("true"));
                            if (valid) {
                                Intent intent = getIntent();
                                intent.putExtra(PinCodeActivity.EXTRA_PIN_CODE, pinCode);
                                intent.putExtra(PinCodeActivity.EXTRA_ADDRESS, address);
                                intent.putExtra(PinCodeActivity.EXTRA_AMOUNT, amount);
                                setResult(PinCodeActivity.RESULT_VERIFIED, intent);
                                finish();
                            } else {
                                Timber.d(object.toString());
                                invalidatePinCode();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            invalidatePinCode();
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        hideProgressDialog();
                        invalidatePinCode();
                    }
                });
    }
}