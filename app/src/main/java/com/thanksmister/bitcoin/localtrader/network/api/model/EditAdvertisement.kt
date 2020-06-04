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

package com.thanksmister.bitcoin.localtrader.network.api.model

class EditAdvertisement (
    val adId: Int,
    val createdAt: String,
    val visible: Boolean,
    val email: String,
    val location: String,
    val limitToFiatAmount: String,
    val countryCode: String,
    val city: String,
    val tradeType: String,
    val minAmount: String,
    val maxAmount: String,
    val maxAmountAvailable: String,
    val priceEquation: String,
    val currency: String,
    val accountInfo: String,
    val message: String,
    val lat: Double,
    val lon: Double,
    val tempPrice: String,
    val tempPriceUsd: String,
    val bankName: String,
    val nextUrl: String,
    val atmModel: String,
    val trackMaxAmount: Boolean,
    val smsVerificationRequired: Boolean,
    val trustedRequired: Boolean,
    val requireIdentification: Boolean,
    val onlineProvider: String,
    val distance: String,
    val requireTradeVolume: String,
    val firstTimeLimitBtc: String,
    val requireFeedbackScore: String,
    val referenceType: String,
    val phoneNumber: String,
    val openingHours: String,
    val requireTrustedByAdvertiser: Boolean,
    val isLocalOffice: Boolean,
    val hiddenByOpeningHours: Boolean,
    val isLowRisk: Boolean,
    val paymentWindowMinutes: Int,
    val ageDaysCoefficientLimit: String,
    val volumeCoefficientBtc: String,
    val floating: Boolean = false,
    val displayReference: Boolean = false
)
