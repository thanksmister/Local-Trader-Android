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

package com.thanksmister.bitcoin.localtrader.data.services;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAd;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToCurrencies;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToJSONObject;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMethod;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToNotifications;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToUser;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.client.Response;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.CHECK_PINCODE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.DELETE_AD;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_AD;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_CONTACT;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_CONTACT_MESSAGES;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_DASHBOARD;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_RECENT_MESSAGES;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_WALLET_BALANCE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_AD_CREATE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_CANCEL;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_CREATE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_DISPUTE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_FUND;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_MESSAGE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_PAID;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_CONTACT_RELEASE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.POST_WALLET_SEND_PIN;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.UPDATE_AD;


@Singleton
public class DataService
{
    private static final String PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire";
    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_ADVERTISEMENT_EXPIRE_TIME = "pref_ads_expire";
    public static final String PREFS_WALLET_EXPIRE_TIME = "pref_wallet_expire";
    public static final String PREFS_CONTACTS_EXPIRE_TIME = "pref_ads_expire";
    public static final String PREFS_USER = "pref_user";

    public static final int CHECK_EXCHANGE_DATA = 3 * 60 * 1000;// 3 minutes
    private static final int CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
    public static final int CHECK_ADVERTISEMENT_DATA = 15 * 60 * 1000;// 15 minutes
    public static final int CHECK_CONTACTS_DATA = 5 * 60 * 1000;// 5 minutes
    public static final int CHECK_WALLET_DATA = 5 * 60 * 1000;// 15 minutes
    
    private final LocalBitcoins localBitcoins;
    private final SharedPreferences sharedPreferences;
    private final BaseApplication baseApplication;
    private int retryLimit = 1;
    
    @Inject
    public DataService(BaseApplication baseApplication, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins) {
        this.baseApplication = baseApplication;
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
    }
    
    public void logout() {
        resetExchangeExpireTime();
        resetAdvertisementsExpireTime();
        resetMethodsExpireTime();
        resetContactsExpireTime();
    }

    public Observable<Authorization> getAuthorization(String code)
    {
        return localBitcoins.getAuthorization("authorization_code", code, baseApplication.getString(R.string.lbc_access_key), baseApplication.getString(R.string.lbc_access_secret))
                .map(new ResponseToAuthorize());
    }

