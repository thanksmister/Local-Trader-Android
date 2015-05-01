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

package com.thanksmister.bitcoin.localtrader.data.services;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Bitfinex;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseBitfinexToExchange;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAd;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToCurrencyList;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToJSONObject;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMethod;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToUser;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.Db;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.client.Response;
import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;


@Singleton
public class DataService
{
    private static final String PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire";
    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_ADVERTISEMENT_EXPIRE_TIME = "pref_ads_expire";
    public static final String PREFS_USER = "pref_user";

    public static final int CHECK_EXCHANGE_DATA = 3 * 60 * 1000;// 3 minutes
    private static final int CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
    public static final int CHECK_ADVERTISEMENT_DATA = 15 * 60 * 1000;// 15 mintues
    
    private final LocalBitcoins localBitcoins;
    private final SharedPreferences sharedPreferences;
    private final BitcoinAverage bitcoinAverage;
    private final BitfinexExchange bitfinexExchange;
    private final BaseApplication baseApplication;

    private List<Currency> currencies;
    private SqlBrite db;
    
    @Inject
    public DataService(SqlBrite db, BaseApplication baseApplication, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins, BitcoinAverage bitcoinAverage, BitfinexExchange bitfinexExchange)
    {
        this.baseApplication = baseApplication;
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
        this.bitcoinAverage = bitcoinAverage;
        this.bitfinexExchange = bitfinexExchange;
        this.db = db;
    }
    
    public void reset()
    {
        resetExchangeExpireTime();
        resetAdvertisementsExpireTime();
        resetMethodsExpireTime();
    }

    public Observable<Exchange> getExchange()
    {
        if (!needToRefreshExchanges()) {
            return Observable.empty();
        }

        return bitfinexExchange.ticker()
                .doOnNext(new Action1<Bitfinex>()
                {
                    @Override
                    public void call(Bitfinex bitfinex)
                    {
                        setExchangeExpireTime();
                    }
                })
                .map(new ResponseBitfinexToExchange());
    }
    
    public Observable<ContactRequest> createContact(String adId, String amount, String message)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<ContactRequest>>()
                {
                    @Override
                    public Observable<ContactRequest> call(SessionItem sessionItem)
                    {
                        return localBitcoins.createContact(adId, sessionItem.access_token(), amount, message)
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
                });
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Boolean>>()
                {
                    @Override
                    public Observable<Boolean> call(SessionItem sessionItem)
                    {
                        return localBitcoins.walletSendPin(pinCode, address, amount, sessionItem.access_token())
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

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(SessionItem sessionItem)
                    {
                        return localBitcoins.getWalletBalance(sessionItem.access_token())
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
                });
    }

