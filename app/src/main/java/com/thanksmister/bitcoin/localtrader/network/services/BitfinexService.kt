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

interface BitfinexService {
    @GET("/v2/ticker/{symbol}")
    fun ticker(@Path("symbol") symbol: String): Observable<JsonElement>
}