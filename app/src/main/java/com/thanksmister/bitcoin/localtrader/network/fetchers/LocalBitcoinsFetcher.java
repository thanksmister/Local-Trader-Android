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

package com.thanksmister.bitcoin.localtrader.network.fetchers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.network.ApiErrorHandler;
import com.thanksmister.bitcoin.localtrader.network.AuthenticationException;
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi;
import com.thanksmister.bitcoin.localtrader.network.NetworkException;
import com.thanksmister.bitcoin.localtrader.network.api.model.OauthResponse;
import com.thanksmister.bitcoin.localtrader.network.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.persistence.Notification;
import com.thanksmister.bitcoin.localtrader.persistence.Preferences;
import com.thanksmister.bitcoin.localtrader.persistence.User;
import com.thanksmister.bitcoin.localtrader.persistence.Wallet;

import java.util.List;
import java.util.TreeMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LocalBitcoinsFetcher {
    
    private final LocalBitcoinsApi networkApi;
    private final Preferences preferences;
    private final Context context;
    private final String AUTH_GRANT_TYPE = "authorization_code";
    private final String REFRESH_GRANT_TYPE = "refresh_token";

    public LocalBitcoinsFetcher(@NonNull Context context, @NonNull LocalBitcoinsApi networkApi, @NonNull Preferences preferences) {
        this.networkApi = networkApi;
        this.preferences = preferences;
        this.context = context;
    }

    public Observable<User> getMyself() {
        String token = preferences.accessToken();
        if(TextUtils.isEmpty(token)) return Observable.empty();
        return networkApi.getMyself(token)
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends User>>() {
                    @Override
                    public ObservableSource<? extends User> apply(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403 || networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry status: " + networkException.getStatus());
                                if (networkException.getCode() != DataServiceUtils.CODE_THREE) {
                                    return refreshOathTokens()
                                            .flatMap(new Function<String, ObservableSource<User>>() {
                                                @Override
                                                public ObservableSource<User> apply(String newToken) throws Exception {
                                                    return networkApi.getMyself(newToken);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                });
    }

    public Observable<OauthResponse> getOauthToken(final String code, final String key, final String secret) {
        return networkApi.getOauthToken(AUTH_GRANT_TYPE, code, key, secret);
    }

    public Observable<List<Notification>> getNotifications() {
        String token = preferences.accessToken();
        if(TextUtils.isEmpty(token)) return Observable.empty();
        return networkApi.getNotifications(token)
                .onErrorResumeNext(throwable -> {
                    Timber.d("onErrorResumeNext");
                    Exception exception = ApiErrorHandler.handleError(context, throwable);
                    Timber.d("exception message: " + exception.getMessage());
                    if(exception instanceof AuthenticationException) {
                        Timber.d("oath exception code: " + ((AuthenticationException) exception).getCode());
                        Timber.d("oauth exception status: " + ((AuthenticationException) exception).getStatus());
                        return refreshOathTokens()
                                .flatMap((Function<String, ObservableSource<List<Notification>>>) newToken ->
                                        networkApi.getNotifications(newToken)
                                        .subscribeOn(Schedulers.io()));
                    }
                    Timber.d("exception code: " + ((NetworkException) exception).getCode());
                    Timber.d("exception status: " + ((NetworkException) exception).getStatus());
                    return Observable.error(exception);
                });
    }

    public Observable<Wallet> getWalletBalance() {
        String token = preferences.accessToken();
        if(TextUtils.isEmpty(token)) return Observable.empty();
        return networkApi.getWalletBalance(token)
                .onErrorResumeNext(throwable -> {
                    NetworkException networkException = null;
                    if (throwable instanceof NetworkException) {
                        networkException = (NetworkException) throwable;
                        if(networkException.getStatus() == DataServiceUtils.STATUS_403 || networkException.getStatus() == DataServiceUtils.STATUS_400) {
                            Timber.d("refreshTokenAndRetry status: " + networkException.getStatus());
                            if (networkException.getCode() != DataServiceUtils.CODE_THREE) {
                                return refreshOathTokens()
                                        .flatMap((Function<String, ObservableSource<Wallet>>) networkApi::getWalletBalance);
                            }
                        }
                        return Observable.error(networkException);
                    }
                    return Observable.error(throwable);
                });
    }

    private Observable<String> refreshOathTokens() {
        String refreshToken = preferences.refreshToken();
        if(TextUtils.isEmpty(refreshToken)) return Observable.empty();
        return networkApi.refreshOauthToken(REFRESH_GRANT_TYPE, refreshToken, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
                .flatMap(oauthResponse -> {
                    preferences.accessToken(oauthResponse.access_token);
                    preferences.refreshToken(oauthResponse.refresh_token);
                    return Observable.just(oauthResponse.access_token);
                });
    }

    public Observable<TreeMap<String, Object>> getCurrencies() {
        return networkApi.getCurrencies();
    }

    public Observable<TreeMap<String, Object>> getMethods() {
        return networkApi.getMethods();
    }
}