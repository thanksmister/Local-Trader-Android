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

import android.content.Intent
import android.net.Uri
import android.os.Bundle

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity

class BitcoinHandler : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.data
        if (data != null) {
            val url = data.toString()
            val scheme = data.scheme // "http"
            val authorized = preferences.hasCredentials()
            if (!authorized) {
                toast("You need to be logged in to perform that action.")
                launchMainApplication()
            } else {
                val intent = MainActivity.createStartIntent(applicationContext, url)
                startActivity(intent)
                finish()
            }
        } else {
            toast(getString(R.string.toast_invalid_address))
            launchMainApplication()
        }
    }

    private fun launchMainApplication() {
        startActivity(Intent(this@BitcoinHandler, MainActivity::class.java))
        finish()
    }
}