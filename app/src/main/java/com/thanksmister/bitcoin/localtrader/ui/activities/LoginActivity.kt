/*
 * Copyright (c) 2019 ThanksMister LLC
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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
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
import com.thanksmister.bitcoin.localtrader.persistence.Preferences.Companion.ALT_BASE_URL
import com.thanksmister.bitcoin.localtrader.persistence.Preferences.Companion.BASE_URL
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.LoginViewModel
import kotlinx.android.synthetic.main.view_login.*
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class LoginActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: LoginViewModel

    private var OAUTH_URL = ""
    private var endpoint: String? = null
    private var whatsNewShown: Boolean = false
    private var webViewLogin: Boolean = false

    val context: Context
        get() = this

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_login)

        if (savedInstanceState != null) {
            whatsNewShown = savedInstanceState.getBoolean(EXTRA_WHATS_NEW)
            webViewLogin = savedInstanceState.getBoolean(EXTRA_WEB_LOGIN)
            endpoint = savedInstanceState.getString(EXTRA_END_POINT)
        }
        
        val currentEndpoint = preferences.getServiceEndpoint()
        loginEndpointText.setText(currentEndpoint)
        OAUTH_URL = (currentEndpoint + "/oauth2/authorize/?ch=2hbo&client_id=" + BuildConfig.LBC_KEY + "&response_type=code&scope=read+write+money_pin")
        urlTextDescription.text = Html.fromHtml(getString(R.string.setup_description))
        urlTextDescription.movementMethod = LinkMovementMethod.getInstance()
        authenticateButton.setOnClickListener {
            dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.dialog_localbitcoins_website_login),
                    DialogInterface.OnClickListener { _, _ ->
                        checkCredentials()
                    }, DialogInterface.OnClickListener { _, _ ->
                        finish()
                    })
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        observeViewModel(viewModel)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_WHATS_NEW, whatsNewShown)
        outState.putBoolean(EXTRA_WEB_LOGIN, webViewLogin)
        outState.putString(EXTRA_END_POINT, endpoint)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dialogUtils.toast(getString(R.string.toast_authentication_canceled))
            if (loginWebView.visibility == View.VISIBLE) {
                loginContent.visibility = View.VISIBLE
                loginWebView.visibility = View.GONE
                return true
            }
            val intent = PromoActivity.createStartIntent(this@LoginActivity)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun observeViewModel(viewModel: LoginViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            Timber.d("getAlertMessage")
            if(message != null && !isFinishing)
                dialogUtils.showAlertDialog(this@LoginActivity, message)
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            Timber.d("getToastMessage")
            if(message != null) {
                dialogUtils.toast(message)
            }
        })
        viewModel.getAuthorized().observe(this, Observer {
            if(it == true && !isFinishing) {
                val intent = Intent(this, SplashActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        })
    }

    // TODO the end point should be a drop-down menu choice, not typed
    private fun checkCredentials() {
        endpoint = loginEndpointText.text.toString().trim()
        val currentEndpoint = preferences.getServiceEndpoint()
        if (TextUtils.isEmpty(endpoint)) {
            dialogUtils.hideProgressDialog()
            dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.alert_valid_endpoint))
            loginEndpointText.setText(BASE_URL)
        } else if (!Patterns.WEB_URL.matcher(endpoint!!).matches()) {
            dialogUtils.hideProgressDialog()
            dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.alert_valid_endpoint))
            loginEndpointText.setText(BASE_URL)
        } else if(endpoint != BASE_URL && endpoint != ALT_BASE_URL) {
            dialogUtils.hideProgressDialog()
            dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.error_invalid_endpoint))
            loginEndpointText.setText(BASE_URL)
        } else if (endpoint != currentEndpoint) {
            dialogUtils.hideProgressDialog()
            loginEndpointText.setText(endpoint)
            try {
                BaseActivity.hideSoftKeyboard(this@LoginActivity)
            } catch (e: NullPointerException) {
                Timber.e("Error closing keyboard")
            }
            setUpWebViewDefaults()
        } else {
            try {
                BaseActivity.hideSoftKeyboard(this@LoginActivity)
            } catch (e: NullPointerException) {
                Timber.e("Error closing keyboard")
            }
            setUpWebViewDefaults()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun setUpWebViewDefaults() {
        loginContent.visibility = View.GONE
        loginWebView.visibility = View.VISIBLE
        if(!isFinishing) {
            dialogUtils.showProgressDialog(this@LoginActivity, getString(R.string.progress_loading_localbitcoins), true)
            try {
                loginWebView.webViewClient = OauthWebViewClient()
                loginWebView.webChromeClient = WebChromeClient()
                val settings = loginWebView.settings
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                // Load website
                loginWebView.loadUrl(OAUTH_URL)
            } catch (e: Exception) {
                Timber.e(e.message)
                loginContent.visibility = View.VISIBLE
                loginWebView.visibility = View.GONE
                dialogUtils.hideProgressDialog()
            }
        }
    }

    fun setAuthorizationCode(code: String) {
        if(!isFinishing) {
            dialogUtils.showProgressDialog(this@LoginActivity, getString(R.string.login_authorizing), true)
            loginContent.visibility = View.VISIBLE
            loginWebView.visibility = View.GONE
            if (!TextUtils.isEmpty(endpoint)) {
                viewModel.setAuthorizationCode(code, endpoint!!)
            } else {
                dialogUtils.toast(getString(R.string.alert_valid_endpoint))
            }
        }
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
            val host = uri.host
            val scheme = uri.scheme
            val path = uri.path
            // Returning false means that you want to handle link in webview
            if (host.contains(BuildConfig.CALLBACK_URL)) {
                val codePattern = Pattern.compile("code=([^&]+)?")
                val codeMatcher = codePattern.matcher(uri.toString())
                if (codeMatcher.find()) {
                    val code = codeMatcher.group().split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (code.size > 0) {
                        setAuthorizationCode(code[1])
                        return false
                    }
                } else {
                    loginContent.visibility = View.VISIBLE
                    loginWebView.visibility = View.GONE
                    if(!isFinishing) {
                        dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.error_invalid_credentials))
                    }
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
                dialogUtils.hideProgressDialog()
                return false
            } else if (path == "/oauth2/canceled" || path == "/logout/") {
                loginContent.visibility = View.VISIBLE
                loginWebView.visibility = View.GONE
                dialogUtils.toast(getString(R.string.toast_authentication_canceled))
                return false
            } else if (path == "/accounts/login/") {
                loginWebView.loadUrl(OAUTH_URL) // reload authentication page
                return false
            } else if (path.contains("/register/app-fresh/") || path.contains("/register")) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://localbitcoins.com/register/?ch=2hbo"))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    dialogUtils.toast(getString(R.string.error_cannot_open_links))
                }
                return true
            } else if (path.contains("error=access_denied")) {
                loginContent.visibility = View.VISIBLE
                loginWebView.visibility = View.GONE
                dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.error_invalid_credentials))
                return false
            } else if (path.contains("cdn-cgi/l/chk_jschl")) {
                //webView.loadUrl(OAUTH_URL); // reload authentication page
                return false
            } else {
                // Returning true means that you need to handle what to do with the url
                // e.g. open web page in a Browser
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e: SecurityException) {
                    dialogUtils.showAlertDialog(this@LoginActivity, getString(R.string.error_traffic_rerouted) + e.message)
                } catch (e: ActivityNotFoundException) {
                    dialogUtils.toast(getString(R.string.error_cannot_open_links))
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