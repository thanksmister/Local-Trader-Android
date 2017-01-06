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

import java.util.LinkedHashMap;

import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import rx.Observable;

public interface LocalBitcoins
{
    String GET_MYSELF = "/api/myself/";
    String GET_ADS = "/api/ads/";
    String GET_WALLET = "/api/wallet/";
    String GET_WALLET_BALANCE = "/api/wallet-balance/";
    String GET_DASHBOARD = "/api/dashboard/";
    String GET_AD = "/api/ad-get/";
    String GET_CONTACT = "/api/contact_info/";
    String GET_RECENT_MESSAGES = "/api/recent_messages/";
    String GET_CONTACT_MESSAGES = "/api/contact_messages/";
    String GET_CURRENCIES = "/api/currencies/";
    String DELETE_AD = "/api/ad-delete/";
    String UPDATE_AD = "/api/ad/";
    String POST_CONTACT_MESSAGE = "/api/contact_message_post/";
    String CHECK_PINCODE = "/api/pincode/";
    String GET_PAYMENT_METHODS = "/api/payment_methods/";
    String GET_COUNTRY_CODES = "/api/countrycodes/";
    String GET_ADS_BY_PLACE = "/get/ads/by/place/";
    String GET_ONLINE_ADS = "/get/online/ads/";
    String GET_ADS_PLACES = "/api/places/";
    String POST_CONTACT_RELEASE = "/api/contact_release_pin/";
    String POST_CONTACT_FUND = "/api/contact_fund/";
    String POST_CONTACT_CANCEL = "/api/contact_cancel/";
    String POST_CONTACT_PAID = "/api/contact_mark_as_paid/";
    String POST_CONTACT_DISPUTE = "/api/contact_dispute/";
    String POST_WALLET_SEND_PIN = "/api/wallet-send-pin/";
    String POST_CONTACT_CREATE = "/api/contact_create/";
    String POST_AD_CREATE = "/api/ad-create/";
    String DOWNLOAD_ATTACHMENT = "/api/download/";

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<Response> getAuthorization(@Field("grant_type") String grant_type, @Field("code") String code, @Field("client_id") String client_id, @Field("client_secret") String client_secret);

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<Response> refreshToken(@Field("grant_type") String grant_type, @Field("refresh_token") String refresh_token, @Field("client_id") String client_id, @Field("client_secret") String client_secret);
    
    @GET(GET_MYSELF)
    Observable<Response> getMyself(@Query("access_token") String token);
    
    @GET(GET_DASHBOARD)
    Observable<Response> getDashboard(@Query("access_token") String token);

    @GET(GET_DASHBOARD + "{type}/")
    Observable<Response> getDashboard(@Query("access_token") String token, 
                                      @Path("type") String type);

    @GET(GET_ADS)
    Observable<Response> getAds(@Query("access_token") String token);

    @GET(GET_WALLET)
    Observable<Response> getWallet(@Query("access_token") String token);

