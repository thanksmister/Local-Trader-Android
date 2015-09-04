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

package com.thanksmister.bitcoin.localtrader.data.api;

import com.squareup.okhttp.OkHttpClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;
import retrofit.client.Client;
import retrofit.client.OkClient;

@Module(
        complete = false,
        library = true
)
public final class ApiModule 
{
    private static final String BITSTAMP_API_ENDPOINT = "https://www.bitstamp.net";
    private static final String BITCOIN_AVERAGE_ENDPOINT = "https://api.bitcoinaverage.com";
    public static final String BASE_URL = "https://localbitcoins.com/";

    @Provides 
    @Singleton Client provideClient(OkHttpClient client) 
    {
        return new OkClient(client);
    }

    @Provides
    @Singleton
    LocalBitcoins provideLocalBitcoins(Client client)
    {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(BASE_URL)
                .build();
        return restAdapter.create(LocalBitcoins.class);
    }

    @Provides
    @Singleton
    BitstampExchange provideBitstampExchange(Client client)
    {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(BITSTAMP_API_ENDPOINT)
                .build();
        return restAdapter.create(BitstampExchange.class);
    }
    
    @Provides
    @Singleton
    BitcoinAverage provideBitcoinAverage(Client client)
    {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(BITCOIN_AVERAGE_ENDPOINT)
                .build();
        return restAdapter.create(BitcoinAverage.class);
    }
}