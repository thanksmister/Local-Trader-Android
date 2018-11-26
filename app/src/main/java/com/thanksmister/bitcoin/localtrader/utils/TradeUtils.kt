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

package com.thanksmister.bitcoin.localtrader.utils

import android.content.Context
import android.text.TextUtils

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Date

import timber.log.Timber

class TradeUtils {

    companion object {

        const val PAYPAL = "PAYPAL"
        const val NETELLER = "NETELLER"
        const val QIWI = "QIWI"
        const val SEPA = "SEPA"
        const val ALTCOIN_ETH = "ALTCOIN_ETH"
        const val INTERNATIONAL_WIRE_SWIFT = "INTERNATIONAL_WIRE_SWIFT"
        const val GIFT_CARD_CODE = "GIFT_CARD_CODE"
        const val NATIONAL_BANK = "NATIONAL_BANK"
        const val CASH_DEPOSIT = "CASH_DEPOSIT"
        const val SPECIFIC_BANK = "SPECIFIC_BANK"
        const val OTHER = "OTHER"
        const val OTHER_REMITTANCE = "OTHER_REMITTANCE"
        const val OTHER_ONLINE_WALLET = "OTHER_ONLINE_WALLET"
        const val OTHER_PRE_PAID_DEBIT = "OTHER_PRE_PAID_DEBIT"
        const val OTHER_ONLINE_WALLET_GLOBAL = "OTHER_ONLINE_WALLET_GLOBAL"
        const val BPAY = "BPAY"
        const val PAYTM = "PAYTM"
        const val INTERAC = "INTERAC"
        const val LYDIA = "LYDIA"
        const val ALIPAY = "ALIPAY"
        const val EASYPAISA = "EASYPAISA"
        const val HAL_CASH = "HAL_CASH"
        const val SWISH = "SWISH"
        const val MOBILEPAY_DANSKE_BANK_DK = "MOBILEPAY_DANSKE_BANK_DK"
        const val MOBILEPAY_DANSKE_BANK = "MOBILEPAY_DANSKE_BANK"
        const val MOBILEPAY_DANSKE_BANK_NO = "MOBILEPAY_DANSKE_BANK_NO"
        const val VIPPS = "VIPPS"

        fun getContactDescription(contact: Contact, context: Context): String? {
            if (isCanceledTrade(contact)) {
                return if (isLocalTrade(contact)) context.getString(R.string.order_description_cancel_local) else context.getString(R.string.order_description_cancel)
            } else if (isReleased(contact)) {
                return context.getString(R.string.order_description_released)
            } else if (isDisputed(contact)) {
                return context.getString(R.string.order_description_disputed)
            } else if (isClosedTrade(contact)) {
                return context.getString(R.string.order_description_closed)
            } else if (isLocalTrade(contact)) {
                return if (youAreAdvertiser(contact) && contact.isSelling) {
                    if (contact.isFunded) {
                        //return canFundTrade(contact)? context.getString(R.string.order_description_funded_local):context.getString(R.string.order_description_funded_local_no_action);
                        context.getString(R.string.order_description_funded_local)
                    } else {
                        //return canReleaseTrade(contact)? context.getString(R.string.order_description_not_funded_local):context.getString(R.string.order_description_not_funded_local_no_action);
                        context.getString(R.string.order_description_not_funded_local)
                    }
                } else {
                    if (contact.isFunded) {
                        context.getString(R.string.order_description_funded_local)
                    } else {
                        context.getString(R.string.order_description_not_funded_local)
                    }
                }
            } else if (isOnlineTrade(contact)) {

                return if (contact.isBuying) {
                    if (isMarkedPaid(contact)) context.getString(R.string.order_description_paid) else context.getString(R.string.order_description_mark_paid)
                } else {
                    if (isMarkedPaid(contact)) context.getString(R.string.order_description_online_paid) else context.getString(R.string.order_description_online_mark_paid)
                }
            }
            return null
        }

        fun getTradeActionButtonLabel(contact: Contact): Int {
            if (isClosedTrade(contact) || isReleased(contact)) {
                return 0
            }
            if (isLocalTrade(contact)) { // selling or buying locally with ad
                return if (contact.isSelling) { // ad to sell bitcoins locally
                    if (contact.isFunded || isFunded(contact)) { // TODO is this available for local?
                        R.string.button_release
                    } else {
                        R.string.button_fund
                    }
                } else R.string.button_cancel
            } else if (isOnlineTrade(contact)) {   // handle online trade ads
                return if (contact.isBuying) { // ad to buy bitcoins
                    if (isMarkedPaid(contact)) R.string.button_dispute else R.string.button_mark_paid
                } else { // ad to sell bitcoins
                    R.string.button_release
                }
            }
            return 0
        }

        fun youAreAdvertiser(contact: Contact): Boolean {
            return if (contact.isSelling) { // you are selling
                contact.advertisement.advertiser.username == contact.seller.username
            } else {  // you are buying
                contact.advertisement.advertiser.username == contact.buyer.username
            }
        }

        fun tradeIsActive(closedAt: String, canceledAt: String): Boolean {
            return TextUtils.isEmpty(closedAt) && TextUtils.isEmpty(canceledAt)
        }

        fun isCanceledTrade(contact: Contact): Boolean {
            return contact.canceledAt != null
        }

        fun isMarkedPaid(contact: Contact): Boolean {
            return contact.paymentCompletedAt != null
        }

        fun isFunded(contact: Contact): Boolean {
            return contact.fundedAt != null
        }

        fun isReleased(contact: Contact): Boolean {
            return contact.releasedAt != null
        }

        fun isDisputed(contact: Contact): Boolean {
            return contact.disputedAt != null
        }

        fun isClosedTrade(contact: Contact): Boolean {
            return contact.closedAt != null
        }

        fun canDisputeTrade(contact: Contact): Boolean {
            return !TextUtils.isEmpty(contact.actions.disputeUrl) && TradeUtils.isOnlineTrade(contact)
                    && !TradeUtils.isCanceledTrade(contact) && !TradeUtils.isClosedTrade(contact)
        }

        fun canCancelTrade(contact: Contact): Boolean {
            return contact.isBuying && !isClosedTrade(contact) && !isCanceledTrade(contact) && !isReleased(contact)
                    && !TextUtils.isEmpty(contact.actions.cancelUrl);
        }

        fun canReleaseTrade(contact: Contact): Boolean {
            return !isClosedTrade(contact)
        }

        fun canFundTrade(contact: Contact): Boolean {
            return !isClosedTrade(contact)
        }

        fun isLocalTrade(tradeType: TradeType): Boolean {
            return tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
        }

        fun isLocalTrade(contact: Contact): Boolean {
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            return tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
        }

        fun isLocalTrade(advertisement: Advertisement): Boolean {
            return TradeType.LOCAL_BUY.name == advertisement.tradeType || TradeType.LOCAL_SELL.name == advertisement.tradeType
        }

        fun isAtm(advertisement: Advertisement): Boolean {
            return !TextUtils.isEmpty(advertisement.atmModel)
        }

        fun isOnlineTrade(contact: Contact): Boolean {
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            return tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL
        }

        fun isOnlineTrade(tradeType: TradeType): Boolean {
            return tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL
        }
        
        fun isOnlineTrade(advertisement: Advertisement): Boolean {
            return TradeType.ONLINE_BUY.name == advertisement.tradeType || TradeType.ONLINE_SELL.name == advertisement.tradeType
        }

        fun isSellTrade(advertisement: Advertisement): Boolean {
            return TradeType.ONLINE_SELL.name == advertisement.tradeType || TradeType.LOCAL_SELL.name == advertisement.tradeType
        }

        fun isBuyTrade(advertisement: Advertisement): Boolean {
            return TradeType.ONLINE_BUY.name == advertisement.tradeType || TradeType.LOCAL_BUY.name == advertisement.tradeType
        }

        fun getMethodForAdvertisement(onlineProvider: String, methods: List<Method>): Method? {
            for (m in methods) {
                if (onlineProvider == m.code) {
                    return m
                }
            }
            return null
        }

        fun getMethodForAdvertisement(advertisement: Advertisement, methods: List<Method>): Method? {
            for (m in methods) {
                if (m.code != null && m.code == advertisement.onlineProvider) {
                    return m
                }
            }

            return null
        }

        fun getPaymentMethod(code: String, methods: List<Method>): String? {
            for (method in methods) {
                if (code == method.code) {
                    return if (TextUtils.isEmpty(method.key)) code else method.key
                }
            }
            return code
        }

        fun getPaymentMethod(context: Context, advertisement: Advertisement, methods: List<Method>): String {
            var paymentMethod = ""
            for (method in methods) {
                if (method.code == advertisement.onlineProvider) {
                    paymentMethod = getPaymentMethod(context, advertisement, method)
                    break
                }
            }
            return paymentMethod
        }

        fun getPaymentMethod(context: Context, advertisement: Advertisement, method: Method?): String {
            var paymentMethod: String? = "Online"
            if (method != null && method.code == advertisement.onlineProvider) {
                /*when (method.code) {
                    "NATIONAL_BANK" -> return context.getString(R.string.text_national_bank_transfer)
                    "CASH_DEPOSIT" -> return context.getString(R.string.text_cash_deposit)
                    "SPECIFIC_BANK" -> return context.getString(R.string.text_bank_transfer)
                }*/
                paymentMethod = method.name!!
            }
            if (!TextUtils.isEmpty(advertisement.bankName) && "NATIONAL_BANK" == advertisement.onlineProvider) {
                return context.getString(R.string.text_common_with, paymentMethod, advertisement.bankName)
            } else if (paymentMethod != null) {
                return paymentMethod
            }
            return ""
        }

        fun getPaymentMethodName(advertisement: Advertisement, method: Method?): String {
            var paymentMethod: String? = "Other"
            if (method != null && method.code == advertisement.onlineProvider) {
                paymentMethod = method.name
            }
            if (paymentMethod != null) {
                return paymentMethod
            }
            return ""
        }

        fun getPaymentMethodName(paymentMethod: String?): String {
            when (paymentMethod) {
                "NATIONAL_BANK" -> return "National Bank transfer"
                "CASH_DEPOSIT" -> return "Cash deposit"
                "SPECIFIC_BANK" -> return "Bank transfer"
            }
            return ""
        }

        fun getContactName(contact: Contact): String? {
            return if (contact.isSelling) {
                contact.buyer.username
            } else {
                contact.seller.username
            }
        }

        fun determineLastSeenIcon(lasOnline: String): Int {
            val now = Date()
            val lastSeen = Dates.parseLastSeenDate(lasOnline)

            val diff = now.time - lastSeen.time

            if (diff > 1800000 && diff < 10800000) {
                return R.drawable.last_seen_shortly
            } else if (diff > 10800000) {
                return R.drawable.last_seen_long
            }

            return R.drawable.last_seen_recently
        }

        fun parseUserString(value: String): Array<String> {
            var value = value
            val nameSplit: Array<String>
            if (!value.contains(" ")) {
                val stringArrayList = ArrayList<String>()
                stringArrayList.add(value)
                nameSplit = stringArrayList.toTypedArray()
                return nameSplit
            }

            // strip out any parenthesis and split on spacing?
            value = value.replace("(\\()".toRegex(), "")
            value = value.replace("(\\))".toRegex(), "")
            value = value.replace("(\\;)".toRegex(), "")
            nameSplit = value.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return nameSplit
        }

        fun parsePaymentService(value: String): String {
            var value = value
            // strip out any parenthesis and split on spacing?
            value = value.replace("(\\_)".toRegex(), " ")

            val words = value.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sb = StringBuilder()
            if (words[0].length > 0) {
                sb.append(Character.toUpperCase(words[0][0]) + words[0].subSequence(1, words[0].length).toString().toLowerCase())
                for (i in 1 until words.size) {
                    sb.append(" ")
                    sb.append(Character.toUpperCase(words[i][0]) + words[i].subSequence(1, words[i].length).toString().toLowerCase())
                }
            }

            return sb.toString()
        }

        fun parsePaymentServiceTitle(value: String?): String {
            if(value == null) return ""
            var value = value
            value = value.replace("(\\_)".toRegex(), " ")

            val words = value.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sb = StringBuilder()
            if (words[0].length > 0) {
                sb.append(Character.toUpperCase(words[0][0]) + words[0].subSequence(1, words[0].length).toString().toLowerCase())
                for (i in 1 until words.size) {
                    sb.append(" ")
                    sb.append(Character.toUpperCase(words[i][0]) + words[i].subSequence(1, words[i].length).toString().toLowerCase())
                }
            }
            return sb.toString()
        }

        fun kilometersToMiles(km: String): String {
            val mi = Doubles.convertToDouble(km) * .62137
            val precision = DecimalFormat("0.00")
            return precision.format(mi)
        }

        fun convertCurrencyAmount(value: String): String {
            try {
                //Locale locUS = new Locale("en_US");
                val numberFormat = NumberFormat.getNumberInstance()
                val result = numberFormat.parse(value).toInt().toDouble()
                val decimalFormat = DecimalFormat("#")
                return decimalFormat.format(result)
            } catch (e: Exception) {
                return value
            }

        }

        fun getBankNameTitle(context: Context, tradeType: TradeType, onlineProvider: String): String? {
            var bankTitle: String? = null
            if (tradeType == TradeType.ONLINE_SELL) {
                when (onlineProvider) {
                    TradeUtils.INTERNATIONAL_WIRE_SWIFT -> bankTitle = "Bank SWIFT"
                    TradeUtils.CASH_DEPOSIT, TradeUtils.SPECIFIC_BANK, TradeUtils.NATIONAL_BANK -> bankTitle = "Bank name (required)"
                    TradeUtils.OTHER, TradeUtils.OTHER_REMITTANCE, TradeUtils.OTHER_PRE_PAID_DEBIT, TradeUtils.OTHER_ONLINE_WALLET_GLOBAL, TradeUtils.OTHER_ONLINE_WALLET -> bankTitle = "Payment method name"
                    TradeUtils.GIFT_CARD_CODE -> bankTitle = "Gift Card Issuer: [AMC Theatres, Airbnb, American Express, Best Buy, Dell, GA2, GameStop, Google Play, Groupon, Home Depot, Lowe, Lyft, Microsoft Windows Store, Netflix, Other, Papa John's Pizza, PlayStation Store, Regal Cinemas, Skype Credit, Target, Uber, Whole Foods Market, Wolt, Xbox]"
                    else -> {
                    }
                }
            } else if (tradeType == TradeType.ONLINE_BUY) {
                when (onlineProvider) {
                    TradeUtils.NATIONAL_BANK, TradeUtils.CASH_DEPOSIT, TradeUtils.SPECIFIC_BANK -> bankTitle = "Bank name (required)"
                    TradeUtils.OTHER, TradeUtils.OTHER_REMITTANCE, TradeUtils.OTHER_PRE_PAID_DEBIT, TradeUtils.OTHER_ONLINE_WALLET_GLOBAL, TradeUtils.OTHER_ONLINE_WALLET -> bankTitle = "Payment method name"
                    TradeUtils.INTERNATIONAL_WIRE_SWIFT -> bankTitle = "Bank SWIFT"
                    TradeUtils.GIFT_CARD_CODE -> bankTitle = "Gift Card Issuer: [AMC Theatres, Airbnb, American Express, Best Buy, Dell, GA2, GameStop, Google Play, Groupon, Home Depot, Lowe, Lyft, Microsoft Windows Store, Netflix, Other, Papa John's Pizza, PlayStation Store, Regal Cinemas, Skype Credit, Target, Uber, Whole Foods Market, Wolt, Xbox]"
                    else -> {
                    }
                }
            }
            return bankTitle
        }

        fun getEthereumPriceEquation(tradeType: TradeType, margin: String): String {
            var equation = "btc_in_eth"
            if (!TextUtils.isEmpty(margin)) {
                var marginValue = 1.0
                try {
                    marginValue = Doubles.convertToDouble(margin)
                } catch (e: Exception) {
                    Timber.e(e.message)
                }

                var marginPercent = 1.0
                if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.ONLINE_BUY) {
                    marginPercent = 1 - marginValue / 100
                } else {
                    marginPercent = 1 + marginValue / 100
                }
                equation = "$equation*$marginPercent"
            } else {
                equation = equation + "*" + Constants.DEFAULT_MARGIN
            }
            return equation
        }

