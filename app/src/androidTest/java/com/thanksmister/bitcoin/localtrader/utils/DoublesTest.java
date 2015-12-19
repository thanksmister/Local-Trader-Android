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
 * Updated: 12/8/15
 */
public class DoublesTest extends AndroidTestCase
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
    
   
    public void testConvertToDouble() throws Exception
    {
        setLocale("es", "ES");
        String value = "1000,00";
        double expected = 1000.00;
        
        double actual = Doubles.convertToDouble(value);
        assertEquals(expected, actual);
        
        value = ",";
        expected = 0;
        actual = Doubles.convertToDouble(value);
        assertEquals(expected, actual);

        value = ".";
        expected = 0;
        actual = Doubles.convertToDouble(value);
        assertEquals(expected, actual);

        value = "";
        expected = 0;
        actual = Doubles.convertToDouble(value);
        assertEquals(expected, actual);
    }

    /*
        float f1    =   123.45678f;
        Locale  locFR   =   new Locale("fr");
        NumberFormat[] nfa  =   new NumberFormat[4];
    
        nfa[0]  =   NumberFormat.getInstance(); //default
        nfa[1]  =   NumberFormat.getInstance(locFR);    //FranceLocale
        nfa[2]  =   NumberFormat.getCurrencyInstance(); //Default Currency
        nfa[3]  =   NumberFormat.getCurrencyInstance(locFR); //French Currency
    */
    public void testConvertToNumber() throws Exception
    {
        String value = "452.20";
        double expected = 452.20;
        double actual;

        setLocale("es", "US");
        actual = Doubles.convertToNumber(value);
        assertEquals(expected, actual);
        
        value = "452,20";
        expected = 452.20;
        
        setLocale("es", "ES");
        actual = Doubles.convertToNumber(value);
        assertEquals(expected, actual);
    }

    public void testConvertToNumberLocal() throws Exception
    {
        String value = "1000,00";
        double expected = 1000.00;
        double actual = Doubles.convertToNumberLocal(value, "en_US");
        assertEquals(expected, actual);
    }
}