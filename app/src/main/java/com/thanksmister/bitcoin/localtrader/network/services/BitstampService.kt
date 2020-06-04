/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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