        fun getPriceEquation(tradeType: TradeType, margin: String, currency: String): String {
            var equation = Constants.DEFAULT_PRICE_EQUATION
            if (currency != Constants.DEFAULT_CURRENCY) {
                equation = equation + "*" + Constants.DEFAULT_CURRENCY + "_in_" + currency
            }
            if (!TextUtils.isEmpty(margin)) {
                var marginValue = 1.0
                try {
                    marginValue = Doubles.convertToDouble(margin)
                } catch (e: Exception) {
                    Timber.e(e.message)
                }

                var marginPercent = 1.0
                if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.ONLINE_BUY) {
                    marginPercent = 1 - marginValue / 100
                } else {
                    marginPercent = 1 + marginValue / 100
                }
                equation = "$equation*$marginPercent"
            } else {
                equation = equation + "*" + Constants.DEFAULT_MARGIN
            }

            return equation
        }

        fun sortMethods(methods: List<Method>): List<Method> {
            Collections.sort(methods, MethodNameComparator())
            return methods
        }

        private class MethodNameComparator : Comparator<Method> {
            override fun compare(o1: Method, o2: Method): Int {
                return if (o1.name != null && o2.name != null) {
                    o1.name!!.toLowerCase().compareTo(o2.name!!.toLowerCase())
                } else 0
            }
        }
    }
}