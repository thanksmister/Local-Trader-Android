/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class Conversions {

    private static int MAXIMUM_BTC_DECIMALS = 8;
    private static int MINIMUM_BTC_DECIMALS = 1;
    private static int MINIMUM_CURRENCY_DECIMALS = 2;

    public static String ZERO_OR_MORE_DECIMALS = "###.##";
    public static String ZERO_DECIMALS = "0";
    public static String ONE_DECIMALS = "0.0";
    public static String TWO_DECIMALS = "0.00";
    public static String THREE_DECIMALS = "0.000";
    public static String FOUR_DECIMALS = "0.0000";
    public static String FIVE_DECIMALS = "0.00000";

    public static double KILOMETERS_TO_MILES = .62137;

    private Conversions() {
    }

    public static String kilometersToMiles(double km) {
        double mi = km * KILOMETERS_TO_MILES;
        return Conversions.formatDecimalToString(mi, TWO_DECIMALS);
    }

    public static double diffOfTwoValues(String value1, String value2) {
        return convertToDouble(value1) - convertToDouble(value2);
    }

    public static String formatDecimalToString(double value, String format) {
        try {
            DecimalFormat decimalFormat = new DecimalFormat(format);
            return decimalFormat.format(value);
        } catch (Exception e) {
            throw new NumberFormatException("Unable to format value to decimal: " + value);
        }
    }

    public static double sumOfTwoValues(String value1, String value2) {
        return convertToDouble(value1) + convertToDouble(value2);
    }

    public static double sumOfTwoValues(double value1, String value2) {
        return value1 + convertToDouble(value2);
    }

    public static String convertDollarsCents(String centValue) {
        // Declaration of variables.
        double dollars;
        double cents;
        String inputNumberString;
        double inputNumber;
        double calculatedAnswer;

        // Convert String to int
        inputNumber = Double.parseDouble(centValue);

        // Calculate the number
        dollars = inputNumber / 100;
        cents = inputNumber % 100;

        return dollars + "." + cents;
    }

    public static String convertToCents(String cents) {
        if (cents == null) return "0.00";
        if (cents.contains(".")) return cents;
        if (cents.length() < 2) {
            return "0.0" + cents;
        } else if (cents.length() == 2) {
            return "0." + cents;
        } else if (cents.length() > 2) {
            String dollars = cents.substring(0, cents.length() - 2);
            String andcents = cents.substring(cents.length() - 2, cents.length());
            return dollars + "." + andcents;
        }

        return cents;
    }


    public static String formatBitcoinAmount(Double amount) {
        try {
            Locale locUS = new Locale("en_US");
            NumberFormat numberFormat = NumberFormat.getNumberInstance(locUS);
            numberFormat.setMaximumFractionDigits(MAXIMUM_BTC_DECIMALS);
            numberFormat.setMinimumFractionDigits(MINIMUM_BTC_DECIMALS);
            return String.valueOf(numberFormat.format(amount));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String formatWholeNumber(Double amount) {
        NumberFormat formatter = new DecimalFormat("###");
        return formatter.format(amount);
    }

    public static String formatCurrencyAmount(Double amount) {
        try {
            Locale locUS = new Locale("en_US");
            NumberFormat numberFormat = NumberFormat.getNumberInstance(locUS);
            numberFormat.setMinimumFractionDigits(MINIMUM_CURRENCY_DECIMALS);
            numberFormat.setMaximumFractionDigits(MINIMUM_CURRENCY_DECIMALS);
            return String.valueOf(numberFormat.format(amount));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "0.00";
    }

    public static float convertToFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String formatBitcoinAmount(String btc) {
        try {
            Locale locUS = new Locale("en_US");
            Double value = Doubles.convertToDouble(btc);
            NumberFormat numberFormat = NumberFormat.getNumberInstance(locUS);
            numberFormat.setMinimumFractionDigits(MINIMUM_BTC_DECIMALS);
            numberFormat.setMaximumFractionDigits(MAXIMUM_BTC_DECIMALS);
            return numberFormat.format(value); // only format, not parse uses max/min values
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static Double convertToDouble(String value) {
        return Doubles.convertToDouble(value);
    }

    /**
     * Computes value of bitcoin based on current rate and fiat amount entered
     *
     * @param fiat Required amount of fiat currency (USD)
     * @param rate Market rate of Bitcoin
     * @return Computed bitcoin amount as <code>String</code>
     */
    public static String computeBitcoinFromFiatAndRate(String fiat, String rate) {
        double fiatAmount = Doubles.convertToDouble(fiat);
        double rateAmount = Doubles.convertToDouble(rate);
        return formatBitcoinAmount(fiatAmount / rateAmount);
    }

    /**
     * Computes value of bitcoin based on current rate and bitcoin amount entered
     *
     * @param bitcoin Bitcoin amount
     * @param rate    Market rate of Bitcoin
     * @return Computed bitcoin amount as <code>String</code>
     */
    public static String computeFiatFromBitcoinAndRate(String bitcoin, String rate) {
        double btcAmount = Doubles.convertToDouble(bitcoin);
        double rateAmount = Doubles.convertToDouble(rate);
        return formatCurrencyAmount(btcAmount * rateAmount);
    }

    public static String formatDealAmount(String btc, String amount) {
        double btcAmount = Doubles.convertToDouble(btc);
        double fiatAmount = Doubles.convertToDouble(amount);
        return formatCurrencyAmount(fiatAmount / btcAmount);
    }
}
