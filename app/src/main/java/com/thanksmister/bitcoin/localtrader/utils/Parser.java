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

package com.thanksmister.bitcoin.localtrader.utils;

import android.annotation.SuppressLint;

import com.google.gson.internal.LinkedTreeMap;
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.network.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate;
import com.thanksmister.bitcoin.localtrader.network.api.model.Method;
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes;
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import timber.log.Timber;

public class Parser {

    public static boolean containsError(String response) {
        return response.contains("error_code") && response.contains("error");
    }

    public static String parseDataMessage(String response, String defaultResponse) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObj = jsonObject.getJSONObject("data");
            if (dataObj.has("message")) {
                return dataObj.getString("message");
            } else {
                return defaultResponse;
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return defaultResponse;
        }
    }

    public static NetworkException parseError(String response) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject errorObj = jsonObject.getJSONObject("error");
            int error_code = ExceptionCodes.INSTANCE.getNO_ERROR_CODE();
            if (errorObj.has("error_code")) {
                error_code = errorObj.getInt("error_code");
            }
            StringBuilder error_message = new StringBuilder(errorObj.getString("message"));
            if (errorObj.has("errors")) {
                error_message = new StringBuilder();
                JSONObject errors = errorObj.getJSONObject("errors");
                Iterator<?> keys = errors.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String message = errors.getString(key);
                    error_message.append(StringUtils.convertCamelCase(key)).append(" ").append(message).append(" ");
                }
            }
            return new NetworkException(error_message.toString(), error_code);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return new NetworkException(e.getMessage(), ExceptionCodes.INSTANCE.getNO_ERROR_CODE());
        }
    }

    public static List<Method> parseMethods(TreeMap<String, Object> treeMap) {
        ArrayList<Method> methods = new ArrayList<Method>();
        for (Object o : treeMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            LinkedTreeMap linkedTreeMap = (LinkedTreeMap) entry.getValue();
            Method method = new Method();
            method.setKey((String) entry.getKey());
            method.setCode((String) linkedTreeMap.get("code"));
            method.setName((String) linkedTreeMap.get("name"));
            method.setCurrencies((ArrayList<String>) linkedTreeMap.get("currencies"));
            methods.add(method);
        }
        Collections.sort(methods, new MethodNameComparator());
        return methods;
    }

    private static class MethodNameComparator implements Comparator<Method> {
        @Override
        public int compare(Method o1, Method o2) {
            return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }
    }

    public static List<Currency> parseCurrencies(TreeMap<String, Object> treeMap) {
        ArrayList<Currency> currencies = new ArrayList<Currency>();
        for (Object o : treeMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            LinkedTreeMap linkedTreeMap = (LinkedTreeMap) entry.getValue();
            Currency currency = new Currency();
            currency.setCode((String) entry.getKey());
            currency.setName((String) linkedTreeMap.get("name"));
            currency.setAltcoin((Boolean) linkedTreeMap.get("altcoin"));
            currencies.add(currency);
        }
        Collections.sort(currencies, new CurrencyComparator());
        return currencies;
    }

    public static class CurrencyComparator implements Comparator<Currency> {
        @Override
        public int compare(Currency o1, Currency o2) {
            return o1.getCode().toLowerCase().compareTo(o2.getCode().toLowerCase());
        }
    }

    public static ExchangeRate parseBitcoinAverageExchangeRate(String exchangeName, String currency, String result) {
        try {
            JSONObject jsonObject;
            jsonObject = new JSONObject(result);
            Iterator<?> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (jsonObject.get(key) instanceof JSONObject) {
                    JSONObject exchangeObj = (JSONObject) jsonObject.get(key);
                    JSONObject rateObject = (JSONObject) exchangeObj.get("rates");
                    String rate = rateObject.getString("last");
                    if (exchangeObj.has("avg_1h")) {
                        rate = exchangeObj.getString("avg_1h");
                    }
                    if (currency.equals(key)) {
                        ExchangeRate exchangeRate = new ExchangeRate();
                        exchangeRate.setCurrency(currency);
                        exchangeRate.setRate(rate);
                        exchangeRate.setName(exchangeName);
                        return exchangeRate;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("DefaultLocale")
    public static ExchangeRate parseBitfinexExchangeRate(String response) {
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(response);
            if (jsonArray.length() > 6) {
                String rate = jsonArray.get(6).toString();
                rate = String.format("%.2f", Doubles.convertToDouble(rate));
                ExchangeRate exchangeRate = new ExchangeRate();
                exchangeRate.setRate(rate);
                exchangeRate.setName("Bitfinex");
                return exchangeRate;
            }
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static ExchangeRate parseCoinbaseExchangeRate(String response) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            String exchangeName = "Coinbase";
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setCurrency(dataObject.getString("currency"));
            exchangeRate.setRate(dataObject.getString("amount"));
            exchangeRate.setName(exchangeName);
            return exchangeRate;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public static Exchange parseMarket(String response) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            String ask = "";
            String bid = "";
            String last = "";
            String display_name = "BitcoinAverage";
            String source = "http://www.bitcoinaverage.com";
            String created_at = "";
            ask = jsonObject.getString("ask");
            bid = jsonObject.getString("bid");
            last = jsonObject.getString("last");
            created_at = jsonObject.getString("timestamp");
            return new Exchange(display_name, ask, bid, last, source, created_at);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}