package com.thanksmister.bitcoin.localtrader.ui.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.main.MainActivity;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class LoginActivity extends BaseActivity implements LoginView
{
    @Inject
    Bus bus;

    @Inject
    LoginPresenter presenter;

    @Inject
    DataService service;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.empty)
    View empty;
    
    @InjectView(R.id.emptyTextView)
    TextView errorTextView;

    @InjectView(R.id.webview)
    WebView webview;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_login);

        ButterKnife.inject(this);

        initWebView();
    }

    @Override
    public void onResume() {

        super.onResume();

        presenter.onResume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        presenter.onDestroy();

        ButterKnife.reset(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //toolbar.inflateMenu(R.menu.main);
        return true;
    }

    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new LoginModule(this));
    }

    // overrides for main view
    @Override
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        webview.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        webview.setVisibility(View.VISIBLE);
    }

    @Override
    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        webview.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    @Override
    public void showMain()
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
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
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            hideProgress();

            //check if the login was successful and the access token returned
            if (url.contains("thanksmr.com")) {
                Pattern codePattern = Pattern.compile("code=([^&]+)?");
                Matcher codeMatcher = codePattern.matcher(url);

                if(codeMatcher.find()){
                    String code [] = codeMatcher.group().split("=");

                    //if (accessToken.length > 0 && expiresIn.length > 0) {
                    if (code.length > 0 ) {
                        Timber.d("code: " + code[1]);
                        presenter.setAuthorizationCode(code[1]);
                        return true;
                    }
                }
                // TODO catch and handle https://localbitcoins.com/accounts/threefactor_login_verification same as Coinbase
                // https://localbitcoins.com/ads reload https://localbitcoins.com/oauth2/authorize/?client_id=c37c50b0b7f4e7ad40c2&response_type=code&scope=read+write
            } else if (url.contains("authorize") || url.contains("oauth2") || url.contains("accounts") || url.contains("threefactor_login_verification") ) {
                // TODO show progress
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
}
