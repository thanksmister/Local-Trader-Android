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
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
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
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONObject;

import java.util.Collections;
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
    public static final String PREFS_CONTACTS_EXPIRE_TIME = "pref_ads_expire";
    public static final String PREFS_USER = "pref_user";

    public static final int CHECK_EXCHANGE_DATA = 3 * 60 * 1000;// 3 minutes
    private static final int CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
    public static final int CHECK_ADVERTISEMENT_DATA = 15 * 60 * 1000;// 15 mintues
    public static final int CHECK_CONTACTS_DATA = 5 * 60 * 1000;// 15 mintues
    
    private final LocalBitcoins localBitcoins;
    private final SharedPreferences sharedPreferences;
    private final BitcoinAverage bitcoinAverage;
    private final BitfinexExchange bitfinexExchange;
    private final BaseApplication baseApplication;
    private final DbManager dbManager;

    private List<Currency> currencies;
    
    @Inject
    public DataService(DbManager dbManager, BaseApplication baseApplication, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins, BitcoinAverage bitcoinAverage, BitfinexExchange bitfinexExchange)
    {
        this.baseApplication = baseApplication;
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
        this.bitcoinAverage = bitcoinAverage;
        this.bitfinexExchange = bitfinexExchange;
        this.dbManager = dbManager;
    }
    
    public Observable<JSONObject> logout()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(SessionItem sessionItem)
                    {
                        resetExchangeExpireTime();
                        resetAdvertisementsExpireTime();
                        resetMethodsExpireTime();
                        
                        return localBitcoins.logOut(sessionItem.access_token())
                                .map(new ResponseToJSONObject());
                    }
                });
    }

    public Observable<Exchange> getExchange()
    {
        Timber.d("Get Exchange");
        
        /*if (!needToRefreshExchanges()) {
            return Observable.empty();
        }
*/
        return bitfinexExchange.ticker()
                .map(new ResponseBitfinexToExchange())
                .doOnNext(new Action1<Exchange>()
                {
                    @Override
                    public void call(Exchange exchange)
                    {
                        setExchangeExpireTime();
                    }
                });
    }
    
    public Observable<ContactRequest> createContact(final String adId, final String amount, final String message)
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
                .doOnNext(new Action1<List<Currency>>()
                {
                    @Override
                    public void call(List<Currency> results)
                    {
                        Timber.d("Cash Currencies.");
                        currencies = results;
                    }
                });
    }
    
    public Observable<JSONObject> updateAdvertisement(final Advertisement advertisement)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(SessionItem sessionItem)
                    {
                        return updateAdvertisementObservable(advertisement, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(updateAdvertisementObservable(advertisement, sessionItem.access_token())));

                    }
                });
    }

    private Observable<JSONObject> updateAdvertisementVisibilityObservable(final Advertisement advertisement, boolean visible, String token)
    {
        final String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city =  advertisement.city;
        }

        return localBitcoins.updateAdvertisement(advertisement.ad_id, token, String.valueOf(visible), advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.currency, String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.trusted_required),
                advertisement.message)
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

    private Observable<JSONObject> updateAdvertisementObservable(final Advertisement advertisement, String token)
    {
        final String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city =  advertisement.city;
        }

        return localBitcoins.updateAdvertisement(advertisement.ad_id, token, String.valueOf(advertisement.visible), advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.currency, String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.trusted_required),
                advertisement.message)
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

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(SessionItem sessionItem)
                    {
                        return createAdvertisementObservable(advertisement, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(createAdvertisementObservable(advertisement, sessionItem.access_token())));
                    }
                });
    }

    /*
    price_equation, lat, lon, city, location_string, countrycode, 
    currency, account_info, bank_name, msg, 
    sms_verification_required, 
    track_max_amount, require_trusted_by_advertiser, require_identification
    Optional arguments: min_amount, max_amount, opening_hours
    trade_type and online_provider
     */
    private Observable<JSONObject> createAdvertisementObservable(final Advertisement advertisement, String access_token)
    {
        String city;
        if(Strings.isBlank(advertisement.city)){
            city = advertisement.location;
        } else {
            city =  advertisement.city;
        }
        
        return localBitcoins.createAdvertisement(access_token, advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.trade_type.name(), advertisement.online_provider,
                String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount),
                String.valueOf(advertisement.trusted_required), advertisement.message, advertisement.currency)
                .map(new ResponseToJSONObject());
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message)
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

    public Observable<Contact> getContact(final String contactID)
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
                    public Observable<Contact> call(final SessionItem sessionItem)
                    {
                        return localBitcoins.getContact(contactID, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.getContact(contactID, sessionItem.access_token())))
                                .map(new ResponseToContact())
                                .flatMap(new Func1<Contact, Observable<? extends Contact>>()
                                {
                                    @Override
                                    public Observable<? extends Contact> call(final Contact contact)
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

    public Observable<List<Contact>> getContacts(final DashboardType dashboardType, boolean force)
    {
        if(!needToRefreshContacts() && !force) {
            return Observable.empty();
        }

        Timber.d("Get Contacts");
        
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseContacts(MockData.DASHBOARD));
        }

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(final SessionItem sessionItem)
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

                                            setContactsExpireTime();

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

                                            setContactsExpireTime();

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

                                            setContactsExpireTime();

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

                                            setContactsExpireTime();

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
                                        contact.messageCount = messages.size();
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
        if(Constants.USE_MOCK_DATA) {
            List<Advertisement> list = Collections.emptyList();
            return Observable.just(Parser.parseAdvertisements(MockData.ADVERTISEMENT_LIST_SUCCESS));
        }
        
        if(!needToRefreshAdvertisements() && !force) {
            return Observable.empty();
        }

        return getAdvertisementsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementsObservable()));
    }
    
    public Observable<JSONObject>updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible)
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<JSONObject>>()
                {
                    @Override
                    public Observable<JSONObject> call(final SessionItem sessionItem)
                    {
                        return updateAdvertisementVisibilityObservable(advertisement, visible, sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(updateAdvertisementVisibilityObservable(advertisement, visible, sessionItem.access_token())));
                    }
                });
    }
    
    public Observable<Boolean> deleteAdvertisement(final String adId)
    {
        Timber.d("Delete Ad");
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
        Timber.d("Get Wallet");

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(final SessionItem sessionItem)
                    {
                        return localBitcoins.getWallet(sessionItem.access_token())
                                .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.getWallet(sessionItem.access_token())))
                                .map(new ResponseToWallet())
                                .flatMap(new Func1<Wallet, Observable<Wallet>>()
                                {
                                    @Override
                                    public Observable<Wallet> call(final Wallet wallet)
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
        
        Timber.d("Get Methods");
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

    public Observable<List<Method>> getMethods(String countryCode)
    {
        Timber.d("Get Methods");
        return localBitcoins.getOnlineProviders(countryCode)
                .map(new ResponseToMethod());
    }
    
    // ----- Private

    public Observable<SessionItem> getTokens()
    {
        Timber.d("Get Tokens");
        
        return dbManager.getTokens();
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
                    return refreshTokens()
                            .flatMap(new Func1<String, Observable<? extends T>>() {
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
        Timber.d("Refresh Tokens");
        
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<String>>()
                {
                    @Override
                    public Observable<String> call(SessionItem sessionItem)
                    {
                        Timber.d("Refreshing Token Using: " + sessionItem.refresh_token());

                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize())
                                .flatMap(new Func1<Authorization, Observable<? extends String>>()
                                {
                                    @Override
                                    public Observable<? extends String> call(Authorization authorization)
                                    {
                                        Timber.d("New Access tokens: " + authorization.access_token);
                                        dbManager.updateTokens(authorization);
                                        return Observable.just(authorization.access_token);
                                    }
                                });
                    }
                });
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
