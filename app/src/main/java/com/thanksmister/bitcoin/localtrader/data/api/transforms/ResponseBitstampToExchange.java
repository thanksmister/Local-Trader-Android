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
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DefaultExchange;
import com.thanksmister.bitcoin.localtrader.utils.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import retrofit.client.Response;
import rx.functions.Func1;
import timber.log.Timber;

public class ResponseBitstampToExchange implements Func1<Bitstamp, DefaultExchange>
{
    @Override
    public DefaultExchange call(Bitstamp bitstamp)
    {
        DefaultExchange exchange = new DefaultExchange();
        exchange.name = "Bitstamp";
        exchange.ask = bitstamp.ask;
        exchange.bid = bitstamp.bid;
        exchange.high = bitstamp.high;
        exchange.low = bitstamp.low;
        exchange.volume = bitstamp.volume;
        exchange.last = bitstamp.last;
                
        return exchange;
    }
}
