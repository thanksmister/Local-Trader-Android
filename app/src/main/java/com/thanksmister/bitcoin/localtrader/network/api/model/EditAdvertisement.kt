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
