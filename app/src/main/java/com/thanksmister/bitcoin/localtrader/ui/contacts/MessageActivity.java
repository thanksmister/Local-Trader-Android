/*
 * Copyright (c) 2016 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.trello.rxlifecycle.ActivityEvent;

import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class MessageActivity extends BaseActivity
{
    public static final String EXTRA_ID = "com.thanksmister.extras.EXTRA_ID";
    public static final String EXTRA_NAME = "com.thanksmister.extras.EXTRA_NAME";

    public static final int REQUEST_MESSAGE_CODE = 760;
    public static final int RESULT_MESSAGE_SENT = 765;
    public static final int RESULT_MESSAGE_CANCELED = 768;

    @Inject
    DataService dataService;

    @InjectView((R.id.messageTitle))
    TextView messageTitle;
    
    @InjectView((R.id.messageText))
    EditText messageText;
    
    private String contactId;
    private String contactName;

    @OnClick(R.id.messageButton)
    public void sendMessageButton()
    {
        validateMessage();
    }
    
    private Subscription subscription = Subscriptions.empty();

    public static Intent createStartIntent(@NonNull Context context, @NonNull String contactId, @NonNull String contactName)
    {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra(EXTRA_ID, contactId);
        intent.putExtra(EXTRA_NAME, contactName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_message);

        ButterKnife.inject(this);
        
        if (savedInstanceState == null) {
            contactId = getIntent().getStringExtra(EXTRA_ID);
            contactName = getIntent().getStringExtra(EXTRA_NAME);
        } else {
            contactId = savedInstanceState.getString(EXTRA_ID);
            contactName = savedInstanceState.getString(EXTRA_NAME);
        }
        
        messageTitle.setText("Message to " + contactName);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        subscription.unsubscribe();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ID, contactId);
        outState.putString(EXTRA_NAME, contactName);
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_MESSAGE_CANCELED);
        finish();
    }

    private void validateMessage()
    {
        String message = messageText.getText().toString();
        
        if (Strings.isBlank(message)) {
            toast("Message is blank...");
            return;
        }

        postMessage(message);
    }

    public void handleMessageSent()
    {
        messageText.setText("");
        
        // hide keyboard and notify
        try{
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.e("Error closing keyboard");
        }
        
        setResult(RESULT_MESSAGE_SENT);
        finish();
    }

    public void postMessage(String message)
    {
        showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_send_message)));

        dataService.postMessage(contactId, message)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Post message subscription safely unsubscribed");
                    }
                })
                .compose(this.<JSONObject>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>()
                {
                    @Override
                    public void call(JSONObject jsonObject)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                hideProgressDialog();
                                handleMessageSent();
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                hideProgressDialog();
                                toast(R.string.toast_error_message);
                            }
                        });
                    }
                });
    }
}