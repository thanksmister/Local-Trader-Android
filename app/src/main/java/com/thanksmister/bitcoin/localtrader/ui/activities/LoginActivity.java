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

package com.thanksmister.bitcoin.localtrader.ui.activities;

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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.network.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.network.api.model.User;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.network.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class LoginActivity extends BaseActivity {
    public String OAUTH_URL = "";
    public final static String EXTRA_WHATS_NEW = "EXTRA_WHATS_NEW";
    public final static String EXTRA_WEB_LOGIN = "EXTRA_WEB_LOGIN";
    public final static String EXTRA_END_POINT = "EXTRA_END_POINT";

    @Inject
    DataService dataService;

    @Inject
    SharedPreferences sharedPreferences;

    @BindView(R.id.content)
    View content;

    @BindView(R.id.authenticateButton)
    Button authenticateButton;

    @BindView(R.id.urlTextDescription)
    TextView editTextDescription;

    @BindView(R.id.apiEndpoint)
    TextView apiEndpoint;

    private Subscription subscription = Subscriptions.unsubscribed();
    private WebView webView;
    private String endpoint;
    private boolean whatsNewShown;
    private boolean webViewLogin;
    private Handler handler;


    public static Intent createStartIntent(Context context) {
        return new Intent(context, LoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_login);

        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            whatsNewShown = savedInstanceState.getBoolean(EXTRA_WHATS_NEW);
            webViewLogin = savedInstanceState.getBoolean(EXTRA_WEB_LOGIN);
            endpoint = savedInstanceState.getString(EXTRA_END_POINT);
        }

        handler = new Handler();
        webView = (WebView) findViewById(R.id.webView);

        final String currentEndpoint = AuthUtils.getServiceEndpoint(preference, sharedPreferences);
        Timber.d("currentEndpoint: " + currentEndpoint);
        apiEndpoint.setText(currentEndpoint);
        OAUTH_URL = currentEndpoint + "/oauth2/authorize/?ch=2hbo&client_id="
                + getString(R.string.lbc_access_key) + "&response_type=code&scope=read+write+money_pin";

        editTextDescription.setText(Html.fromHtml(getString(R.string.setup_description)));
        editTextDescription.setMovementMethod(LinkMovementMethod.getInstance());

        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCredentials();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_WHATS_NEW, whatsNewShown);
        outState.putBoolean(EXTRA_WEB_LOGIN, webViewLogin);
        outState.putString(EXTRA_END_POINT, endpoint);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            content.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            toast(getString(R.string.toast_authentication_canceled));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
        subscription.unsubscribe();
    }

    private void checkCredentials() {
        endpoint = apiEndpoint.getText().toString();
        final String currentEndpoint = AuthUtils.getServiceEndpoint(preference, sharedPreferences);

        Timber.d("endpoint: " + endpoint);
        Timber.d("currentEndpoint: " + currentEndpoint);
        
        if (TextUtils.isEmpty(endpoint)) {
            hideProgressDialog();
            showAlertDialog(new AlertDialogEvent(null, "The service end point should not be a valid URL."));
        } else if (!Patterns.WEB_URL.matcher(endpoint).matches()) {
            hideProgressDialog();
            showAlertDialog(new AlertDialogEvent(null, "The service end point should not be a valid URL."));
        } else if (!currentEndpoint.equals(endpoint)) {
            hideProgressDialog();
            showAlertDialog(new AlertDialogEvent(null, "Changing the service end point requires an application restart. Do you want to update the end point and restart now?"), 
                    new Action0() {
                        @Override
                        public void call() {
                            // save for shared preferences
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showProgressDialog(new ProgressDialogEvent("Restarting...."), true);
                                    apiEndpoint.setText(endpoint);
                                    Timber.d("endpoint: " + endpoint);
                                    Timber.d("currentEndpoint: " + currentEndpoint);
                                    AuthUtils.setServiceEndPoint(preference, endpoint);
                                    Timber.d("savedendpoint: " +  AuthUtils.getServiceEndpoint(preference, sharedPreferences));
                                    handler = new Handler();
                                    handler.postDelayed(refreshRunnable, 100);
                                }
                            });
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            apiEndpoint.setText(currentEndpoint);
                        }
                    });
        } else {
            // hide keyboard and notify
            try{
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (NullPointerException e) {
                Timber.e("Error closing keyboard");
            }

            setUpWebViewDefaults();
        }
    }

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            hideProgressDialog();
            Intent intent = LoginActivity.createStartIntent(LoginActivity.this);
            PendingIntent restartIntent = PendingIntent.getActivity(LoginActivity.this, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) LoginActivity.this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);
            System.exit(0);
        }
    };

    public Context getContext() {
        return this;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults() {
        content.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        showProgressDialog(new ProgressDialogEvent(getString(R.string.progress_loading_localbitcoins)), true);

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
            settings.setDisplayZoomControls(false);

            // Enable remote debugging via chrome://inspect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }

            // Load website
            webView.loadUrl(OAUTH_URL);

        } catch (Exception e) {

            Timber.e(e.getMessage());
            if (!BuildConfig.DEBUG) {
                Crashlytics.log("WebView");
                Crashlytics.logException(e);
            }

            content.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            hideProgressDialog();
        }
    }

    public void setAuthorizationCode(final String code) {
        showProgressDialog(new ProgressDialogEvent(getString(R.string.login_authorizing)), true);

        content.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        Observable<Authorization> tokenObservable = dataService.getAuthorization(code);
        subscription = tokenObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization>() {
                    @Override
                    public void call(Authorization authorization) {
                        getMyself(authorization.access_token, authorization.refresh_token, endpoint);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        toast(getString(R.string.error_authentication));
                    }
                });
    }

    public void setTokens(final String accessToken, final String refreshToken, final User user, final String endpoint) {
        // save for preference manager
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putString("pref_key_api", endpoint).apply();

        AuthUtils.setAccessToken(preference, accessToken);
        AuthUtils.setRefreshToken(preference, refreshToken);
        AuthUtils.setUsername(preference, user.username);
        AuthUtils.setFeedbackScore(preference, user.feedback_score);
        AuthUtils.setTrades(preference, String.valueOf(user.trading_partners_count));
        AuthUtils.setServiceEndPoint(preference, endpoint);

        Timber.d("Username: " + user.username);
        
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void getMyself(final String accessToken, final String refreshToken, final String endpoint) {
        subscription = dataService.getMyself(accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        hideProgressDialog();
                        toast(getString(R.string.authentication_success, user.username));
                        setTokens(accessToken, refreshToken, user, endpoint);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        if (DataServiceUtils.isHttp403Error(throwable)) {
                            showAlertDialog(new AlertDialogEvent(null, getString(R.string.error_invalid_credentials)));
                        } else {
                            handleError(throwable);
                        }
                    }
                });
    }


    private class OauthWebViewClient extends WebViewClient {
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
                    || path.contains("threefactor_login_verification")) {

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

            } else if (path.contains("cdn-cgi/l/chk_jschl")) {

                //webView.loadUrl(OAUTH_URL); // reload authentication page
                return false;

            } else {
                // Returning true means that you need to handle what to do with the url
                // e.g. open web page in a Browser
                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (SecurityException e) {
                    showAlertDialog(new AlertDialogEvent("Security Error", "It appears your traffic is being rerouted, you may want to try LocalBitcions.net. Here is the information: " + e.getMessage()));
                } catch (ActivityNotFoundException e) {
                    toast("Can't open external links from authentication flow.");
                }

                return true;
            }

            return false;
        }
    }
}