    private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed) {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                if(DataServiceUtils.isHttp403Error(throwable)) {
                    return refreshTokens()
                            .flatMap(new Func1<String, Observable<? extends T>>() {
                                @Override
                                public Observable<? extends T> call(String token)
                                {
                                    return toBeResumed;
                                }
                            });
                } else if (DataServiceUtils.isHttp400Error(throwable)) {
                    RetroError error = DataServiceUtils.createRetroError(throwable);
                    if(error.getCode() == DataServiceUtils.CODE_THREE) {
                        return refreshTokens()
                                .flatMap(new Func1<String, Observable<? extends T>>() {
                                    @Override
                                    public Observable<? extends T> call(String token)
                                    {
                                        return toBeResumed;
                                    }
                                });
                    } else {
                        return Observable.error(throwable);
                    }
                }
                // re-throw this error because it's not recoverable from here
                return Observable.error(throwable);
            }
        };
    }

    private Observable<String> refreshTokens() {
        final String refreshToken = AuthUtils.getRefreshToken(sharedPreferences);
        return localBitcoins.refreshToken("refresh_token", refreshToken, baseApplication.getString(R.string.lbc_access_key), baseApplication.getString(R.string.lbc_access_secret))
                .map(new ResponseToAuthorize())
                .flatMap(new Func1<Authorization, Observable<? extends String>>()
                {
                    @Override
                    public Observable<? extends String> call(Authorization authorization)
                    {
                        AuthUtils.setAccessToken(sharedPreferences, authorization.access_token);
                        AuthUtils.setRefreshToken(sharedPreferences, authorization.refresh_token);
                        return Observable.just(authorization.access_token);
                    }
                });
    }

    public Observable<List<ExchangeCurrency>> getCurrencies() {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return  localBitcoins.getCurrencies(accessToken)
                .map(new ResponseToCurrencies());
    }
    
    public Observable<ContactRequest> createContact(final String adId, final TradeType tradeType, final String countryCode, 
                                                    final String onlineProvider, final String amount, final String name, 
                                                    final String phone, final String email, final String iban, final String bic, 
                                                    final String reference, final String message)
    {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount, 
                    name, phone, email, iban, bic, reference, message, retryLimit)
                    .map(new ResponseToContactRequest());
        }
        
        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                name, phone, email, iban, bic, reference, message)
                .onErrorResumeNext(refreshTokenAndRetry(createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                        name, phone, email, iban, bic, reference, message)))
                .map(new ResponseToContactRequest());
    }


    private Observable<Response> createContactObservable(final String adId, final TradeType tradeType, final String countryCode,
                                                         final String onlineProvider, final String amount, final String name,
                                                         final String phone, final String email, final String iban, final String bic,
                                                         final String reference, final String message)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    return localBitcoins.createContactNational(accessToken, adId, amount, name, iban, bic, reference, message);
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                    return localBitcoins.createContactQiwi(accessToken, adId, amount, phone);
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                    return localBitcoins.createContactPayPal(accessToken, adId, amount, email);
                case TradeUtils.SEPA:
                    return localBitcoins.createContactSepa(accessToken, adId, amount, name, iban, bic, reference);
            }
        }
        
        return localBitcoins.createContact(accessToken, adId, amount, message);
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount) {
        return sendPinCodeMoneyObservable(pinCode, address, amount)
                .onErrorResumeNext(refreshTokenAndRetry(sendPinCodeMoneyObservable(pinCode, address, amount)))
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>()
                {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject)
                    {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        }
                        return Observable.just(true);
                    }
                });
    }
    
    private Observable<Response> sendPinCodeMoneyObservable(final String pinCode, final String address, final String amount)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.walletSendPin(accessToken, pinCode, address, amount);
    }

    public Observable<Wallet> getWalletBalance() {
        return getWalletBalanceObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletBalanceObservable()))
                .map(new ResponseToWalletBalance())
                .flatMap(new Func1<Wallet, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(Wallet wallet)
                    {
                        return getWalletBitmap(wallet);
                    }
                });
    }

    private Observable<Response> getWalletBalanceObservable() {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getWalletBalance(accessToken);
    }

    public Observable<JSONObject> validatePinCode(final String pinCode) {
        return validatePinCodeObservable(pinCode)
                .onErrorResumeNext(refreshTokenAndRetry(validatePinCodeObservable(pinCode)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> validatePinCodeObservable(final String pinCode) {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.checkPinCode(accessToken, pinCode);
    }
    
    public Observable<JSONObject> contactAction(final String contactId, final String pinCode, final ContactAction action) {
        return contactActionObservable(contactId, pinCode, action)
                .onErrorResumeNext(refreshTokenAndRetry(contactActionObservable(contactId, pinCode, action)))
                .map(new ResponseToJSONObject());
    }
    
    private Observable<Response> contactActionObservable(final String contactId, final String pinCode, final ContactAction action) {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        switch (action) {
            case RELEASE:
                return localBitcoins.releaseContactPinCode(accessToken, contactId, pinCode);
            case CANCEL:
                return localBitcoins.contactCancel(accessToken, contactId);
            case DISPUTE:
                return localBitcoins.contactDispute(accessToken, contactId);
            case PAID:
                return localBitcoins.markAsPaid(accessToken, contactId);
            case FUND:
                return localBitcoins.contactFund(accessToken, contactId);
        }

        return Observable.error(new Error("Unable to perform action on contact"));
    }
    
    public Observable<JSONObject> updateAdvertisement(final Advertisement advertisement) {
        return updateAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(updateAdvertisementObservable(advertisement)))
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject)
                    {
                        return Observable.just(jsonObject);
                    }
                });
    }

    private Observable<Response> updateAdvertisementObservable(final Advertisement advertisement) {
        final String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.updateAdvertisement(
                accessToken, advertisement.ad_id, advertisement.account_info, advertisement.bank_name, city, advertisement.country_code, advertisement.currency,
                String.valueOf(advertisement.lat), advertisement.location, String.valueOf(advertisement.lon), advertisement.max_amount, advertisement.min_amount,
                advertisement.message, advertisement.price_equation, String.valueOf(advertisement.trusted_required), String.valueOf(advertisement.sms_verification_required),
                String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.visible), String.valueOf(advertisement.require_identification),
                advertisement.require_feedback_score, advertisement.require_trade_volume, advertisement.first_time_limit_btc);
    }

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement) {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return createAdvertisementObservable(advertisement, retryLimit)
                    .map(new ResponseToJSONObject());
        }
        
        return createAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(createAdvertisementObservable(advertisement)))
                .map(new ResponseToJSONObject());
    }
    
    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement) {
        String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }

        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.createAdvertisement(accessToken, advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.trade_type.name(), advertisement.online_provider,
                String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount),
                String.valueOf(advertisement.trusted_required), String.valueOf(advertisement.require_identification), 
                advertisement.require_feedback_score, advertisement.require_trade_volume, 
                advertisement.first_time_limit_btc, advertisement.message, advertisement.currency);
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message)
    {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return postMessageObservable(contact_id, message, retryLimit)
                    .map(new ResponseToJSONObject());
        }
        
        return postMessageObservable(contact_id, message)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageObservable(contact_id, message)))
                .map(new ResponseToJSONObject());
    }
    
    public Observable<JSONObject> postMessageWithAttachment(final String contact_id, final String message, final File file)
    {
        return postMessageWithAttachmentObservable(contact_id, message, file)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageWithAttachmentObservable(contact_id, message, file)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> postMessageObservable(final String contact_id, final String message)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.contactMessagePost(accessToken, contact_id, message);
    }

    private Observable<Response> postMessageWithAttachmentObservable(final String contact_id, final String message, final File file)
    {
        TypedFile typedFile = new TypedFile("multipart/form-data", file);
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return localBitcoins.contactMessagePostWithAttachment(accessToken, contact_id, params, typedFile);
    }

    public Observable<User> getMyself(String accessToken)
    {
        return localBitcoins.getMyself(accessToken)
                .map(new ResponseToUser());
    }
    
    public Observable<Contact> getContact(final String contact_id)
    {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return getContactObservable(contact_id, retryLimit)
                    .map(new ResponseToContact())
                    .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                        @Override
                        public Observable<? extends Contact> call(final Contact contact)
                        {
                            return getContactMessagesObservable(contact.contact_id, retryLimit)
                                    .map(new ResponseToMessages())
                                    .map(new Func1<List<Message>, Contact>()
                                    {
                                        @Override
                                        public Contact call(List<Message> messages)
                                        {
                                            contact.messages = messages;
                                            return contact;
                                        }
                                    });
                        }
                    });
        }
        
        return getContactObservable(contact_id)
                .onErrorResumeNext(refreshTokenAndRetry(getContactObservable(contact_id)))
                .map(new ResponseToContact())
                .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                    @Override
                    public Observable<? extends Contact> call(final Contact contact)
                    {
                        return getContactMessagesObservable(contact.contact_id)
                                .onErrorResumeNext(refreshTokenAndRetry(getContactMessagesObservable(contact_id)))
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, Contact>()
                                {
                                    @Override
                                    public Contact call(List<Message> messages)
                                    {
                                        contact.messages = messages;
                                        return contact;
                                    }
                                });
                    }
                });
    }
    
    private Observable<Response> getContactObservable(final String contact_id)
    {
        
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getContact(accessToken, contact_id);
    }

    public Observable<List<Message>> getRecentMessages()
    {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return getRecentMessagesObservable(retryLimit)
                    .map(new ResponseToMessages());
        }
        
        return getRecentMessagesObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getRecentMessagesObservable()))
                .map(new ResponseToMessages());
    }

    private Observable<Response> getRecentMessagesObservable()
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.recentMessages(accessToken);
    }

    private Observable<Response> getContactMessagesObservable(final String contact_id)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.contactMessages(accessToken, contact_id);
    }

    public Observable<List<Notification>> getNotifications()
    {
        return getNotificationsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getNotificationsObservable()))
                .map(new ResponseToNotifications());
    }

    private Observable<Response> getNotificationsObservable()
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getNotifications(accessToken);
    }

    public Observable<JSONObject> markNotificationRead(final String notificationId)
    {
        return markNotificationReadObservable(notificationId)
                .onErrorResumeNext(refreshTokenAndRetry(markNotificationReadObservable(notificationId)))
                .map(new ResponseToJSONObject());
    }
    
    private Observable<Response> markNotificationReadObservable(final String notificationId)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.markNotificationRead(accessToken, notificationId);
    }
    
    public Observable<List<Contact>> getContacts(final DashboardType dashboardType)
    {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            switch (dashboardType){
                case RELEASED:
                case CLOSED:
                case CANCELED:
                    return getContactsObservable(dashboardType, retryLimit)
                            .map(new ResponseToContacts())
                            .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                            {
                                @Override
                                public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                {
                                    setContactsExpireTime();
                                    return Observable.just(contacts);
                                }
                            });
                default:
                    return getContactsObservable(retryLimit)
                            .map(new ResponseToContacts())
                            .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                            {
                                @Override
                                public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                {
                                    setContactsExpireTime();
                                    return Observable.just(contacts);
                                }
                            });
            }
        }
        
        switch (dashboardType){
            case RELEASED:
            case CLOSED:
            case CANCELED:
                return getContactsObservable(dashboardType)
                        .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable(dashboardType)))
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                        {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                            {
                                setContactsExpireTime();
                                return Observable.just(contacts);
                            }
                        });
            default:
                return getContactsObservable()
                        .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable()))
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                        {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                            {
                                setContactsExpireTime();
                                return Observable.just(contacts);
                            }
                        });
        }
    }

    private Observable<Response> getContactsObservable()
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getDashboard(accessToken);
    }

    private Observable<Response> getContactsObservable(final DashboardType dashboardType)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getDashboard(accessToken, dashboardType.name().toLowerCase());
    }

    public Observable<Advertisement> getAdvertisement(final String adId)
    {
        Timber.d("getAdvertisement");
        return getAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementObservable(adId)))
                .map(new ResponseToAd());
    }

    private Observable<Response> getAdvertisementObservable(final String adId)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getAdvertisement(accessToken, adId);
    }

    public Observable<List<Advertisement>> getAdvertisements(boolean force)
    {
        if(!needToRefreshAdvertisements() && !force) {
            return Observable.empty();
        }

        return getAdvertisementsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementsObservable()))
                .doOnNext(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        setAdvertisementsExpireTime();
                    }
                })
                .map(new ResponseToAds());
    }

    private Observable<Response> getAdvertisementsObservable()
    {
        Timber.d("getAdvertisementsObservable");
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getAds(accessToken);
    }
    
    public Observable<JSONObject>updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible)
    {
        advertisement.visible = visible;
        
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return updateAdvertisementObservable(advertisement, retryLimit)
                    .map(new ResponseToJSONObject())
                    .flatMap(new Func1<JSONObject, Observable<JSONObject>>()
                    {
                        @Override
                        public Observable<JSONObject> call(JSONObject jsonObject)
                        {
                            return Observable.just(jsonObject);
                        }
                    });
        }
        
        return updateAdvertisementObservable(advertisement)
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject)
                    {
                        return Observable.just(jsonObject);
                    }
                });
    }
    
    public Observable<Boolean> deleteAdvertisement(final String adId)
    {
        if(AuthUtils.hasCredentialsHmac(sharedPreferences)) {
            return deleteAdvertisementObservable(adId, retryLimit)
                    .map(new ResponseToJSONObject())
                    .flatMap(new Func1<JSONObject, Observable<Boolean>>()
                    {
                        @Override
                        public Observable<Boolean> call(JSONObject jsonObject)
                        {
                            if (Parser.containsError(jsonObject)) {
                                throw new Error("Error deleting advertisement");
                            }
                            return Observable.just(true);
                        }
                    });
        }
        
        return deleteAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(deleteAdvertisementObservable(adId)))
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>()
                {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject)
                    {
                        if (Parser.containsError(jsonObject)) {
                            throw new Error("Error deleting advertisement");
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Response> deleteAdvertisementObservable(final String adId)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.deleteAdvertisement(accessToken, adId);
    }

    public Observable<Wallet> getWallet(boolean force)
    {
        return getWalletObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletObservable()))
                .map(new ResponseToWallet())
                .flatMap(new Func1<Wallet, Observable<Wallet>>() {
                    @Override
                    public Observable<Wallet> call(final Wallet wallet) {
                        setWalletExpireTime();
                        return generateBitmap(wallet.address)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .map(new Func1<Bitmap, Wallet>() {
                                    @Override
                                    public Wallet call(Bitmap bitmap) {
                                        wallet.qrImage = bitmap;
                                        return wallet;
                                    }
                                }).onErrorReturn(new Func1<Throwable, Wallet>() {
                                    @Override
                                    public Wallet call(Throwable throwable) {
                                        return wallet;
                                    }
                                });
                    }
                });
    }

    private Observable<Response> getWalletObservable()
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getWallet(accessToken);
    }
    
    ////  HMAC ////
    
    private Observable<Response> createContactObservable(final String adId, final TradeType tradeType, final String countryCode,
                                                         final String onlineProvider, final String amount, final String name,
                                                         final String phone, final String email, final String iban, final String bic,
                                                         final String reference, final String message, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = POST_CONTACT_CREATE + adId + "/";
        StringBuilder params = new StringBuilder();
        String signature;
        
        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    
                    params.append("amount=");
                    params.append(Uri.encode(amount, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-receiver_name=");
                    params.append(Uri.encode(name, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-iban=");
                    params.append(Uri.encode(iban, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-swift_bic=");
                    params.append(Uri.encode(bic, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-reference=");
                    params.append(Uri.encode(reference, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-message=");
                    params.append(Uri.encode(message, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
                    
                    return localBitcoins.createContactNational(key, nonce, signature, adId, amount, name, iban, bic, reference, message)
                            .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>()
                            {
                                @Override
                                public Observable<? extends Response> call(final Throwable throwable)
                                {
                                    if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0) {
                                        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                                                name, phone, email, iban, bic, reference, message, retry - 1);
                                    }
                                    return Observable.error(throwable); // bubble up the exception
                                }
                            });
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                    
                    params.append("amount=");
                    params.append(Uri.encode(amount, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-phone_number=");
                    params.append(Uri.encode(phone, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
                    
                    return localBitcoins.createContactQiwi(key, nonce, signature, adId, amount, phone)
                            .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>()
                            {
                                @Override
                                public Observable<? extends Response> call(final Throwable throwable)
                                {
                                    if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0) {
                                        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                                                name, phone, email, iban, bic, reference, message, retry - 1);
                                    }
                                    return Observable.error(throwable); // bubble up the exception
                                }
                            });
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                    
                    params.append("amount=");
                    params.append(Uri.encode(amount, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-receiver_email=");
                    params.append(Uri.encode(email, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
                    
                    return localBitcoins.createContactPayPal(key, nonce, signature, adId, amount, email)
                            .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>()
                            {
                                @Override
                                public Observable<? extends Response> call(final Throwable throwable)
                                {
                                    if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0) {
                                        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                                                name, phone, email, iban, bic, reference, message, retry - 1);
                                    }
                                    return Observable.error(throwable); // bubble up the exception
                                }
                            });
                case TradeUtils.SEPA:

                    params.append("amount=");
                    params.append(Uri.encode(amount, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-receiver_name=");
                    params.append(Uri.encode(name, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-iban=");
                    params.append(Uri.encode(iban, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-swift_bic=");
                    params.append(Uri.encode(bic, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    params.append("&details-reference=");
                    params.append(Uri.encode(reference, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
                    signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
                    
                    return localBitcoins.createContactSepa(key, nonce, signature, adId, amount, name, iban, bic, reference)
                            .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>()
                            {
                                @Override
                                public Observable<? extends Response> call(final Throwable throwable)
                                {
                                    if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0) {
                                        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                                                name, phone, email, iban, bic, reference, message, retry - 1);
                                    }
                                    return Observable.error(throwable); // bubble up the exception
                                }
                            });
            }
        }
        
        params.append("amount=");
        params.append(Uri.encode(amount, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        params.append("&message=");
        params.append(Uri.encode(message, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
        
        return localBitcoins.createContact(key, nonce, signature, adId, amount, message)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                                    name, phone, email, iban, bic, reference, message, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> sendPinCodeMoneyObservable(final String pinCode, final String address, final String amount, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = POST_WALLET_SEND_PIN;

        StringBuilder params = new StringBuilder();
        params.append("pincode=");
        params.append(Uri.encode(pinCode, NetworkUtils.DEFAULT_ENCODING));
        params.append("address=");
        params.append(Uri.encode(address, NetworkUtils.DEFAULT_ENCODING));
        params.append("amount=");
        params.append(Uri.encode(amount, NetworkUtils.DEFAULT_ENCODING));
        String signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);

        return localBitcoins.walletSendPin(key, nonce, signature, pinCode, address, amount)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return sendPinCodeMoneyObservable(pinCode, address, amount, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> getWalletBalanceObservable(final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_WALLET_BALANCE;
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.getWalletBalance(key, nonce, signature)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getWalletBalanceObservable(retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> validatePinCodeObservable(final String pinCode, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = CHECK_PINCODE;

        StringBuilder params = new StringBuilder();
        params.append("pincode=");
        params.append(Uri.encode(pinCode, NetworkUtils.DEFAULT_ENCODING));
        String signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);

        return localBitcoins.checkPinCode(key, nonce, signature, pinCode)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return validatePinCodeObservable(pinCode, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> contactActionObservable(final String contactId, final String pinCode, final ContactAction action, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        switch (action) {
            case RELEASE:
                String releaseUrl = POST_CONTACT_RELEASE + contactId + "/";
                StringBuilder params = new StringBuilder();
                params.append("pincode=");
                params.append(Uri.encode(pinCode, NetworkUtils.DEFAULT_ENCODING));
                String releaseSignature = NetworkUtils.createSignature(releaseUrl, params.toString(), nonce, key, secret);
                return localBitcoins.releaseContactPinCode(key, nonce, releaseSignature, contactId, pinCode)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(final Throwable throwable)
                            {
                                if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                                    return contactActionObservable(contactId, pinCode, action, retry - 1);
                                }
                                return Observable.error(throwable); // bubble up the exception
                            }
                        });

            case CANCEL:
                final String cancelUrl = POST_CONTACT_CANCEL + contactId + "/";
                final String cancelSignature = NetworkUtils.createSignature(cancelUrl, nonce, key, secret);
                return localBitcoins.contactCancel(key, nonce, cancelSignature, contactId)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(final Throwable throwable)
                            {
                                if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                                    return contactActionObservable(contactId, pinCode, action, retry - 1);
                                }
                                return Observable.error(throwable); // bubble up the exception
                            }
                        });
            case DISPUTE:
                final String disputeUrl = POST_CONTACT_DISPUTE + contactId + "/";
                final String disputeSignature = NetworkUtils.createSignature(disputeUrl, nonce, key, secret);
                return localBitcoins.contactDispute(key, nonce, disputeSignature, contactId)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(final Throwable throwable)
                            {
                                if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                                    return contactActionObservable(contactId, pinCode, action, retry - 1);
                                }
                                return Observable.error(throwable); // bubble up the exception
                            }
                        });
            case PAID:
                final String paidUrl = POST_CONTACT_PAID + contactId + "/";
                final String paidSignature = NetworkUtils.createSignature(paidUrl, nonce, key, secret);
                return localBitcoins.markAsPaid(key, nonce, paidSignature, contactId)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(final Throwable throwable)
                            {
                                if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                                    return contactActionObservable(contactId, pinCode, action, retry - 1);
                                }
                                return Observable.error(throwable); // bubble up the exception
                            }
                        });
            case FUND:
                final String fundUrl = POST_CONTACT_FUND + contactId + "/";
                final String fundSignature = NetworkUtils.createSignature(fundUrl, nonce, key, secret);
                return localBitcoins.contactFund(key, nonce, fundSignature, contactId)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(final Throwable throwable)
                            {
                                if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                                    return contactActionObservable(contactId, pinCode, action, retry - 1);
                                }
                                return Observable.error(throwable); // bubble up the exception
                            }
                        });
        }

        return Observable.error(new Error("Unable to perform action on contact"));
    }

    private Observable<Response> updateAdvertisementObservable(final Advertisement advertisement, final int retry)
    {
        final String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }

        StringBuilder params = new StringBuilder();
        params.append("min_amount=");
        params.append(Uri.encode(advertisement.min_amount, NetworkUtils.DEFAULT_ENCODING));

        params.append("&max_amount=");
        params.append(Uri.encode(advertisement.max_amount, NetworkUtils.DEFAULT_ENCODING));
        
        if(!Strings.isBlank(advertisement.account_info)) {
            params.append("account_info=");
            params.append(Uri.encode(advertisement.account_info, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.account_info = null;
        }
        
        if(!Strings.isBlank(advertisement.bank_name)) {
            params.append("&bank_name=");
            params.append(Uri.encode(advertisement.bank_name, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.bank_name = null;
        }
        
        if(!Strings.isBlank(advertisement.city)) {
            params.append("&city=");
            params.append(Uri.encode(city, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        }else {
            advertisement.city = null;
        }

        if(!Strings.isBlank(advertisement.location)) {
            params.append("&location_string=");
            params.append(Uri.encode(advertisement.location, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.location = null;
        }
        
        params.append("&countrycode=");
        params.append(Uri.encode(advertisement.country_code, NetworkUtils.DEFAULT_ENCODING));
        params.append("&currency=");
        params.append(Uri.encode(advertisement.currency, NetworkUtils.DEFAULT_ENCODING));
        params.append("&lat=");
        params.append(Uri.encode(String.valueOf(advertisement.lat), NetworkUtils.DEFAULT_ENCODING));
        params.append("&lon=");
        params.append(Uri.encode(String.valueOf(advertisement.lon), NetworkUtils.DEFAULT_ENCODING));
        
        if(!Strings.isBlank(advertisement.message)) {
            params.append("&msg=");
            params.append(Uri.encode(advertisement.message, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.message = null;
        }
        
        params.append("&price_equation=");
        params.append(Uri.encode(advertisement.price_equation, NetworkUtils.DEFAULT_ENCODING));
        params.append("&require_trusted_by_advertiser=");
        params.append(Uri.encode(String.valueOf(advertisement.trusted_required), NetworkUtils.DEFAULT_ENCODING));
        params.append("&sms_verification_required=");
        params.append(Uri.encode(String.valueOf(advertisement.sms_verification_required), NetworkUtils.DEFAULT_ENCODING));
        params.append("&track_max_amount=");
        params.append(Uri.encode(String.valueOf(advertisement.track_max_amount), NetworkUtils.DEFAULT_ENCODING));
        params.append("&visible=");
        params.append(Uri.encode(String.valueOf(advertisement.visible), NetworkUtils.DEFAULT_ENCODING));
        
        Timber.d("Params: " + params.toString());
        
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = UPDATE_AD + advertisement.ad_id + "/";
        String signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
        return localBitcoins.updateAdvertisement(
                key, nonce, signature, 
                advertisement.ad_id,
                advertisement.min_amount,
                advertisement.max_amount,
                advertisement.account_info, 
                advertisement.bank_name, 
                city,
                advertisement.location,
                advertisement.country_code, 
                advertisement.currency,
                String.valueOf(advertisement.lat),
                String.valueOf(advertisement.lon),
                advertisement.message, advertisement.price_equation, 
                String.valueOf(advertisement.trusted_required), 
                String.valueOf(advertisement.sms_verification_required),
                String.valueOf(advertisement.track_max_amount), 
                String.valueOf(advertisement.visible))
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return updateAdvertisementObservable(advertisement, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement, final int retry)
    {
        final String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }

        StringBuilder params = new StringBuilder();
        params.append("min_amount=");
        params.append(Uri.encode(advertisement.min_amount, NetworkUtils.DEFAULT_ENCODING));
        params.append("&max_amount=");
        params.append(Uri.encode(advertisement.max_amount, NetworkUtils.DEFAULT_ENCODING));
        params.append("&price_equation=");
        params.append(Uri.encode(advertisement.price_equation, NetworkUtils.DEFAULT_ENCODING));
        params.append("&trade_type=");
        params.append(Uri.encode(advertisement.trade_type.name(), NetworkUtils.DEFAULT_ENCODING));
        params.append("&online_provider=");
        params.append(Uri.encode(advertisement.online_provider, NetworkUtils.DEFAULT_ENCODING));
        params.append("&lat=");
        params.append(Uri.encode(String.valueOf(advertisement.lat), NetworkUtils.DEFAULT_ENCODING));
        params.append("&lon=");
        params.append(Uri.encode(String.valueOf(advertisement.lon), NetworkUtils.DEFAULT_ENCODING));
        
        if(!Strings.isBlank(city)) {
            params.append("&city=");
            params.append(Uri.encode(city, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.city = null;
        }
        
        if(!Strings.isBlank(advertisement.location)) {
            params.append("&location_string=");
            params.append(Uri.encode(advertisement.location, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.location = null;
        }
        
        params.append("&countrycode=");
        params.append(Uri.encode(advertisement.country_code, NetworkUtils.DEFAULT_ENCODING));
        
        if(!Strings.isBlank(advertisement.account_info)) {
            params.append("&account_info=");
            params.append(Uri.encode(advertisement.account_info, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        }  else {
            advertisement.account_info = null;
        }
        
        if(!Strings.isBlank(advertisement.bank_name)) {
            params.append("&bank_name=");
            params.append(Uri.encode(advertisement.bank_name, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.bank_name = null;
        }
        
        params.append("&require_trusted_by_advertiser=");
        params.append(Uri.encode(String.valueOf(advertisement.trusted_required), NetworkUtils.DEFAULT_ENCODING));
        params.append("&sms_verification_required=");
        params.append(Uri.encode(String.valueOf(advertisement.sms_verification_required), NetworkUtils.DEFAULT_ENCODING));
        params.append("&track_max_amount=");
        params.append(Uri.encode(String.valueOf(advertisement.track_max_amount), NetworkUtils.DEFAULT_ENCODING));
        
        if(!Strings.isBlank(advertisement.message)) {
            params.append("&msg=");
            params.append(Uri.encode(advertisement.message, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        } else {
            advertisement.message = null;
        }
        
        params.append("&currency=");
        params.append(Uri.encode(advertisement.currency, NetworkUtils.DEFAULT_ENCODING));
        
        Timber.d("Params: " + params.toString());
        
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = POST_AD_CREATE;
        String signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);
        
        return localBitcoins.createAdvertisement(key, nonce, signature, 
                advertisement.min_amount,
                advertisement.max_amount, 
                advertisement.price_equation, 
                advertisement.trade_type.name(), 
                advertisement.online_provider,
                String.valueOf(advertisement.lat), 
                String.valueOf(advertisement.lon),
                city,
                advertisement.location, 
                advertisement.country_code,
                advertisement.account_info, 
                advertisement.bank_name,
                String.valueOf(advertisement.trusted_required),
                String.valueOf(advertisement.sms_verification_required), 
                String.valueOf(advertisement.track_max_amount),
                advertisement.message, 
                advertisement.currency)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return createAdvertisementObservable(advertisement, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }
    
    private Observable<Response> postMessageObservable(final String contact_id, final String message, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = POST_CONTACT_MESSAGE + contact_id + "/";

        StringBuilder params = new StringBuilder();
        params.append("msg=");
        params.append(Uri.encode(message, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        String signature = NetworkUtils.createSignature(url, params.toString(), nonce, key, secret);

        return localBitcoins.contactMessagePost(key, nonce, signature, contact_id, message)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return postMessageObservable(contact_id, message, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<User> getMyself(String key, String nonce, String signature)
    {
        return localBitcoins.getMyself(key, nonce, signature)
                .map(new ResponseToUser());
    }

    private Observable<Response> getContactObservable(final String contact_id, final int retry)
    {
        Timber.d("getContactObservable: " + contact_id);

        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_CONTACT + contact_id + "/";
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.getContact(key, nonce, signature, contact_id)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getContactObservable(contact_id, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }
    
    private Observable<Response> getRecentMessagesObservable(final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_RECENT_MESSAGES;
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.recentMessages(key, nonce, signature)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getRecentMessagesObservable(retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> getContactMessagesObservable(final String contact_id, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_CONTACT_MESSAGES + contact_id + "/";
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.contactMessages(key, nonce, signature, contact_id)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getContactMessagesObservable(contact_id, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }
    
    private Observable<Response> getContactsObservable(final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_DASHBOARD;
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);
        return localBitcoins.getDashboard(key, nonce, signature)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getContactsObservable(retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });

    }

    private Observable<Response> getContactsObservable(final DashboardType dashboardType, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_DASHBOARD + dashboardType.name().toLowerCase() + "/";
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.getDashboard(key, nonce, signature, dashboardType.name().toLowerCase())
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getContactsObservable(dashboardType, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Response> getAdvertisementObservable(final String adId, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_AD + adId + "/";
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.getAdvertisement(key, nonce, signature, adId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return getAdvertisementObservable(adId, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }
    
    private Observable<Response> deleteAdvertisementObservable(final String adId, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = DELETE_AD + adId + "/";
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.deleteAdvertisement(key, nonce, signature, adId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if ((DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable)) && retry > 0)  {
                            return deleteAdvertisementObservable(adId, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }
    
    private Observable<Wallet> getWalletBitmap(final Wallet wallet) {
        return generateBitmap(wallet.address)
                .map(new Func1<Bitmap, Wallet>()
                {
                    @Override
                    public Wallet call(Bitmap bitmap)
                    {
                        wallet.qrImage = bitmap;
                        return wallet;
                    }
                });
    }

    private Observable<Bitmap> generateBitmap(final String address) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber)
            {
                try {
                    subscriber.onNext(WalletUtils.encodeAsBitmap(address, baseApplication.getApplicationContext()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<Method>> getMethods() {
        if(!needToRefreshMethods()) {
            return Observable.empty();
        }
        
        return localBitcoins.getOnlineProviders()
                .doOnNext(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        setMethodsExpireTime();
                    }
                })
                .map(new ResponseToMethod());
    }
   
    private void resetContactsExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_CONTACTS_EXPIRE_TIME);
            preference.delete();
        }
    }

    private void setContactsExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_CONTACTS_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_CONTACTS_DATA; // 1 hours
            preference.set(expire);
        }
    }
   
    private boolean needToRefreshAdvertisements() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private void resetExchangeExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_EXCHANGE_EXPIRE_TIME);
            preference.delete();
        }
    }

    private boolean needToRefreshMethods() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_METHODS_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private void resetMethodsExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_METHODS_EXPIRE_TIME);
            preference.delete();
        }
    }

    private void setMethodsExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_METHODS_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_METHODS_DATA; // 1 hours
            preference.set(expire);
        }
    }

    private void resetAdvertisementsExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME);
            preference.delete();
        }
    }

    private void setAdvertisementsExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_ADVERTISEMENT_DATA; // 1 hours
            preference.set(expire);
        }
    }
    
    private void setWalletExpireTime() {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_WALLET_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_WALLET_DATA; // 1 hours
            preference.set(expire);
        }
    }
}