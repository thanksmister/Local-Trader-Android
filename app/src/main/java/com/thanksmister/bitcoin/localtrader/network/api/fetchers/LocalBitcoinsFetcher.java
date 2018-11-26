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

package com.thanksmister.bitcoin.localtrader.network.api.fetchers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisements;
import com.thanksmister.bitcoin.localtrader.network.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.network.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.network.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.network.api.model.Message;
import com.thanksmister.bitcoin.localtrader.network.api.model.Messages;
import com.thanksmister.bitcoin.localtrader.network.api.model.Method;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notifications;
import com.thanksmister.bitcoin.localtrader.network.api.model.Place;
import com.thanksmister.bitcoin.localtrader.network.api.model.Places;
import com.thanksmister.bitcoin.localtrader.network.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.network.api.model.User;
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes;
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException;
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler;
import com.thanksmister.bitcoin.localtrader.persistence.Preferences;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import timber.log.Timber;

public class LocalBitcoinsFetcher {

    private final LocalBitcoinsApi networkApi;
    private final Preferences preferences;
    private final Context context;

    public LocalBitcoinsFetcher(Context context,  @NonNull LocalBitcoinsApi networkApi, Preferences preferences) {
        this.context = context;
        this.networkApi = networkApi;
        this.preferences = preferences;
    }

    public Observable<Authorization> getAuthorization(String code) {
        return networkApi.getAuthorization("authorization_code", code, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET);
    }

