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
import android.support.design.widget.TextInputLayout
import android.text.*
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.utils.Calculations
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Doubles
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import timber.log.Timber

class TradeRequestActivity : BaseActivity() {

    internal var editAmountText: EditText? = null
    internal var editBitcoinText: EditText? = null
    internal var tradeAmountTitle: TextView? = null
    internal var tradeLimit: TextView? = null
    internal var tradeCurrency: TextView? = null
    internal var detailsEthereumAddress: EditText? = null
    internal var detailsSortCode: EditText? = null
    internal var detailsBSB: EditText? = null
    internal var detailsAccountNumber: EditText? = null
    internal var detailsBillerCode: EditText? = null
    internal var detailsEthereumAddressLayout: TextInputLayout? = null
    internal var detailsSortCodeLayout: TextInputLayout? = null
    internal var detailsBSBLayout: TextInputLayout? = null
    internal var detailsAccountNumberLayout: TextInputLayout? = null
    internal var detailsBillerCodeLayout: TextInputLayout? = null
    internal var detailsPhoneNumberLayout: TextInputLayout? = null
    internal var detailsPhoneNumber: EditText? = null
    internal var detailsReceiverEmailLayout: TextInputLayout? = null
    internal var detailsReceiverEmail: EditText? = null
    internal var detailsReceiverNameLayout: TextInputLayout? = null
    internal var detailsReceiverName: EditText? = null
    internal var detailsIbanLayout: TextInputLayout? = null
    internal var detailsIbanName: EditText? = null
    internal var detailsSwiftBicLayout: View? = null
    internal var detailsSwiftBic: EditText? = null

    internal var detailsReferenceLayout: View? = null

    internal var detailsReference: EditText? = null

    internal var tradeMessage: EditText? = null

    internal var tradeMessageLayout: TextInputLayout? = null

    internal var editEtherAmountText: EditText? = null

    internal var bitcoinLayout: LinearLayout? = null

    internal var ethereumLayout: LinearLayout? = null

    internal var fiatLayout: LinearLayout? = null

    private var adId: String? = null
    private var adPrice: String? = null
    private var adMin: String? = null
    private var adMax: String? = null
    private var currency: String? = null
    private var profileName: String? = null
    private var tradeType: TradeType? = TradeType.NONE
    private var countryCode: String? = null
    private var onlineProvider: String? = null

    fun sendButtonClicked() {
        validateChangesAndSend()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_trade_request)

        if (savedInstanceState == null) {
            adId = intent.getStringExtra(EXTRA_AD_ID)
            val tradeTypeString = intent.getStringExtra(EXTRA_AD_TRADE_TYPE)
            if (!TextUtils.isEmpty(tradeTypeString)) {
                tradeType = TradeType.valueOf(intent.getStringExtra(EXTRA_AD_TRADE_TYPE))
            }
            countryCode = intent.getStringExtra(EXTRA_AD_COUNTRY_CODE)
            onlineProvider = intent.getStringExtra(EXTRA_AD_ONLINE_PROVIDER)
            adPrice = intent.getStringExtra(EXTRA_AD_PRICE)
            adMin = intent.getStringExtra(EXTRA_AD_MIN_AMOUNT)
            adMax = intent.getStringExtra(EXTRA_AD_MAX_AMOUNT)
            currency = intent.getStringExtra(EXTRA_AD_CURRENCY)
            profileName = intent.getStringExtra(EXTRA_AD_PROFILE_NAME)
        } else {
            adId = savedInstanceState.getString(EXTRA_AD_ID)
            val tradeTypeString = savedInstanceState.getString(EXTRA_AD_TRADE_TYPE)
            if (!TextUtils.isEmpty(tradeTypeString)) {
                tradeType = TradeType.valueOf(tradeTypeString)
            }
            countryCode = savedInstanceState.getString(EXTRA_AD_COUNTRY_CODE)
            onlineProvider = savedInstanceState.getString(EXTRA_AD_ONLINE_PROVIDER)
            adPrice = savedInstanceState.getString(EXTRA_AD_PRICE)
            adMin = savedInstanceState.getString(EXTRA_AD_MIN_AMOUNT)
            adMax = savedInstanceState.getString(EXTRA_AD_MAX_AMOUNT)
            currency = savedInstanceState.getString(EXTRA_AD_CURRENCY)
            profileName = savedInstanceState.getString(EXTRA_AD_PROFILE_NAME)
        }

