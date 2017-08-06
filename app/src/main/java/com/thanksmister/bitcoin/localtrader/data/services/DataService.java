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
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
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

import dpreference.DPreference;
import retrofit.client.Response;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;


@Singleton
public class DataService {
    private static final String PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire";
    private static final String PREFS_CURRENCY_EXPIRE_TIME = "pref_currency_expire";
    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_ADVERTISEMENT_EXPIRE_TIME = "pref_ads_expire";
    public static final String PREFS_WALLET_EXPIRE_TIME = "pref_wallet_expire";
    public static final String PREFS_WALLET_BALANCE_EXPIRE_TIME = "pref_wallet_balance_expire";
    public static final String PREFS_CONTACTS_EXPIRE_TIME = "pref_contacts_expire";
   
    private static final int CHECK_CURRENCY_DATA = 604800000;// // 1 week 604800000
    private static final int CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
    public static final int CHECK_ADVERTISEMENT_DATA = 3600000;// 1 hour
    public static final int CHECK_CONTACTS_DATA = 5 * 60 * 1000;// 5 minutes
    public static final int CHECK_WALLET_DATA = 15 * 60 * 1000;// 15 minutes
    public static final int CHECK_WALLET_BALANCE_DATA = 15 * 60 * 1000;// 15 minutes

    private final LocalBitcoins localBitcoins;
    private final SharedPreferences sharedPreferences;
    private final DPreference preference;
    private final BaseApplication baseApplication;

    @Inject
    public DataService(BaseApplication baseApplication, DPreference preference, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins) {
        this.baseApplication = baseApplication;
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
        this.preference = preference;
    }

    public void logout() {
        resetExchangeExpireTime();
        resetAdvertisementsExpireTime();
        resetMethodsExpireTime();
        resetContactsExpireTime();
    }

    public Observable<Authorization> getAuthorization(String code) {
        return localBitcoins.getAuthorization("authorization_code", code, baseApplication.getString(R.string.lbc_access_key), baseApplication.getString(R.string.lbc_access_secret))
                .map(new ResponseToAuthorize());
    }

