/*
 * Copyright (c) 2018 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.network

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.GsonBuilder
import com.thanksmister.bitcoin.localtrader.network.adapters.DataTypeAdapterFactory
import com.thanksmister.bitcoin.localtrader.network.api.model.OauthResponse
import com.thanksmister.bitcoin.localtrader.persistence.Currency
import com.thanksmister.bitcoin.localtrader.persistence.Notification
import com.thanksmister.bitcoin.localtrader.persistence.User
import com.thanksmister.bitcoin.localtrader.persistence.Wallet
import com.thanksmister.bitcoin.localtrader.utils.Parser
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

import java.util.concurrent.TimeUnit
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import okhttp3.Interceptor
import okhttp3.ResponseBody
import java.io.IOException


class LocalBitcoinsApi(base_url:String) {

    private val service: LocalBitcoinsRequest

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10000, TimeUnit.SECONDS)
                .readTimeout(10000, TimeUnit.SECONDS)
                .addNetworkInterceptor(StethoInterceptor())
                .build()

        val gson = GsonBuilder()
                .registerTypeAdapterFactory(DataTypeAdapterFactory())
                .create()

        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(base_url)
                .build()

        service = retrofit.create(LocalBitcoinsRequest::class.java)
    }

    fun getMyself(token: String): Observable<User> {
        return service.getMyself(token)
    }

    fun getOauthToken(code: String, grant_type: String, client_id: String, client_secret: String): Observable<OauthResponse> {
        return service.getOauthToken(code, grant_type, client_id, client_secret)
    }

    fun refreshOauthToken(grant_type: String, refresh_token: String, client_id: String, client_secret: String): Observable<OauthResponse> {
        return service.refreshToken(grant_type, refresh_token, client_id, client_secret)
    }

    fun getNotifications(token:String): Observable<List<Notification>> {
        return service.getNotifications(token)
    }

    fun getWalletBalance(token:String): Observable<Wallet> {
        return service.getWalletBalance(token)
    }

    fun getCurrencies(): Observable<TreeMap<String, Any>> {
        return service.getCurrencies()
    }

    fun getMethods(): Observable<TreeMap<String, Any>> {
        return service.getMethods()
    }

    fun markNotificationRead(token:String, id: String): Observable<ResponseBody> {
        return service.markNotificationRead(token, id)
    }
}