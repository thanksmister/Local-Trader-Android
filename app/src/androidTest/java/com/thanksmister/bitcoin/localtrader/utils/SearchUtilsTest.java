/*
 * Copyright (c) 2017 ThanksMister LLC
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
import android.content.SharedPreferences;
import android.location.Address;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class SearchUtilsTest {
    
    Context context;
    SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        sharedPreferences = context.getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        SearchUtils.INSTANCE.clearSearchLocationAddress(sharedPreferences);
    }

    @After
    public void tearDown() throws Exception {
        SearchUtils.INSTANCE.clearSearchLocationAddress(sharedPreferences);
    }
    
    @Test
    public void getSearchLocationAddress() throws Exception {

        Address address = SearchUtils.INSTANCE.getSearchLocationAddress(sharedPreferences);
        assertNotNull(address);

        assertEquals(null, address.getCountryName());
        assertEquals(null, address.getAddressLine(0));
        assertEquals(null, address.getCountryCode());
        assertEquals(null, address.getLocality());
        if(address.hasLatitude() && address.hasLongitude()) {
            assertEquals(0.0, address.getLatitude());
            assertEquals(0.0, address.getLongitude());
        }
        
        address = new Address(Locale.US);
        address.setAddressLine(0, "Address Line");
        address.setLocality("Locality");
        address.setCountryCode("US");
        address.setCountryName("Country");
        address.setLatitude(-34.6157142);
        address.setLongitude(-58.5033604);
        
        SearchUtils.INSTANCE.setSearchLocationAddress(sharedPreferences, address);
        address = SearchUtils.INSTANCE.getSearchLocationAddress(sharedPreferences);
        assertNotNull(address);

        assertEquals("Country", address.getCountryName());
        assertEquals("Address Line", address.getAddressLine(0));
        assertEquals("US", address.getCountryCode());
        assertEquals("Locality", address.getLocality());
        assertEquals(-34.6157142, address.getLatitude());
        assertEquals(-58.5033604, address.getLongitude());
    }

    @Test
    public void coordinatesToAddress() throws Exception {
        Address address = SearchUtils.INSTANCE.coordinatesToAddress("-34.6157142", "-58.5033604");
        String displayAddress = SearchUtils.INSTANCE.getDisplayAddress(address);
        assertEquals("-34.6157142, -58.5033604", displayAddress);
    }
    
    @Test
    public void coordinatesValid() throws Exception {
        boolean valid = SearchUtils.INSTANCE.coordinatesValid("-34.6157142", "-58.5033604");
        assertEquals(true, valid);

        valid = SearchUtils.INSTANCE.coordinatesValid("0", "-58.5033604");
        assertEquals(false, valid);

        valid = SearchUtils.INSTANCE.coordinatesValid("0", "0");
        assertEquals(false, valid);

        valid = SearchUtils.INSTANCE.coordinatesValid("91", "-181");
        assertEquals(false, valid);
    }

    @Test
    public void getDisplayAddress() throws Exception {
        Address address = new Address(Locale.US);
        String displayAddress = SearchUtils.INSTANCE.getDisplayAddress(address);
        assertEquals("", displayAddress);
        
        address = new Address(Locale.US);
        address.setAddressLine(0, "Address Line");
        address.setLocality("Locality");
        address.setCountryCode("US");
        address.setCountryName("Country");
        address.setLatitude(-34.6157142);
        address.setLongitude(-58.5033604);

        SearchUtils.INSTANCE.setSearchLocationAddress(sharedPreferences, address);
        address = SearchUtils.INSTANCE.getSearchLocationAddress(sharedPreferences);

        displayAddress = SearchUtils.INSTANCE.getDisplayAddress(address);
        assertEquals("Address Line, Country", displayAddress);

        address = new Address(Locale.US);
        address.setLatitude(-34.6157142);
        address.setLongitude(-58.5033604);
        assertEquals(-34.6157142, address.getLatitude());
        assertEquals(-58.5033604, address.getLongitude());

        displayAddress = SearchUtils.INSTANCE.getDisplayAddress(address);
        assertEquals("-34.6157142, -58.5033604", displayAddress);
    }
}