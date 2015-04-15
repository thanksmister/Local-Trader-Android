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

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindActivity;

public class LoginActivity extends BaseActivity
{
    @Inject
    SqlBrite db;

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

            //check if the login was successful and the access token returned
            if (url.contains("thanksmr.com")) {
                Pattern codePattern = Pattern.compile("code=([^&]+)?");
                Matcher codeMatcher = codePattern.matcher(url);

                if (codeMatcher.find()) {
                    String code[] = codeMatcher.group().split("=");

                    //if (accessToken.length > 0 && expiresIn.length > 0) {
                    if (code.length > 0) {
                        Timber.d("code: " + code[1]);
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
                showError("Access denied, invalid credentials");
                return false;
            }

            return false;
        }
    }

    public void setAuthorizationCode(final String code)
    {
        Observable<Authorization> tokenObservable = bindActivity(this, dbManager.getAuthorization(code));
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
                Timber.e(throwable.getLocalizedMessage());
                Toast.makeText(getContext(), getString(R.string.error_authentication), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void getUser(String token)
    {
        Observable<User> userObservable = bindActivity(this, dbManager.getMyself(token));
        userObservable.subscribe(new Action1<User>()
        {
            @Override
            public void call(User user)
            {
                Timber.d("User: " + user.username);
                StringPreference stringPreference = new StringPreference(sharedPreferences, DbManager.PREFS_USER);
                stringPreference.set(user.username);

                Toast.makeText(getContext(), "Login successful for " + user.username, Toast.LENGTH_SHORT).show();
                showMain();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                showError("Login error");
            }
        });
    }
}
