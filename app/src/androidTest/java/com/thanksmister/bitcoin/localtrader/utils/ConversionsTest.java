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

import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.AndroidTestCase;

import java.util.Locale;

/**
 * Author: Michael Ritchie
 * Updated: 12/19/15
 */
public class ConversionsTest extends AndroidTestCase
{

    public void setUp() throws Exception
    {
        super.setUp();
    }

    protected void setLocale(String language, String country)
    {
        // create new local
        Locale locale = new Locale(language, country);

        // here we update locale for date formatters
        Locale.setDefault(locale);

        // here we update locale for app resources

        Resources res = getContext().getResources();

        Configuration config = res.getConfiguration();
        config.locale = locale;

        res.updateConfiguration(config, res.getDisplayMetrics());
    }
    
    public void testFormatBitcoinAmount() throws Exception
    {
        setLocale("en", "US");
        String actual = Conversions.formatBitcoinAmount("1.323343493930209320932093903");
        assertEquals("1.32334349", actual);
        
        setLocale("es", "ES");
        actual = Conversions.formatBitcoinAmount("1");
        assertEquals("1,0", actual);
    }
    
    public void testFormatCurrencyAmount() throws Exception
    {
        setLocale("en", "US");
        String actual = Conversions.formatCurrencyAmount(10.3233434);
        assertEquals("10.32", actual);
        
        setLocale("es", "ES");
        actual = Conversions.formatCurrencyAmount(10.3233434);
        assertEquals("10,32", actual);
    }

    public void testFormatBitcoinAmountDouble() throws Exception
    {
        setLocale("en", "US");
        String actual = Conversions.formatBitcoinAmount(1.0);
        assertEquals("1.0", actual);
        
        setLocale("es", "ES");
        actual = Conversions.formatBitcoinAmount(1.323343493930209320932093903);
        assertEquals("1,32334349", actual);
    }
    
    public void testFormatDealAmount() throws Exception
    {
        setLocale("en", "US");
        String actual = Conversions.formatDealAmount("1.5", "465.12");
        assertEquals("310.08", actual);
        
        setLocale("es", "ES");
        actual = Conversions.formatDealAmount("1,5", "465,12");
        assertEquals("310,08", actual);
    }
    
    public void testComputedBitcoinFromFiatAndRate() throws Exception
    {
        setLocale("en", "US");
        String actual = Conversions.computeBitcoinFromFiatAndRate("450.55", "461.23");
        assertEquals("0.97684452", actual);
        
        setLocale("es", "ES");
        actual = Conversions.computeBitcoinFromFiatAndRate("450,55", "461,32");
        assertEquals("0,97684452", actual);
    }

    public void testComputedFiatFromBitcoinAndRate() throws Exception
    {
        setLocale("en", "US");
        String actual = Conversions.computeFiatFromBitcoinAndRate(".09331586", "460.96");
        assertEquals("43.01", actual);
        
        setLocale("es", "ES");
        actual = Conversions.computeFiatFromBitcoinAndRate(",09331586", "460,96");
        assertEquals("43,01", actual);
    }
}