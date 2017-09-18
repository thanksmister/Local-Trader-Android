/*
 * Copyright (c) 2015 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

import timber.log.Timber;

public class Calculations
{
    public static String computedValueOfBitcoin(String bid, String ask, String btc)
    {
        if(TextUtils.isEmpty(btc)) return "";
        double averageExchangeUSD = Calculations.calculateAverageBidAskValue(bid, ask);
        double value = Calculations.calculateUSDValue(averageExchangeUSD, Conversions.convertToDouble(btc));
        return Conversions.formatCurrencyAmount(value);
    }
    
    public static String computedValueOfBitcoin(String rate, String btc)
    {
        try{
            if(TextUtils.isEmpty(btc)) return "";
            double averageExchangeUSD = Doubles.convertToDouble(rate);
            double value = Calculations.calculateUSDValue(averageExchangeUSD, Doubles.convertToDouble(btc));
            return Conversions.formatCurrencyAmount(value);
        } catch (Exception e) {
            return "";
        }
    }
    
    public static String calculateAverageBidAskFormatted(String bid, String ask)
    {
        return Conversions.formatCurrencyAmount(calculateAverageBidAskValue(bid, ask));
    }

    public static double calculateAverageBidAskValue(String bid, String ask)
    {
        double bidDouble = Conversions.convertToDouble(bid);
        double askDouble = Conversions.convertToDouble(ask);

        return (bidDouble + askDouble) / 2;
    }
    
    public static double calculateUSDValue(double bitcoinUSD, double btcAmount)
    {
        return bitcoinUSD * btcAmount;
    }
    
    public static class DecimalPlacesInputFilter implements InputFilter
    {
        private final int decimalDigits;

        public DecimalPlacesInputFilter(int decimalDigits) {
            this.decimalDigits = decimalDigits;
        }

        @Override
        public CharSequence filter(CharSequence source,
                                   int start,
                                   int end,
                                   Spanned dest,
                                   int dstart,
                                   int dend) {


            int dotPos = -1;
            int len = dest.length();
            for (int i = 0; i < len; i++) {
                char c = dest.charAt(i);
                if (c == '.' || c == ',') {
                    dotPos = i;
                    break;
                }
            }
            
            if (dotPos >= 0) {
                // protects against many decimals
                if (source.equals(".") || source.equals(","))
                {
                    return "";
                }
                // if the text is entered before the dot
                if (dend <= dotPos) {
                    return null;
                }
                if (len - dotPos > decimalDigits) {
                    return "";
                }
            }

            return null;
        }
    }

    public static String calculateBitcoinToEther(String bitcoinAmount, String tempPrice) {
        try {
            if (TextUtils.isEmpty(bitcoinAmount) || bitcoinAmount.equals("0")) {
                return "";
            }

            double bitcoinDouble = Doubles.convertToDouble(bitcoinAmount);
            double tempPriceDouble = Doubles.convertToDouble(tempPrice);
            double eth = bitcoinDouble*tempPriceDouble;
            return String.valueOf(eth);
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return "";
    }

    public static String calculateEtherToBitcoin(String ethereumAmount, String tempPrice) {
        try {
            if (TextUtils.isEmpty(ethereumAmount) || ethereumAmount.equals("0")) {
                return "";
            }

            double ethereumDouble = Doubles.convertToDouble(ethereumAmount);
            double tempPriceDouble = Doubles.convertToDouble(tempPrice);
            double btc = ethereumDouble/tempPriceDouble;
            return String.valueOf(btc);
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return "";
    }

    public static boolean calculateEthereumWithinRange(String ethereum, String adMin, String adMax) {
        if(TextUtils.isEmpty(adMin) || TextUtils.isEmpty(adMax)) return true;
        try {
            double ethDouble = Doubles.convertToDouble(ethereum);
            double minDouble = Doubles.convertToDouble(adMin);
            double maxDouble = Doubles.convertToDouble(adMax);
            return(ethDouble >= minDouble && ethDouble <= maxDouble);
        } catch (Exception e) {
            Timber.e(e.getMessage());
            return true;
        }
    }
}
