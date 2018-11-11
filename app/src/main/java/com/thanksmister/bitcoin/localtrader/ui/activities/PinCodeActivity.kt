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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.utils.Strings

import org.json.JSONException
import org.json.JSONObject

import timber.log.Timber

class PinCodeActivity : BaseActivity() {

    internal var pinCode1: ImageView? = null
    internal var pinCode2: ImageView? = null
    internal var pinCode3: ImageView? = null
    internal var pinCode4: ImageView? = null
    internal var description: TextView? = null

    private var pinComplete = false
    private var pinCode: String? = ""
    private var address: String? = null
    private var amount: String? = null

    fun button0Clicked() {
        addPinCode("0")
    }

    fun button1Clicked() {
        addPinCode("1")
    }

    fun button2Clicked() {
        addPinCode("2")
    }

    fun button3Clicked() {
        addPinCode("3")
    }

    fun button4Clicked() {
        addPinCode("4")
    }

    fun button5Clicked() {
        addPinCode("5")
    }

    fun button6Clicked() {
        addPinCode("6")
    }

    fun button7Clicked() {
        addPinCode("7")
    }

    fun button8Clicked() {
        addPinCode("8")
    }

    fun button9Clicked() {
        addPinCode("9")
    }

    fun buttonDelClicked() {
        removePinCode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_release)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN) // show keyboard

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
            supportActionBar!!.title = getString(R.string.title_enter_pincode)
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_AMOUNT))
                amount = savedInstanceState.getString(EXTRA_AMOUNT)
            if (savedInstanceState.containsKey(EXTRA_ADDRESS))
                address = savedInstanceState.getString(EXTRA_ADDRESS)
            if (savedInstanceState.containsKey(EXTRA_PIN_CODE))
                pinCode = savedInstanceState.getString(EXTRA_PIN_CODE)

        } else {
            if (intent.hasExtra(EXTRA_AMOUNT))
                amount = intent.getStringExtra(EXTRA_AMOUNT)
            if (intent.hasExtra(EXTRA_ADDRESS))
                address = intent.getStringExtra(EXTRA_ADDRESS)
        }

        description!!.text = Html.fromHtml(getString(R.string.pin_code_trade))
        description!!.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (address != null)
            outState.putString(EXTRA_ADDRESS, address)
        if (amount != null)
            outState.putString(EXTRA_AMOUNT, amount)
        if (pinCode != null)
            outState.putString(EXTRA_PIN_CODE, pinCode)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            toast(R.string.toast_pin_code_canceled)
            setResult(PinCodeActivity.RESULT_CANCELED)
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        toast(R.string.toast_pin_code_canceled)
        setResult(PinCodeActivity.RESULT_CANCELED)
        finish()
    }

    private fun invalidatePinCode() {
        pinCode = ""
        pinComplete = false
        showFilledPins(0)
        toast(R.string.toast_pin_code_invalid)
    }

    private fun addPinCode(code: String) {
        if (pinComplete) return

        pinCode += code

        showFilledPins(pinCode!!.length)

        if (pinCode!!.length == MAX_PINCODE_LENGTH) {
            pinComplete = true
            onSetPinCodeClick(pinCode)
        }
    }

    private fun removePinCode() {
        if (pinComplete) return

        if (!Strings.isBlank(pinCode)) {
            pinCode = pinCode!!.substring(0, pinCode!!.length - 1)
            showFilledPins(pinCode!!.length)
        }
    }

    private fun onSetPinCodeClick(pinCode: String?) {
        validatePinCode(pinCode, address, amount)
    }

    private fun showFilledPins(pinsShown: Int) {
        when (pinsShown) {
            1 -> {
                pinCode1!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode2!!.setImageResource(R.drawable.ic_pin_code_off)
                pinCode3!!.setImageResource(R.drawable.ic_pin_code_off)
                pinCode4!!.setImageResource(R.drawable.ic_pin_code_off)
            }
            2 -> {
                pinCode1!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode2!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode3!!.setImageResource(R.drawable.ic_pin_code_off)
                pinCode4!!.setImageResource(R.drawable.ic_pin_code_off)
            }
            3 -> {
                pinCode1!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode2!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode3!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode4!!.setImageResource(R.drawable.ic_pin_code_off)
            }
            4 -> {
                pinCode1!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode2!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode3!!.setImageResource(R.drawable.ic_pin_code_on)
                pinCode4!!.setImageResource(R.drawable.ic_pin_code_on)
            }
            else -> {
                pinCode1!!.setImageResource(R.drawable.ic_pin_code_off)
                pinCode2!!.setImageResource(R.drawable.ic_pin_code_off)
                pinCode3!!.setImageResource(R.drawable.ic_pin_code_off)
                pinCode4!!.setImageResource(R.drawable.ic_pin_code_off)
            }
        }
    }

    private fun validatePinCode(pinCode: String?, address: String?, amount: String?) {
        showProgressDialog(getString(R.string.progress_pin_verify), true)
        /*
        subscription = dataService.validatePinCode(pinCode)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject jsonObject) {
                        hideProgressDialog();

                        try {
                            JSONObject object = jsonObject.getJSONObject("data");
                            Boolean valid = (object.getString("pincode_ok").equals("true"));
                            if (valid) {
                                Intent intent = getIntent();
                                intent.putExtra(PinCodeActivity.EXTRA_PIN_CODE, pinCode);
                                intent.putExtra(PinCodeActivity.EXTRA_ADDRESS, address);
                                intent.putExtra(PinCodeActivity.EXTRA_AMOUNT, amount);
                                setResult(PinCodeActivity.RESULT_VERIFIED, intent);
                                finish();
                            } else {
                                Timber.d(object.toString());
                                invalidatePinCode();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            invalidatePinCode();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        invalidatePinCode();
                    }
                });*/
    }

    companion object {
        val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        val EXTRA_PIN_CODE = "EXTRA_PIN_CODE"
        val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        val EXTRA_AMOUNT = "EXTRA_AMOUNT"

        val MAX_PINCODE_LENGTH = 4
        val REQUEST_CODE = 648
        val RESULT_VERIFIED = 7652
        val RESULT_CANCELED = 7653
        fun createStartIntent(context: Context): Intent {
            return Intent(context, PinCodeActivity::class.java)
        }

        fun createStartIntent(context: Context, address: String, amount: String): Intent {
            val intent = Intent(context, PinCodeActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            intent.putExtra(EXTRA_AMOUNT, amount)
            return intent
        }
    }
}