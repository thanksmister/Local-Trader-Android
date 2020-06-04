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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity

class LocalMarketsActivity : AppCompatActivity() {

    private val downloadButton: Button by lazy {
        findViewById<Button>(R.id.downloadButton)
    }

    private val descriptionText: TextView by lazy {
        findViewById<TextView>(R.id.descriptionText)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_markets)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = "Local Markets"
        }

        descriptionText.text = getString(R.string.local_markets)
        downloadButton.setOnClickListener {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://localmarkets.app"))
                startActivity(browserIntent)
            } catch (exception: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, LocalMarketsActivity::class.java)
        }
    }
}