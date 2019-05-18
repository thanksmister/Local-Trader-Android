/*
 * Copyright (c) 2019 ThanksMister LLC
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
import com.thanksmister.bitcoin.localtrader.network.api.model.NewAddress;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notifications;
import com.thanksmister.bitcoin.localtrader.network.api.model.Place;
import com.thanksmister.bitcoin.localtrader.network.api.model.Places;
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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

    private Observable<String> refreshTokens() {
        Timber.d("accessToken: " + preferences.getAccessToken());
        Timber.d("refreshToken: " + preferences.getRefreshToken());
        return networkApi.refreshToken("refresh_token", preferences.getRefreshToken(), BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
                .flatMap(new Function<Authorization, ObservableSource<? extends String>>() {
                    @Override
                    public ObservableSource<? extends String> apply(Authorization authorization) {
                        Timber.d("authorization " + authorization);
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

    /*private <T> Function<Throwable, ? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed) {

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
                } else if (throwable instanceof SocketTimeoutException) {
                    return Observable.error(throwable); // bubble up the exception;
                }
                return Observable.error(networkException); // bubble up the exception;
            }
        };
    }*/

    public class retryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {
        private final int maxRetries;
        private final int retryDelayMillis;
        private int retryCount;

        public retryWithDelay(final int maxRetries, final int retryDelayMillis) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.retryCount = 0;
        }

        @Override
        public Observable<?> apply(final Observable<? extends Throwable> attempts) {
            return attempts
                    .flatMap(new Function<Throwable, Observable<?>>() {
                        @Override
                        public Observable<?> apply(final Throwable throwable) {
                            RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                            Timber.d("refreshTokenAndRetry error: " + throwable.getMessage());
                            final NetworkException networkException = errorHandler.create(throwable);
                            if (RetrofitErrorHandler.Companion.isHttp403Error(networkException.getCode())) {
                                Timber.e("Retrying error code: " + networkException.getCode());
                                return refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap(new Function<String, Observable<?>>() {
                                            @Override
                                            public Observable<?> apply(String s) throws Exception {
                                                if (++retryCount < maxRetries) {
                                                    // When this Observable calls onNext, the original
                                                    // Observable will be retried (i.e. re-subscribed).
                                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                                }
                                                return Observable.error(networkException); // bubble up the exception;
                                            }
                                        });
                            } else if (RetrofitErrorHandler.Companion.isHttp400Error(networkException.getCode())) {
                                Timber.e("Retrying error code: " + networkException.getCode());
                                return refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap(new Function<String, Observable<?>>() {
                                            @Override
                                            public Observable<?> apply(String s) throws Exception {
                                                if (++retryCount < maxRetries) {
                                                    // When this Observable calls onNext, the original
                                                    // Observable will be retried (i.e. re-subscribed).
                                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                                }
                                                return Observable.error(networkException); // bubble up the exception;
                                            }
                                        });
                            } else if (ExceptionCodes.INSTANCE.getCODE_THREE() == networkException.getCode()) {
                                Timber.e("Retrying error code: " + networkException.getCode());
                                return refreshTokens()
                                        .subscribeOn(Schedulers.computation())
                                        .flatMap(new Function<String, Observable<?>>() {
                                            @Override
                                            public Observable<?> apply(String s) throws Exception {
                                                if (++retryCount < maxRetries) {
                                                    // When this Observable calls onNext, the original
                                                    // Observable will be retried (i.e. re-subscribed).
                                                    return Observable.timer(10, TimeUnit.MILLISECONDS);
                                                }
                                                return Observable.error(networkException); // bubble up the exception;
                                            }
                                        });
                            } else if (throwable instanceof SocketTimeoutException) {
                                return Observable.error(throwable); // bubble up the exception;
                            }
                            return Observable.error(networkException); // bubble up the exception;
                        }
                    });
        }
    }

    public Observable<User> getMyself() {
        return getMyselfObservable()
                .retryWhen(new retryWithDelay(1, 500));
                /*.onErrorResumeNext(new Function<Throwable, Observable<User>>() {
                    @Override
                    public Observable<User> apply(Throwable throwable) throws Exception {
                        RetrofitErrorHandler errorHandler = new RetrofitErrorHandler(context);
                        Timber.d("refreshTokenAndRetry error: " + throwable.getMessage());
                        final NetworkException networkException = errorHandler.create(throwable);
                        if (RetrofitErrorHandler.Companion.isHttp403Error(networkException.getCode())) {
                            Timber.e("Retrying error code: " + networkException.getCode());
                            return refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap(new Function<String, Observable<User>>() {
                                        @Override
                                        public Observable<User> apply(String s) throws Exception {
                                            return getMyselfObservable();
                                        }
                                    });
                        } else if (RetrofitErrorHandler.Companion.isHttp400Error(networkException.getCode())) {
                            Timber.e("Retrying error code: " + networkException.getCode());
                            return refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap(new Function<String, Observable<User>>() {
                                        @Override
                                        public Observable<User> apply(String s) throws Exception {
                                            return getMyselfObservable();
                                        }
                                    });
                        } else if (ExceptionCodes.INSTANCE.getCODE_THREE() == networkException.getCode() ) {
                            Timber.e("Retrying error code: " + networkException.getCode());
                            return refreshTokens()
                                    .subscribeOn(Schedulers.computation())
                                    .flatMap(new Function<String, Observable<User>>() {
                                        @Override
                                        public Observable<User> apply(String s) throws Exception {
                                            return getMyselfObservable();
                                        }
                                    });
                        } else if (throwable instanceof SocketTimeoutException) {
                            return Observable.error(throwable); // bubble up the exception;
                        }
                        return Observable.error(networkException); // bubble up the exception;
                    }
                });*/
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
                .retryWhen(new retryWithDelay(1, 500))
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
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<Contact> getContactObservable(final int contactId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getContactInfo(accessToken, contactId);
    }

    public Observable<Wallet> getWallet() {
        return getWalletObservable()
                .retryWhen(new retryWithDelay(1, 500));
    }

    public Observable<NewAddress> getWalletAddress() {
        return getWalletAddressObservable()
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<NewAddress> getWalletAddressObservable() {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getWalletAddress(accessToken);
    }

    private Observable<Wallet> getWalletObservable() {
        final String accessToken = preferences.getAccessToken();
        return networkApi.getWallet(accessToken);
    }

    public Observable<Wallet> getWalletBalance() {
        Timber.d("getWalletBalance");
        return getWalletBalanceObservable()
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<Wallet> getWalletBalanceObservable() {
        Timber.d("getWalletBalanceObservable token " + preferences.getAccessToken());
        final String accessToken = preferences.getAccessToken();
        return networkApi.getWalletBalance(accessToken);
    }

    public Observable<List<Contact>> getContacts() {
        return getContactsObservable()
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500))
                .flatMap(new Function<Dashboard, ObservableSource<List<Contact>>>() {
                    @Override
                    public ObservableSource<List<Contact>> apply(Dashboard dashboard) throws Exception {
                        return Observable.just(dashboard.getItems());
                    }
                });
    }

    public Observable<List<Notification>> getNotifications() {
        return getNotificationsObservable()
                .retryWhen(new retryWithDelay(1, 500));
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
        return sendPinCodeMoneyObservable(pinCode, address, amount);
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
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<JsonElement> markNotificationReadObservable(final String notificationId) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.markNotificationRead(accessToken, String.valueOf(notificationId));
    }

    public Observable<List<Message>> getContactMessages(final int contactId) {
        return getContactMessagesReadObservable(contactId)
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500));
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
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<JsonElement> validatePinCodeObservable(final String pinCode) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.checkPinCode(accessToken, pinCode);
    }

    public Observable<JsonElement> postMessage(final int contactId, final String message) {
        return postMessageObservable(contactId, message)
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<JsonElement> postMessageObservable(final int contactId, final String message) {
        final String accessToken = preferences.getAccessToken();
        return networkApi.contactMessagePost(accessToken, contactId, message);
    }

    public Observable<JsonElement> postMessageWithAttachment(final int contactId, final String message, final File file) {
        return postMessageWithAttachmentObservable(contactId, message, file)
                .retryWhen(new retryWithDelay(1, 500));
    }

    private Observable<JsonElement> postMessageWithAttachmentObservable(final int contactId, final String message, final File file) {
        final String accessToken = preferences.getAccessToken();
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multiPartBody = MultipartBody.Part.createFormData("document", file.getName(), requestBody);

        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return networkApi.contactMessagePostWithAttachment(accessToken, contactId, params, multiPartBody);
    }
}