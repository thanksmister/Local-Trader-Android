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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.fragments.SettingsFragment

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        if (supportActionBar != null) {
            supportActionBar!!.setTitle(R.string.title_activity_settings)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // Display the fragment as the main content
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.content, SettingsFragment()).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}
