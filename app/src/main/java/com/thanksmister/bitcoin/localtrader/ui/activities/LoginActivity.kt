/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.LoginViewModel
import kotlinx.android.synthetic.main.view_login.*
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class LoginActivity : BaseActivity() {

    var OAUTH_URL = ""

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: LoginViewModel

    private var endpoint: String? = null
    private var whatsNewShown: Boolean = false
    private var webViewLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_login)

        if (savedInstanceState != null) {
            whatsNewShown = savedInstanceState.getBoolean(EXTRA_WHATS_NEW)
            webViewLogin = savedInstanceState.getBoolean(EXTRA_WEB_LOGIN)
            endpoint = savedInstanceState.getString(EXTRA_END_POINT)
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        observeViewModel(viewModel)
        lifecycle.addObserver(dialogUtils)

        val endpoint = preferences.endPoint()
        apiEndpointText.setText(endpoint)

        OAUTH_URL = (endpoint + "/oauth2/authorize/?ch=2hbo&client_id=" + BuildConfig.LBC_KEY + "&response_type=code&scope=read+write+money_pin")

        urlTextDescription.text = Html.fromHtml(getString(R.string.setup_description))
        urlTextDescription.movementMethod = LinkMovementMethod.getInstance()
        authenticateButton.setOnClickListener { checkCredentials() }
        loginProgressBar.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_WHATS_NEW, whatsNewShown)
        outState.putBoolean(EXTRA_WEB_LOGIN, webViewLogin)
        outState.putString(EXTRA_END_POINT, endpoint)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            toast(getString(R.string.toast_authentication_canceled))
            if (webView.visibility == View.VISIBLE) {
                showContentView()
                return true
            }
            val intent = PromoActivity.createStartIntent(this@LoginActivity)
            startActivity(intent)
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun observeViewModel(viewModel: LoginViewModel) {
        viewModel.getNavigateNextView().observe(this, Observer {response ->
            if(response == true) {
                loginProgressBar.visibility = View.GONE
                navigateSplashScreen()
            }
        })
        viewModel.getAlertMessage().observe(this, Observer {message ->
            showContentView()
            dialogUtils.showAlertDialog(this@LoginActivity, message!!)
        })
    }
    
    private fun checkCredentials() {
        val endpoint = apiEndpointText.text.toString()
        if (TextUtils.isEmpty(endpoint)) {
            toast(getString(R.string.alert_valid_endpoint))
        } else if (!Patterns.WEB_URL.matcher(endpoint).matches()) {
            toast(getString(R.string.alert_valid_endpoint))
        } else {
            preferences.endPoint(endpoint)
            setUpWebViewDefaults()
        }
    }

    // TODO this would be better with fragments and animation using navigation from jetpack
    private fun showContentView() {
        loginProgressBar.visibility = View.GONE
        apiEndpointText.isEnabled = true
        authenticateButton.isEnabled = true
        loginContentView.visibility = View.VISIBLE
        webView!!.visibility = View.GONE
    }

    private fun showContentViewLoading() {
        loginProgressBar.visibility = View.VISIBLE
        apiEndpointText.isEnabled = false
        authenticateButton.isEnabled = false
        loginContentView.visibility = View.VISIBLE
        webView!!.visibility = View.GONE
    }

    private fun showWebView() {
        apiEndpointText.isEnabled = false
        authenticateButton.isEnabled = false
        loginContentView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        loginProgressBar.visibility = View.GONE
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun setUpWebViewDefaults() {
        showWebView()
        try {
            webView.webViewClient = OauthWebViewClient()
            webView.webChromeClient = WebChromeClient()

            val settings = webView!!.settings

            // Enable Javascript
            settings.javaScriptEnabled = true

            // Use WideViewport and Zoom out if there is no viewport defined
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            // Enable pinch to zoom without the zoom buttons
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            // Enable remote debugging via chrome://inspect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            // Load website
            webView.loadUrl(OAUTH_URL)
        } catch (e: Exception) {
            Timber.e(e.message)
            if (!BuildConfig.DEBUG) {
                Crashlytics.log("WebView")
                Crashlytics.logException(e)
            }
            showContentView()
        }
    }

    fun setAuthorizationCode(code: String) {
        showContentViewLoading()
        viewModel.getOauthTokens(code, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
    }

    fun navigateSplashScreen() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private inner class OauthWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)
            return handleUri(uri)
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return handleUri(uri)
        }

        private fun handleUri(uri: Uri): Boolean {
            Timber.d("Uri =$uri")

            val host = uri.host
            val scheme = uri.scheme
            val path = uri.path

            Timber.d("Host =$host")
            Timber.d("Scheme =$scheme")
            Timber.d("Path =$path")

            // Returning false means that you want to handle link in webview
            if (host.contains("thanksmr.com")) {
                val codePattern = Pattern.compile("code=([^&]+)?")
                val codeMatcher = codePattern.matcher(uri.toString())
                if (codeMatcher.find()) {
                    val code = codeMatcher.group().split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (code.isNotEmpty()) {
                        setAuthorizationCode(code[1])
                        return false
                    }
                } else {
                    loginContentView.visibility = View.VISIBLE
                    webView.visibility = View.GONE
                    dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.alert_authentication_error_title), getString(R.string.error_invalid_credentials))
                    return false
                }
            } else if (path.contains("authorize")
                    || path.contains("/oauth2/authorize/")
                    || path.contains("/oauth2/authorize/confirm")
                    || path == "/accounts/login/app/"
                    || path == "/accounts/prelogin/app/"
                    || path == "/oauth2/redirect"
                    || path.contains("/oauth2")
                    || path.contains("/accounts")
                    || path.contains("threefactor_login_verification")) {
                hideProgressDialog()
                return false
            } else if (path == "/oauth2/canceled" || path == "/logout/") {
                showContentView()
                toast(R.string.toast_authentication_canceled)
                return false
            } else if (path == "/accounts/login/") {
                webView.loadUrl(OAUTH_URL) // reload authentication page
                return false
            } else if (path.contains("/register/app-fresh/") || path.contains("/register")) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://localbitcoins.com/register/?ch=2hbo"))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.error_cannot_open_links))
                }
                return true
            } else if (path.contains("error=access_denied")) {
                showContentView()
                dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.alert_authentication_error_title), getString(R.string.error_invalid_credentials))
                return false
            } else if (path.contains("cdn-cgi/l/chk_jschl")) {
                return false
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e: SecurityException) {
                    showAlertDialog(AlertDialogEvent(getString(R.string.error_security_error_title), getString(R.string.error_traffic_rerouted) + e.message))
                } catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.error_cannot_open_links))
                }
                return true
            }
            return false
        }
    }

    companion object {
        const val EXTRA_WHATS_NEW = "EXTRA_WHATS_NEW"
        const val EXTRA_WEB_LOGIN = "EXTRA_WEB_LOGIN"
        const val EXTRA_END_POINT = "EXTRA_END_POINT"
        fun createStartIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }
}