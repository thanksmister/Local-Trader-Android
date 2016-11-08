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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class NetworkUtils
{
    public static final String DEFAULT_ENCODING = "UTF-8";
    private static final String HmacSHA256 = "HmacSHA256";
   
    public static String hmacSha256Hex(String message, String secret) throws Exception
    {
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), HmacSHA256);
        Mac mac = Mac.getInstance(HmacSHA256);
        mac.init(keySpec);
        byte[] bytes = mac.doFinal(message.getBytes());
        return asHex(bytes).toLowerCase();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    private static String asHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Generates a nonce value for signing.  
     * A 63 bit positive integer, for example unix timestamp as milliseconds.
     * @return String generated nonce
     */
    public static String generateNonce()
    {
        String nonce = String.valueOf(new BigInteger(String.valueOf(System.currentTimeMillis()))) + "1000";
        Timber.d("Nonce: " + nonce);
        return nonce;
    }

    public static String generateNonce(String callName)
    {
        String nonce = String.valueOf(new BigInteger(String.valueOf(System.currentTimeMillis()))) + "1000";
        Timber.d("Nonce: " + callName + " " + nonce );
        return nonce;
    }

    /**
     * Generate a Hmac signature for signing requests
     * @param relative_path
     * @param nonce
     * @param hmac_auth_key
     * @param hmac_auth_secret
     * @return
     * @throws Exception
     */
    public static String createSignature(String relative_path, String nonce, String hmac_auth_key, String hmac_auth_secret)
    {
        try {
            String signature = null;
            String message = String.valueOf(nonce) + hmac_auth_key + relative_path;
            signature = hmacSha256Hex(message, hmac_auth_secret);
            return signature.toUpperCase();
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
        
        return null;
    }

    /**
     * Generate a Hmac signature for signging requests
     * @param relative_path
     * @param nonce
     * @param hmac_auth_key
     * @param hmac_auth_secret
     * @return
     * @throws Exception
     */
    public static String createSignature(String relative_path, String params, String nonce, String hmac_auth_key, String hmac_auth_secret)
    {
        try {
            String signature = null;
            String message = String.valueOf(nonce) + hmac_auth_key + relative_path + params;
            signature = hmacSha256Hex(message, hmac_auth_secret);
            return signature.toUpperCase();
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return null;
    }
    
    public static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public static boolean isNetworkConnected(Context context)
    {
        boolean isConnected;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        isConnected = (networkInfo != null && networkInfo.isConnectedOrConnecting());

        return isConnected;
    }
}
