/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.services

import com.google.gson.JsonElement
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CoinbaseService {

    /*@GET("/v2/exchange-rates?currency=BTC")
    Observable<Response> exchangeRates();

    @GET("/v2/currencies")
    Observable<Response> currencies();
*/
    @GET("/v2/prices/BTC-{currency}/spot")
    fun spotPrice(@Path("currency") currency: String): Observable<JsonElement>
}