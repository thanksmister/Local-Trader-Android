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

import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.AndroidRuntimeException
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_share_qrcode.*
import timber.log.Timber

class ShareQrCodeActivity : Activity() {

    private val disposable = CompositeDisposable()
    private var address: String? = null
    private var amount: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_share_qrcode)

        if (savedInstanceState != null) {
            address = savedInstanceState.getString(EXTRA_QR_ADDRESS)
            amount = savedInstanceState.getString(EXTRA_QR_AMOUNT)
        } else {
            address = intent.getStringExtra(EXTRA_QR_ADDRESS)
            amount = intent.getStringExtra(EXTRA_QR_AMOUNT)
        }

        qrCopyButton.setOnClickListener { setRequestOnClipboard() }
        qrCancelButton.setOnClickListener { finish() }
        qrShareButton.setOnClickListener { shareBitcoinRequest() }
        if(address != null && amount != null) {
            generateAddressBitmap(address!!, amount!!)
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_QR_ADDRESS, address)
        outState.putString(EXTRA_QR_AMOUNT, amount)
        super.onSaveInstanceState(outState)
    }

    private fun generateAddressBitmap(bitcoinAddress: String, bitcoinAmount: String) {
        disposable.add(
                generateBitmapObservable(bitcoinAddress, bitcoinAmount)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ bitmap ->
                            if(bitmap != null) {
                                qrImage.setImageBitmap(bitmap)
                            }
                        }, { error ->
                            // TODO we want to report theset to crashalytics
                            Timber.e(error.message)
                            Toast.makeText(this@ShareQrCodeActivity, getString(R.string.toast_error_qrcode), Toast.LENGTH_SHORT).show()
                        }))
    }

    private fun generateBitmapObservable(address: String, amount: String): Observable<Bitmap> {
        return Observable.create { subscriber ->
            try {
                val bitmap = WalletUtils.encodeAsBitmap(address, amount,this@ShareQrCodeActivity)
                subscriber.onNext(bitmap)
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    fun setRequestOnClipboard() {
        val bitcoinUrl = if (TextUtils.isEmpty(amount)) WalletUtils.generateBitCoinURI(address, amount) else WalletUtils.generateBitCoinURI(address, amount)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.bitcoin_request_clipboard_title), bitcoinUrl)
        clipboard.primaryClip = clip
        Toast.makeText(this, getString(R.string.bitcoin_request_copied_toast), Toast.LENGTH_LONG).show()
    }

    private fun shareBitcoinRequest() {
        val bitcoinUrl = if (TextUtils.isEmpty(amount)) WalletUtils.generateBitCoinURI(address, amount) else WalletUtils.generateBitCoinURI(address, amount)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bitcoinUrl)))
        } catch (ex: ActivityNotFoundException) {
            try {
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.type = "text/plain"
                sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bitcoin_request_clipboard_title))
                sendIntent.putExtra(Intent.EXTRA_TEXT, bitcoinUrl)
                startActivity(Intent.createChooser(sendIntent, getString(R.string.text_share_using)))
            } catch (e: AndroidRuntimeException) {
                Timber.e(e.message)
            }
        }
    }

    companion object {
        const val EXTRA_QR_ADDRESS = "EXTRA_QR_ADDRESS"
        const val EXTRA_QR_AMOUNT = "EXTRA_QR_AMOUNT"
        fun createStartIntent(context: Context, address: String, amount: String): Intent {
            val intent = Intent(context, ShareQrCodeActivity::class.java)
            intent.putExtra(EXTRA_QR_ADDRESS, address)
            intent.putExtra(EXTRA_QR_AMOUNT, amount)
            return intent
        }
    }
}