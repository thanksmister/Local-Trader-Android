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

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AndroidRuntimeException
import android.view.*
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.ShareQrCodeActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.WalletViewModel
import com.thanksmister.bitcoin.localtrader.utils.Calculations
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Doubles
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_request.*
import timber.log.Timber
import javax.inject.Inject

class RequestFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: WalletViewModel

    private val disposable = CompositeDisposable()
    private var address:String? = null
    private var rate:String? = null
    private var generatedNewAddress = false

    private val clipboardText: String
        get() {
            try {
                var clipText = ""
                val clipboardManager = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboardManager.primaryClip
                if (clip != null) {
                    val item = clip.getItemAt(0)
                    if (item.text != null)
                        clipText = item.text.toString()
                }

                return clipText
            } catch (e: Exception) {
            }

            return ""
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.request, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_paste -> {
                setAmountFromClipboard()
                return true
            }
            R.id.action_share -> {
                if (address != null) {
                    shareAddress(address)
                }
                return true
            }
            R.id.action_blockchain -> {
                if (address != null) {
                    viewBlockChain(address)
                }
                return true
            }
            R.id.action_new_address -> {
                generateNewAddress()
                return true
            }
            else -> {
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qrButton.setOnClickListener {
            validateForm()
        }
        codeImage.setOnClickListener {
            setAddressOnClipboard(requestAddressButton.text.toString())
        }
        requestAddressButton.setOnClickListener {
            setAddressOnClipboard(requestAddressButton.text.toString())
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: WalletViewModel) {
        viewModel.getShowProgress().observe(this, Observer { show ->
            if (show != null && activity != null && show) {
                dialogUtils.showProgressDialog(activity!!, getString(R.string.dialog_loading))
            } else {
                dialogUtils.hideProgressDialog()
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.showAlertDialog(activity!!, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.toast(message)
            }
        })
        viewModel.getBitmap().observe(this, Observer { bitmap ->
            if (bitmap != null && activity != null) {
                codeImage.setImageBitmap(bitmap)
            }
        })
        disposable.add(viewModel.getWalletData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    Timber.d("data: ${data}")
                    if(data != null) {
                        if(data.rate != null) {
                            rate = data.rate
                            setCurrencyAmount()
                        }
                        Timber.d("data.address: ${data.address}")
                        if (!data.address.isNullOrEmpty()) {
                            address = data.address
                            requestAddressButton.text = address
                        } else if (!generatedNewAddress) {
                            dialogUtils.showAlertDialog(activity!!, getString(R.string.error_receiving_address), DialogInterface.OnClickListener { dialog, which ->
                                generatedNewAddress = true
                                generateNewAddress()
                            }, DialogInterface.OnClickListener { dialog, which ->
                                // na-da
                            })
                        }
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requestAmountText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                if (requestAmountText != null && requestAmountText!!.hasFocus()) {
                    val bitcoin = charSequence.toString()
                    calculateCurrencyAmount(bitcoin)
                }
            }
            override fun afterTextChanged(editable: Editable) {}
        })
        requestFiatEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                if (requestFiatEditText != null && requestFiatEditText.hasFocus()) {
                    calculateBitcoinAmount(charSequence.toString())
                }
            }
            override fun afterTextChanged(editable: Editable) {}
        })
        val currency = preferences.exchangeCurrency
        requestCurrencyText.text = currency
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
    }

    private fun generateNewAddress() {
        Timber.d("generateNewAddress")
        dialogUtils.showProgressDialog(activity!!, getString(R.string.progress_new_address))
        viewModel.getWalletAddress()
    }

    private fun showGeneratedQrCodeActivity(bitcoinAddress: String?, bitcoinAmount: String) {
        if (bitcoinAddress != null && activity != null) {
            val intent = ShareQrCodeActivity.createStartIntent(activity!!, bitcoinAddress, bitcoinAmount)
            startActivity(intent)
        }
    }

    private fun setAmountFromClipboard() {
        val clipText = clipboardText
        if (TextUtils.isEmpty(clipText)) {
            dialogUtils.toast(R.string.toast_clipboard_empty)
            return
        }
        if (WalletUtils.validAmount(clipText)) {
            setAmount(WalletUtils.parseBitcoinAmount(clipText))
        } else {
            dialogUtils.toast(getString(R.string.toast_invalid_clipboard_contents))
        }
    }

    private fun setAmount(bitcoinAmount: String) {
        if (!TextUtils.isEmpty(bitcoinAmount)) {
            requestAmountText.setText(bitcoinAmount)
            calculateCurrencyAmount(bitcoinAmount)
        }
    }

    private fun setCurrencyAmount() {
        if (TextUtils.isEmpty(requestAmountText.text)) {
            calculateCurrencyAmount("0.00")
        } else {
            calculateCurrencyAmount(requestAmountText.text.toString())
        }
    }

    private fun validateForm() {
        if (address == null) {
            dialogUtils.toast(getString(R.string.toast_no_valid_address_bitcoin))
            return
        }
        var amount = ""
        if (requestAmountText != null) {
            amount = requestAmountText!!.text.toString()
            if (TextUtils.isEmpty(amount)) {
                dialogUtils.toast(getString(R.string.error_missing_amount))
                return
            }
        }
        val bitcoinAmount = Conversions.formatBitcoinAmount(amount)
        showGeneratedQrCodeActivity(address, bitcoinAmount)
    }

    private fun calculateBitcoinAmount(requestAmount: String) {
        if (Doubles.convertToDouble(requestAmount) == 0.0) {
            requestAmountText.setText("")
            return
        }
        if(rate != null) {
            val btc = Math.abs(Doubles.convertToDouble(requestAmount) / Doubles.convertToDouble(rate))
            val amount = Conversions.formatBitcoinAmount(btc)
            if (requestAmountText != null) {
                requestAmountText!!.setText(amount)
            }
        }
    }

    private fun calculateCurrencyAmount(bitcoin: String) {
        if (Doubles.convertToDouble(bitcoin) == 0.0) {
            if (requestFiatEditText != null)
                requestFiatEditText.setText("")
            return
        }
        if(rate != null) {
            val value = Calculations.computedValueOfBitcoin(rate, bitcoin)
            if (requestFiatEditText != null) {
                requestFiatEditText.setText(value)
            }
        }
    }

    private fun setAddressOnClipboard(address: String) {
        if (!TextUtils.isEmpty(address)) {
            val clipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(activity!!.getString(R.string.wallet_address_clipboard_title), address)
            clipboard.primaryClip = clip
            Toast.makeText(activity, activity!!.getString(R.string.wallet_address_copied_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun viewBlockChain(address: String?) {
        if (!TextUtils.isEmpty(address)) {
            val blockChainIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BLOCKCHAIN_INFO_ADDRESS + address!!))
            startActivity(blockChainIntent)
        }
    }

    private fun shareAddress(address: String?) {
        if (!TextUtils.isEmpty(address)) {
            var sendIntent: Intent
            try {
                sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WalletUtils.generateBitCoinURI(address)))
                sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(sendIntent)
            } catch (ex: ActivityNotFoundException) {
                try {
                    sendIntent = Intent(Intent.ACTION_SEND)
                    sendIntent.type = "text/plain"
                    sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.wallet_my_address_share))
                    sendIntent.putExtra(Intent.EXTRA_TEXT, address)
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.share_using)))
                } catch (e: AndroidRuntimeException) {
                    Timber.e(e.message)
                }
            }
        }
    }

    companion object {
        fun newInstance(): RequestFragment {
            return RequestFragment()
        }
    }
}