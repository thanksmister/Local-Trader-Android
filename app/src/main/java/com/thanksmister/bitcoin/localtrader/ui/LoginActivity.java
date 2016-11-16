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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class LoginActivity extends BaseActivity
{
    @Inject
    DataService dataService;

    @Inject
    SharedPreferences sharedPreferences;
    
    @InjectView(R.id.content)
    View content;
    
    @InjectView(R.id.authenticateButton)
    Button authenticateButton;

    @InjectView(R.id.hmacKey)
    EditText hmacKey;

    @InjectView(R.id.hmacSecret)
    EditText hmacSecret;
    
    @InjectView(R.id.editTextDescription)
    TextView editTextDescription;

    private Subscription subscription = Subscriptions.unsubscribed();
    private int retryLimit = 1;

    public static Intent createStartIntent(Context context)
    {
        return new Intent(context, LoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_login);

        ButterKnife.inject(this);

        if (BuildConfig.DEBUG) {
            hmacKey.setText(R.string.hmac_key);
            hmacSecret.setText(R.string.hmac_secret);
        }

        editTextDescription.setText(Html.fromHtml(getString(R.string.setup_description)));
        editTextDescription.setMovementMethod(LinkMovementMethod.getInstance());
        authenticateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkCredentials();
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        subscription.unsubscribe();
    }

    private void checkCredentials()
    {
        String key = hmacKey.getText().toString();
        String secret = hmacSecret.getText().toString();
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(secret)) {
            showProgressDialog(new ProgressDialogEvent(getString(R.string.login_authorizing)));
            getMyself(key, secret);
        } else {
            hideProgressDialog();
            showAlertDialog(new AlertDialogEvent(null, getString(R.string.setup_form_error)));
        }
    }
    
    public Context getContext()
    {
        return this;
    }

    public void setAuthorization(final String key, final String secret, final User user)
    {
        AuthUtils.setHmacKey(sharedPreferences, key);
        AuthUtils.setHmacSecret(sharedPreferences, secret);
        AuthUtils.setUsername(sharedPreferences, user.username);
        AuthUtils.setFeedbackScore(sharedPreferences, user.feedback_score);
        AuthUtils.setTrades(sharedPreferences, String.valueOf(user.trading_partners_count));

        Timber.d("Username: " + user.username);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void getMyself(final String key, final String secret)
    {
        final String nonce = NetworkUtils.generateNonce();
        final String signature = NetworkUtils.createSignature(LocalBitcoins.GET_MYSELF, nonce, key, secret);

        subscription = dataService.getMyself(key, nonce, signature)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends User>>()
                {
                    @Override
                    public Observable<? extends User> call(Throwable throwable)
                    {
                        if (DataServiceUtils.isHttp41Error(throwable) && retryLimit > 0) {
                            retryLimit--;
                            return dataService.getMyself(key, nonce, signature);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                })
                .subscribe(new Action1<User>()
                {
                    @Override
                    public void call(User user)
                    {
                        hideProgressDialog();
                        toast(getString(R.string.authentication_success, user.username));
                        setAuthorization(key, secret, user);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        hideProgressDialog();
                        if (DataServiceUtils.isHttp403Error(throwable)) {
                            showAlertDialog(new AlertDialogEvent(null, getString(R.string.setup_form_error)));
                        } else {
                            handleError(throwable);
                        }
                    }
                });
    }
}