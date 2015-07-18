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
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;
//import timber.log.Timber;

import static rx.android.app.AppObservable.bindActivity;

public class LoginActivity extends BaseActivity
{
    @Inject
    DataService dataService;
    
    @Inject
    BriteDatabase db;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    DbManager dbManager;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.retryTextView)
    TextView errorTextView;

    @InjectView(R.id.webview)
    WebView webview;
    
    private Subscription subscription = Subscriptions.unsubscribed();

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

        initWebView();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscription.unsubscribe();
    }

    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        webview.setVisibility(View.GONE);
    }

    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        webview.setVisibility(View.VISIBLE);
    }

    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        webview.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    public void showMain()
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public Context getContext()
    {
        return this;
    }

    private void initWebView()
    {
        //load the url of the oAuth login page and client
        webview.setWebViewClient(new OauthWebViewClient());
        webview.setWebChromeClient(new WebChromeClient());

        //activates JavaScript (just in case)
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webview.loadUrl(Constants.OAUTH_URL);
    }

    private class OauthWebViewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            hideProgress();
            //Timber.e("URL: " + url);
            //check if the login was successful and the access token returned
            if (url.contains("thanksmr.com")) {
                Pattern codePattern = Pattern.compile("code=([^&]+)?");
                Matcher codeMatcher = codePattern.matcher(url);

                if (codeMatcher.find()) {
                    String code[] = codeMatcher.group().split("=");

                    //if (accessToken.length > 0 && expiresIn.length > 0) {
                    if (code.length > 0) {
                        //Timber.d("code: " + code[1]);
                        setAuthorizationCode(code[1]);
                        return true;
                    }
                }
                
            } else if (url.contains("authorize") || url.contains("oauth2") || url.contains("accounts") || url.contains("threefactor_login_verification")) {
                hideProgress();
                return false;
            } else if (url.contains("ads")) { // hack to get past 3 factor screen
                webview.loadUrl(Constants.OAUTH_URL); // reload authentication page
            } else if (url.contains("error=access_denied")) {
                handleError(new Throwable(getString(R.string.error_invalid_credentials)));
                return false;
            }

            return false;
        }
    }

    public void setAuthorizationCode(final String code)
    {
        Observable<Authorization> tokenObservable = bindActivity(this, dataService.getAuthorization(code));
        tokenObservable.subscribe(new Action1<Authorization>()
        {
            @Override
            public void call(Authorization authorization)
            {
                db.insert(SessionItem.TABLE, new SessionItem.Builder().access_token(authorization.access_token).refresh_token(authorization.refresh_token).build());
                getUser(authorization.access_token);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                //Timber.e(throwable.getLocalizedMessage());
                toast(getString(R.string.error_authentication));
            }
        });
    }

    public void getUser(String token)
    {
        Observable<User> userObservable = bindActivity(this, dataService.getMyself(token));
        subscription = userObservable.subscribe(new Action1<User>()
        {
            @Override
            public void call(User user)
            {
                //Timber.d("User: " + user.username);
                StringPreference stringPreference = new StringPreference(sharedPreferences, DbManager.PREFS_USER);
                stringPreference.set(user.username);

                toast("Login successful for " + user.username);
                showMain();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                toast("Login error");
            }
        });
    }
}
