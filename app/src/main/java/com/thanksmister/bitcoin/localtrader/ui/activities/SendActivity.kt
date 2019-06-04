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