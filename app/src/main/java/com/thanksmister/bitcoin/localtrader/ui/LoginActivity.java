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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class LoginActivity extends BaseActivity
{
    public String OAUTH_URL = "";
    public final static String EXTRA_WHATS_NEW = "EXTRA_WHATS_NEW";
    public final static String EXTRA_WEB_LOGIN = "EXTRA_WEB_LOGIN";
    public final static String EXTRA_END_POINT = "EXTRA_END_POINT";
    
    @Inject
    DataService dataService;

    @Inject
    SharedPreferences sharedPreferences;
    
    @InjectView(R.id.content)
    View content;
    
    @InjectView(R.id.authenticateButton)
    Button authenticateButton;

   /* @InjectView(R.id.hmacKey)
    EditText hmacKey;

    @InjectView(R.id.hmacSecret)
    EditText hmacSecret;*/
    
    /*@InjectView(R.id.hmacCheckBox)
    CheckBox hmacCheckBox;*/
    
    @InjectView(R.id.urlTextDescription)
    TextView editTextDescription;
    
    /*@InjectView(R.id.hmacTextDescription)
    TextView hmacTextDescription;*/

    @InjectView(R.id.apiEndpoint)
    TextView apiEndpoint;

    private Subscription subscription = Subscriptions.unsubscribed();
    private WebView webView;
    private String endpoint;
    private boolean whatsNewShown;
    private boolean webViewLogin;
    private boolean useHmacAuthentication;

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

        /*if (BuildConfig.DEBUG) {
            hmacKey.setText(R.string.hmac_key);
            hmacSecret.setText(R.string.hmac_secret);
        }*/
        
        if(savedInstanceState != null) {
            whatsNewShown = savedInstanceState.getBoolean(EXTRA_WHATS_NEW);
            webViewLogin = savedInstanceState.getBoolean(EXTRA_WEB_LOGIN);
            endpoint = savedInstanceState.getString(EXTRA_END_POINT);
        }

        webView = (WebView) findViewById(R.id.webView);
        
        final String currentEndpoint = AuthUtils.getServiceEndpoint(sharedPreferences);
        apiEndpoint.setText(currentEndpoint);

        OAUTH_URL = currentEndpoint + "/oauth2/authorize/?ch=2hbo&client_id="
                + getString(R.string.lbc_access_key) + "&response_type=code&scope=read+write+money_pin";
        
        editTextDescription.setText(Html.fromHtml(getString(R.string.setup_description)));
        editTextDescription.setMovementMethod(LinkMovementMethod.getInstance());

        //hmacTextDescription.setText(Html.fromHtml(getString(R.string.setup_description_hmac)));
        //hmacTextDescription.setMovementMethod(LinkMovementMethod.getInstance());
        authenticateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkCredentials();
            }
        });

        /*hmacCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                useHmacAuthentication = hmacCheckBox.isChecked();
                
                if(hmacCheckBox.isChecked()) {
                    showAlertDialog(new AlertDialogEvent("Warning", "Be aware of the security risks when using HMAC if your device is ever compromised.<br/><br/>You may mitigate the risks by creating an authentication with only read/write permissions.<br/><br/>However adding the money_pin permissions means an attacker that can guess your PIN code may be able to access your funds."));
                } else {
                    hmacKey.setText("");
                    hmacSecret.setText("");
                }

                hmacKey.setEnabled(useHmacAuthentication);
                hmacSecret.setEnabled(useHmacAuthentication);
            }
        });*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_WHATS_NEW, whatsNewShown);
        outState.putBoolean(EXTRA_WEB_LOGIN, webViewLogin);
        outState.putString(EXTRA_END_POINT, endpoint);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            content.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            toast("Authentication canceled...");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        subscription.unsubscribe();
    }

    private void checkCredentials()
    {
        endpoint = apiEndpoint.getText().toString();
        final String currentEndpoint = AuthUtils.getServiceEndpoint(sharedPreferences);
        
        if(TextUtils.isEmpty(endpoint)) {
            hideProgressDialog();
            showAlertDialog(new AlertDialogEvent(null, "The service end point should not be a valid URL."));
            return;
        } else if (!Patterns.WEB_URL.matcher(endpoint).matches()){
            showAlertDialog(new AlertDialogEvent(null, "The service end point should not be a valid URL."));
            return;
        } else if (!currentEndpoint.equals(endpoint)) {
            showAlertDialog(new AlertDialogEvent(null, "Changing the service end point requires an application restart. Do you want to update the end point and restart now?"), new Action0()
            {
                @Override
                public void call()
                {
                    // save for preference manager
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                    SharedPreferences.Editor prefEditor = preferences.edit();
                    prefEditor.putString(getString(R.string.pref_key_api), endpoint).apply();
                    
                    // save for shared preferences
                    AuthUtils.setServiceEndPoint(sharedPreferences, endpoint);
                    
                    Intent intent = LoginActivity.createStartIntent(LoginActivity.this);
                    PendingIntent restartIntent = PendingIntent.getActivity(LoginActivity.this, 0, intent, 0);
                    AlarmManager alarmManager = (AlarmManager) LoginActivity.this.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);
                    System.exit(0);
                }
            }, new Action0()
            {
                @Override
                public void call()
                {
                    apiEndpoint.setText(currentEndpoint);
                }
            });
            
            return;
        }
        
        // check if we are using HMAC security
        /*if(useHmacAuthentication) {
            String key = hmacKey.getText().toString();
            String secret = hmacSecret.getText().toString();
            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(secret)) {
                showProgressDialog(new ProgressDialogEvent(getString(R.string.login_authorizing)));
                getMyselfHmac(key, secret, endpoint);
            } else {
                showAlertDialog(new AlertDialogEvent(null, getString(R.string.setup_form_error)));
            }
        } else {
            setUpWebViewDefaults();
        }*/

        setUpWebViewDefaults();
    }
    
    public Context getContext()
    {
        return this;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults()
    {
        content.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        showProgressDialog(new ProgressDialogEvent("Loading LocalBitcoins..."));

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
            if(!BuildConfig.DEBUG) {
                Crashlytics.log("WebView");
                Crashlytics.logException(e);
            }
            
            content.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            hideProgressDialog();
        }
    }

    public void setAuthorizationCode(final String code)
    {
        showProgressDialog(new ProgressDialogEvent(getString(R.string.login_authorizing)));
        
        content.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        
        Observable<Authorization> tokenObservable = dataService.getAuthorization(code);
        subscription = tokenObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization>()
                {
                    @Override
                    public void call(Authorization authorization)
                    {
                        getMyself(authorization.access_token, authorization.refresh_token, endpoint);
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

    public void setTokens(final String accessToken, final String refreshToken, final User user, final String endpoint)
    {
        // save for preference manager
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putString("pref_key_api", endpoint).apply();
        
        AuthUtils.setAccessToken(sharedPreferences, accessToken);
        AuthUtils.setRefreshToken(sharedPreferences, refreshToken);
        AuthUtils.setUsername(sharedPreferences, user.username);
        AuthUtils.setFeedbackScore(sharedPreferences, user.feedback_score);
        AuthUtils.setTrades(sharedPreferences, String.valueOf(user.trading_partners_count));
        AuthUtils.setServiceEndPoint(sharedPreferences, endpoint);

        Timber.d("Username: " + user.username);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void setAuthorization(final String key, final String secret, final User user, final String endpoint)
    {
        // save for preference manager
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putString("pref_key_api", endpoint).apply();

        AuthUtils.setHmacKey(sharedPreferences, key);
        AuthUtils.setHmacSecret(sharedPreferences, secret);
        AuthUtils.setUsername(sharedPreferences, user.username);
        AuthUtils.setFeedbackScore(sharedPreferences, user.feedback_score);
        AuthUtils.setTrades(sharedPreferences, String.valueOf(user.trading_partners_count));
        AuthUtils.setServiceEndPoint(sharedPreferences, endpoint);

        Timber.d("Username: " + user.username);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void getMyself(final String accessToken, final String refreshToken, final String endpoint)
    {
        subscription = dataService.getMyself(accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<User>()
                {
                    @Override
                    public void call(User user)
                    {
                        hideProgressDialog();
                        toast(getString(R.string.authentication_success, user.username));
                        setTokens(accessToken, refreshToken, user, endpoint);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        hideProgressDialog();
                        if (DataServiceUtils.isHttp403Error(throwable)) {
                            showAlertDialog(new AlertDialogEvent(null, getString(R.string.error_invalid_credentials)));
                        } else {
                            handleError(throwable);
                        }
                    }
                });
    }

    public void getMyselfHmac(final String key, final String secret, final String endpoint)
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
                        if (DataServiceUtils.isHttp41Error(throwable)) {
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
                        setAuthorization(key, secret, user, endpoint);
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

    private class OauthWebViewClient extends WebViewClient
    {
        /*// here you execute an action when the URL you want is about to load
        @Override
        public void onLoadResource(WebView  view, String  url){
            if( url.equals("http://cnn.com") ){
                // do whatever you want
            }
        }
        */
        
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            return handleUri(uri);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            return handleUri(uri);
        }

        private boolean handleUri(final Uri uri) {
            
            Timber.d("Uri =" + uri);
            
            final String host = uri.getHost();
            final String scheme = uri.getScheme();
            final String path = uri.getPath();
            
            Timber.d("Host =" + host);
            Timber.d("Scheme =" + scheme);
            Timber.d("Path =" + path);

            // Returning false means that you want to handle link in webview
            if (host.contains("thanksmr.com")) {
                Pattern codePattern = Pattern.compile("code=([^&]+)?");
                Matcher codeMatcher = codePattern.matcher(uri.toString());
                if (codeMatcher.find()) {
                    String code[] = codeMatcher.group().split("=");
                    if (code.length > 0) {
                        setAuthorizationCode(code[1]);
                        return false;
                    }
                } else {
                    content.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                    showAlertDialog(new AlertDialogEvent("Authentication Error", getString(R.string.error_invalid_credentials)));
                    return false;
                }
                    
            } else if (path.contains("authorize")
                    || path.contains("/oauth2/authorize/")
                    || path.contains("/oauth2/authorize/confirm")
                    || path.equals("/accounts/login/app/")
                    || path.equals("/accounts/prelogin/app/")
                    || path.equals("/oauth2/redirect")
                    || path.contains("/oauth2")
                    || path.contains("/accounts")
                    || path.contains("threefactor_login_verification")
                    || path.contains("/accounts/threefactor_login_verification/")) {

                hideProgressDialog();
                return false;

            } else if (path.equals("/oauth2/canceled") || path.equals("/logout/")) {

                content.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                toast("Authentication canceled...");
                return false;
                
            } else if (path.equals("/accounts/login/")) {

                webView.loadUrl(OAUTH_URL); // reload authentication page
                return false;
                
            } else if (path.contains("/register/app-fresh/") || path.contains("/register")) {

                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://localbitcoins.com/register/?ch=2hbo"));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    toast("Can't open external links from authentication flow.");
                }

                return true;

            } else if (path.contains("error=access_denied")) {

                content.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                showAlertDialog(new AlertDialogEvent("Authentication Error", getString(R.string.error_invalid_credentials)));
                return false;

            } else {
                // Returning true means that you need to handle what to do with the url
                // e.g. open web page in a Browser
                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    toast("Can't open external links from authentication flow.");
                }

                return true;
            }

            return false;
        }
    }
}