    private <T> Func1<Throwable, ? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed) {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                if (DataServiceUtils.isHttp403Error(throwable)) {
                    return refreshTokens()
                            .flatMap(new Func1<String, Observable<? extends T>>() {
                                @Override
                                public Observable<? extends T> call(String token) {
                                    return toBeResumed;
                                }
                            });
                } else if (DataServiceUtils.isHttp400Error(throwable)) {
                    RetroError error = DataServiceUtils.createRetroError(throwable);
                    if (error.getCode() == DataServiceUtils.CODE_THREE) {
                        return refreshTokens()
                                .flatMap(new Func1<String, Observable<? extends T>>() {
                                    @Override
                                    public Observable<? extends T> call(String token) {
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
        final String refreshToken = AuthUtils.getRefreshToken(preference, sharedPreferences);
        return localBitcoins.refreshToken("refresh_token", refreshToken, baseApplication.getString(R.string.lbc_access_key), baseApplication.getString(R.string.lbc_access_secret))
                .map(new ResponseToAuthorize())
                .flatMap(new Func1<Authorization, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Authorization authorization) {
                        AuthUtils.setAccessToken(preference, authorization.access_token);
                        AuthUtils.setRefreshToken(preference, authorization.refresh_token);
                        return Observable.just(authorization.access_token);
                    }
                });
    }

    public Observable<List<ExchangeCurrency>> getCurrencies() {
        if(!needToRefreshCurrency()) {
            return Observable.just(null);
        }
        
        return localBitcoins.getCurrencies()
                .doOnNext(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        setCurrencyExpireTime();
                    }
                })
                .map(new ResponseToCurrencies());
    }

    public Observable<ContactRequest> createContact(final String adId, final TradeType tradeType, final String countryCode,
                                                    final String onlineProvider, final String amount, final String name,
                                                    final String phone, final String email, final String iban, final String bic,
                                                    final String reference, final String message, final String sortCode,
                                                    final String billerCode, final String accountNumber, final String bsb,
                                                    final String ethereumAddress) {
        
        return createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
                .onErrorResumeNext(refreshTokenAndRetry(createContactObservable(adId, tradeType, countryCode, onlineProvider, amount,
                        name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)))
                .map(new ResponseToContactRequest());
    }


    private Observable<Response> createContactObservable(final String adId, final TradeType tradeType, final String countryCode,
                                                         final String onlineProvider, final String amount, final String name,
                                                         final String phone, final String email, final String iban, final String bic,
                                                         final String reference, final String message, final String sortCode,
                                                         final String billerCode, final String accountNumber, final String bsb, 
                                                         final String ethereumAddress) {
        
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        
        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    switch (countryCode) {
                        case "UK":
                            return localBitcoins.createContactNational_UK(accessToken, adId, amount, name, sortCode, reference, accountNumber, message);
                        case "AU":
                            return localBitcoins.createContactNational_AU(accessToken, adId, amount, name, bsb, reference, accountNumber, message);
                        case "FI":
                            return localBitcoins.createContactNational_FI(accessToken, adId, amount, name, iban, bic, reference, message);
                        default:
                            return localBitcoins.createContactNational(accessToken, adId, amount, message);
                    }
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.LYDIA:
                case TradeUtils.SWISH:
                    return localBitcoins.createContactPhone(accessToken, adId, amount, phone, message);
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    return localBitcoins.createContactEmail(accessToken, adId, amount, email, message);
                case TradeUtils.SEPA:
                    return localBitcoins.createContactSepa(accessToken, adId, amount, name, iban, bic, reference, message);
                case TradeUtils.ALTCOIN_ETH:
                    return localBitcoins.createContactEthereumAddress(accessToken, adId, amount, ethereumAddress, message);
                case TradeUtils.BPAY:
                    return localBitcoins.createContactBPay(accessToken, adId, amount, billerCode, reference, message);
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    return localBitcoins.createContactPhone(accessToken, adId, amount, phone, message);
                
            }
        }

        return localBitcoins.createContact(accessToken, adId, amount, message);
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount) {
        return sendPinCodeMoneyObservable(pinCode, address, amount)
                .onErrorResumeNext(refreshTokenAndRetry(sendPinCodeMoneyObservable(pinCode, address, amount)))
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

    private Observable<Response> sendPinCodeMoneyObservable(final String pinCode, final String address, final String amount) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.walletSendPin(accessToken, pinCode, address, amount);
    }

    public Observable<Wallet> getWalletBalance() {
        
        if(!needToRefreshWalletBalance()) {
            return Observable.just(null);
        }
        
        return getWalletBalanceObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletBalanceObservable()))
                .map(new ResponseToWalletBalance())
                .flatMap(new Func1<Wallet, Observable<Wallet>>() {
                    @Override
                    public Observable<Wallet> call(Wallet wallet) {
                        setWalletBalanceExpireTime();
                        return getWalletBitmap(wallet);
                    }
                });
    }

    private Observable<Response> getWalletBalanceObservable() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getWalletBalance(accessToken);
    }

    public Observable<JSONObject> validatePinCode(final String pinCode) {
        return validatePinCodeObservable(pinCode)
                .onErrorResumeNext(refreshTokenAndRetry(validatePinCodeObservable(pinCode)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> validatePinCodeObservable(final String pinCode) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.checkPinCode(accessToken, pinCode);
    }

    public Observable<JSONObject> contactAction(final String contactId, final String pinCode, final ContactAction action) {
        return contactActionObservable(contactId, pinCode, action)
                .onErrorResumeNext(refreshTokenAndRetry(contactActionObservable(contactId, pinCode, action)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> contactActionObservable(final String contactId, final String pinCode, final ContactAction action) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
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
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        return Observable.just(jsonObject);
                    }
                });
    }

    private Observable<Response> updateAdvertisementObservable(final Advertisement advertisement) {
        final String city;
        if (Strings.isBlank(advertisement.city)) {
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.updateAdvertisement(
                accessToken, advertisement.ad_id, advertisement.account_info, advertisement.bank_name, city, advertisement.country_code, advertisement.currency,
                String.valueOf(advertisement.lat), advertisement.location, String.valueOf(advertisement.lon), advertisement.max_amount, advertisement.min_amount,
                advertisement.message, advertisement.price_equation, String.valueOf(advertisement.trusted_required), String.valueOf(advertisement.sms_verification_required),
                String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.visible), String.valueOf(advertisement.require_identification),
                advertisement.require_feedback_score, advertisement.require_trade_volume, advertisement.first_time_limit_btc, 
                advertisement.phone_number, advertisement.opening_hours);
    }

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement) {
        return createAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(createAdvertisementObservable(advertisement)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement) {
        String city;
        if (Strings.isBlank(advertisement.city)) {
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }

        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.createAdvertisement(accessToken, advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.trade_type.name(), advertisement.online_provider,
                String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount),
                String.valueOf(advertisement.trusted_required), String.valueOf(advertisement.require_identification),
                advertisement.require_feedback_score, advertisement.require_trade_volume,
                advertisement.first_time_limit_btc, advertisement.message, advertisement.currency,
                advertisement.phone_number, advertisement.opening_hours);

        
        
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message) {
        return postMessageObservable(contact_id, message)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageObservable(contact_id, message)))
                .map(new ResponseToJSONObject());
    }

    public Observable<JSONObject> postMessageWithAttachment(final String contact_id, final String message, final File file) {
        return postMessageWithAttachmentObservable(contact_id, message, file)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageWithAttachmentObservable(contact_id, message, file)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> postMessageObservable(final String contact_id, final String message) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.contactMessagePost(accessToken, contact_id, message);
    }

    private Observable<Response> postMessageWithAttachmentObservable(final String contact_id, final String message, final File file) {
        TypedFile typedFile = new TypedFile("multipart/form-data", file);
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return localBitcoins.contactMessagePostWithAttachment(accessToken, contact_id, params, typedFile);
    }

    public Observable<User> getMyself(String accessToken) {
        return localBitcoins.getMyself(accessToken)
                .map(new ResponseToUser());
    }

    public Observable<Contact> getContactInfo(final String contact_id) {
        return getContactInfoObservable(contact_id)
                .onErrorResumeNext(refreshTokenAndRetry(getContactInfoObservable(contact_id)))
                .map(new ResponseToContact())
                .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                    @Override
                    public Observable<? extends Contact> call(final Contact contact) {
                        return getContactMessagesObservable(contact.contact_id)
                                .onErrorResumeNext(refreshTokenAndRetry(getContactMessagesObservable(contact_id)))
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, Contact>() {
                                    @Override
                                    public Contact call(List<Message> messages) {
                                        if(messages != null) {
                                            contact.messages = messages; 
                                        }
                                        return contact;
                                    }
                                });
                    }
                });
    }

    private Observable<Response> getContactInfoObservable(final String contact_id) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getContactInfo(accessToken, contact_id);
    }

    public Observable<List<Message>> getRecentMessages() {
        return getRecentMessagesObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getRecentMessagesObservable()))
                .map(new ResponseToMessages());
    }

    private Observable<Response> getRecentMessagesObservable() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.recentMessages(accessToken);
    }

    private Observable<Response> getContactMessagesObservable(final String contact_id) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.contactMessages(accessToken, contact_id);
    }

    public Observable<List<Notification>> getNotifications() {
        return getNotificationsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getNotificationsObservable()))
                .map(new ResponseToNotifications());
    }

    private Observable<Response> getNotificationsObservable() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getNotifications(accessToken);
    }

    public Observable<JSONObject> markNotificationRead(final String notificationId) {
        return markNotificationReadObservable(notificationId)
                .onErrorResumeNext(refreshTokenAndRetry(markNotificationReadObservable(notificationId)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> markNotificationReadObservable(final String notificationId) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.markNotificationRead(accessToken, notificationId);
    }

    public Observable<List<Contact>> getContacts(final DashboardType dashboardType) {
        switch (dashboardType) {
            case RELEASED:
            case CLOSED:
            case CANCELED:
                return getContactsObservable(dashboardType)
                        .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable(dashboardType)))
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                                //setContactsExpireTime();
                                return Observable.just(contacts);
                            }
                        });
            default:
                return getContactsObservable()
                        .onErrorResumeNext(refreshTokenAndRetry(getContactsObservable()))
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                                //setContactsExpireTime();
                                return Observable.just(contacts);
                            }
                        });
        }
    }

    private Observable<Response> getContactsObservable() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getDashboard(accessToken);
    }

    private Observable<Response> getContactsObservable(final DashboardType dashboardType) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getDashboard(accessToken, dashboardType.name().toLowerCase());
    }

    public Observable<Advertisement> getAdvertisement(final String adId) {
        Timber.d("getEditAdvertisement");
        return getAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementObservable(adId)))
                .map(new ResponseToAd());
    }

    private Observable<Response> getAdvertisementObservable(final String adId) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getAdvertisement(accessToken, adId);
    }

    public Observable<List<Advertisement>> getAdvertisements(boolean force) {

        Timber.d("getAdvertisements needToRefreshAdvertisements: " + needToRefreshAdvertisements());
        Timber.d("getAdvertisements force: " + force);
        
        if (!needToRefreshAdvertisements() && !force) {
            return Observable.just(null);
        }

        return getAdvertisementsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementsObservable()))
                .doOnNext(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        Timber.d("getAdvertisementsm setAdvertisementsExpireTime");
                        setAdvertisementsExpireTime();
                    }
                })
                .map(new ResponseToAds());
    }

    private Observable<Response> getAdvertisementsObservable() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getAds(accessToken);
    }

    public Observable<JSONObject> updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible) {
        advertisement.visible = visible;
        return updateAdvertisementObservable(advertisement)
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        return Observable.just(jsonObject);
                    }
                });
    }

    public Observable<Boolean> deleteAdvertisement(final String adId) {
        return deleteAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(deleteAdvertisementObservable(adId)))
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            throw new Error("Error deleting editAdvertisement");
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Response> deleteAdvertisementObservable(final String adId) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.deleteAdvertisement(accessToken, adId);
    }

    public Observable<Wallet> getWallet() {
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

    private Observable<Response> getWalletObservable() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getWallet(accessToken);
    }

    ////  HMAC ////
    


    private Observable<Wallet> getWalletBitmap(final Wallet wallet) {
        return generateBitmap(wallet.address)
                .map(new Func1<Bitmap, Wallet>() {
                    @Override
                    public Wallet call(Bitmap bitmap) {
                        wallet.qrImage = bitmap;
                        return wallet;
                    }
                });
    }

    private Observable<Bitmap> generateBitmap(final String address) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
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
        if (!needToRefreshMethods()) {
            return Observable.just(null);
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
        preference.removePreference(PREFS_CONTACTS_EXPIRE_TIME);
    }

    private void setContactsExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_CONTACTS_DATA; // 1 hours
        preference.putLong(PREFS_CONTACTS_EXPIRE_TIME, expire);
    }

    private void resetExchangeExpireTime() {
        preference.removePreference(PREFS_EXCHANGE_EXPIRE_TIME);
    }

    public boolean needToRefreshMethods() {
        return System.currentTimeMillis() > preference.getLong(PREFS_METHODS_EXPIRE_TIME, -1);
    }

    public void resetMethodsExpireTime() {
        preference.removePreference(PREFS_METHODS_EXPIRE_TIME);
    }

    private void setMethodsExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_METHODS_DATA; // 1 hours
        preference.putLong(PREFS_METHODS_EXPIRE_TIME, expire);
    }

    public boolean needToRefreshCurrency() {
        return System.currentTimeMillis() > preference.getLong(PREFS_CURRENCY_EXPIRE_TIME, -1);
    }

    public void setCurrencyExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_CURRENCY_DATA; // 1 hours
        preference.putLong(PREFS_CURRENCY_EXPIRE_TIME, expire);
    }

    private void resetAdvertisementsExpireTime() {
        preference.removePreference(PREFS_ADVERTISEMENT_EXPIRE_TIME);
    }

    public boolean needToRefreshContacts() {
        return System.currentTimeMillis() > preference.getLong(PREFS_CONTACTS_EXPIRE_TIME, -1);
    }

    public boolean needToRefreshAdvertisements() {
        return System.currentTimeMillis() > preference.getLong(PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
    }

    private void setAdvertisementsExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_ADVERTISEMENT_DATA; // 1 hour
        preference.putLong(PREFS_ADVERTISEMENT_EXPIRE_TIME, expire);
    }

    public boolean needToRefreshWalletBalance() {
        return System.currentTimeMillis() > preference.getLong(PREFS_WALLET_BALANCE_EXPIRE_TIME, -1);
    }

    public void setWalletBalanceExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_WALLET_BALANCE_DATA; // 1 hours
        preference.putLong(PREFS_WALLET_BALANCE_EXPIRE_TIME, expire);
    }

    public boolean needToRefreshWallet() {
        return System.currentTimeMillis() > preference.getLong(PREFS_WALLET_EXPIRE_TIME, -1);
    }

    private void setWalletExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_WALLET_DATA; // 1 hours
        preference.putLong(PREFS_WALLET_EXPIRE_TIME, expire);
    }
}