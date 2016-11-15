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
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAd;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToJSONObject;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMethod;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToUser;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.client.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.CHECK_PINCODE;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.DELETE_AD;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_AD;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_ADS;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_CONTACT;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_CONTACT_MESSAGES;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_DASHBOARD;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_RECENT_MESSAGES;
import static com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins.GET_WALLET;
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
    private final BitcoinAverage bitcoinAverage;
    private final BaseApplication baseApplication;
    private final DbManager dbManager;

    private List<Currency> currencies;
    private int retryLimit = 1;
    
    @Inject
    public DataService(DbManager dbManager, BaseApplication baseApplication, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins, BitcoinAverage bitcoinAverage)
    {
        this.baseApplication = baseApplication;
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
        this.bitcoinAverage = bitcoinAverage;
        this.dbManager = dbManager;
    }
    
    public void logout()
    {
        resetExchangeExpireTime();
        resetAdvertisementsExpireTime();
        resetMethodsExpireTime();
        resetContactsExpireTime();
    }
    
    public Observable<ContactRequest> createContact(final String adId, final String amount, final String message)
    {
        return createContactObservable(adId, amount, message, retryLimit)
                .map(new ResponseToContactRequest())
                .flatMap(new Func1<ContactRequest, Observable<ContactRequest>>()
                {
                    @Override
                    public Observable<ContactRequest> call(ContactRequest contactRequest)
                    {
                        return Observable.just(contactRequest);
                    }
                });
    }


    private Observable<Response> createContactObservable(final String adId, final String amount, final String message, final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = POST_CONTACT_CREATE + adId + "/";
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.createContact(key, nonce, signature, adId, amount, message)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return createContactObservable(adId, amount, message, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount)
    {
        return sendPinCodeMoneyObservable(pinCode, address, amount, retryLimit)
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return sendPinCodeMoneyObservable(pinCode, address, amount, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<Wallet> getWalletBalance()
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseWalletBalance(MockData.WALLET_BALANCE))
                    .flatMap(new Func1<Wallet, Observable<Wallet>>()
                    {
                        @Override
                        public Observable<Wallet> call(Wallet wallet)
                        {
                            return getWalletBitmap(wallet);
                        }
                    });
        }

        return getWalletBalanceObservable(retryLimit)
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return getWalletBalanceObservable(retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<JSONObject> validatePinCode(final String pinCode)
    {
        return validatePinCodeObservable(pinCode, retryLimit)
                .map(new ResponseToJSONObject());
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return validatePinCodeObservable(pinCode, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    private Observable<Wallet> getWalletBitmap(final Wallet wallet)
    {
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

    private Observable<Bitmap> generateBitmap(final String address)
    {
        return Observable.create(new Observable.OnSubscribe<Bitmap>()
        {
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
    
    public Observable<JSONObject> contactAction(final String contactId, final String pinCode, final ContactAction action)
    {
        return contactActionObservable(contactId, pinCode, action, retryLimit)
                .map(new ResponseToJSONObject());
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
                                if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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
                                if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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
                                if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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
                                if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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
                                if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                                    return contactActionObservable(contactId, pinCode, action, retry - 1);
                                }
                                return Observable.error(throwable); // bubble up the exception
                            }
                        });
        }

        return Observable.error(new Error("Unable to perform action on contact"));
    }
    
    public Observable<JSONObject> updateAdvertisement(final Advertisement advertisement)
    {
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

    private Observable<Response> updateAdvertisementObservable(final Advertisement advertisement, final int retry)
    {
        final String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }

        StringBuilder params = new StringBuilder();
        params.append("account_info=");
        params.append(Uri.encode(advertisement.account_info, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        params.append("&bank_name=");
        params.append(Uri.encode(advertisement.bank_name, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        params.append("&city=");
        params.append(Uri.encode(city, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        params.append("&countrycode=");
        params.append(Uri.encode(advertisement.country_code, NetworkUtils.DEFAULT_ENCODING));
        params.append("&currency=");
        params.append(Uri.encode(advertisement.currency, NetworkUtils.DEFAULT_ENCODING));
        params.append("&lat=");
        params.append(Uri.encode(String.valueOf(advertisement.lat), NetworkUtils.DEFAULT_ENCODING));
        params.append("&location_string=");
        params.append(Uri.encode(advertisement.location, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
        params.append("&lon=");
        params.append(Uri.encode(String.valueOf(advertisement.lon), NetworkUtils.DEFAULT_ENCODING));
        params.append("&max_amount=");
        params.append(Uri.encode(advertisement.max_amount, NetworkUtils.DEFAULT_ENCODING));
        params.append("&min_amount=");
        params.append(Uri.encode(advertisement.min_amount, NetworkUtils.DEFAULT_ENCODING));
        params.append("&msg=");
        params.append(Uri.encode(advertisement.message, NetworkUtils.DEFAULT_ENCODING).replace("'", "%27").replace("%20", "+"));
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
                key, nonce, signature, advertisement.ad_id, advertisement.account_info, advertisement.bank_name, city, advertisement.country_code, advertisement.currency,
                String.valueOf(advertisement.lat), advertisement.location, String.valueOf(advertisement.lon), advertisement.max_amount, advertisement.min_amount,
                advertisement.message, advertisement.price_equation, String.valueOf(advertisement.trusted_required), String.valueOf(advertisement.sms_verification_required),
                String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.visible))
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return updateAdvertisementObservable(advertisement, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement)
    {
        return createAdvertisementObservable(advertisement, retryLimit)
                .map(new ResponseToJSONObject());
    }
    
    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement, final int retry)
    {
        String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city = advertisement.city;
        }

        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = POST_AD_CREATE;
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.createAdvertisement(key, nonce, signature, advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.trade_type.name(), advertisement.online_provider,
                String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount),
                String.valueOf(advertisement.trusted_required), advertisement.message, advertisement.currency)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return createAdvertisementObservable(advertisement, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message)
    {
        return postMessageObservable(contact_id, message, retryLimit)
            .map(new ResponseToJSONObject());
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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

    public Observable<Contact> getContact(final String contact_id)
    {
        Timber.d("getContact: " + contact_id);
        
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return getContactObservable(contact_id, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<List<Message>> getRecentMessages()
    {
        return getRecentMessagesObservable(retryLimit)
                .map(new ResponseToMessages());
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return getContactMessagesObservable(contact_id, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<List<Contact>> getContacts(final DashboardType dashboardType)
    {
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return getContactsObservable(dashboardType, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<Advertisement> getAdvertisement(final String adId)
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseAdvertisement(MockData.ADVERTISEMENT_LOCAL_SELL));
        }

        return getAdvertisementObservable(adId, retryLimit)
                .map(new ResponseToAd());
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return getAdvertisementObservable(adId, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<List<Advertisement>> getAdvertisements()
    {
        return getAdvertisementsObservable(retryLimit)
                .doOnNext(new Action1<Response>()
                {
                    @Override
                    public void call(Response response)
                    {
                        setAdvertisementsExpireTime();
                    }
                })
                .map(new ResponseToAds());
    }

    private Observable<Response> getAdvertisementsObservable(final int retry)
    {
        Timber.d("getAdvertisementsObservable");
        
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_ADS;
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.getAds(key, nonce, signature)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        Timber.d("onErrorResumeNext retry: " + retry);
                        
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            Timber.d("isHttp41Error retry");
                            return getAdvertisementsObservable(retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }
    
    public Observable<JSONObject>updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible)
    {
        advertisement.visible = visible;
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
    
    public Observable<Boolean> deleteAdvertisement(final String adId)
    {
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
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return deleteAdvertisementObservable(adId, retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<Wallet> getWallet(boolean force)
    {
        return getWalletObservable(retryLimit)
                .map(new ResponseToWallet())
                .flatMap(new Func1<Wallet, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(final Wallet wallet)
                    {
                        setWalletExpireTime();
                        
                        return generateBitmap(wallet.address)
                                .map(new Func1<Bitmap, Wallet>()
                                {
                                    @Override
                                    public Wallet call(Bitmap bitmap)
                                    {
                                        wallet.qrImage = bitmap;
                                        return wallet;
                                    }
                                }).onErrorReturn(new Func1<Throwable, Wallet>()
                                {
                                    @Override
                                    public Wallet call(Throwable throwable)
                                    {
                                        return wallet;
                                    }
                                });
                    }
                });
    }

    private Observable<Response> getWalletObservable(final int retry)
    {
        final String key = AuthUtils.getHmacKey(sharedPreferences);
        final String secret = AuthUtils.getHmacSecret(sharedPreferences);
        final String nonce = NetworkUtils.generateNonce();
        final String url = GET_WALLET;
        final String signature = NetworkUtils.createSignature(url, nonce, key, secret);

        return localBitcoins.getWallet(key, nonce, signature)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(final Throwable throwable)
                    {
                        if (DataServiceUtils.isHttp41Error(throwable) || DataServiceUtils.isHttp42Error(throwable) && retry > 0) {
                            return getWalletObservable(retry - 1);
                        }
                        return Observable.error(throwable); // bubble up the exception
                    }
                });
    }

    public Observable<List<Method>> getMethods()
    {
        if(!needToRefreshMethods()) {
            return Observable.empty();
        }
        
        return localBitcoins.getOnlineProviders()
                .doOnNext(new Action1<Response>()
                {
                    @Override
                    public void call(Response response)
                    {
                        setMethodsExpireTime();
                    }
                })
                .map(new ResponseToMethod());
    }
   
    private boolean needToRefreshContacts()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_CONTACTS_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private void resetContactsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_CONTACTS_EXPIRE_TIME);
            preference.delete();
        }
    }

    private void setContactsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_CONTACTS_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_CONTACTS_DATA; // 1 hours
            preference.set(expire);
        }
    }
   
    private boolean needToRefreshAdvertisements()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private void resetExchangeExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_EXCHANGE_EXPIRE_TIME);
            preference.delete();
        }
    }

    private boolean needToRefreshMethods()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_METHODS_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private void resetMethodsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_METHODS_EXPIRE_TIME);
            preference.delete();
        }
    }

    private void setMethodsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_METHODS_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_METHODS_DATA; // 1 hours
            preference.set(expire);
        }
    }

    private void resetAdvertisementsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME);
            preference.delete();
        }
    }

    private void setAdvertisementsExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_ADVERTISEMENT_DATA; // 1 hours
            preference.set(expire);
        }
    }
    
    private void setWalletExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_WALLET_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_WALLET_DATA; // 1 hours
            preference.set(expire);
        }
    }
}
