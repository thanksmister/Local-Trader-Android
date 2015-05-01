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

package com.thanksmister.bitcoin.localtrader.data.api.transforms;


import com.thanksmister.bitcoin.localtrader.data.api.model.Bitfinex;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;

import rx.functions.Func1;

public class ResponseBitfinexToExchange implements Func1<Bitfinex, Exchange>
{
    @Override
    public Exchange call(Bitfinex bitfinex)
    {
        Exchange exchange = new Exchange();
        exchange.name = "Bitfinex";
        exchange.ask = bitfinex.ask;
        exchange.bid = bitfinex.bid;
        exchange.high = bitfinex.high;
        exchange.low = bitfinex.low;
        exchange.volume = bitfinex.volume;
        exchange.last = bitfinex.last_price;
                
        return exchange;
    }
}