        if (tradeType == null || TradeType.NONE.name == tradeType!!.name) {
            /*showAlertDialog(new AlertDialogEvent(getString(R.string.error_title), getString(R.string.error_invalid_trade_type)), new Action0() {
                @Override
                public void call() {
                    if (!BuildConfig.DEBUG) {
                        Crashlytics.logException(new Throwable("Bad trade type for requested trade: " + tradeType + " advertisement Id: " + adId));
                    }
                    finish();
                }
            });
            return;*/
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = getString(R.string.text_trade_with, profileName)
        }

        val tradeDescription = findViewById<View>(R.id.tradeDescription) as TextView
        tradeDescription.text = Html.fromHtml(getString(R.string.trade_request_description))
        tradeDescription.movementMethod = LinkMovementMethod.getInstance()

        tradeAmountTitle!!.text = getString(R.string.trade_request_title, currency)

        if (adMin == null) {
            tradeLimit!!.text = ""
        } else if (adMax == null) {
            tradeLimit!!.text = getString(R.string.trade_limit_min, adMin, currency)
        } else { // no maximum set
            tradeLimit!!.text = getString(R.string.trade_limit, adMin, adMax, currency)
        }

        tradeCurrency!!.text = currency

        editAmountText!!.filters = arrayOf<InputFilter>(Calculations.DecimalPlacesInputFilter(2))
        editAmountText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editAmountText!!.hasFocus()) {
                    val amount = editable.toString()
                    calculateBitcoinAmount(amount)
                }
            }
        })

        editBitcoinText!!.filters = arrayOf<InputFilter>(Calculations.DecimalPlacesInputFilter(8))
        editBitcoinText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun afterTextChanged(editable: Editable) {

                if (editBitcoinText!!.hasFocus()) {
                    val bitcoin = editable.toString()
                    if (onlineProvider == TradeUtils.ALTCOIN_ETH) {
                        val ether = Calculations.calculateBitcoinToEther(bitcoin, adPrice)
                        val withinRange = Calculations.calculateEthereumWithinRange(ether, adMin, adMax)
                        if (!withinRange) {
                            editEtherAmountText!!.setTextColor(resources.getColorStateList(R.color.red_light_up))
                        } else {
                            editEtherAmountText!!.setTextColor(resources.getColorStateList(R.color.light_green))
                        }
                        editEtherAmountText!!.setText(ether)

                    } else {
                        calculateCurrencyAmount(bitcoin)
                    }
                }
            }
        })

        editEtherAmountText!!.filters = arrayOf<InputFilter>(Calculations.DecimalPlacesInputFilter(18))
        editEtherAmountText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editEtherAmountText!!.hasFocus()) {
                    val ether = editable.toString()
                    val withinRange = Calculations.calculateEthereumWithinRange(ether, adMin, adMax)
                    if (!withinRange) {
                        editEtherAmountText!!.setTextColor(resources.getColorStateList(R.color.red_light_up))
                    } else {
                        editEtherAmountText!!.setTextColor(resources.getColorStateList(R.color.light_green))
                    }
                    val bitcoin = Calculations.calculateEtherToBitcoin(ether, adPrice)
                    editBitcoinText!!.setText(bitcoin)
                }
            }
        })

        showOptions()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_AD_ID, adId)
        outState.putString(EXTRA_AD_PRICE, adPrice)
        outState.putString(EXTRA_AD_MIN_AMOUNT, adMin)
        outState.putString(EXTRA_AD_MAX_AMOUNT, adMax)
        outState.putString(EXTRA_AD_CURRENCY, currency)
        outState.putString(EXTRA_AD_PROFILE_NAME, profileName)
        outState.putString(EXTRA_AD_TRADE_TYPE, tradeType!!.name)
        outState.putString(EXTRA_AD_COUNTRY_CODE, countryCode)
        outState.putString(EXTRA_AD_ONLINE_PROVIDER, onlineProvider)
    }

    private fun showOptions() {
        if (tradeType == TradeType.ONLINE_BUY) {
            when (onlineProvider) {
                TradeUtils.NATIONAL_BANK -> when (countryCode) {
                    "UK" -> {
                        detailsAccountNumberLayout!!.visibility = View.VISIBLE
                        detailsSortCodeLayout!!.visibility = View.VISIBLE
                        detailsReceiverNameLayout!!.visibility = View.VISIBLE
                        detailsReferenceLayout!!.visibility = View.VISIBLE
                    }
                    "AU" -> {
                        detailsAccountNumberLayout!!.visibility = View.VISIBLE
                        detailsBSBLayout!!.visibility = View.VISIBLE
                        detailsReceiverNameLayout!!.visibility = View.VISIBLE
                        detailsReferenceLayout!!.visibility = View.VISIBLE
                    }
                    "FI" -> {
                        detailsAccountNumberLayout!!.visibility = View.VISIBLE
                        detailsIbanLayout!!.visibility = View.VISIBLE
                        detailsSwiftBicLayout!!.visibility = View.VISIBLE
                        detailsReceiverNameLayout!!.visibility = View.VISIBLE
                        detailsReferenceLayout!!.visibility = View.VISIBLE
                    }
                }
                TradeUtils.VIPPS, TradeUtils.EASYPAISA, TradeUtils.HAL_CASH, TradeUtils.QIWI, TradeUtils.LYDIA, TradeUtils.SWISH -> detailsPhoneNumberLayout!!.visibility = View.VISIBLE
                TradeUtils.PAYPAL, TradeUtils.NETELLER, TradeUtils.INTERAC, TradeUtils.ALIPAY -> detailsReceiverEmailLayout!!.visibility = View.VISIBLE
                TradeUtils.SEPA -> {
                    detailsReceiverNameLayout!!.visibility = View.VISIBLE
                    detailsIbanLayout!!.visibility = View.VISIBLE
                    detailsSwiftBicLayout!!.visibility = View.VISIBLE
                    detailsReferenceLayout!!.visibility = View.VISIBLE
                }
                TradeUtils.BPAY -> {
                    detailsBillerCodeLayout!!.visibility = View.VISIBLE
                    detailsReferenceLayout!!.visibility = View.VISIBLE
                }
                TradeUtils.PAYTM -> detailsPhoneNumberLayout!!.visibility = View.VISIBLE
                TradeUtils.ALTCOIN_ETH -> {
                    fiatLayout!!.visibility = View.GONE
                    ethereumLayout!!.visibility = View.VISIBLE
                    detailsEthereumAddressLayout!!.visibility = View.VISIBLE
                }
                TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> detailsPhoneNumberLayout!!.visibility = View.VISIBLE
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            when (onlineProvider) {
                TradeUtils.PAYTM, TradeUtils.QIWI, TradeUtils.SWISH -> detailsPhoneNumberLayout!!.visibility = View.VISIBLE
                TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> detailsPhoneNumberLayout!!.visibility = View.VISIBLE
                TradeUtils.ALTCOIN_ETH -> {
                    fiatLayout!!.visibility = View.GONE
                    ethereumLayout!!.visibility = View.VISIBLE
                    detailsEthereumAddressLayout!!.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun validateChangesAndSend() {

        var amount = editAmountText!!.text.toString()
        if (onlineProvider == TradeUtils.ALTCOIN_ETH) {
            amount = editEtherAmountText!!.text.toString()
        }
        var cancel = false
        try {
            if (TextUtils.isEmpty(amount)) {
                toast(getString(R.string.toast_valid_trade_amount))
                cancel = true
            } else if (!TextUtils.isEmpty(adMax) && Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
                toast(getString(R.string.toast_enter_lower_amount, adMax, currency))
                cancel = true
            } else if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) {
                toast(getString(R.string.toast_enter_higher_amount, adMin, currency))
                cancel = true
            }
        } catch (e: Exception) {
            Timber.e(e.message)
            cancel = true
        }

        val phone = detailsPhoneNumber!!.text.toString()
        val receiverEmail = detailsReceiverEmail!!.text.toString()
        val receiverName = detailsReceiverName!!.text.toString()
        val reference = detailsReference!!.text.toString()
        val bic = detailsSwiftBic!!.text.toString()
        val iban = detailsIbanName!!.text.toString()
        val ethereumAddress = detailsEthereumAddress!!.text.toString()
        val accountNumber = detailsAccountNumber!!.text.toString()
        val sortCode = detailsSortCode!!.text.toString()
        val billerCode = detailsBillerCode!!.text.toString()
        val bsb = detailsBSB!!.text.toString()

        if (tradeType == TradeType.ONLINE_BUY) {
            when (onlineProvider) {
                TradeUtils.NATIONAL_BANK -> when (countryCode) {
                    "UK" -> if (TextUtils.isEmpty(sortCode)
                            || TextUtils.isEmpty(receiverName)
                            || TextUtils.isEmpty(reference)
                            || TextUtils.isEmpty(accountNumber)) {
                        toast(getString(R.string.toast_complete_all_fields))
                    }
                    "AU" -> if (TextUtils.isEmpty(bsb)
                            || TextUtils.isEmpty(receiverName)
                            || TextUtils.isEmpty(reference)
                            || TextUtils.isEmpty(accountNumber)) {
                        toast(getString(R.string.toast_complete_all_fields))
                    }
                    "FI" -> if (TextUtils.isEmpty(iban)
                            || TextUtils.isEmpty(receiverName)
                            || TextUtils.isEmpty(bic)
                            || TextUtils.isEmpty(reference)
                            || TextUtils.isEmpty(accountNumber)) {
                        toast(getString(R.string.toast_complete_all_fields))
                        cancel = true
                    }
                }
                TradeUtils.ALTCOIN_ETH -> if (TextUtils.isEmpty(ethereumAddress)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
                TradeUtils.VIPPS, TradeUtils.EASYPAISA, TradeUtils.HAL_CASH, TradeUtils.QIWI, TradeUtils.LYDIA, TradeUtils.SWISH -> if (TextUtils.isEmpty(phone)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
                TradeUtils.PAYPAL, TradeUtils.NETELLER, TradeUtils.INTERAC, TradeUtils.ALIPAY -> if (TextUtils.isEmpty(receiverEmail)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
                TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> if (TextUtils.isEmpty(phone)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
                TradeUtils.BPAY -> if (TextUtils.isEmpty(billerCode) || TextUtils.isEmpty(reference)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
                TradeUtils.SEPA -> if (TextUtils.isEmpty(receiverName) || TextUtils.isEmpty(iban) || TextUtils.isEmpty(bic) || TextUtils.isEmpty(reference)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            when (onlineProvider) {
                TradeUtils.QIWI, TradeUtils.SWISH, TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> if (TextUtils.isEmpty(phone)) {
                    toast(getString(R.string.toast_complete_all_fields))
                    cancel = true
                }
            }
        }
        var message = ""
        if (!TextUtils.isEmpty(tradeMessage!!.text.toString())) {
            message = tradeMessage!!.text.toString()
        }

        if (!cancel) {
            sendTradeRequest(adId, amount, receiverName, phone, receiverEmail, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
        }
    }

    fun sendTradeRequest(adId: String?, amount: String, name: String, phone: String,
                         email: String, iban: String, bic: String, reference: String, message: String,
                         sortCode: String, billerCode: String, accountNumber: String, bsb: String, ethereumAddress: String) {

        showProgressDialog(getString(R.string.progress_sending_trade_request))

        //        dataService.createContact(
        //                adId, tradeType, countryCode, onlineProvider,
        //                amount, name, phone, email,
        //                iban, bic, reference, message,
        //                sortCode, billerCode, accountNumber,
        //                bsb, ethereumAddress)
        //
        //                .subscribeOn(Schedulers.newThread())
        //                .observeOn(AndroidSchedulers.mainThread())
        //                .subscribe(new Action1<ContactRequest>() {
        //                    @Override
        //                    public void call(ContactRequest contactRequest) {
        //                        runOnUiThread(new Runnable() {
        //                            @Override
        //                            public void run() {
        //                                hideProgressDialog();
        //                                toast(getString(R.string.toast_trade_request_sent) + profileName + "!");
        //                                finish();
        //                            }
        //                        });
        //                    }
        //                }, new Action1<Throwable>() {
        //                    @Override
        //                    public void call(final Throwable throwable) {
        //                        runOnUiThread(new Runnable() {
        //                            @Override
        //                            public void run() {
        //                                reportError(throwable);
        //                                handleError(throwable);
        //                            }
        //                        });
        //                    }
        //
        //                });
    }

    private fun calculateBitcoinAmount(amount: String) {
        try {
            if (TextUtils.isEmpty(amount) || amount == "0") {
                editBitcoinText!!.setText("")
                return
            }
        } catch (e: Exception) {
            reportError(e)
            return
        }

        try {
            val value = Doubles.convertToDouble(amount) / Doubles.convertToDouble(adPrice)
            editBitcoinText!!.setText(Conversions.formatBitcoinAmount(value))

            if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin) || Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
                editAmountText!!.setTextColor(resources.getColorStateList(R.color.red_light_up))
            } else {
                editAmountText!!.setTextColor(resources.getColorStateList(R.color.light_green))
            }
        } catch (e: Exception) {
            Timber.e(e.message)
        }

    }

    private fun calculateCurrencyAmount(bitcoin: String) {
        if (TextUtils.isEmpty(bitcoin) || bitcoin == "0") {
            editAmountText!!.setText("")
            return
        }

        try {
            val value = Doubles.convertToDouble(bitcoin) * Doubles.convertToDouble(adPrice)
            val amount = Conversions.formatCurrencyAmount(value)
            editAmountText!!.setText(amount)

            if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin) || Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
                editAmountText!!.setTextColor(resources.getColorStateList(R.color.red_light_up))
            } else {
                editAmountText!!.setTextColor(resources.getColorStateList(R.color.light_green))
            }
        } catch (e: Exception) {
            Timber.e(e.message)
        }

    }

    companion object {

        val EXTRA_AD_ID = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ID"
        val EXTRA_AD_PRICE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PRICE"
        val EXTRA_AD_COUNTRY_CODE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_COUNTRY_CODE"
        val EXTRA_AD_ONLINE_PROVIDER = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ONLINE_PROVIDER"
        val EXTRA_AD_TRADE_TYPE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_TRADE_TYPE"
        val EXTRA_AD_MIN_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MIN_AMOUNT"
        val EXTRA_AD_MAX_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MAX_AMOUNT"
        val EXTRA_AD_CURRENCY = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_CURRENCY"
        val EXTRA_AD_PROFILE_NAME = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PROFILE_NAME"

        fun createStartIntent(context: Context, adId: Int, tradeType: String?, countryCode: String?, onlineProvider: String?,
                              adPrice: String?, adMin: String?, adMax: String?, currency: String?, profileName: String?): Intent {

            val intent = Intent(context, TradeRequestActivity::class.java)
            intent.putExtra(EXTRA_AD_ID, adId)
            intent.putExtra(EXTRA_AD_TRADE_TYPE, tradeType)
            intent.putExtra(EXTRA_AD_COUNTRY_CODE, countryCode)
            intent.putExtra(EXTRA_AD_ONLINE_PROVIDER, onlineProvider)
            intent.putExtra(EXTRA_AD_PRICE, adPrice)
            intent.putExtra(EXTRA_AD_MIN_AMOUNT, adMin)
            intent.putExtra(EXTRA_AD_MAX_AMOUNT, adMax)
            intent.putExtra(EXTRA_AD_CURRENCY, currency)
            intent.putExtra(EXTRA_AD_PROFILE_NAME, profileName)
            return intent
        }
    }
}