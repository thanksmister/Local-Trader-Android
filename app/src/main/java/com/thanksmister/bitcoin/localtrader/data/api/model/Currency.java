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

    /*
    "USD": {"volume_btc": "789.32", "rates": {"last": "307.60"}, 
    "avg_1h": 227.6409551042917, "avg_24h": 256.0059476817888, 
    "avg_12h": 252.3810984000065}
     */

public class Currency
{
    public Long _id;
    public String ticker;
    public String last;
    public float volume_btc;
    public String date;
}
