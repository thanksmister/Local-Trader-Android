/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import timber.log.Timber;


public class WalletUtils
{

    private static final String CLASS_NAME = " " + WalletUtils.class.getSimpleName() + " ";
    private static int BITCOIN_ADDRESS_BYTES_LENGTH = 21;
    
    public static Bitmap encodeAsBitmap(String address, Context appContext)
    {
        return encodeAsBitmap(address, null, appContext);
    }

    public static Bitmap encodeAsBitmap(String address, String amount, Context appContext)
    {
        String contentsToEncode;
        if(amount == null) {
            contentsToEncode = generateBitCoinURI(address);
        } else {
            contentsToEncode = generateBitCoinURI(address, amount);
        }
    
        if (contentsToEncode == null) {
            return null;
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int windowWidth = displayMetrics.widthPixels;
        int windowHeight = displayMetrics.heightPixels;
        int windowSize = Math.min(windowWidth, windowHeight) - (int) dipToPixels(appContext, appContext.getResources().getDimension(R.dimen.activity_horizontal_margin));

        Map<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);

        BitMatrix result;
        try {
            result = new QRCodeWriter().encode(contentsToEncode, BarcodeFormat.QR_CODE, windowSize, windowSize, hints);
        } catch (WriterException we) {
            return null;
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.DKGRAY : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(windowSize, windowSize, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }
    
    private static float dipToPixels(Context context, float dipValue) 
    {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static String generateBitCoinURI(String address)
    {
        String uri = "bitcoin:";
        uri += address;
        return uri;
    }

    public static String generateBitCoinURI(String address, String amount)
    {
        if(amount == null) {
            return generateBitCoinURI(address);
        }
        String uri = "bitcoin:";
        uri += address + "?";
        if (!TextUtils.isEmpty(amount)) {
            uri += "amount=" + amount;
        }
        return uri;
    }

    public static boolean validBitcoinAddress(String address) 
    {
        if(address == null || (address.trim().length() < 1 || address.length() < 1)) {
            return false;
        }
        
        //"if (!address.matches(\"[a-zA-Z0-9]*\"))
        String [] invalidChars = address.split("/[^1-9A-HJ-NP-Za-km-z]/");
        if(invalidChars.length > 1) {
            return false;
        }

        try {
            byte[] bytes = Base58.decodeChecked(address);
            if (bytes == null || bytes.length != BITCOIN_ADDRESS_BYTES_LENGTH) {
                return false;
            }
        } catch (Error e) {
            return false;
        }
       
        return true;
    }

    // TODO this is hacky, let's find a way to pull out a BTC address from any string
    public static String parseBitcoinAddressFromTransaction(String address)
    {
        Timber.d("Address: " + address);
        
        if(address == null) return null;

        if(address.toLowerCase().contains("send")) {

            try {
                String pattern = "(?<=Send to )[^#\\?\\:]+";
                Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher matcher = compiledPattern.matcher(address);

                if (matcher.find()) {
                    return matcher.group();
                }
            } catch (PatternSyntaxException e) {
                return address;
            }
        } else if (address.toLowerCase().contains("internal")) {
            try {
                String pattern = "(?<=Internal transaction to )[^#\\?\\:]+";
                Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher matcher = compiledPattern.matcher(address);

                if (matcher.find()) {
                    return matcher.group();
                }
            } catch (PatternSyntaxException e) {
                return address;
            }
        }

        return address;
    }
  
    public static String parseBitcoinAddress(String address) 
    {
        if(address == null) return null;
  
        if(address.toLowerCase().contains("bitcoin") || address.toLowerCase().contains("amount")  ) {

            try {
                String pattern = "(?<=bitcoin:)[^#\\?\\:]+";
                Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher matcher = compiledPattern.matcher(address);

                if (matcher.find()) {
                    return matcher.group();
                }
            } catch (PatternSyntaxException e) {
                return address;
            }
        }
       
        return address;
    }

    public static String parseBitcoinAmount(String address)
    {
        if(address == null) return null;
        if(address.contains("amount") ) {
            String pattern = "(?<=\\?amount=)[^#\\?\\:]+";
            Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher matcher = compiledPattern.matcher(address);

            if(matcher.find()){
                return matcher.group();
            }
        }
        
        return null;
    }
    
    public static boolean invalidValue(String bitcoinAmount)
    {
        return bitcoinAmount.trim().length() < 1 
                || bitcoinAmount.length() < 1 
                || bitcoinAmount.equals("0.0") 
                || bitcoinAmount.equals("0");
    }

    public static boolean validAmount(String amount)
    {
        if(amount == null || amount.isEmpty() || amount.equals("")) return false;
        
        try {
            double result = Double.valueOf(amount);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return true;
        }
        
        return true;
    }
    
    public static ArrayList<String> parseContactIdsFromTransactions(ArrayList<Transaction> transactions)
    {
        ArrayList<String> contactIds = new ArrayList<String>();
        for (Transaction transaction : transactions) {  
            String contactId = parseContactIdFromTransaction(transaction);
            if(contactId != null)
                contactIds.add(contactId);
        }
        
        return contactIds;
    }
    
    public static String parseContactIdFromTransaction(Transaction transaction)
    {
        if(transaction.tx_type.equals("3")) {
            // Contact #545335 trade canceled
            if(transaction.description.contains("canceled")) {
                String pattern = "[0-9]+";
                Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher matcher = compiledPattern.matcher(transaction.description);
                if(matcher.find()){
                    return  matcher.group();
                }
            }
        }
        
        return null;
    }

}
