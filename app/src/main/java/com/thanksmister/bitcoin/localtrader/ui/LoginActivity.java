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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
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
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class LoginActivity extends BaseActivity
{
    public String OAUTH_URL = "";

    @Inject
    DataService dataService;
    
    @Inject
    BriteDatabase db;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.loginProgress)
    View progress;

    //@InjectView(R.id.webView)
    WebView webView;
    
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
        
        OAUTH_URL = "https://localbitcoins.com/oauth2/authorize/?ch=2hbo&client_id="
                + getString(R.string.lbc_access_key) + "&response_type=code&scope=read+write+money_pin";
        
        setUpWebViewDefaults();

        //initWebView();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults() {

        try {
            
            webView = (WebView) findViewById(R.id.webView);

            webView.setWebViewClient(new OauthWebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            
            WebSettings settings = webView.getSettings();

            // Enable Javascript
            settings.setJavaScriptEnabled(true);

            // Use WideViewport and Zoom out if there is no viewport defined
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);

            // Enable pinch to zoom without the zoom buttons
            settings.setBuiltInZoomControls(true);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                // Hide the zoom controls for HONEYCOMB+
                settings.setDisplayZoomControls(false);
            }

            // Enable remote debugging via chrome://inspect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }

            // Load website
            webView.loadUrl(OAUTH_URL);

        } catch (Exception e) {
            Timber.e(e.getMessage());
            Crashlytics.log("WebView");
            Crashlytics.logException(e);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscription.unsubscribe();
    }
    
    public void hideProgress()
    {
        if(progress != null)
            progress.setVisibility(View.GONE);
        
        if(webView != null)
            webView.setVisibility(View.VISIBLE);
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

    @Deprecated
    private void initWebView()
    {
        try{

            webView = (WebView) findViewById(R.id.webView);

            //load the url of the oAuth login page and client
            webView.setWebViewClient(new OauthWebViewClient());
            webView.setWebChromeClient(new WebChromeClient());

            //activates JavaScript (just in case)
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webView.loadUrl(OAUTH_URL);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class OauthWebViewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            hideProgress();
            
            //check if the login was successful and the access token returned
            if (url.contains("thanksmr.com")) {
                Pattern codePattern = Pattern.compile("code=([^&]+)?");
                Matcher codeMatcher = codePattern.matcher(url);

                if (codeMatcher.find()) {
                    String code[] = codeMatcher.group().split("=");

                    if (code.length > 0) {
                        setAuthorizationCode(code[1]);
                        return true;
                    }
                }
                
            } else if (url.contains("authorize") || url.contains("oauth2") || url.contains("accounts") || url.contains("threefactor_login_verification")) {
                hideProgress();
                return false;
            } else if (url.contains("ads")) { // hack to get past 3 factor screen
                webView.loadUrl(OAUTH_URL); // reload authentication page
            } else if (url.contains("error=access_denied")) {
                handleError(new Throwable(getString(R.string.error_invalid_credentials)));
                return false;
            }

            return false;
        }
    }

    public void setAuthorizationCode(final String code)
    {
        Observable<Authorization> tokenObservable = dataService.getAuthorization(code);
        subscription = tokenObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization>()
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
                        toast(getString(R.string.error_authentication));
                    }
                });
    }

    public void getUser(String token)
    {
        Observable<User> userObservable = dataService.getMyself(token);
        subscription = userObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<User>()
                {
                    @Override
                    public void call(User user)
                    {
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
