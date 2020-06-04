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

package com.thanksmister.bitcoin.localtrader.network.services;

import com.google.gson.JsonElement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisements;
import com.thanksmister.bitcoin.localtrader.network.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.network.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.network.api.model.Messages;
import com.thanksmister.bitcoin.localtrader.network.api.model.NewAddress;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notifications;
import com.thanksmister.bitcoin.localtrader.network.api.model.Places;
import com.thanksmister.bitcoin.localtrader.network.api.model.User;
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet;

import java.util.LinkedHashMap;
import java.util.TreeMap;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LocalBitcoinsService {

    String GET_MYSELF = "/api/myself/";
    String GET_ADS = "/api/ads/";
    String GET_WALLET = "/api/wallet/";
    String GET_WALLET_ADDRESS = "/api/wallet-addr/";
    String GET_WALLET_BALANCE = "/api/wallet-balance/";
    String GET_DASHBOARD = "/api/dashboard/";
    String GET_AD = "/api/ad-get/";
    String GET_CONTACT_INFO = "/api/contact_info/";
    String GET_NOTIFICATIONS = "/api/notifications/";
    String GET_CONTACT_MESSAGES = "/api/contact_messages/";
    String GET_CURRENCIES = "/api/currencies/";
    String DELETE_AD = "/api/ad-delete/";
    String UPDATE_AD = "/api/ad/";
    String CHECK_PINCODE = "/api/pincode/";
    String GET_PAYMENT_METHODS = "/api/payment_methods/";
    String GET_ADS_PLACES = "/api/places/";
    String POST_NOTIFICATIONS_MARK_READ = "/api/notifications/mark_as_read/";
    String POST_CONTACT_MESSAGE = "/api/contact_message_post/";
    String POST_CONTACT_RELEASE = "/api/contact_release_pin/";
    String POST_CONTACT_FUND = "/api/contact_fund/";
    String POST_CONTACT_CANCEL = "/api/contact_cancel/";
    String POST_CONTACT_PAID = "/api/contact_mark_as_paid/";
    String POST_CONTACT_DISPUTE = "/api/contact_dispute/";
    String POST_WALLET_SEND_PIN = "/api/wallet-send-pin/";
    String POST_CONTACT_CREATE = "/api/contact_create/";
    String POST_AD_CREATE = "/api/ad-create/";

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<Authorization> getAuthorization(@Field("grant_type") String grant_type,
                                               @Field("code") String code,
                                               @Field("client_id") String client_id,
                                               @Field("client_secret") String client_secret);

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<Authorization> refreshToken(@Field("grant_type") String grant_type,
                                           @Field("refresh_token") String refresh_token,
                                           @Field("client_id") String client_id,
                                           @Field("client_secret") String client_secret);

    @GET(GET_MYSELF)
    Observable<User> getMyself(@Query("access_token") String token);

    @GET(GET_CURRENCIES)
    Observable<TreeMap<String, Object>> getCurrencies();

    @GET(GET_DASHBOARD)
    Observable<Dashboard> getDashboard(@Query("access_token") String token);

    @GET(GET_DASHBOARD + "{type}/")
    Observable<Dashboard> getDashboard(@Path("type") String type,
                                       @Query("access_token") String token);

    @GET(GET_ADS)
    Observable<Advertisements> getAdvertisements(@Query("access_token") String token);

    @GET(GET_WALLET)
    Observable<Wallet> getWallet(@Query("access_token") String token);

    @GET(GET_WALLET_ADDRESS)
    Observable<NewAddress> getWalletAddress(@Query("access_token") String token);

    @GET(GET_WALLET_BALANCE)
    Observable<Wallet> getWalletBalance(@Query("access_token") String token);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContact(@Path("ad_id") String ad_id,
                                             @Query("access_token") String token,
                                             @Field("amount") String amount,
                                             @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactEmail(@Path("ad_id") String ad_id,
                                               @Query("access_token") String token,
                                               @Field("amount") String amount,
                                               @Field("receiver_email") String email,
                                               @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactPhone(@Path("ad_id") String ad_id,
                                               @Query("access_token") String token,
                                               @Field("amount") String amount,
                                               @Field("phone_number") String phone,
                                               @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactEthereumAddress(@Path("ad_id") String ad_id,
                                                         @Query("access_token") String token,
                                                         @Field("amount") String amount,
                                                         @Field("ethereum_address") String ethereum,
                                                         @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactBPay(@Path("ad_id") String ad_id,
                                              @Query("access_token") String token,
                                              @Field("amount") String amount,
                                              @Field("biller_code") String billerCode,
                                              @Field("reference") String reference,
                                              @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactNational(@Path("ad_id") String ad_id,
            @Query("access_token") String token,
                                                  @Field("amount") String amount,
                                                  @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactNational_UK(@Path("ad_id") String ad_id,
                                                     @Query("access_token") String token,
                                                     @Field("amount") String amount,
                                                     @Field("receiver_name") String name,
                                                     @Field("sort_code") String sortCode,
                                                     @Field("reference") String reference,
                                                     @Field("account_number") String accountNumber,
                                                     @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactNational_FI(@Path("ad_id") String ad_id,
            @Query("access_token") String token,
                                                     @Field("amount") String amount,
                                                     @Field("receiver_name") String name,
                                                     @Field("iban") String iban,
                                                     @Field("swift_bic") String bic,
                                                     @Field("reference") String reference,
                                                     @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactNational_AU(@Path("ad_id") String ad_id,
                                                     @Query("access_token") String token,
                                                     @Field("amount") String amount,
                                                     @Field("receiver_name") String name,
                                                     @Field("bbs") String bsb,
                                                     @Field("reference") String reference,
                                                     @Field("account_number") String accountNumber,
                                                     @Field("message") String message);

    @FormUrlEncoded
    @POST(POST_CONTACT_CREATE + "{ad_id}/")
    Observable<ContactRequest> createContactSepa(@Path("ad_id") String ad_id,
                                              @Query("access_token") String token,
                                              @Field("amount") String amount,
                                              @Field("receiver_name") String name,
                                              @Field("iban") String iban,
                                              @Field("swift_bic") String bic,
                                              @Field("reference") String reference,
                                              @Field("message") String message);

    @GET("/{type}/{country_code}/{country_name}/.json")
    Observable<Advertisements> searchOnlineAds(@Path("type") String type,
                                               @Path("country_code") String country_code,
                                               @Path("country_name") String country_name);

    @GET("/{type}/{currency}/{payment_method}/.json")
    Observable<Advertisements> searchOnlineAdsCurrencyPayment(@Path("type") String type,
                                                              @Path("currency") String currency,
                                                              @Path("payment_method") String payment_method);

    @GET("/{type}/{currency}/.json")
    Observable<Advertisements> searchOnlineAdsCurrency(@Path("type") String type, @Path("currency") String currency);

    @GET("/{type}/{payment_method}/.json")
    Observable<Advertisements> searchOnlineAdsPayment(@Path("type") String type, @Path("payment_method") String payment_method);

    @GET("/{type}/.json")
    Observable<Advertisements> searchOnlineAdsAll(@Path("type") String type);


    @GET("/{type}/{country_code}/{country_name}/{payment_method}/.json")
    Observable<Advertisements> searchOnlineAds(@Path("type") String type,
                                               @Path("country_code") String country_code,
                                               @Path("country_name") String country_name,
                                               @Path("payment_method") String payment_method);

    @GET(GET_PAYMENT_METHODS)
    Observable<TreeMap<String, Object>> getOnlineProviders();

    @GET(GET_PAYMENT_METHODS + "{countrycode}/")
    Observable<TreeMap<String, Object>> getOnlineProviders(@Path("countrycode") String countrycode);

    @GET(GET_CONTACT_INFO + "{contact_id}/")
    Observable<Contact> getContactInfo(@Path("contact_id") int contact_id,
                                       @Query("access_token") String token);

    @GET(GET_CONTACT_MESSAGES + "{contact_id}/")
    Observable<Messages> contactMessages(@Path("contact_id") int contact_id,
                                         @Query("access_token") String token);

    @GET(GET_AD + "{ad_id}/")
    Observable<Advertisements> getAdvertisement(@Path("ad_id") int ad_id,
                                               @Query("access_token") String token);

    @FormUrlEncoded
    @POST(UPDATE_AD + "{ad_id}/")
    Observable<JsonElement> updateAdvertisement(@Path("ad_id") String ad_id,
                                                @Query("access_token") String token,
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
                                                @Field("first_time_limit_btc") String first_time_limit_btc,
                                                @Field("phone_number") String phone_number);

    @POST(DELETE_AD + "{ad_id}/")
    Observable<JsonElement> deleteAdvertisement(@Path("ad_id") int ad_id,
                                                @Query("access_token") String token);

    @FormUrlEncoded
    @POST(POST_AD_CREATE)
    Observable<JsonElement> createAdvertisement(@Query("access_token") String token,
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
                                                @Field("currency") String currency,
                                                @Field("phone_number") String phone_number,
                                                @Field("opening_hours") String opening_hours);

    @POST(POST_CONTACT_DISPUTE + "{contact_id}/")
    Observable<JsonElement> contactDispute(@Path("contact_id") int contact_id,
                                           @Query("access_token") String token);

    @FormUrlEncoded
    @POST(POST_CONTACT_MESSAGE + "{contact_id}/")
    Observable<JsonElement> contactMessagePost(@Path("contact_id") int contact_id,
                                               @Query("access_token") String token,
                                               @Field("msg") String msg);

    @Multipart
    @POST(POST_CONTACT_MESSAGE + "{contact_id}/")
    Observable<JsonElement> contactMessagePostWithAttachment(@Path("contact_id") int contact_id,
                                                             @Query("access_token") String token,
                                                             @PartMap LinkedHashMap<String, String> params,
                                                             @Part MultipartBody.Part part);

    @POST(POST_CONTACT_PAID + "{contact_id}/")
    Observable<JsonElement> markAsPaid(@Path("contact_id") int contact_id,
                                       @Query("access_token") String token);

    @FormUrlEncoded
    @POST(POST_CONTACT_RELEASE + "{contact_id}/")
    Observable<JsonElement> releaseContactPinCode(@Path("contact_id") int contact_id,
                                                  @Query("access_token") String token,
                                                  @Field("pincode") String pincode);

    @POST(POST_CONTACT_CANCEL + "{contact_id}/")
    Observable<JsonElement> contactCancel(@Path("contact_id") int contact_id,
                                          @Query("access_token") String token);

    @POST(POST_CONTACT_FUND + "{contact_id}/")
    Observable<JsonElement> contactFund(@Path("contact_id") int contact_id,
                                        @Query("access_token") String token);

    @FormUrlEncoded
    @POST(CHECK_PINCODE)
    Observable<JsonElement> checkPinCode(@Query("access_token") String token,
                                         @Field("pincode") String pin_code);

    @FormUrlEncoded
    @POST(POST_WALLET_SEND_PIN)
    Observable<JsonElement> walletSendPin(@Query("access_token") String token,
                                          @Field("pincode") String pincode,
                                          @Field("address") String address,
                                          @Field("amount") String amount);

    @GET(GET_NOTIFICATIONS)
    Observable<Notifications> getNotifications(@Query("access_token") String token);


    @POST(POST_NOTIFICATIONS_MARK_READ + "{notification_id}/")
    Observable<JsonElement> markNotificationRead(@Path("notification_id") String notificationId,
                                                 @Query("access_token") String token);
}