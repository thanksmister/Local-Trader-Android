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
 *
 */

package com.thanksmister.bitcoin.localtrader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager

import com.org.bitcoinj.core.Base58
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.thanksmister.bitcoin.localtrader.R

import java.util.Hashtable
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import timber.log.Timber


object WalletUtils {

    private val BITCOIN_ADDRESS_BYTES_LENGTH = 21

    fun encodeAsBitmap(address: String, appContext: Context): Bitmap? {
        return encodeAsBitmap(address, null, appContext)
    }

    fun encodeAsBitmap(address: String, amount: String?, appContext: Context): Bitmap? {
        val contentsToEncode: String? = if (amount == null) {
            generateBitCoinURI(address)
        } else {
            generateBitCoinURI(address, amount)
        }

        val displayMetrics = DisplayMetrics()
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val windowWidth = displayMetrics.widthPixels
        val windowHeight = displayMetrics.heightPixels
        val windowSize = Math.min(windowWidth, windowHeight) - dipToPixels(appContext, appContext.resources.getDimension(R.dimen.activity_horizontal_margin)).toInt()

        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.MARGIN] = 0

        val result: BitMatrix
        try {
            result = QRCodeWriter().encode(contentsToEncode, BarcodeFormat.QR_CODE, windowSize, windowSize, hints)
        } catch (we: WriterException) {
            return null
        } catch (iae: IllegalArgumentException) {
            // Unsupported format
            return null
        }

        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result.get(x, y)) Color.DKGRAY else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(windowSize, windowSize, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return bitmap
    }

    private fun dipToPixels(context: Context, dipValue: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
    }

    fun generateBitCoinURI(address: String): String {
        var uri = "bitcoin:"
        uri += address
        return uri
    }

    fun generateBitCoinURI(address: String, amount: String?): String {
        if (amount == null) {
            return generateBitCoinURI(address)
        }
        var uri = "bitcoin:"
        uri += "$address?"
        if (!TextUtils.isEmpty(amount)) {
            uri += "amount=$amount"
        }
        return uri
    }

    /**
     * Validates a Bitcoin address, but warning that this is not thread safe
     * and needs to be run on separate thread.
     *
     * @param address
     * @return
     */
    fun validBitcoinAddress(address: String?): Boolean {

        if (address == null || address.trim { it <= ' ' }.isEmpty() || address.isEmpty()) {
            return false
        }

        //"if (!address.matches(\"[a-zA-Z0-9]*\"))
        val invalidChars = address.split("/[^1-9A-HJ-NP-Za-km-z]/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (invalidChars.size > 1) {
            return false
        }

        try {
            val bytes = Base58.decodeChecked(address)
            if (bytes == null || bytes.size != BITCOIN_ADDRESS_BYTES_LENGTH) {
                return false
            }
        } catch (e: Error) {
            return false
        }

        return true
    }

    // TODO this is hacky, let's find a way to pull out a BTC address from any string
    fun parseBitcoinAddressFromTransaction(address: String?): String? {
        address?.let {
            if (it.toLowerCase().contains("send")) {
                try {
                    val pattern = "(?<=Send to )[^#\\?\\:]+"
                    val compiledPattern = Pattern.compile(pattern, Pattern.DOTALL)
                    val matcher = compiledPattern.matcher(address)

                    if (matcher.find()) {
                        return matcher.group()
                    }
                } catch (e: PatternSyntaxException) {
                    return it
                }
            } else if (it.toLowerCase().contains("internal")) {
                try {
                    val pattern = "(?<=Internal transaction to )[^#\\?\\:]+"
                    val compiledPattern = Pattern.compile(pattern, Pattern.DOTALL)
                    val matcher = compiledPattern.matcher(address)

                    if (matcher.find()) {
                        return matcher.group()
                    }
                } catch (e: PatternSyntaxException) {
                    return it
                }
            }
            return it
        }
        return address
    }

    fun parseBitcoinAddress(address: String?): String? {
        if (address == null) return null

        if (address.toLowerCase().contains("bitcoin") || address.toLowerCase().contains("amount")) {

            try {
                val pattern = "(?<=bitcoin:)[^#\\?\\:]+"
                val compiledPattern = Pattern.compile(pattern, Pattern.DOTALL)
                val matcher = compiledPattern.matcher(address)

                if (matcher.find()) {
                    return matcher.group()
                }
            } catch (e: PatternSyntaxException) {
                return address
            }

        }

        return address
    }

    fun parseBitcoinAmount(address: String?): String? {
        if (address == null) return null
        if (address.contains("amount")) {
            val pattern = "(?<=\\?amount=)[^#\\?\\:]+"
            val compiledPattern = Pattern.compile(pattern, Pattern.DOTALL)
            val matcher = compiledPattern.matcher(address)

            if (matcher.find()) {
                return matcher.group()
            }
        }
        return null
    }

    fun validAmount(amount: String?): Boolean {
        if (amount == null || amount.isEmpty())
            return false
        try {
            java.lang.Double.valueOf(amount)
        } catch (e: NumberFormatException) {
            return false
        } catch (e: NullPointerException) {
            return true
        }

        return true
    }
}