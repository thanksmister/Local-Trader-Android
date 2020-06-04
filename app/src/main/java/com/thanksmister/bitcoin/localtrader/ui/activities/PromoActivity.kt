/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import kotlinx.android.synthetic.main.view_promo.*

class PromoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_promo)
        registerButton.setOnClickListener {
            showRegistration()
        }
        loginButton.setOnClickListener {
            showLoginView()
        }
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        } else {
            finish()
        }
        System.exit(0);
    }

    private fun showLoginView() {
        val intent = LoginActivity.createStartIntent(this@PromoActivity)
        startActivity(intent)
    }

    private fun showRegistration() {
        val url = Constants.REGISTRATION_URL
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: SecurityException) {
            dialogUtils.showAlertDialog(this@PromoActivity, getString(R.string.error_traffic_rerouted))
        } catch (e: ActivityNotFoundException) {
            dialogUtils.showAlertDialog(this@PromoActivity, getString(R.string.toast_error_no_installed_ativity))
        }
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, PromoActivity::class.java)
        }
    }
}