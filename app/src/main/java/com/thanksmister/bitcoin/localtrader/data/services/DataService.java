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
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAd;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
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

    public Observable<Authorization> getAuthorization(String code)
    {
        return localBitcoins.getAuthorization("authorization_code", code, baseApplication.getString(R.string.lbc_access_key), baseApplication.getString(R.string.lbc_access_secret))
                .map(new ResponseToAuthorize());
    }

    private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed)
    {
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

    private Observable<String> refreshTokens()
    {
        Timber.d("Refresh Tokens");
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
    
    public Observable<ContactRequest> createContact(final String adId, final String amount, final String message)
    {
        return createContactObservable(adId, amount, message)
                .onErrorResumeNext(createContactObservable(adId, amount, message))
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


    private Observable<Response> createContactObservable(final String adId, final String amount, final String message)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.createContact(accessToken, adId, amount, message);
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount)
    {
        return sendPinCodeMoneyObservable(pinCode, address, amount)
                .onErrorResumeNext(sendPinCodeMoneyObservable(pinCode, address, amount))
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

        return getWalletBalanceObservable()
                .onErrorResumeNext(getWalletBalanceObservable())
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

    private Observable<Response> getWalletBalanceObservable()
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getWalletBalance(accessToken);
    }

    public Observable<JSONObject> validatePinCode(final String pinCode)
    {
        return validatePinCodeObservable(pinCode)
                .onErrorResumeNext(validatePinCodeObservable(pinCode))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> validatePinCodeObservable(final String pinCode)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.checkPinCode(accessToken, pinCode);
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
        return contactActionObservable(contactId, pinCode, action)
                .onErrorResumeNext(contactActionObservable(contactId, pinCode, action))
                .map(new ResponseToJSONObject());
    }
    
    private Observable<Response> contactActionObservable(final String contactId, final String pinCode, final ContactAction action)
    {
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
    
    public Observable<JSONObject> updateAdvertisement(final Advertisement advertisement)
    {
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

    private Observable<Response> updateAdvertisementObservable(final Advertisement advertisement)
    {
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
                String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.visible));
    }

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement)
    {
        return createAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(createAdvertisementObservable(advertisement)))
                .map(new ResponseToJSONObject());
    }
    
    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement)
    {
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
                String.valueOf(advertisement.trusted_required), advertisement.message, advertisement.currency);
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message)
    {
        return postMessageObservable(contact_id, message)
                .onErrorResumeNext(refreshTokenAndRetry(postMessageObservable(contact_id, message)))
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> postMessageObservable(final String contact_id, final String message)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.contactMessagePost(accessToken, contact_id, message);
    }

    public Observable<User> getMyself(String accessToken)
    {
        return localBitcoins.getMyself(accessToken)
                .map(new ResponseToUser());
    }

    public Observable<Contact> getContact(final String contact_id)
    {
        Timber.d("getContact: " + contact_id);
        
        return getContactObservable(contact_id)
                .onErrorResumeNext(refreshTokenAndRetry(getContactObservable(contact_id)))
                .map(new ResponseToContact())
                .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                    @Override
                    public Observable<? extends Contact> call(final Contact contact)
                    {
                        return getContactMessagesObservable(contact.contact_id)
                                .onErrorResumeNext(getContactMessagesObservable(contact_id))
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
        Timber.d("getContactObservable: " + contact_id);
        
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getContact(accessToken, contact_id);
    }

    public Observable<List<Message>> getRecentMessages()
    {
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

    public Observable<List<Contact>> getContacts(final DashboardType dashboardType)
    {
        switch (dashboardType){
            case RELEASED:
            case CLOSED:
            case CANCELED:
                return getContactsObservable(dashboardType)
                        .onErrorResumeNext(getContactsObservable(dashboardType))
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
                        .onErrorResumeNext(getContactsObservable())
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
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseAdvertisement(MockData.ADVERTISEMENT_LOCAL_SELL));
        }

        return getAdvertisementObservable(adId)
                .onErrorResumeNext(getAdvertisementObservable(adId))
                .map(new ResponseToAd());
    }

    private Observable<Response> getAdvertisementObservable(final String adId)
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getAdvertisement(accessToken, adId);
    }

    public Observable<List<Advertisement>> getAdvertisements()
    {
        return getAdvertisementsObservable()
                .onErrorResumeNext(getAdvertisementsObservable())
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

    private Observable<Response> getAdvertisementsObservable()
    {
        Timber.d("getAdvertisementsObservable");
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getAds(accessToken);
    }
    
    public Observable<JSONObject>updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible)
    {
        advertisement.visible = visible;
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
        return deleteAdvertisementObservable(adId)
                .onErrorResumeNext(deleteAdvertisementObservable(adId))
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
                .onErrorResumeNext(getWalletObservable())
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

    private Observable<Response> getWalletObservable()
    {
        final String accessToken = AuthUtils.getAccessToken(sharedPreferences);
        return localBitcoins.getWallet(accessToken);
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