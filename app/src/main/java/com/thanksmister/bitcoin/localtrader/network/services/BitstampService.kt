/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *   
 *  Mozilla Public License 2.0
 *  
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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