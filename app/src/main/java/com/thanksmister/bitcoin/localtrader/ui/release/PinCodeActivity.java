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

package com.thanksmister.bitcoin.localtrader.ui.release;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.ui.advertise.AdvertiserModule;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class PinCodeActivity extends BaseActivity implements PinCodeView
{
    public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";
    public static final String EXTRA_PIN_CODE = "EXTRA_PIN_CODE";
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    public static final String EXTRA_AMOUNT = "EXTRA_AMOUNT";
    
    public static final int REQUEST_CODE = 648;
    
    public static final int RESULT_VERIFIED = 7652;

    @Inject
    PinCodePresenter presenter;

    @InjectView(R.id.pinCode1)
    TextView pinCode1;

    @InjectView(R.id.pinCode2)
    TextView pinCode2;

    @InjectView(R.id.pinCode3)
    TextView pinCode3;

    @InjectView(R.id.pinCode4)
    TextView pinCode4;
    
    private boolean pinComplete;
    private String pinTxt1;
    private String pinTxt2;
    private String pinTxt3;
    private String pinTxt4;
    
    private String address;
    private String amount;
    private boolean sendingInProgress;

    public static Intent createStartIntent(Context context)
    {
        Intent intent = new Intent(context, PinCodeActivity.class);
        return intent;
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

        getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE); // show keyboard
 
        if (savedInstanceState != null) {
            
            sendingInProgress = savedInstanceState.getBoolean(EXTRA_PROGRESS);
            
            if(savedInstanceState.containsKey(EXTRA_AMOUNT))
                amount = savedInstanceState.getString(EXTRA_AMOUNT);

            if(savedInstanceState.containsKey(EXTRA_ADDRESS))
                address = savedInstanceState.getString(EXTRA_ADDRESS);
        } else {
            
            if(getIntent().hasExtra(EXTRA_AMOUNT))
                amount = getIntent().getStringExtra(EXTRA_AMOUNT);

            if(getIntent().hasExtra(EXTRA_ADDRESS))
                address = getIntent().getStringExtra(EXTRA_ADDRESS);
        }

        pinCode4.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void afterTextChanged(Editable editable)
            {
                pinTxt4 = editable.toString();
                if(!pinTxt4.trim().isEmpty() && !pinTxt4.equals("") && pinComplete) {
                    String pinCode = pinTxt1;
                    pinCode += pinTxt2;
                    pinCode += pinTxt3;
                    pinCode += pinTxt4;
                    if(pinCode.length() > 0) {
                        onSetPinCodeClick(pinCode);
                    } else {
                        pinComplete = false;
                    }
                } else {
                    
                    pinCode4.setCursorVisible(true);
                    pinCode4.setFocusable(true);
                    pinCode4.setFocusableInTouchMode(true);
                    pinCode4.requestFocus();
                    pinComplete = false;
                }
            }
        });

        pinCode3.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void afterTextChanged(Editable editable)
            {
                pinTxt3 = editable.toString();
                if(!pinTxt3.trim().isEmpty() && !pinTxt3.equals("")) {
                    pinCode3.setCursorVisible(false);
                    pinCode3.setFocusable(false);
                    pinCode3.setFocusableInTouchMode(false);

                    pinCode4.setCursorVisible(true);
                    pinCode4.setFocusableInTouchMode(true);
                    pinCode4.setFocusable(true);
                    pinCode4.requestFocus();
                    
                    pinComplete = true;
                } else {
                    
                    pinCode3.setCursorVisible(true);
                    pinCode3.setFocusable(true);
                    pinCode3.setFocusableInTouchMode(true);
                    pinCode3.requestFocus();
                    pinComplete = false;
                }
            }
        });

        pinCode2.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void afterTextChanged(Editable editable)
            {
                pinTxt2 = editable.toString();
                if(!pinTxt2.trim().isEmpty() && !pinTxt2.equals("")) {
                    pinCode2.setCursorVisible(false);
                    pinCode2.setFocusable(false);
                    pinCode2.setFocusableInTouchMode(false);

                    pinCode3.setCursorVisible(true);
                    pinCode3.setFocusableInTouchMode(true);
                    pinCode3.setFocusable(true);
                    pinCode3.requestFocus();

                    pinComplete = true;
                } else {
                    
                    pinCode2.setCursorVisible(true);
                    pinCode2.setFocusable(true);
                    pinCode2.setFocusableInTouchMode(true);
                    pinCode2.requestFocus();
                    pinComplete = false;
                }
            }
        });
    
        pinCode1.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }
            @Override
            public void afterTextChanged(Editable editable)
            {
                pinTxt1 = editable.toString();
                if(!pinTxt1.trim().isEmpty() && !pinTxt1.equals("")) {
                    pinCode1.setCursorVisible(false);
                    pinCode1.setFocusable(false);
                    pinCode1.setFocusableInTouchMode(false);

                    pinCode2.setCursorVisible(true);
                    pinCode2.setFocusableInTouchMode(true);
                    pinCode2.setFocusable(true);
                    pinCode2.requestFocus();
                    
                    pinComplete = true;
                } else {
                    
                    pinCode1.setCursorVisible(true);
                    pinCode1.setFocusable(true);
                    pinCode1.setFocusableInTouchMode(true);
                    pinCode1.requestFocus();
                    pinComplete = false;
                }
            }
        });

        pinCode1.requestFocus();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        presenter.onResume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putBoolean(EXTRA_PROGRESS, sendingInProgress);
        
        if(address != null)
            outState.putString(EXTRA_ADDRESS, address);
        
        if(amount != null)
            outState.putString(EXTRA_AMOUNT, amount);
    }

    private void onSetPinCodeClick(String pinCode)
    {
        sendingInProgress = true;
        
        presenter.validatePinCode(pinCode, address, amount);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
        // block back button while in progress
        if (keyCode == KeyEvent.KEYCODE_BACK && sendingInProgress) {   
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public Context getContext()
    {
        return this;
    }
    
}