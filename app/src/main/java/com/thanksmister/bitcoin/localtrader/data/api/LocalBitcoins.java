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

package com.thanksmister.bitcoin.localtrader.data.api;

import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface LocalBitcoins
{
    @GET("/bitcoinaverage/ticker-all-currencies/")
    Observable<Response> getCurrencies();
    
    @GET("/api/myself/")
    Observable<Response> getMyself(@Query("access_token") String token);
    
    @GET("/api/account_info/{username}/")
    Observable<Response> getAccountInfo(@Path("username") String username);

    @GET("/api/dashboard/")
    Observable<Response> getDashboard(@Query("access_token") String token);

    @GET("/api/dashboard/{type}/")
    Observable<Response> getDashboard(@Query("access_token") String token, @Path("type") String type);

    @GET("/api/ads/")
    Observable<Response> getAds(@Query("access_token") String token);

    @GET("/api/wallet/")
    Observable<Response> getWallet(@Query("access_token") String token);

    @GET("/api/wallet-balance/")
    Observable<Response> getWalletBalance(@Query("access_token") String token);

    @FormUrlEncoded
    @POST("/api/contact_create/{ad_id}/")
    Observable<Response> createContact(@Path("ad_id") String ad_id, @Query("access_token") String token, @Field("amount") String amount, @Field("message") String message);

    @GET("/{type}/{country_code}/{country_name}/.json")
    Observable<Response> searchOnlineAds(@Path("type") String type, @Path("country_code") String country_code, @Path("country_name") String country_name);

    @GET("/{type}/{country_code}/{country_name}/{payment_method}/.json")
    Observable<Response> searchOnlineAds(@Path("type") String type, @Path("country_code") String country_code, @Path("country_name") String country_name, @Path("payment_method") String payment_method);
   
    @GET("/api/places/")
    Observable<Response> getPlaces(@Query("lat") double lat, @Query("lon") double lon);

    @GET("/{type}/{num}/{loc}/.json")
    Observable<Response> searchAdsByPlace(@Path("type") String type, @Path("num") String num, @Path("loc") String loc);
    
    @GET("/api/payment_methods/")
    Observable<Response> getOnlineProviders();

    @GET("/api/payment_methods/{countrycode}/")
    Observable<Response> getOnlineProviders(@Path("countrycode") String countrycode);

    @GET("/api/contact_info/{contact_id}/")
    Observable<Response> getContact(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @GET("/api/contact_messages/{contact_id}/")
    Observable<Response> contactMessages(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @POST("/api/ad-get/{ad_id}/")
    Observable<Response> getAdvertisement(@Path("ad_id") String ad_id, @Query("access_token") String token);
    
    @FormUrlEncoded
    @POST("/api/ad/{ad_id}/")
    Observable<Response> updateAdvertisement(@Path("ad_id") String ad_id,
                                             @Query("access_token") String token, @Field("visible") String visible,
                                             @Field("min_amount") String min_amount, @Field("max_amount") String max_amount, @Field("price_equation") String price_equation,
                                             @Field("lat") String lat, @Field("lon") String lon, @Field("city") String city,
                                             @Field("location_string") String location_string, @Field("countrycode") String countrycode,
                                             @Field("account_info") String account_info, @Field("bank_name") String bank_name,
                                             @Field("sms_verification_required") String sms_verification_required, @Field("track_max_amount") String track_max_amount,
                                             @Field("require_trusted_by_advertiser") String require_trusted_by_advertiser, @Field("msg") String msg);

    @POST("/api/ad-delete/{ad_id}/")
    Observable<Response> deleteAdvertisement(@Path("ad_id") String ad_id, @Query("access_token") String token);

    @FormUrlEncoded
    @POST("/api/ad-create/{ad_id}/")
    Observable<Response> createAdvertisement(@Path("ad_id") String ad_id, @Query("access_token") String token,
                                             @Field("min_amount") String min_amount, @Field("max_amount") String max_amount, @Field("price_equation") String price_equation,
                                             @Field("trade_type") String trade_type, @Field("online_provider") String online_provider,
                                             @Field("lat") String lat, @Field("lon") String lon, @Field("city") String city,
                                             @Field("location_string") String location_string, @Field("countrycode") String countrycode,
                                             @Field("account_info") String account_info, @Field("bank_name") String bank_name,
                                             @Field("sms_verification_required") String sms_verification_required, @Field("track_max_amount") String track_max_amount,
                                             @Field("require_trusted_by_advertiser") String require_trusted_by_advertiser, @Field("msg") String msg);

    @POST("/api/contact_dispute/{contact_id}/")
    Observable<Response> contactDispute(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @FormUrlEncoded
    @POST("/api/contact_message_post/{contact_id}/")
    Observable<Response> contactMessagePost(@Path("contact_id") String contact_id, @Query("access_token") String token, @Field("msg") String msg);
    
    @POST("/api/contact_mark_as_paid/{contact_id}/")
    Observable<Response> markAsPaid(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @POST("/api/contact_release/{contact_id}/")
    Observable<Response> releaseContact(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @FormUrlEncoded
    @POST("/api/contact_release_pin/{contact_id}/")
    Observable<Response> releaseContactPinCode(@Path("contact_id") String contact_id, @Field("pincode") String pincode,  @Query("access_token") String token);

    @POST("/api/contact_cancel/{contact_id}/")
    Observable<Response> contactCancel(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @POST("/api/contact_fund/{contact_id}/")
    Observable<Response> contactFund(@Path("contact_id") String contact_id, @Query("access_token") String token);

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<Response> getAuthorization(@Field("grant_type") String grant_type, @Field("code") String code, @Field("client_id") String client_id, @Field("client_secret") String client_secret);

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<Response> refreshToken(@Field("grant_type") String grant_type, @Field("refresh_token") String refresh_token, @Field("client_id") String client_id, @Field("client_secret") String client_secret);

    @FormUrlEncoded
    @POST("/api/pincode/")
    Observable<Response> checkPinCode(@Field("pincode") String pin_code, @Query("access_token") String token);
    
    @FormUrlEncoded
    @POST("/api/wallet-send-pin/")
    Observable<Response> walletSendPin(@Field("pincode") String pincode, @Field("address") String address, @Field("amount") String amount, @Query("access_token") String token);

    @FormUrlEncoded
    @POST("/api/logout/")
    Observable<Response> logOut(@Query("access_token") String token);
}