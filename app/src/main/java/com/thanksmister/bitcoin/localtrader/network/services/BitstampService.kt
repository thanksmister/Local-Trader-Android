/*
 * Copyright (c) 2019 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.network.services

import com.thanksmister.bitcoin.localtrader.network.api.model.Bitstamp

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface BitstampService {
    @GET("/api/ticker/")
    fun ticker(): Observable<Bitstamp>

    @GET("/api/v2/ticker/{currency_pair}/")
    fun ticker(@Path("currency_pair") currency_pair: String): Observable<Bitstamp>
}