    public Observable<JSONObject> validatePinCode(final String pinCode)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(SessionItem sessionItem)
                    {
                        return localBitcoins.checkPinCode(pinCode, sessionItem.access_token())
                                .map(new ResponseToJSONObject());
                    }
                });
    }

    private Observable<Wallet> getWalletBitmap(final Wallet wallet)
    {
        return generateBitmap(wallet.address.address)
                .map(bitmap -> {
                    wallet.qrImage = bitmap;
                    return wallet;
                });
    }

    private Observable<Bitmap> generateBitmap(final String address)
    {
        return Observable.create((Subscriber<? super Bitmap> subscriber) -> {
            try {
                subscriber.onNext(WalletUtils.encodeAsBitmap(address, baseApplication.getApplicationContext()));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }


    public Observable<JSONObject> contactAction(String contactId, String pinCode, ContactAction action)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(SessionItem sessionItem)
                    {
                        switch (action) {
                            case RELEASE:
                                return localBitcoins.releaseContactPinCode(contactId, pinCode, sessionItem.access_token())
                                        .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.releaseContactPinCode(contactId, pinCode, sessionItem.access_token())))
                                        .map(new ResponseToJSONObject());
                            case CANCEL:
                                return localBitcoins.contactCancel(contactId, sessionItem.access_token())
                                        .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.contactCancel(contactId, sessionItem.access_token())))
                                        .map(new ResponseToJSONObject());
                            case DISPUTE:
                                return localBitcoins.contactDispute(contactId, sessionItem.access_token())
                                        .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.contactDispute(contactId, sessionItem.access_token())))
                                        .map(new ResponseToJSONObject());
                            case PAID:
                                return localBitcoins.markAsPaid(contactId, sessionItem.access_token())
                                        .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.markAsPaid(contactId, sessionItem.access_token())))
                                        .map(new ResponseToJSONObject());
                            case FUND:
                                return localBitcoins.contactFund(contactId, sessionItem.access_token())
                                        .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.contactFund(contactId, sessionItem.access_token())))
                                        .map(new ResponseToJSONObject());
                        }

                        return Observable.error(new Error("Unable to perform action on contact"));
                    }
                });
    }

    public Observable<List<Currency>> getCurrencies()
    {
        if(currencies != null){
            return Observable.just(currencies);
        }

        return bitcoinAverage.tickers()
                .map(new ResponseToCurrencyList())
                .doOnNext(new Action1<List<Currency>>() {
                    @Override
                    public void call(List<Currency> results) {
                        Timber.d("Cash Currencies.");
                        currencies = results;
                    }
                });
    }

    public Observable<Advertisement> createAdvertisement(Advertisement advertisement)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Advertisement>>()
                {
                    @Override
                    public Observable<Advertisement> call(SessionItem sessionItem)
                    {
                        return createAdvertisementObservable(advertisement, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(createAdvertisementObservable(advertisement, sessionItem.access_token())))
                                .map(new ResponseToAd());
                    }
                });
    }

    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement, String access_token)
    {
        return localBitcoins.createAdvertisement(advertisement.ad_id, access_token, advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.trade_type.name(), advertisement.online_provider,
                String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                advertisement.city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount),
                String.valueOf(advertisement.trusted_required), advertisement.message);
    }

    public Observable<JSONObject> postMessage(String contact_id, final String message)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(SessionItem sessionItem)
                    {
                        return localBitcoins.contactMessagePost(contact_id, sessionItem.access_token(), message)
                                .map(new ResponseToJSONObject());
                    }
                });

    }

    public Observable<User> getMyself(String token)
    {
        return localBitcoins.getMyself(token)
                .map(new ResponseToUser());
    }

   

    public Observable<Contact> getContact(String contactID)
    {
        if(Constants.USE_MOCK_DATA) {
            Contact contact = Parser.parseContact(MockData.CONTACT_LOCAL_SELL);
            assert contact != null;
            contact.messages = Parser.parseMessages(MockData.MESSAGES);
            return Observable.just(contact);
        }

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Contact>>()
                {
                    @Override
                    public Observable<Contact> call(SessionItem sessionItem)
                    {
                        return localBitcoins.getContact(contactID, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.getContact(contactID, sessionItem.access_token())))
                                .map(new ResponseToContact())
                                .flatMap(new Func1<Contact, Observable<? extends Contact>>()
                                {
                                    @Override
                                    public Observable<? extends Contact> call(Contact contact)
                                    {
                                        return localBitcoins.contactMessages(contact.contact_id, sessionItem.access_token())
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
                });
    }

    public Observable<List<Contact>> getContacts(DashboardType dashboardType)
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseContacts(MockData.DASHBOARD));
        }

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(SessionItem sessionItem)
                    {
                        if (dashboardType == DashboardType.RELEASED) {
                            return localBitcoins.getDashboard(sessionItem.access_token(), "released")
                                    .map(new ResponseToContacts())
                                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                                    {
                                        @Override
                                        public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                        {

                                            if (contacts.isEmpty()) {
                                                return Observable.just(contacts);
                                            }

                                            return getContactsMessages(contacts, sessionItem.access_token());
                                        }
                                    });
                        } else if (dashboardType == DashboardType.CANCELED) {
                            return localBitcoins.getDashboard(sessionItem.access_token(), "canceled")
                                    .map(new ResponseToContacts())
                                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                                    {
                                        @Override
                                        public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                        {
                                            if (contacts.isEmpty()) {
                                                return Observable.just(contacts);
                                            }
                                            return getContactsMessages(contacts, sessionItem.access_token());
                                        }
                                    });
                        } else if (dashboardType == DashboardType.CLOSED) {
                            return localBitcoins.getDashboard(sessionItem.access_token(), "closed")
                                    .map(new ResponseToContacts())
                                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                                    {
                                        @Override
                                        public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                        {
                                            if (contacts.isEmpty()) {
                                                return Observable.just(contacts);
                                            }
                                            return getContactsMessages(contacts, sessionItem.access_token());
                                        }
                                    });
                        } else {
                            return localBitcoins.getDashboard(sessionItem.access_token())
                                    .map(new ResponseToContacts())
                                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>()
                                    {
                                        @Override
                                        public Observable<? extends List<Contact>> call(final List<Contact> contacts)
                                        {
                                            if (contacts.isEmpty()) {
                                                return Observable.just(contacts);
                                            }
                                            return getContactsMessages(contacts, sessionItem.access_token());
                                        }
                                    });
                        }
                    }
                });
    }

    private Observable<List<Contact>> getContactsMessages(final List<Contact> contacts, final String access_token)
    {
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>()
                {
                    @Override
                    public Observable<? extends List<Contact>> call(final Contact contact)
                    {
                        return localBitcoins.contactMessages(contact.contact_id, access_token)
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, List<Contact>>()
                                {
                                    @Override
                                    public List<Contact> call(List<Message> messages)
                                    {
                                        contact.messages = messages;
                                        return contacts;
                                    }
                                });
                    }
                }).toBlocking().last());
    }

    public Observable<Advertisement> getAdvertisement(final String adId)
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseAdvertisement(MockData.ADVERTISEMENT_LOCAL_SELL));
        }

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Advertisement>>()
                {
                    @Override
                    public Observable<Advertisement> call(SessionItem sessionItem)
                    {
                        return localBitcoins.getAdvertisement(adId, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.getAdvertisement(adId, sessionItem.access_token())))
                                .map(new ResponseToAd());
                    }
                });
    }

    public Observable<List<Advertisement>> getAdvertisements(boolean force)
    {
        if(!needToRefreshAdvertisements() && !force) {
            return Observable.empty();
        }

        return getAdvertisementsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementsObservable()));
    }

    public Observable<Boolean> updateAdvertisementVisibility(final AdvertisementItem advertisement, final boolean visible)
    {
        return updateAdvertisementVisibilityObservable(advertisement, visible)
                .onErrorResumeNext(refreshTokenAndRetry(updateAdvertisementVisibilityObservable(advertisement, visible)));
    }

    private Observable<Boolean> updateAdvertisementVisibilityObservable(final AdvertisementItem advertisement, boolean visible)
    {
        String city;
        if(Strings.isBlank(advertisement.city())){
            city = advertisement.location_string();
        } else {
            city =  advertisement.city();
        }

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Boolean>>()
                {
                    @Override
                    public Observable<Boolean> call(SessionItem sessionItem)
                    {
                        return localBitcoins.updateAdvertisement(advertisement.ad_id(), sessionItem.access_token(), String.valueOf(visible), advertisement.min_amount(),
                                advertisement.max_amount(), advertisement.price_equation(), advertisement.currency(), String.valueOf(advertisement.lat()), String.valueOf(advertisement.lon()),
                                city, advertisement.location_string(), advertisement.country_code(), advertisement.account_info(), advertisement.bank_name(),
                                String.valueOf(advertisement.sms_verification_required()), String.valueOf(advertisement.track_max_amount()), String.valueOf(advertisement.trusted_required()),
                                advertisement.message())
                                .map(new ResponseToJSONObject())
                                .flatMap(new Func1<JSONObject, Observable<Boolean>>()
                                {
                                    @Override
                                    public Observable<Boolean> call(JSONObject jsonObject)
                                    {
                                        if (Parser.containsError(jsonObject)) {
                                            throw new Error("Error updating advertisement visibility");
                                        }

                                        return Observable.just(true);
                                    }
                                });

                    }
                });
    }

    public Observable<Boolean> deleteAdvertisement(final String adId)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Boolean>>()
                {
                    @Override
                    public Observable<Boolean> call(SessionItem sessionItem)
                    {
                        return localBitcoins.deleteAdvertisement(adId, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.deleteAdvertisement(adId, sessionItem.access_token())))
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
                });
    }

    private Observable<List<Advertisement>> getAdvertisementsObservable()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Advertisement>>>()
                {
                    @Override
                    public Observable<List<Advertisement>> call(SessionItem sessionItem)
                    {
                        Timber.d("Access Token: " + sessionItem.access_token());

                        return localBitcoins.getAds(sessionItem.access_token())
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
                });
    }

    public Observable<Wallet> getWallet()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(SessionItem sessionItem)
                    {
                        return localBitcoins.getWallet(sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.getWallet(sessionItem.access_token())))
                                .map(new ResponseToWallet())
                                .flatMap(new Func1<Wallet, Observable<Wallet>>()
                                {
                                    @Override
                                    public Observable<Wallet> call(Wallet wallet)
                                    {
                                        return generateBitmap(wallet.address.address)
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
                        Timber.d("Methods Expire");
                    }
                })
                .map(new ResponseToMethod());
                
        
    }
    
    // ----- Private

    public Observable<SessionItem> getTokens()
    {
        return db.createQuery(SessionItem.TABLE, SessionItem.QUERY)
                .map(SessionItem.MAP);
    }

    public Observable<Authorization> getAuthorization(String code)
    {
        return localBitcoins.getAuthorization("authorization_code", code, Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                .map(new ResponseToAuthorize());
    }

    private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed)
    {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                // Here check if the error thrown really is a 401
                if (DataServiceUtils.isHttp403Error(throwable)) {
                    return refreshTokens().flatMap(new Func1<String, Observable<? extends T>>() {
                        @Override
                        public Observable<? extends T> call(String token)
                        {
                            return toBeResumed;
                        }
                    });
                }
                // re-throw this error because it's not recoverable from here
                return Observable.error(throwable);
            }
        };
    }

    private Observable<String> refreshTokens()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<String>>()
                {
                    @Override
                    public Observable<String> call(SessionItem sessionItem)
                    {
                        Timber.d("Refresh Token: " + sessionItem.refresh_token());

                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize())
                                .flatMap(new Func1<Authorization, Observable<? extends String>>()
                                {
                                    @Override
                                    public Observable<? extends String> call(Authorization authorization)
                                    {
                                        Timber.d("New Access tokens: " + authorization.access_token);
                                        updateTokens(authorization);
                                        return Observable.just(authorization.access_token);
                                    }
                                });
                    }
                });
    }

    private void updateTokens(Authorization authorization)
    {
        SessionItem.Builder builder = new SessionItem.Builder()
                .access_token(authorization.access_token)
                .refresh_token(authorization.refresh_token);

        Cursor cursor = db.query(SessionItem.QUERY);
        if(cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, SessionItem.ID);
                db.update(SessionItem.TABLE, builder.build(), SessionItem.ID + " = ?", String.valueOf(id));
            } finally {
                cursor.close();
            }
        } else {
            db.insert(ContactItem.TABLE, builder.build());
        }
    }

    private boolean needToRefreshAdvertisements()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private boolean needToRefreshExchanges()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_EXCHANGE_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
        }
    }

    private void setExchangeExpireTime()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_EXCHANGE_EXPIRE_TIME, -1);
            long expire = System.currentTimeMillis() + CHECK_EXCHANGE_DATA; // 1 hours
            preference.set(expire);
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
    
    /*private class QrCodeTask extends AsyncTask<Object, Void, Object[]>
    {
        private Context context;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        protected Object[] doInBackground(Object... params)
        {
            Wallet wallet = (Wallet) params[0];
            context = (Context) params[1];
            Bitmap qrCode = null;

            wallet.qrImage = WalletUtils.encodeAsBitmap(wallet.address.address, context.getApplicationContext());

            return new Object[]{wallet};
        }

        protected void onPostExecute(Object[] result)
        {
            Wallet wallet = (Wallet) result[0];
            //updateWalletQrCode(wallet.id, wallet.qrImage, context);
        }
    }*/
}
