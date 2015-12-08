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

import junit.framework.TestCase;

/**
 * Author: Michael Ritchie
 * Updated: 12/8/15
 */
public class DoublesTest extends TestCase
{

    public void testValueOrDefault() throws Exception
    {

    }

    public void testConvertToDouble() throws Exception
    {
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
        String value = "1000,00";
        double expected = 1000.00;
        double actual = Doubles.convertToNumber(value);
        
        assertEquals(expected, actual);
    }

    public void testConvertToNumberLocal() throws Exception
    {
        String value = "1000,00";
        double expected = 1000.00;
        double actual = Doubles.convertToNumberLocal(value, "fr");
        assertEquals(expected, actual);
    }
}