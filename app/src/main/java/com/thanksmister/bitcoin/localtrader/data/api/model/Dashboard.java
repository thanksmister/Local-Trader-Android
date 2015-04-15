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

import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dashboard
{
    public List<Contact> contacts = Collections.emptyList();
    public List<MethodItem> methods = Collections.emptyList();
    public List<AdvertisementItem> advertisements = Collections.emptyList();
    //public Wallet wallet = new Wallet();
    public Exchange exchange;
    
    public List<AdvertisementItem> getActiveAdvertisements()
    {
        ArrayList<AdvertisementItem> list = new ArrayList<>();
        for(AdvertisementItem advertisement : advertisements) {
            if(advertisement.visible()) {
                list.add(advertisement);
            }
        }
        
        return list;
    }

    public List<AdvertisementItem> getInactiveAdvertisements()
    {
        ArrayList<AdvertisementItem> list = new ArrayList<>();
        for(AdvertisementItem advertisement : advertisements) {
            if(!advertisement.visible()) {
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
