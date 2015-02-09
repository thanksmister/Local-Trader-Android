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

package com.thanksmister.bitcoin.localtrader.data.api.model;

import com.thanksmister.bitcoin.localtrader.utils.Calculations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dashboard
{
    public List<Contact> contacts = Collections.emptyList();
    public List<Advertisement> advertisements = Collections.emptyList();
    //public Wallet wallet = new Wallet();
    public DefaultExchange exchange;
    
    public List<Advertisement> getActiveAdvertisements()
    {
        ArrayList<Advertisement> list = new ArrayList<>();
        for(Advertisement advertisement : advertisements) {
            if(advertisement.visible) {
                list.add(advertisement);
            }
        }
        
        return list;
    }

    public List<Advertisement> getInactiveAdvertisements()
    {
        ArrayList<Advertisement> list = new ArrayList<>();
        for(Advertisement advertisement : advertisements) {
            if(!advertisement.visible) {
                list.add(advertisement);
            }
        }

        return list;
    }

    public String getBitstampValue()
    {
        return Calculations.calculateAverageBidAskFormatted(exchange.bid, exchange.ask);
    }
}
