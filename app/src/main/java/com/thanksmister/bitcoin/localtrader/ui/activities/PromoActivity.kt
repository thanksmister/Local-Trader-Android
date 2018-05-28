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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import kotlinx.android.synthetic.main.view_promo.*


class PromoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_promo)
        registerButton.setOnClickListener({
            showRegistration()
        })
        loginButton.setOnClickListener({
            showLoginView()
        })
    }

    fun showLoginView() {
        val intent = LoginActivity.createStartIntent(this@PromoActivity)
        startActivity(intent)
    }

    fun showRegistration() {
        val url = Constants.REGISTRATION_URL
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: SecurityException) {
            showAlertDialogLinks(getString(R.string.error_traffic_rerouted))
        } catch (e: ActivityNotFoundException) {
            showAlertDialogLinks(getString(R.string.toast_error_no_installed_ativity))
        }
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, PromoActivity::class.java)
        }
    }
}