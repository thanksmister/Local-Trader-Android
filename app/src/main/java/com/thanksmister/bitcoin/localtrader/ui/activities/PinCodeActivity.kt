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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.WindowManager
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.PinCodeViewModel
import kotlinx.android.synthetic.main.view_release.*
import javax.inject.Inject

class PinCodeActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: PinCodeViewModel

    private var pinComplete = false
    private var pinCode: String? = ""
    private var address: String? = null
    private var amount: String? = null


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

        pinDescriptionText.text = Html.fromHtml(getString(R.string.pin_code_trade))
        pinDescriptionText.movementMethod = LinkMovementMethod.getInstance()

        button0.setOnClickListener {
            addPinCode("0")
        }
        button1.setOnClickListener {
            addPinCode("1")
        }
        button2.setOnClickListener {
            addPinCode("2")
        }
        button3.setOnClickListener {
            addPinCode("3")
        }
        button4.setOnClickListener {
            addPinCode("4")
        }
        button5.setOnClickListener {
            addPinCode("5")
        }
        button6.setOnClickListener {
            addPinCode("6")
        }
        button7.setOnClickListener {
            addPinCode("7")
        }
        button8.setOnClickListener {
            addPinCode("8")
        }
        button9.setOnClickListener {
            addPinCode("9")
        }
        buttonDel.setOnClickListener {
            removePinCode()
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PinCodeViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: PinCodeViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message?.message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@PinCodeActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@PinCodeActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.toast(message)
            }
        })
        viewModel.getPinCodeStatus().observe(this, Observer { status ->
            if (status == true) {
                dialogUtils.hideProgressDialog()
                intent.putExtra(PinCodeActivity.EXTRA_PIN_CODE, pinCode);
                intent.putExtra(PinCodeActivity.EXTRA_ADDRESS, address);
                intent.putExtra(PinCodeActivity.EXTRA_AMOUNT, amount);
                setResult(PinCodeActivity.RESULT_VERIFIED, intent);
                finish();
            } else if (status == false) {
                dialogUtils.hideProgressDialog()
                invalidatePinCode();
            }
        })
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
            dialogUtils.toast(R.string.toast_pin_code_canceled)
            setResult(PinCodeActivity.RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        dialogUtils.toast(R.string.toast_pin_code_canceled)
        setResult(PinCodeActivity.RESULT_CANCELED)
        finish()
    }

    private fun invalidatePinCode() {
        pinCode = ""
        pinComplete = false
        showFilledPins(0)
        dialogUtils.toast(R.string.toast_pin_code_invalid)
    }

    private fun addPinCode(code: String) {
        if (pinComplete) return
        pinCode += code
        showFilledPins(pinCode!!.length)
        if (pinCode != null && pinCode!!.length == MAX_PINCODE_LENGTH) {
            pinComplete = true
            onSetPinCodeClick(pinCode!!)
        }
    }

    private fun removePinCode() {
        if (pinComplete) return
        if (!TextUtils.isEmpty(pinCode)) {
            pinCode = pinCode!!.substring(0, pinCode!!.length - 1)
            showFilledPins(pinCode!!.length)
        }
    }

    private fun onSetPinCodeClick(pinCode: String) {
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

    private fun validatePinCode(pinCode: String, address: String?, amount: String?) {
        dialogUtils.showProgressDialog(this@PinCodeActivity, getString(R.string.progress_pin_verify), true)
        viewModel.validatePinCode(pinCode)
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
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_PIN_CODE = "EXTRA_PIN_CODE"
        const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        const val EXTRA_AMOUNT = "EXTRA_AMOUNT"

        const val MAX_PINCODE_LENGTH = 4
        const val REQUEST_CODE = 648
        const val RESULT_VERIFIED = 7652
        const val RESULT_CANCELED = 7653

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