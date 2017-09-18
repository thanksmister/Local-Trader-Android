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

public class CalculationsTest extends TestCase {

    public void testCalculateBitcoinToEither() throws Exception {
        
        String btc = "1";
        String tempPriceEth = "11.40";
        String expected = "11.4";
        String results = Calculations.calculateBitcoinToEther(btc, tempPriceEth);
        assertEquals(expected, results);

        btc = ".5";
        expected = "5.7";
        results = Calculations.calculateBitcoinToEther(btc, tempPriceEth);
        assertEquals(expected, results);
    }

    public void testCalculateEtherToBitcoin() throws Exception {
        
        String eth = "10.72";
        String tempPriceEth = "10.72";
        String expected = "1.0";
        String results = Calculations.calculateEtherToBitcoin(eth, tempPriceEth);
        assertEquals(expected, results);

        eth = ".5";
        expected = "0.046641791044776115";
        results = Calculations.calculateEtherToBitcoin(eth, tempPriceEth);
        assertEquals(expected, results);
    }
    
    public void testCalculateEthereumWithinRange() throws Exception {
        
        String eth = "14";
        String adMin = "1";
        String adMax = "20";
        boolean results = Calculations.calculateEthereumWithinRange(eth, adMin, adMax);
        assertEquals(true, results);

        eth = "22";
        results = Calculations.calculateEthereumWithinRange(eth, adMin, adMax);
        assertEquals(false, results);
    }
}