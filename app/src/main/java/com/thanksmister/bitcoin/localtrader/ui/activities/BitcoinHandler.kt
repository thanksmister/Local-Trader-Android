/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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