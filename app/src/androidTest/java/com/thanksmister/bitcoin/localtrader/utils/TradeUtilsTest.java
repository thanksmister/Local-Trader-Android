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

public class TradeUtilsTest extends TestCase
{
    public void testKilometersToMiles() throws Exception
    {
        String miles = TradeUtils.kilometersToMiles("2");
        assertEquals("1.24", miles); // two decimals
    }

    public void testConvertCurrencyAmount() throws Exception
    {
        String actual = TradeUtils.convertCurrencyAmount("100");
        String expected = "100";
        assertEquals(actual, expected);

        actual = TradeUtils.convertCurrencyAmount("100,00");
        expected = "100";
        assertEquals(actual, expected);

        actual = TradeUtils.convertCurrencyAmount("100.00");
        expected = "100";
        assertEquals(expected, actual);

        actual = TradeUtils.convertCurrencyAmount("512.47");
        expected = "512";
        assertEquals(expected, actual);
    }
}