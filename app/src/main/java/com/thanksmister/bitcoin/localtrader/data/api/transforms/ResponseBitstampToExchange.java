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

package com.thanksmister.bitcoin.localtrader.data.api.transforms;


import com.thanksmister.bitcoin.localtrader.data.api.model.Bitstamp;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;

import rx.functions.Func1;

public class ResponseBitstampToExchange implements Func1<Bitstamp, Exchange>
{
    @Override
    public Exchange call(Bitstamp bitstamp)
    {
        Exchange exchange = new Exchange();
        exchange.name = "Bitstamp";
        exchange.ask = bitstamp.ask;
        exchange.bid = bitstamp.bid;
        exchange.high = bitstamp.high;
        exchange.low = bitstamp.low;
        exchange.volume = bitstamp.volume;
        exchange.last = bitstamp.last;
        exchange.timestamp = bitstamp.timestamp;
                
        return exchange;
    }
}
