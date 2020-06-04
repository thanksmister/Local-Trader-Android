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
import com.thanksmister.bitcoin.localtrader.ui.fragments.SendFragment

class SendActivity : BaseActivity() {

    private var address: String? = null
    private var amount: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
            supportActionBar!!.title = getString(R.string.view_title_send)
        }

        if (savedInstanceState != null) {
            address = savedInstanceState.getString(EXTRA_QR_ADDRESS)
            amount = savedInstanceState.getString(EXTRA_QR_AMOUNT)
        } else {
            address = intent.getStringExtra(EXTRA_QR_ADDRESS)
            amount = intent.getStringExtra(EXTRA_QR_AMOUNT)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.sendContainer, SendFragment.newInstance(address, amount))
                    .commitNow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SendActivity.EXTRA_QR_ADDRESS, address)
        outState.putString(SendActivity.EXTRA_QR_AMOUNT, amount)
        super.onSaveInstanceState(outState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
       /* if (requestCode == PinCodeActivity.REQUEST_CODE) {
            if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
                val pinCode = intent!!.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE)
                val address = intent.getStringExtra(PinCodeActivity.EXTRA_ADDRESS)
                val amount = intent.getStringExtra(PinCodeActivity.EXTRA_AMOUNT)
                pinCodeEvent(pinCode, address, amount)
            }
        }*/
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> {
            }
        }
        return false
    }

    companion object {
        const val EXTRA_QR_ADDRESS = "EXTRA_QR_ADDRESS"
        const val EXTRA_QR_AMOUNT = "EXTRA_QR_AMOUNT"
        fun createStartIntent(context: Context, address: String?, amount: String?): Intent {
            val intent = Intent(context, SendActivity::class.java)
            intent.putExtra(EXTRA_QR_ADDRESS, address)
            intent.putExtra(EXTRA_QR_AMOUNT, amount)
            return intent
        }
    }
}