    private Observable<String> refreshTokens(String refreshToken) {
        Timber.d("refreshTokens");
        return networkApi.refreshToken("refresh_token", refreshToken, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
                .flatMap(new Function<Authorization, ObservableSource<? extends String>>() {
                    @Override
                    public ObservableSource<? extends String> apply(Authorization authorization) {
                        if(authorization != null) {
                            Timber.d("authorization.getAccessToken() " + authorization.getAccessToken());
                            Timber.d("authorization.getRefreshToken() " + authorization.getRefreshToken());
                            preferences.setAccessToken(authorization.getAccessToken());
                            preferences.setRefreshToken(authorization.getRefreshToken());
                            return Observable.just(authorization.getAccessToken());
                        } else {
                            return null;
                        }
                    }
                });
    }

    private <T> Function<Throwable, ? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed) {

        Timber.d("refreshTokenAndRetry");

        return new Function<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> apply(Throwable throwable) throws Exception {
                RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                Timber.d("refreshTokenAndRetry error: " + throwable.getMessage());
                final NetworkException networkException = errorHandler.create(throwable);
                if (RetrofitErrorHandler.Companion.isHttp403Error(networkException.getCode())) {
                    Timber.e("Retrying error code: " + networkException.getCode());
                    return refreshTokens(preferences.getRefreshToken())
                            .subscribeOn(Schedulers.computation())
                            .flatMap(new Function<String, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(String s) throws Exception {
                                    return toBeResumed;
                                }
                            });
                } else if (RetrofitErrorHandler.Companion.isHttp400Error(networkException.getCode())) {
                    Timber.e("Retrying error code: " + networkException.getCode());
                    return refreshTokens(preferences.getRefreshToken())
                            .subscribeOn(Schedulers.computation())
                            .flatMap(new Function<String, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(String s) throws Exception {
                                    return toBeResumed;
                                }
                            });
                } else if (ExceptionCodes.INSTANCE.getCODE_THREE() == networkException.getCode()) {
                    Timber.e("Retrying error code: " + networkException.getCode());
                    return refreshTokens(preferences.getRefreshToken())
                            .subscribeOn(Schedulers.computation())
                            .flatMap(new Function<String, ObservableSource<? extends T>>() {
                                @Override
                                public ObservableSource<? extends T> apply(String s) throws Exception {
                                    return toBeResumed;
                                }
                            });
                }
                return Observable.error(networkException); // bubble up the exception;
            }
        };
    }

    public Observable<User> getMyself() {
        return getMyselfObservable()
                        .onErrorResumeNext(refreshTokenAndRetry(getMyselfObservable()));
    }

    private Observable<User> getMyselfObservable() {
        String accessToken = preferences.getAccessToken();
        return networkApi.getMyself(accessToken);
    }

    public Observable<List<Currency>> getCurrencies() {
        return networkApi.getCurrencies()
                .flatMap(new Function<TreeMap<String, Object>, ObservableSource<List<Currency>>>() {
                    @Override
                    public ObservableSource<List<Currency>> apply(TreeMap<String, Object> stringTreeMap) throws Exception {
                        List<Currency> currencies = Parser.parseCurrencies(stringTreeMap);
                        if(currencies == null) {
                            currencies = new ArrayList<>();
                        }
                        return Observable.just(currencies);
                    }
                })
                .onErrorReturn(new Function<Throwable, List<Currency>>() {
                    @Override
                    public List<Currency> apply(Throwable throwable) {
                        RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                        final NetworkException networkException = errorHandler.create(throwable);
                        Timber.e("Currency error code: " + networkException.getMessage());
                        throw new Error(networkException);
                    }
                });
    }

    public Observable<List<Advertisement>> getAdvertisements() {
        return getAdvertisementsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementsObservable()))
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getListItems());
                    }
                });
    }

    private Observable<Advertisements> getAdvertisementsObservable() {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getAdvertisements(accessToken);
    }

    public Observable<JsonElement> updateAdvertisement(Advertisement advertisement) {
        return updateAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(updateAdvertisementObservable(advertisement)));
    }

    private Observable<JsonElement> updateAdvertisementObservable(Advertisement advertisement) {
        final String accessToken = preferences.getAccessToken();
        final String city;
        if (TextUtils.isEmpty(advertisement.getCity())) {
            city = advertisement.getLocation();
        } else {
            city = advertisement.getCity();
        }
        Timber.d("opening hours: " + advertisement.getOpeningHours());
        return networkApi.updateAdvertisement(
                accessToken, String.valueOf(advertisement.getAdId()), advertisement.getAccountInfo(), advertisement.getBankName(), city, advertisement.getCountryCode(), advertisement.getCurrency(),
                String.valueOf(advertisement.getLat()), advertisement.getLocation(), String.valueOf(advertisement.getLon()), advertisement.getMaxAmount(), advertisement.getMinAmount(),
                advertisement.getMessage(), advertisement.getPriceEquation(), String.valueOf(advertisement.getTrustedRequired()), String.valueOf(advertisement.getSmsVerificationRequired()),
                String.valueOf(advertisement.getTrackMaxAmount()), String.valueOf(advertisement.getVisible()), String.valueOf(advertisement.getRequireIdentification()),
                advertisement.getRequireFeedbackScore(), advertisement.getRequireTradeVolume(), advertisement.getFirstTimeLimitBtc(),
                advertisement.getPhoneNumber());
    }

    public Observable<JsonElement> deleteAdvertisement(final int adId) {
        return deleteAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(deleteAdvertisementObservable(adId)));
    }

    private Observable<JsonElement> deleteAdvertisementObservable(final int adId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.deleteAdvertisement(accessToken, adId);
    }

    public Observable<List<Method>> getMethods() {
        return networkApi.getOnlineProviders()
                .flatMap(new Function<TreeMap<String, Object>, ObservableSource<List<Method>>>() {
                    @Override
                    public ObservableSource<List<Method>> apply(TreeMap<String, Object> stringTreeMap) throws Exception {
                        List<Method> methods = Parser.parseMethods(stringTreeMap);
                        if(methods == null) methods = new ArrayList<>();
                        return Observable.just(methods);
                    }
                })
                .onErrorReturn(new Function<Throwable, List<Method>>() {
                    @Override
                    public List<Method> apply(Throwable throwable) throws Exception {
                        RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                        final NetworkException networkException = errorHandler.create(throwable);
                        Timber.e("Method error code: " + networkException.getMessage());
                        throw new Error(networkException);
                    }
                });
    }

    public Observable<List<Advertisement>> getAdvertisement(final int adId) {
        return getAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementObservable(adId)));
    }

    private Observable<List<Advertisement>> getAdvertisementObservable(final int adId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getAdvertisement(accessToken, adId)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getListItems());
                    }
                });
    }

    public Observable<Contact> getContact(final int contactId) {
        return getContactObservable(contactId)
                .onErrorResumeNext(refreshTokenAndRetry(getContactObservable(contactId)));
    }

    private Observable<Contact> getContactObservable(final int contactId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getContactInfo(accessToken, contactId);
    }

    public Observable<Wallet> getWallet() {
        return getWalletObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletObservable()));
    }

    private Observable<Wallet> getWalletObservable() {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getWallet(accessToken);
    }

    public Observable<Wallet> getWalletBalance() {
        Timber.d("getWalletBalance");
        return getWalletBalanceObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletBalanceObservable()));
    }

    private Observable<Wallet> getWalletBalanceObservable() {
        Timber.d("getWalletBalanceObservable token " + preferences.getAccessToken());
        final String accessToken = preferences.getAccessToken();
        return networkApi.getWalletBalance(accessToken);
    }

    public Observable<List<Contact>> getContacts() {
        return getContactsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable()));
    }

    private Observable<List<Contact>> getContactsObservable() {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getDashboard(accessToken)
                .flatMap(new Function<Dashboard, ObservableSource<List<Contact>>>() {
                    @Override
                    public ObservableSource<List<Contact>> apply(Dashboard dashboard) throws Exception {
                        return Observable.just(dashboard.getItems());
                    }
                });
    }

    public Observable<List<Contact>> getContactsByType(final DashboardType dashboardType) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getDashboard(accessToken, dashboardType.name().toLowerCase())
                .onErrorResumeNext(refreshTokenAndRetry(networkApi.getDashboard(accessToken, dashboardType.name().toLowerCase())))
                .flatMap(new Function<Dashboard, ObservableSource<List<Contact>>>() {
                    @Override
                    public ObservableSource<List<Contact>> apply(Dashboard dashboard) throws Exception {
                        return Observable.just(dashboard.getItems());
                    }
                });
    }

    public Observable<List<Notification>> getNotifications() {
        return getNotificationsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getNotificationsObservable()));
    }

    private Observable<List<Notification>> getNotificationsObservable() {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getNotifications(accessToken)
                .flatMap(new Function<Notifications, ObservableSource<List<Notification>>>() {
                    @Override
                    public ObservableSource<List<Notification>> apply(Notifications notifications) throws Exception {
                        return Observable.just(notifications.getItems());
                    }
                });
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount) {
        return sendPinCodeMoneyObservable(pinCode, address, amount)
                .onErrorResumeNext(refreshTokenAndRetry(sendPinCodeMoneyObservable(pinCode, address, amount)));
    }

    private Observable<Boolean> sendPinCodeMoneyObservable(final String pinCode, final String address, final String amount) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.walletSendPin(accessToken, pinCode, address, amount)
                .flatMap(new Function<JsonElement, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(JsonElement jsonElement) throws Exception {
                        if (Parser.containsError(jsonElement.toString())) {
                            NetworkException exception = Parser.parseError(jsonElement.toString());
                            throw new Error(exception);
                        }
                        return Observable.just(true);
                    }
                });
    }

    public Observable<JsonElement> markNotificationRead(final String notificationId) {
        return markNotificationReadObservable(String.valueOf(notificationId))
                .onErrorResumeNext(refreshTokenAndRetry(markNotificationReadObservable(String.valueOf(notificationId))));
    }

    private Observable<JsonElement> markNotificationReadObservable(final String notificationId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.markNotificationRead(accessToken, String.valueOf(notificationId));
    }

    public Observable<List<Message>> getContactMessages(final int contactId) {
        return getContactMessagesReadObservable(contactId)
                .onErrorResumeNext(refreshTokenAndRetry(getContactMessagesReadObservable(contactId)));
    }

    private Observable<List<Message>> getContactMessagesReadObservable(final int contactId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.contactMessages(accessToken, contactId)
                .flatMap(new Function<Messages, ObservableSource<List<Message>>>() {
                    @Override
                    public ObservableSource<List<Message>> apply(Messages messages) throws Exception {
                        return Observable.just(messages.getItems());
                    }
                });
    }

    public Observable<List<Place>> getPlaces(final double lat, final double lon) {
        return networkApi.getPlaces(lat, lon)
                .flatMap(new Function<Places, ObservableSource<List<Place>>>() {
                    @Override
                    public ObservableSource<List<Place>> apply(Places places) throws Exception {
                        return Observable.just(places.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchAdsByPlace(String type, String num, String location) {
        return networkApi.searchAdsByPlace(type, num, location)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAds(String type, String num, String location) {
        return networkApi.searchOnlineAds(type, num, location)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAds(String type, String num, String location, String paymentMethod) {
        return networkApi.searchOnlineAds(type, num, location, paymentMethod)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAdsCurrency(String type, String currency, String paymentMethod) {
        return networkApi.searchOnlineAdsCurrency(type, currency, paymentMethod)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAdsCurrency(String type, String currency) {
        return networkApi.searchOnlineAdsCurrency(type, currency)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAdsPayment(String type, String paymentMethod) {
        return networkApi.searchOnlineAdsPayment(type, paymentMethod)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAdsCurrencyPayment(String type, String currency, String paymentMethod) {
        return networkApi.searchOnlineAdsCurrencyPayment(type, currency, paymentMethod)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<List<Advertisement>> searchOnlineAdsAll(String type) {
        return networkApi.searchOnlineAdsAll(type)
                .flatMap(new Function<Advertisements, ObservableSource<List<Advertisement>>>() {
                    @Override
                    public ObservableSource<List<Advertisement>> apply(Advertisements advertisements) throws Exception {
                        return Observable.just(advertisements.getItems());
                    }
                });
    }

    public Observable<ContactRequest> createContact(final String adId, final TradeType tradeType, final String countryCode,
                                                    final String onlineProvider, final String amount, final String name,
                                                    final String phone, final String email, final String iban, final String bic,
                                                    final String reference, final String message, final String sortCode,
                                                    final String billerCode, final String accountNumber, final String bsb,
                                                    final String ethereumAddress) {

        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount, name, phone, email,
                iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
                .onErrorResumeNext(refreshTokenAndRetry((createContactObservable(adId, tradeType, countryCode, onlineProvider, amount, name, phone, email,
                        iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress))));
    }

    private Observable<ContactRequest> createContactObservable( final String adId, final TradeType tradeType, final String countryCode,
                                                         final String onlineProvider, final String amount, final String name,
                                                         final String phone, final String email, final String iban, final String bic,
                                                         final String reference, final String message, final String sortCode,
                                                         final String billerCode, final String accountNumber, final String bsb,
                                                         final String ethereumAddress) {

        final String accessToken = preferences.getAccessToken();
        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    switch (countryCode) {
                        case "UK":
                            return networkApi.createContactNationalUK(accessToken, adId, amount, name, sortCode, reference, accountNumber, message);
                        case "AU":
                            return networkApi.createContactNationalAU(accessToken, adId, amount, name, bsb, reference, accountNumber, message);
                        case "FI":
                            return networkApi.createContactNationalFI(accessToken, adId, amount, name, iban, bic, reference, message);
                        default:
                            return networkApi.createContactNational(accessToken, adId, amount, message);
                    }
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.LYDIA:
                case TradeUtils.SWISH:
                    return networkApi.createContactPhone(accessToken, adId, amount, phone, message);
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    return networkApi.createContactEmail(accessToken, adId, amount, email, message);
                case TradeUtils.SEPA:
                    return networkApi.createContactSepa(accessToken, adId, amount, name, iban, bic, reference, message);
                case TradeUtils.ALTCOIN_ETH:
                    return networkApi.createContactEthereumAddress(accessToken, adId, amount, ethereumAddress, message);
                case TradeUtils.BPAY:
                    return networkApi.createContactBPay(accessToken, adId, amount, billerCode, reference, message);
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    return networkApi.createContactPhone(accessToken, adId, amount, phone, message);

            }
        }

        return networkApi.createContact(accessToken, adId, amount, message);
    }

    public Observable<JsonElement> contactAction(final int contactId, final String pinCode, final ContactAction action) {
        return contactActionObservable(contactId, pinCode, action)
                .onErrorResumeNext(refreshTokenAndRetry(contactActionObservable(contactId, pinCode, action)));
    }

    private Observable<JsonElement> contactActionObservable(final int contactId, final String pinCode, final ContactAction action) {
        final String accessToken = preferences.getAccessToken();
        switch (action) {
            case RELEASE:
                return networkApi.releaseContactPinCode(accessToken, contactId, pinCode);
            case CANCEL:
                return networkApi.contactCancel(accessToken, contactId);
            case DISPUTE:
                return networkApi.contactDispute(accessToken, contactId);
            case PAID:
                return networkApi.markAsPaid(accessToken, contactId);
            case FUND:
                return networkApi.contactFund(accessToken, contactId);
        }
        return Observable.error(new NetworkException("Unable to perform action on contact", ExceptionCodes.INSTANCE.getNO_ERROR_CODE()));
    }

    public Observable<JsonElement> validatePinCode(final String pinCode) {
        return validatePinCodeObservable(pinCode)
                .onErrorResumeNext(refreshTokenAndRetry(validatePinCodeObservable(pinCode)));
    }

    private Observable<JsonElement> validatePinCodeObservable(final String pinCode) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.checkPinCode(accessToken, pinCode);
    }

    public Observable<JsonElement> postMessage(final int contactId, final String message) {
        return postMessageObservable(contactId, message)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageObservable(contactId, message)));
    }

    private Observable<JsonElement> postMessageObservable(final int contactId, final String message) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.contactMessagePost(accessToken, contactId, message);
    }

    public Observable<JsonElement> postMessageWithAttachment(final int contactId, final String message, final File file) {
        return postMessageWithAttachmentObservable(contactId, message, file)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageWithAttachmentObservable(contactId, message, file)));
    }

    private Observable<JsonElement> postMessageWithAttachmentObservable(final int contactId, final String message, final File file) {
        final String accessToken = preferences.getAccessToken();
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return networkApi.contactMessagePostWithAttachment(accessToken, contactId, params, requestBody);
    }


    /*
    public Observable<JSONObject> postMessageWithAttachment(final String contact_id, final String message, final File file) {
        final String accessToken = preferences.getAccessToken();
        return postMessageWithAttachmentObservable(accessToken, contact_id, message, file)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return postMessageWithAttachmentObservable(token, contact_id, message, file);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return postMessageWithAttachmentObservable(token, contact_id, message, file);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> postMessageObservable(final String accessToken, final String contact_id, final String message) {
        return networkApi.contactMessagePost(accessToken, contact_id, message);
    }

    private Observable<Response> postMessageWithAttachmentObservable(final String accessToken, final String contact_id, final String message, final File file) {
        TypedFile typedFile = new TypedFile("multipart/form-data", file);
        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return networkApi.contactMessagePostWithAttachment(accessToken, contact_id, params, typedFile);
    }
     */

    /*
     public Observable<JSONObject> postMessage(final String contact_id, final String message) {
        final String accessToken = preferences.getAccessToken();
        return postMessageObservable(accessToken, contact_id, message)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return postMessageObservable(token, contact_id, message);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return postMessageObservable(token, contact_id, message);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }
     */

    /*
    public Observable<JSONObject> validatePinCode(final String pinCode) {
        final String accessToken = preferences.getAccessToken();
        return validatePinCodeObservable(accessToken, pinCode)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return validatePinCodeObservable(token, pinCode);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return validatePinCodeObservable(token, pinCode);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> validatePinCodeObservable(final String accessToken, final String pinCode) {
        return networkApi.checkPinCode(accessToken, pinCode);
    }
     */

    /*
     public Observable<JSONObject> contactAction(final String contactId, final String pinCode, final ContactAction action) {
        final String accessToken = preferences.getAccessToken();
        ;
        return contactActionObservable(accessToken, contactId, pinCode, action)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return contactActionObservable(token, contactId, pinCode, action);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return contactActionObservable(token, contactId, pinCode, action);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> contactActionObservable(final String accessToken, final String contactId, final String pinCode, final ContactAction action) {
        switch (action) {
            case RELEASE:
                return networkApi.releaseContactPinCode(accessToken, contactId, pinCode);
            case CANCEL:
                return networkApi.contactCancel(accessToken, contactId);
            case DISPUTE:
                return networkApi.contactDispute(accessToken, contactId);
            case PAID:
                return networkApi.markAsPaid(accessToken, contactId);
            case FUND:
                return networkApi.contactFund(accessToken, contactId);
        }
        return Observable.error(new Error("Unable to perform action on contact"));
    }

     */



    /*public Observable<ContactRequest> createContact(final String adId, final TradeType tradeType, final String countryCode,
                                                    final String onlineProvider, final String amount, final String name,
                                                    final String phone, final String email, final String iban, final String bic,
                                                    final String reference, final String message, final String sortCode,
                                                    final String billerCode, final String accountNumber, final String bsb,
                                                    final String ethereumAddress) {

        final String accessToken = preferences.getAccessToken();
        ;

        return createContactObservable(accessToken, adId, tradeType, countryCode, onlineProvider, amount,
                name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return createContactObservable(token, adId, tradeType, countryCode, onlineProvider, amount,
                                                        name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return createContactObservable(token, adId, tradeType, countryCode, onlineProvider, amount,
                                                            name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToContactRequest());
    }


    private Observable<Response> createContactObservable(final String accessToken, final String adId, final TradeType tradeType, final String countryCode,
                                                         final String onlineProvider, final String amount, final String name,
                                                         final String phone, final String email, final String iban, final String bic,
                                                         final String reference, final String message, final String sortCode,
                                                         final String billerCode, final String accountNumber, final String bsb,
                                                         final String ethereumAddress) {

        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    switch (countryCode) {
                        case "UK":
                            return networkApi.createContactNationalUK(accessToken, adId, amount, name, sortCode, reference, accountNumber, message);
                        case "AU":
                            return networkApi.createContactNationalAU(accessToken, adId, amount, name, bsb, reference, accountNumber, message);
                        case "FI":
                            return networkApi.createContactNationalFI(accessToken, adId, amount, name, iban, bic, reference, message);
                        default:
                            return networkApi.createContactNational(accessToken, adId, amount, message);
                    }
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.LYDIA:
                case TradeUtils.SWISH:
                    return networkApi.createContactPhone(accessToken, adId, amount, phone, message);
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    return networkApi.createContactEmail(accessToken, adId, amount, email, message);
                case TradeUtils.SEPA:
                    return networkApi.createContactSepa(accessToken, adId, amount, name, iban, bic, reference, message);
                case TradeUtils.ALTCOIN_ETH:
                    return networkApi.createContactEthereumAddress(accessToken, adId, amount, ethereumAddress, message);
                case TradeUtils.BPAY:
                    return networkApi.createContactBPay(accessToken, adId, amount, billerCode, reference, message);
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    return networkApi.createContactPhone(accessToken, adId, amount, phone, message);

            }
        }

        return networkApi.createContact(accessToken, adId, amount, message);
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount) {
        final String accessToken = preferences.getAccessToken();
        return sendPinCodeMoneyObservable(accessToken, pinCode, address, amount)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                return sendPinCodeMoneyObservable(token, pinCode, address, amount);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return sendPinCodeMoneyObservable(token, pinCode, address, amount);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Response> sendPinCodeMoneyObservable(String accessToken, final String pinCode, final String address, final String amount) {
        return networkApi.walletSendPin(accessToken, pinCode, address, amount);
    }

    public Observable<Wallet> getWalletBalance() {
        final String accessToken = preferences.getAccessToken();
        return getWalletBalanceObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                return getWalletBalanceObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    return getWalletBalanceObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToWalletBalance());
    }

    private Observable<Response> getWalletBalanceObservable(final String accessToken) {
        return networkApi.getWalletBalance(accessToken);
    }

    public Observable<JSONObject> validatePinCode(final String pinCode) {
        final String accessToken = preferences.getAccessToken();
        return validatePinCodeObservable(accessToken, pinCode)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return validatePinCodeObservable(token, pinCode);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return validatePinCodeObservable(token, pinCode);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> validatePinCodeObservable(final String accessToken, final String pinCode) {
        return networkApi.checkPinCode(accessToken, pinCode);
    }

    public Observable<JSONObject> contactAction(final String contactId, final String pinCode, final ContactAction action) {
        final String accessToken = preferences.getAccessToken();
        ;
        return contactActionObservable(accessToken, contactId, pinCode, action)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return contactActionObservable(token, contactId, pinCode, action);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return contactActionObservable(token, contactId, pinCode, action);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> contactActionObservable(final String accessToken, final String contactId, final String pinCode, final ContactAction action) {
        switch (action) {
            case RELEASE:
                return networkApi.releaseContactPinCode(accessToken, contactId, pinCode);
            case CANCEL:
                return networkApi.contactCancel(accessToken, contactId);
            case DISPUTE:
                return networkApi.contactDispute(accessToken, contactId);
            case PAID:
                return networkApi.markAsPaid(accessToken, contactId);
            case FUND:
                return networkApi.contactFund(accessToken, contactId);
        }
        return Observable.error(new Error("Unable to perform action on contact"));
    }

    public Observable<JSONObject> updateAdvertisement(final Advertisement advertisement) {
        final String accessToken = preferences.getAccessToken();
        return updateAdvertisementObservable(accessToken, advertisement)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return updateAdvertisementObservable(token, advertisement);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return updateAdvertisementObservable(token, advertisement);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        return Observable.just(jsonObject);
                    }
                });
    }

    private Observable<Response> updateAdvertisementObservable(final String accessToken, final Advertisement advertisement) {
        final String city;
        if (Strings.isBlank(advertisement.getCity())) {
            city = advertisement.getLocation();
        } else {
            city = advertisement.getCity();
        }
        return networkApi.updateAdvertisement(
                accessToken, String.valueOf(advertisement.getAd_id()), advertisement.getAccount_info(), advertisement.getBank_name(), city, advertisement.getCountry_code(), advertisement.getCurrency(),
                String.valueOf(advertisement.getLat()), advertisement.getLocation(), String.valueOf(advertisement.getLon()), advertisement.getMax_amount(), advertisement.getMin_amount(),
                advertisement.getMessage(), advertisement.getPrice_equation(), String.valueOf(advertisement.getTrusted_required()), String.valueOf(advertisement.getSms_verification_required()),
                String.valueOf(advertisement.getTrack_max_amount()), String.valueOf(advertisement.getVisible()), String.valueOf(advertisement.getRequire_identification()),
                advertisement.getRequire_feedback_score(), advertisement.getRequire_trade_volume(), advertisement.getFirst_time_limit_btc(),
                advertisement.getPhone_number(), advertisement.getOpening_hours());
    }

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement) {
        final String accessToken = preferences.getAccessToken();
        return createAdvertisementObservable(accessToken, advertisement)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return createAdvertisementObservable(token, advertisement);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return createAdvertisementObservable(token, advertisement);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> createAdvertisementObservable(final String accessToken, final Advertisement advertisement) {
        String city;
        if (TextUtils.isEmpty(advertisement.getCity())) {
            city = advertisement.getLocation();
        } else {
            city = advertisement.getCity();
        }

        return networkApi.createAdvertisement(accessToken, advertisement.getMin_amount(),
                advertisement.getMax_amount(), advertisement.getPrice_equation(), advertisement.getTrade_type(), advertisement.getOnline_provider(),
                String.valueOf(advertisement.getLat()), String.valueOf(advertisement.getLon()),
                city, advertisement.getLocation(), advertisement.getCountry_code(), advertisement.getAccount_info(), advertisement.getBank_name(),
                String.valueOf(advertisement.getSms_verification_required()), String.valueOf(advertisement.getTrack_max_amount()),
                String.valueOf(advertisement.getTrusted_required()), String.valueOf(advertisement.getRequire_identification()),
                advertisement.getRequire_feedback_score(), advertisement.getRequire_trade_volume(),
                advertisement.getFirst_time_limit_btc(), advertisement.getMessage(), advertisement.getCurrency(),
                advertisement.getPhone_number(), advertisement.getOpening_hours());
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message) {
        final String accessToken = preferences.getAccessToken();
        return postMessageObservable(accessToken, contact_id, message)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return postMessageObservable(token, contact_id, message);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return postMessageObservable(token, contact_id, message);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    public Observable<JSONObject> postMessageWithAttachment(final String contact_id, final String message, final File file) {
        final String accessToken = preferences.getAccessToken();
        return postMessageWithAttachmentObservable(accessToken, contact_id, message, file)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return postMessageWithAttachmentObservable(token, contact_id, message, file);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return postMessageWithAttachmentObservable(token, contact_id, message, file);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> postMessageObservable(final String accessToken, final String contact_id, final String message) {
        return networkApi.contactMessagePost(accessToken, contact_id, message);
    }

    private Observable<Response> postMessageWithAttachmentObservable(final String accessToken, final String contact_id, final String message, final File file) {
        TypedFile typedFile = new TypedFile("multipart/form-data", file);
        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return networkApi.contactMessagePostWithAttachment(accessToken, contact_id, params, typedFile);
    }

    public Observable<User> getMyself(String accessToken) {
        return networkApi.getMyself(accessToken)
                .map(new ResponseToUser());
    }

    public Observable<Contact> getContactInfo(final String contact_id) {
        final String accessToken = preferences.getAccessToken();
        return getContactInfoObservable(accessToken, contact_id)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return getContactInfoObservable(token, contact_id);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return getContactInfoObservable(token, contact_id);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToContact())
                .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                    @Override
                    public Observable<? extends Contact> call(final Contact contact) {
                        return getContactMessagesObservable(String.valueOf(contact.getContact_id()))
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, Contact>() {
                                    @Override
                                    public Contact call(List<Message> messages) {
                                        if (messages != null) {
                                            contact.setMessages(messages);
                                        }
                                        return contact;
                                    }
                                });
                    }
                });
    }

    private Observable<Response> getContactInfoObservable(String accessToken, final String contact_id) {
        return networkApi.getContactInfo(accessToken, contact_id);
    }

    private Observable<Response> getContactMessagesObservable(final String contact_id) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.contactMessages(accessToken, contact_id);
    }

    public Observable<List<Notification>> getNotifications() {
        final String accessToken = preferences.getAccessToken();
        return getNotificationsObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return getNotificationsObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return getNotificationsObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToNotifications());
    }

    private Observable<Response> getNotificationsObservable(String accessToken) {
        return networkApi.getNotifications(accessToken);
    }

    public Observable<JSONObject> markNotificationRead(final String notificationId) {
        final String accessToken = preferences.getAccessToken();
        return markNotificationReadObservable(accessToken, notificationId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return markNotificationReadObservable(token, notificationId);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return markNotificationReadObservable(token, notificationId);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> markNotificationReadObservable(String accessToken, final String notificationId) {
        return networkApi.markNotificationRead(accessToken, notificationId);
    }

    public Observable<List<Contact>> getContacts(final DashboardType dashboardType) {
        final String accessToken = preferences.getAccessToken();
        switch (dashboardType) {
            case RELEASED:
            case CLOSED:
            case CANCELED:
                return getContactsObservable(accessToken, dashboardType)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(Throwable throwable) {
                                NetworkException networkException = null;
                                if (throwable instanceof NetworkException) {
                                    networkException = (NetworkException) throwable;
                                    throwable = networkException.getCause();
                                }
                                if (networkException != null) {
                                    if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                        return refreshTokens()
                                                .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                    @Override
                                                    public Observable<? extends Response> call(String token) {

                                                        return getContactsObservable(token, dashboardType);
                                                    }
                                                });
                                    } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                        if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                            return refreshTokens()
                                                    .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                        @Override
                                                        public Observable<? extends Response> call(String token) {

                                                            return getContactsObservable(token, dashboardType);
                                                        }
                                                    });
                                        }
                                    }
                                    return Observable.error(networkException);
                                }
                                return Observable.error(throwable);
                            }
                        })
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                                return Observable.just(contacts);
                            }
                        });
            default:
                return getContactsObservable(accessToken)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(Throwable throwable) {
                                NetworkException networkException = null;
                                if (throwable instanceof NetworkException) {
                                    networkException = (NetworkException) throwable;
                                }
                                if (networkException != null) {
                                    if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                        return refreshTokens()
                                                .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                    @Override
                                                    public Observable<? extends Response> call(String token) {

                                                        return getContactsObservable(token);
                                                    }
                                                });
                                    } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                        if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                            return refreshTokens()
                                                    .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                        @Override
                                                        public Observable<? extends Response> call(String token) {

                                                            return getContactsObservable(token);
                                                        }
                                                    });
                                        }
                                    }
                                    return Observable.error(networkException);
                                }
                                return Observable.error(throwable);
                            }
                        })
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                                return Observable.just(contacts);
                            }
                        });
        }
    }

    private Observable<Dashboard> getContactsObservable(String accessToken) {
        return networkApi.getDashboard(accessToken);
    }

    private Observable<Dashboard> getContactsObservable(String accessToken, final DashboardType dashboardType) {
        return networkApi.getDashboard(accessToken, dashboardType.name().toLowerCase());
    }

    public Observable<Advertisement> getAdvertisement(final String adId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getAdvertisement(accessToken, adId)

                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends Advertisement>>() {
                    @Override
                    public ObservableSource<? extends Advertisement> apply(Throwable throwable) throws Exception {
                        return null;
                    }
                })

                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Advertisement>>() {
                    @Override
                    public Observable<? extends Advertisement> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return networkApi.getAdvertisement(token, adId);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return networkApi.getAdvertisement(token, adId);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToAd());
    }

    public Observable<JSONObject> updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible) {
        advertisement.setVisible(visible);
        final String accessToken = preferences.getAccessToken();
        return updateAdvertisementObservable(accessToken, advertisement)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return updateAdvertisementObservable(token, advertisement);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return updateAdvertisementObservable(token, advertisement);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        } else {
                            return Observable.just(jsonObject);
                        }
                    }
                });
    }

    public Observable<Boolean> deleteAdvertisement(final String adId) {
        final String accessToken = preferences.getAccessToken();
        return deleteAdvertisementObservable(accessToken, adId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return deleteAdvertisementObservable(token, adId);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return deleteAdvertisementObservable(token, adId);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Response> deleteAdvertisementObservable(String accessToken, final String adId) {
        return networkApi.deleteAdvertisement(accessToken, adId);
    }

    public Observable<Wallet> getWallet() {
        final String accessToken = preferences.getAccessToken();
        return getWalletObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if (networkException != null) {
                            if (networkException.getStatus() == DataServiceUtils.STATUS_403) {

                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {

                                                return getWalletObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {

                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {

                                                    return getWalletObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToWallet());
    }

    private Observable<Response> getWalletObservable(String accessToken) {
        return networkApi.getWallet(accessToken);
    }

    public Observable<List<Method>> getMethods() {
        return networkApi.getOnlineProviders()
                .map(new ResponseToMethod());
    }*/
}