    @GET(GET_WALLET_BALANCE)
    Observable<Response> getWalletBalance(@Query("access_token") String token);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContact(@Query("access_token") String token,
                                       @Path("ad_id") String ad_id,
                                       @Field("amount") String amount, 
                                       @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactPayPal(@Query("access_token") String token,
                                             @Path("ad_id") String ad_id,
                                             @Field("amount") String amount,
                                             @Field("details-receiver_email") String email);
    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactNational(@Query("access_token") String token,
                                           @Path("ad_id") String ad_id,
                                           @Field("amount") String amount,
                                           @Field("details-receiver_name") String name,
                                           @Field("details-iban") String iban,
                                           @Field("details-swift_bic") String bic,
                                           @Field("details-reference") String reference,
                                           @Field("details-message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactQiwi(@Query("access_token") String token,
                                           @Path("ad_id") String ad_id,
                                           @Field("amount") String amount,
                                           @Field("details-phone_number") String phone);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactSepa(@Query("access_token") String token,
                                           @Path("ad_id") String ad_id,
                                           @Field("amount") String amount,
                                           @Field("details-receiver_name") String name,
                                           @Field("details-iban") String iban,
                                           @Field("details-swift_bic") String bic,
                                           @Field("details-reference") String reference);

    @GET("/{type}/{country_code}/{country_name}/.json")
    Observable<Response> searchOnlineAds(@Path("type") String type, 
                                         @Path("country_code") String country_code, 
                                         @Path("country_name") String country_name);

    @GET("/{type}/{country_code}/{country_name}/{payment_method}/.json")
    Observable<Response> searchOnlineAds(@Path("type") String type, 
                                         @Path("country_code") String country_code, 
                                         @Path("country_name") String country_name, 
                                         @Path("payment_method") String payment_method);
   
    @GET(GET_ADS_PLACES)
    Observable<Response> getPlaces(@Query("lat") double lat, 
                                   @Query("lon") double lon);

    @GET("/{type}/{num}/{loc}/.json")
    Observable<Response> searchAdsByPlace(@Path("type") String type, 
                                          @Path("num") String num, 
                                          @Path("loc") String loc);
    
    @GET(GET_PAYMENT_METHODS)
    Observable<Response> getOnlineProviders();

    @GET(GET_PAYMENT_METHODS + "{countrycode}/")
    Observable<Response> getOnlineProviders(@Path("countrycode") String countrycode);

    @GET(GET_CONTACT + "{contact_id}/")
    Observable<Response> getContact(@Query("access_token") String token,
                                    @Path("contact_id") String contact_id);

    @GET(GET_CONTACT_MESSAGES + "{contact_id}/")
    Observable<Response> contactMessages(@Query("access_token") String token,
                                         @Path("contact_id") String contact_id);

    @GET(GET_AD + "{ad_id}/")
    Observable<Response> getAdvertisement(@Query("access_token") String token,
                                          @Path("ad_id") String ad_id);
    
    @FormUrlEncoded
    @POST(UPDATE_AD + "{ad_id}/")
    Observable<Response> updateAdvertisement(@Query("access_token") String token,
                                             @Path("ad_id") String ad_id,
                                             @Field("account_info") String account_info,
                                             @Field("bank_name") String bank_name,
                                             @Field("city") String city,
                                             @Field("countrycode") String countrycode,
                                             @Field("currency") String currency,
                                             @Field("lat") String lat,
                                             @Field("location_string") String location_string,
                                             @Field("lon") String lon,
                                             @Field("max_amount") String max_amount,
                                             @Field("min_amount") String min_amount,
                                             @Field("msg") String msg,
                                             @Field("price_equation") String price_equation,
                                             @Field("require_trusted_by_advertiser") String require_trusted_by_advertiser,
                                             @Field("sms_verification_required") String sms_verification_required,
                                             @Field("track_max_amount") String track_max_amount,
                                             @Field("visible") String visible,
                                             @Field("require_identification") String require_identification,
                                             @Field("require_feedback_score") String require_feedback_score,
                                             @Field("require_trade_volume") String require_trade_volume,
                                             @Field("first_time_limit_btc") String first_time_limit_btc);
    @POST(DELETE_AD + "{ad_id}/")
    Observable<Response> deleteAdvertisement(@Query("access_token") String token,
                                             @Path("ad_id") String ad_id);

    @FormUrlEncoded
    @POST(POST_AD_CREATE)
    Observable<Response> createAdvertisement(@Query("access_token") String token,
                                             @Field("min_amount") String min_amount, 
                                             @Field("max_amount") String max_amount, 
                                             @Field("price_equation") String price_equation,
                                             @Field("trade_type") String trade_type, 
                                             @Field("online_provider") String online_provider,
                                             @Field("lat") String lat, 
                                             @Field("lon") String lon, 
                                             @Field("city") String city,
                                             @Field("location_string") String location_string, 
                                             @Field("countrycode") String countrycode,
                                             @Field("account_info") String account_info, 
                                             @Field("bank_name") String bank_name,
                                             @Field("sms_verification_required") String sms_verification_required, 
                                             @Field("track_max_amount") String track_max_amount,
                                             @Field("require_trusted_by_advertiser") String require_trusted_by_advertiser, 
                                             @Field("require_identification") String require_identification, 
                                             @Field("require_feedback_score") String require_feedback_score, 
                                             @Field("require_trade_volume") String require_trade_volume, 
                                             @Field("first_time_limit_btc") String first_time_limit_btc, 
                                             @Field("msg") String msg, 
                                             @Field("currency") String currency);

    @POST(POST_CONTACT_DISPUTE + "{contact_id}/")
    Observable<Response> contactDispute(@Query("access_token") String token,
                                        @Path("contact_id") String contact_id);

    @FormUrlEncoded
    @POST(POST_CONTACT_MESSAGE + "{contact_id}/")
    Observable<Response> contactMessagePost(@Query("access_token") String token,
                                            @Path("contact_id") String contact_id, 
                                            @Field("msg") String msg);
    
    @Multipart
    @POST(POST_CONTACT_MESSAGE + "{contact_id}/")
    Observable<Response> contactMessagePostWithAttachment(@Query("access_token") String token,
                                            @Path("contact_id") String contact_id,
                                            @PartMap LinkedHashMap<String,String> params,
                                            @Part("document") TypedFile document);
    
    @POST(POST_CONTACT_PAID + "{contact_id}/")
    Observable<Response> markAsPaid(@Query("access_token") String token,
                                    @Path("contact_id") String contact_id);
    
    @FormUrlEncoded
    @POST(POST_CONTACT_RELEASE + "{contact_id}/")
    Observable<Response> releaseContactPinCode(@Query("access_token") String token,
                                               @Path("contact_id") String contact_id, 
                                               @Field("pincode") String pincode);

    @POST(POST_CONTACT_CANCEL + "{contact_id}/")
    Observable<Response> contactCancel(@Query("access_token") String token,
                                       @Path("contact_id") String contact_id);

    @POST(POST_CONTACT_FUND + "{contact_id}/")
    Observable<Response> contactFund(@Query("access_token") String token,
                                     @Path("contact_id") String contact_id);
    
    @FormUrlEncoded
    @POST(CHECK_PINCODE)
    Observable<Response> checkPinCode(@Query("access_token") String token,
                                      @Field("pincode") String pin_code);
    
    @FormUrlEncoded
    @POST(POST_WALLET_SEND_PIN)
    Observable<Response> walletSendPin(@Query("access_token") String token,
                                       @Field("pincode") String pincode,
                                       @Field("address") String address, 
                                       @Field("amount") String amount);

    @GET(GET_RECENT_MESSAGES)
    Observable<Response> recentMessages(@Query("access_token") String token);


    @GET(GET_MYSELF)
    Observable<Response> getMyself(@Header("Apiauth-Key") String key,
                                   @Header("Apiauth-Nonce") String nonce,
                                   @Header("Apiauth-Signature") String signature);

    @GET(GET_DASHBOARD)
    Observable<Response> getDashboard(@Header("Apiauth-Key") String key,
                                      @Header("Apiauth-Nonce") String nonce,
                                      @Header("Apiauth-Signature") String signature);

    @GET(GET_DASHBOARD + "{type}/")
    Observable<Response> getDashboard(@Header("Apiauth-Key") String key,
                                      @Header("Apiauth-Nonce") String nonce,
                                      @Header("Apiauth-Signature") String signature,
                                      @Path("type") String type);

    @GET(GET_ADS)
    Observable<Response> getAds(@Header("Apiauth-Key") String key,
                                @Header("Apiauth-Nonce") String nonce,
                                @Header("Apiauth-Signature") String signature);

    @GET(GET_WALLET)
    Observable<Response> getWallet(@Header("Apiauth-Key") String key,
                                   @Header("Apiauth-Nonce") String nonce,
                                   @Header("Apiauth-Signature") String signature);

    @GET(GET_WALLET_BALANCE)
    Observable<Response> getWalletBalance(@Header("Apiauth-Key") String key,
                                          @Header("Apiauth-Nonce") String nonce,
                                          @Header("Apiauth-Signature") String signature);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContact(@Header("Apiauth-Key") String key,
                                       @Header("Apiauth-Nonce") String nonce,
                                       @Header("Apiauth-Signature") String signature,
                                       @Path("ad_id") String ad_id,
                                       @Field("amount") String amount,
                                       @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactPayPal(@Header("Apiauth-Key") String key,
                                           @Header("Apiauth-Nonce") String nonce,
                                           @Header("Apiauth-Signature") String signature,
                                           @Path("ad_id") String ad_id,
                                           @Field("amount") String amount,
                                           @Field("details-receiver_email") String email);
    
    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactNational(@Header("Apiauth-Key") String key,
                                               @Header("Apiauth-Nonce") String nonce,
                                               @Header("Apiauth-Signature") String signature,
                                               @Path("ad_id") String ad_id,
                                               @Field("amount") String amount,
                                               @Field("details-receiver_name") String name,
                                               @Field("details-iban") String iban,
                                               @Field("details-swift_bic") String bic,
                                               @Field("details-reference") String reference,
                                               @Field("details-message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactQiwi(@Header("Apiauth-Key") String key,
                                             @Header("Apiauth-Nonce") String nonce,
                                             @Header("Apiauth-Signature") String signature,
                                             @Path("ad_id") String ad_id,
                                             @Field("amount") String amount,
                                             @Field("details-phone_number") String phone);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<Response> createContactSepa(@Header("Apiauth-Key") String key,
                                           @Header("Apiauth-Nonce") String nonce,
                                           @Header("Apiauth-Signature") String signature,
                                           @Path("ad_id") String ad_id,
                                           @Field("amount") String amount, 
                                           @Field("details-receiver_name") String name,
                                           @Field("details-iban") String iban,
                                           @Field("details-swift_bic") String bic,
                                           @Field("details-reference") String reference);

    @GET(GET_CONTACT + "{contact_id}/")
    Observable<Response> getContact(@Header("Apiauth-Key") String key,
                                    @Header("Apiauth-Nonce") String nonce,
                                    @Header("Apiauth-Signature") String signature,
                                    @Path("contact_id") String contact_id);

    @GET(GET_CONTACT_MESSAGES + "{contact_id}/")
    Observable<Response> contactMessages(@Header("Apiauth-Key") String key,
                                         @Header("Apiauth-Nonce") String nonce,
                                         @Header("Apiauth-Signature") String signature,
                                         @Path("contact_id") String contact_id);

    @GET(GET_AD + "{ad_id}/")
    Observable<Response> getAdvertisement(@Header("Apiauth-Key") String key,
                                          @Header("Apiauth-Nonce") String nonce,
                                          @Header("Apiauth-Signature") String signature,
                                          @Path("ad_id") String ad_id);

    @FormUrlEncoded
    @POST(UPDATE_AD + "{ad_id}/")
    Observable<Response> updateAdvertisement(@Header("Apiauth-Key") String key,
                                             @Header("Apiauth-Nonce") String nonce,
                                             @Header("Apiauth-Signature") String signature,
                                             @Path("ad_id") String ad_id,
                                             @Field("min_amount") String min_amount,
                                             @Field("max_amount") String max_amount,
                                             @Field("account_info") String account_info,
                                             @Field("bank_name") String bank_name,
                                             @Field("city") String city,
                                             @Field("location_string") String location_string,
                                             @Field("countrycode") String countrycode,
                                             @Field("currency") String currency,
                                             @Field("lat") String lat,
                                             @Field("lon") String lon,
                                             @Field("msg") String msg,
                                             @Field("price_equation") String price_equation,
                                             @Field("require_trusted_by_advertiser") String require_trusted_by_advertiser,
                                             @Field("sms_verification_required") String sms_verification_required,
                                             @Field("track_max_amount") String track_max_amount,
                                             @Field("visible") String visible);
    @POST(DELETE_AD + "{ad_id}/")
    Observable<Response> deleteAdvertisement(@Header("Apiauth-Key") String key,
                                             @Header("Apiauth-Nonce") String nonce,
                                             @Header("Apiauth-Signature") String signature,
                                             @Path("ad_id") String ad_id);

    @FormUrlEncoded
    @POST(POST_AD_CREATE)
    Observable<Response> createAdvertisement(@Header("Apiauth-Key") String key,
                                             @Header("Apiauth-Nonce") String nonce,
                                             @Header("Apiauth-Signature") String signature,
                                             @Field("min_amount") String min_amount,
                                             @Field("max_amount") String max_amount, 
                                             @Field("price_equation") String price_equation,
                                             @Field("trade_type") String trade_type, 
                                             @Field("online_provider") String online_provider,
                                             @Field("lat") String lat, 
                                             @Field("lon") String lon, 
                                             @Field("city") String city,
                                             @Field("location_string") String location_string, 
                                             @Field("countrycode") String countrycode,
                                             @Field("account_info") String account_info, 
                                             @Field("bank_name") String bank_name,
                                             @Field("require_trusted_by_advertiser") String require_trusted_by_advertiser,
                                             @Field("sms_verification_required") String sms_verification_required, 
                                             @Field("track_max_amount") String track_max_amount,
                                             @Field("msg") String msg,
                                             @Field("currency") String currency);

    @POST(POST_CONTACT_DISPUTE + "{contact_id}/")
    Observable<Response> contactDispute(@Header("Apiauth-Key") String key,
                                        @Header("Apiauth-Nonce") String nonce,
                                        @Header("Apiauth-Signature") String signature,
                                        @Path("contact_id") String contact_id);

    @FormUrlEncoded
    @POST(POST_CONTACT_MESSAGE + "{contact_id}/")
    Observable<Response> contactMessagePost(@Header("Apiauth-Key") String key,
                                            @Header("Apiauth-Nonce") String nonce,
                                            @Header("Apiauth-Signature") String signature,
                                            @Path("contact_id") String contact_id,
                                            @Field("msg") String msg);

    @POST(POST_CONTACT_PAID + "{contact_id}/")
    Observable<Response> markAsPaid(@Header("Apiauth-Key") String key,
                                    @Header("Apiauth-Nonce") String nonce,
                                    @Header("Apiauth-Signature") String signature,
                                    @Path("contact_id") String contact_id);

    @FormUrlEncoded
    @POST(POST_CONTACT_RELEASE + "{contact_id}/")
    Observable<Response> releaseContactPinCode(@Header("Apiauth-Key") String key,
                                               @Header("Apiauth-Nonce") String nonce,
                                               @Header("Apiauth-Signature") String signature,
                                               @Path("contact_id") String contact_id,
                                               @Field("pincode") String pincode);

    @POST(POST_CONTACT_CANCEL + "{contact_id}/")
    Observable<Response> contactCancel(@Header("Apiauth-Key") String key,
                                       @Header("Apiauth-Nonce") String nonce,
                                       @Header("Apiauth-Signature") String signature,
                                       @Path("contact_id") String contact_id);

    @POST(POST_CONTACT_FUND + "{contact_id}/")
    Observable<Response> contactFund(@Header("Apiauth-Key") String key,
                                     @Header("Apiauth-Nonce") String nonce,
                                     @Header("Apiauth-Signature") String signature,
                                     @Path("contact_id") String contact_id);

    @FormUrlEncoded
    @POST(CHECK_PINCODE)
    Observable<Response> checkPinCode(@Header("Apiauth-Key") String key,
                                      @Header("Apiauth-Nonce") String nonce,
                                      @Header("Apiauth-Signature") String signature,
                                      @Field("pincode") String pin_code);

    @FormUrlEncoded
    @POST(POST_WALLET_SEND_PIN)
    Observable<Response> walletSendPin(@Header("Apiauth-Key") String key,
                                       @Header("Apiauth-Nonce") String nonce,
                                       @Header("Apiauth-Signature") String signature,
                                       @Field("pincode") String pincode,
                                       @Field("address") String address,
                                       @Field("amount") String amount);

    @GET(GET_RECENT_MESSAGES)
    Observable<Response> recentMessages(@Header("Apiauth-Key") String key,
                                        @Header("Apiauth-Nonce") String nonce,
                                        @Header("Apiauth-Signature") String signature);
}