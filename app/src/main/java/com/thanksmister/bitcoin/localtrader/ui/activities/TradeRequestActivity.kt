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
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SearchViewModel
import com.thanksmister.bitcoin.localtrader.utils.Calculations
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Doubles
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import kotlinx.android.synthetic.main.view_trade_request.*
import timber.log.Timber
import javax.inject.Inject

class TradeRequestActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: SearchViewModel

    private var adId: Int = 0
    private var adPrice: String? = null
    private var adMin: String? = null
    private var adMax: String? = null
    private var currency: String? = null
    private var profileName: String? = null
    private var tradeType: TradeType? = TradeType.NONE
    private var countryCode: String? = null
    private var onlineProvider: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_trade_request)

        if (savedInstanceState == null) {
            adId = intent.getIntExtra(EXTRA_AD_ID, 0)
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
            adId = savedInstanceState.getInt(EXTRA_AD_ID)
            val tradeTypeString = savedInstanceState.getString(EXTRA_AD_TRADE_TYPE)
            tradeTypeString?.let {
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
            dialogUtils.showAlertDialog(this@TradeRequestActivity, getString(R.string.error_invalid_trade_type), DialogInterface.OnClickListener { dialog, which ->
                if (!BuildConfig.DEBUG) {
                    Crashlytics.logException(Throwable("Bad trade type for requested trade: " + tradeType + " advertisement Id: " + adId));
                }
                finish();
            })
            return
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = getString(R.string.text_trade_with, profileName)
        }

        requestDescription.text = Html.fromHtml(getString(R.string.trade_request_description))
        requestDescription.movementMethod = LinkMovementMethod.getInstance()
        requestAmountTitle.text = getString(R.string.trade_request_title, currency)

        if (adMin == null) {
            requestLimit.text = ""
        } else if (adMax == null) {
            requestLimit.text = getString(R.string.trade_limit_min, adMin, currency)
        } else { // no maximum set
            requestLimit.text = getString(R.string.trade_limit, adMin, adMax, currency)
        }

        requestButton.setOnClickListener {
            validateChangesAndSend()
        }

        requestCurrency.text = currency
        requestAmountText.filters = arrayOf<InputFilter>(Calculations.DecimalPlacesInputFilter(2))
        requestAmountText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (requestAmountText.hasFocus()) {
                    val amount = editable.toString()
                    calculateBitcoinAmount(amount)
                }
            }
        })

        requestBitcoinText.filters = arrayOf<InputFilter>(Calculations.DecimalPlacesInputFilter(8))
        requestBitcoinText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (requestBitcoinText!!.hasFocus()) {
                    val bitcoin = editable.toString()
                    if (onlineProvider == TradeUtils.ALTCOIN_ETH) {
                        val ether = Calculations.calculateBitcoinToEther(bitcoin, adPrice)
                        val withinRange = Calculations.calculateEthereumWithinRange(ether, adMin, adMax)
                        if (!withinRange) {
                            requestEthereumAmount.setTextColor(resources.getColorStateList(R.color.red_light_up))
                        } else {
                            requestEthereumAmount.setTextColor(resources.getColorStateList(R.color.light_green))
                        }
                        requestEthereumAmount.setText(ether)
                    } else {
                        calculateCurrencyAmount(bitcoin)
                    }
                }
            }
        })

        requestEthereumAmount.filters = arrayOf<InputFilter>(Calculations.DecimalPlacesInputFilter(18))
        requestEthereumAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (requestEthereumAmount.hasFocus()) {
                    val ether = editable.toString()
                    val withinRange = Calculations.calculateEthereumWithinRange(ether, adMin, adMax)
                    if (!withinRange) {
                        requestEthereumAmount.setTextColor(resources.getColorStateList(R.color.red_light_up))
                    } else {
                        requestEthereumAmount.setTextColor(resources.getColorStateList(R.color.light_green))
                    }
                    val bitcoin = Calculations.calculateEtherToBitcoin(ether, adPrice)
                    requestBitcoinText.setText(bitcoin)
                }
            }
        })

        showOptions()

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: SearchViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@TradeRequestActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@TradeRequestActivity, message, DialogInterface.OnClickListener { dialog, which ->
                    finish()
                })
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if(message != null) {
                dialogUtils.toast(message)
            }
        })
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
        outState.putInt(EXTRA_AD_ID, adId)
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
                        requestAccountNumberLayout.visibility = View.VISIBLE
                        requestSortCodeLayout.visibility = View.VISIBLE
                        requestReceiverNameLayout.visibility = View.VISIBLE
                        requestReferenceLayout.visibility = View.VISIBLE
                    }
                    "AU" -> {
                        requestAccountNumberLayout.visibility = View.VISIBLE
                        requestBSBLayout.visibility = View.VISIBLE
                        requestReceiverNameLayout.visibility = View.VISIBLE
                        requestReferenceLayout.visibility = View.VISIBLE
                    }
                    "FI" -> {
                        requestAccountNumberLayout!!.visibility = View.VISIBLE
                        requestIbanLayout.visibility = View.VISIBLE
                        requestSwiftBicLayout.visibility = View.VISIBLE
                        requestReceiverNameLayout.visibility = View.VISIBLE
                        requestReferenceLayout.visibility = View.VISIBLE
                    }
                }
                TradeUtils.VIPPS, TradeUtils.EASYPAISA, TradeUtils.HAL_CASH, TradeUtils.QIWI, TradeUtils.LYDIA, TradeUtils.SWISH -> requestPhoneNumberLayout!!.visibility = View.VISIBLE
                TradeUtils.PAYPAL, TradeUtils.NETELLER, TradeUtils.INTERAC, TradeUtils.ALIPAY -> requestReceiverEmailLayout!!.visibility = View.VISIBLE
                TradeUtils.SEPA -> {
                    requestReceiverNameLayout.visibility = View.VISIBLE
                    requestIbanLayout.visibility = View.VISIBLE
                    requestSwiftBicLayout.visibility = View.VISIBLE
                    requestReferenceLayout.visibility = View.VISIBLE
                }
                TradeUtils.BPAY -> {
                    requestBillerCodeLayout.visibility = View.VISIBLE
                    requestReferenceLayout.visibility = View.VISIBLE
                }
                TradeUtils.PAYTM -> requestPhoneNumberLayout.visibility = View.VISIBLE
                TradeUtils.ALTCOIN_ETH -> {
                    requestFiatLayout.visibility = View.GONE
                    requestEthereumLayout.visibility = View.VISIBLE
                    requestEthereumAddressLayout.visibility = View.VISIBLE
                }
                TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> requestPhoneNumberLayout.visibility = View.VISIBLE
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            when (onlineProvider) {
                TradeUtils.PAYTM, TradeUtils.QIWI, TradeUtils.SWISH -> requestPhoneNumberLayout.visibility = View.VISIBLE
                TradeUtils.MOBILEPAY_DANSKE_BANK, TradeUtils.MOBILEPAY_DANSKE_BANK_DK, TradeUtils.MOBILEPAY_DANSKE_BANK_NO -> requestPhoneNumberLayout.visibility = View.VISIBLE
                TradeUtils.ALTCOIN_ETH -> {
                    requestFiatLayout.visibility = View.GONE
                    requestEthereumLayout.visibility = View.VISIBLE
                    requestEthereumAddressLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun validateChangesAndSend() {
        var amount = requestAmountText.text.toString()
        if (onlineProvider == TradeUtils.ALTCOIN_ETH) {
            amount = requestEthereumAmount.text.toString()
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

        val phone = requestPhoneNumber.text.toString()
        val receiverEmail = requestReceiverEmail.text.toString()
        val receiverName = requestReceiverName.text.toString()
        val reference = requestReference.text.toString()
        val bic = requestSwiftBic.text.toString()
        val iban = requestIbanName.text.toString()
        val ethereumAddress = requestEthereumAddress.text.toString()
        val accountNumber = requestAccountNumber.text.toString()
        val sortCode = requestSortCode.text.toString()
        val billerCode = requestBillerCode.text.toString()
        val bsb = requestBSB.text.toString()

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
        if (!TextUtils.isEmpty(requestMessage.text.toString())) {
            message = requestMessage.text.toString()
        }

        if (!cancel && adId > 0) {
            sendTradeRequest(adId, amount, receiverName, phone, receiverEmail, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
        }
    }

    private fun sendTradeRequest(adId: Int, amount: String, name: String, phone: String,
                         email: String, iban: String, bic: String, reference: String, message: String,
                         sortCode: String, billerCode: String, accountNumber: String, bsb: String, ethereumAddress: String) {

        dialogUtils.showProgressDialog(this@TradeRequestActivity, getString(R.string.progress_sending_trade_request))
        viewModel.createContact(tradeType, countryCode, onlineProvider, adId, amount, name, phone, email,
                iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
    }

    private fun calculateBitcoinAmount(amount: String) {
        try {
            if (TextUtils.isEmpty(amount) || amount == "0") {
                requestBitcoinText.setText("")
                return
            }
        } catch (e: Exception) {
            Timber.e(e.message)
            return
        }

        try {
            val value = Doubles.convertToDouble(amount) / Doubles.convertToDouble(adPrice)
            requestBitcoinText.setText(Conversions.formatBitcoinAmount(value))
            if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin) || Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
                requestBitcoinText.setTextColor(resources.getColorStateList(R.color.red_light_up))
            } else {
                requestBitcoinText.setTextColor(resources.getColorStateList(R.color.light_green))
            }
        } catch (e: Exception) {
            Timber.e(e.message)
        }

    }

    private fun calculateCurrencyAmount(bitcoin: String) {
        if (TextUtils.isEmpty(bitcoin) || bitcoin == "0") {
            requestAmountText.setText("")
            return
        }

        try {
            val value = Doubles.convertToDouble(bitcoin) * Doubles.convertToDouble(adPrice)
            val amount = Conversions.formatCurrencyAmount(value)
            requestAmountText.setText(amount)

            if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin) || Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
                requestAmountText.setTextColor(resources.getColorStateList(R.color.red_light_up))
            } else {
                requestAmountText.setTextColor(resources.getColorStateList(R.color.light_green))
            }
        } catch (e: Exception) {
            Timber.e(e.message)
        }

    }

    companion object {
        const val EXTRA_AD_ID = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ID"
        const val EXTRA_AD_PRICE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PRICE"
        const val EXTRA_AD_COUNTRY_CODE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_COUNTRY_CODE"
        const val EXTRA_AD_ONLINE_PROVIDER = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ONLINE_PROVIDER"
        const val EXTRA_AD_TRADE_TYPE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_TRADE_TYPE"
        const val EXTRA_AD_MIN_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MIN_AMOUNT"
        const val EXTRA_AD_MAX_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MAX_AMOUNT"
        const val EXTRA_AD_CURRENCY = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_CURRENCY"
        const val EXTRA_AD_PROFILE_NAME = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PROFILE_NAME"
        fun createStartIntent(context: Context, adId: Int, tradeType: String, countryCode: String?, onlineProvider: String?,
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