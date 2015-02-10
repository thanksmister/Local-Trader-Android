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

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.BitstampExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.DefaultExchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseBitfinexToExchange;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseBitstampToExchange;
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
import com.thanksmister.bitcoin.localtrader.data.database.CupboardProvider;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.IntPreference;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.data.rx.EndObserver;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import nl.qbusict.cupboard.ProviderCompartment;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

@Singleton
public class DataService
{
    // TODO add "fields" to all API calls to reduce info
    
    public static final String PREFS_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_TOKENS_EXPIRE_TIME = "pref_tokens_expire";
    public static final String PREFS_USER = "pref_user";
    public static final String PREFS_LOGGED_IN = "pref_logged_in";
    public static final String PREFS_SELECTED_EXCHANGE = "selected_exchange";

    public static final int CHECK_EXCHANGE_DATA = 5 * 60 * 1000;// 5 minutes

    public static final String USD = "USD";

    private final LocalBitcoins localBitcoins;
    private final SharedPreferences sharedPreferences;
    private final BitcoinAverage bitcoinAverage;
    private BaseApplication application;
    
    private final BitstampExchange bitstampExchange;
    private final BitfinexExchange bitfinexExchange;

    PublishSubject<User> userRequest;
    PublishSubject<Dashboard> dashboardPublishSubject;
    PublishSubject<List<Advertisement>> advertisementsPublishSubject;
    PublishSubject<Advertisement> advertisementPublishSubject;
    PublishSubject<Advertisement> advertisementEditPublishSubject;
    PublishSubject<Wallet> walletPublishSubject;
    PublishSubject<Wallet> walletBalancePublishSubject;
    PublishSubject<Bitmap> requestPublishSubject;
    PublishSubject<ContactRequest> contactRequestPublishSubject;
    PublishSubject<Contact> contactPublishSubject;
    PublishSubject<List<Contact>> contactsPublishSubject;
    PublishSubject<Object> contactActionPublishSubject;
    PublishSubject<List<Currency>> currencyRequest;
    PublishSubject<Authorization> authorizationRequest;
    PublishSubject<User> myselfRequest;
    
    PublishSubject<String> pinCodePublishSubject;
    PublishSubject<String> sendMoneyPublishSubject;
   
    private Dashboard dashboardInfo;
    private Wallet wallet;
    private Wallet walletBalance;
    private List<Advertisement> advertisements;
    private List<Method> methods;
    private List<Method> paymentMethods;
    private HashMap<DashboardType, List<Contact>> contacts = new HashMap<DashboardType, List<Contact>>();
    
    @Inject
    public DataService(BaseApplication application, SharedPreferences sharedPreferences, LocalBitcoins localBitcoins, BitstampExchange bitstampExchange, BitcoinAverage bitcoinAverage, BitfinexExchange bitfinexExchange)
    {
        this.application = application;
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
        this.bitstampExchange = bitstampExchange;
        this.bitfinexExchange = bitfinexExchange;
        this.bitcoinAverage = bitcoinAverage;
    }

    public Subscription getContact(final Observer<Contact> observer, final String contact_id)
    {
        if(contactPublishSubject != null) {
            return contactPublishSubject.subscribe(observer);
        }

        contactPublishSubject = PublishSubject.create();
        contactPublishSubject.subscribe(new EndObserver<Contact>() {
            @Override
            public void onEnd() {
                contactPublishSubject = null;
            }

            @Override
            public void onNext(Contact contact) {
            }
        });

        Subscription subscription = contactPublishSubject.subscribe(observer);
        getContactObservable(contact_id)
                .onErrorResumeNext(refreshTokenAndRetry(getContactObservable(contact_id)))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactPublishSubject);

        return subscription;
    }

    public Subscription getUser(final Observer<User> observer, final String username)
    {
        if(userRequest != null) {
            return userRequest.subscribe(observer);
        }

        userRequest = PublishSubject.create();
        userRequest.subscribe(new EndObserver<User>() {
            @Override
            public void onEnd() {
                userRequest = null;
            }

            @Override
            public void onNext(User user) {
            }
        });

        Subscription subscription = userRequest.subscribe(observer);
        getUserObserver(username)
            .subscribe(userRequest);

        return subscription;
    }

    public Subscription getDashboardByType(final Observer<List<Contact>> observer, final DashboardType dashboardType)
    {
        Timber.d("getDashboardByType: " + dashboardType.name());
        
        if(contacts.containsKey(dashboardType)) {
            observer.onNext(contacts.get(dashboardType));
        }
        
        if(contactsPublishSubject != null) {
            return contactsPublishSubject.subscribe(observer);
        }

        contactsPublishSubject = PublishSubject.create();
        contactsPublishSubject.subscribe(new Observer<List<Contact>>() {
            @Override
            public void onCompleted(){
                contactsPublishSubject = null;
            }

            @Override
            public void onError(Throwable e) {
                if(e != null) 
                    Timber.e("Contacts Dashboard error: " + e);

                contactsPublishSubject = null;
            }

            @Override
            public void onNext(List<Contact> results) {
                contacts.put(dashboardType, results); // poor man's cache
            }
        });

        Subscription subscription = contactsPublishSubject.subscribe(observer);
        getDashboardContacts(dashboardType)
                .onErrorResumeNext(refreshTokenAndRetry(getDashboardContacts(dashboardType)))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactsPublishSubject);

        return subscription;
    }

    public Subscription getDashboardInfo(Observer<Dashboard> observer)
    {
        if(dashboardInfo != null) {
            observer.onNext(dashboardInfo);
        }
        
        if(dashboardPublishSubject != null) {
            return dashboardPublishSubject.subscribe(observer); // join it
        }

        dashboardPublishSubject = PublishSubject.create();
        dashboardPublishSubject.subscribe(new Observer<Dashboard>(){
            @Override
            public void onCompleted(){
                dashboardPublishSubject = null;
            }

            @Override
            public void onError(Throwable e){
            }

            @Override
            public void onNext(Dashboard dashboard) {
                dashboardInfo = dashboard; // cache it for a bit
            }
        });

        Subscription subscription = dashboardPublishSubject.subscribe(observer);
        getDashboardObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getDashboardObservable()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dashboardPublishSubject);

        return subscription;
    }

    // TODO only get minimal fields for advertisements
    public Subscription getAdvertisements(Observer<List<Advertisement>> observer)
    {
        if(advertisements != null) {
            observer.onNext(advertisements);
        }
        
        if(advertisementsPublishSubject != null) {
            return advertisementsPublishSubject.subscribe(observer); // join it
        }

        advertisementsPublishSubject = PublishSubject.create();
        advertisementsPublishSubject.subscribe(new Observer<List<Advertisement>>() {
            @Override
            public void onCompleted() {
                advertisementsPublishSubject = null;
            }

            @Override
            public void onError(Throwable e) {
                if(e != null)
                    Timber.e("Advertisement error: " + e);
                // TODO handle network or authentication errors
            }

            @Override
            public void onNext(List<Advertisement> results) {
                advertisements = results; // cache it for a bit
            }
        });

        Subscription subscription = advertisementsPublishSubject.subscribe(observer);
        getAdvertisementsObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementsObservable()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(advertisementsPublishSubject);

        return subscription;
    }

    public Subscription getWallet(Observer<Wallet> observer)
    {
        if(wallet != null) {
            observer.onNext(wallet); // return account info then refresh
        }
        
        if(walletPublishSubject != null) {
            return walletPublishSubject.subscribe(observer); // join it
        }

        walletPublishSubject = PublishSubject.create();
        walletPublishSubject.subscribe(new Observer<Wallet>() {
            @Override
            public void onCompleted()
            {
                walletPublishSubject = null;
            }

            @Override
            public void onError(Throwable e){
                Timber.e("Accounts Dashboard error: " + e);
            }

            @Override
            public void onNext(Wallet results) {
                wallet = results; // cache it for a bit
            }
        });

        Subscription subscription = walletPublishSubject.subscribe(observer);
        getWalletObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletObservable()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(walletPublishSubject);

        return subscription;
    }

    public Subscription getWalletBalance(final Observer<Wallet> observer)
    {
        if(walletBalance != null) {
            observer.onNext(walletBalance); // return walletBalance right away then refresh
            // TODO lets set some timers on updates
        }
        
        if(walletBalancePublishSubject != null) {
            return walletBalancePublishSubject.subscribe(observer);
        }

        walletBalancePublishSubject = PublishSubject.create();
        walletBalancePublishSubject.subscribe(new EndObserver<Wallet>() {
            @Override
            public void onEnd() {
                walletBalancePublishSubject = null;
            }

            @Override
            public void onNext(Wallet data) {
                walletBalance = data; // store walletBalance
            }
        });

        Subscription subscription = walletBalancePublishSubject.subscribe(observer);
        getWalletBalanceObservable()
                .onErrorResumeNext(refreshTokenAndRetry(getWalletBalanceObservable()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(walletBalancePublishSubject);

        return subscription;
    }

    public Subscription generateRequestCode(final Observer<Bitmap> observer, String address, String amount)
    {
        if(requestPublishSubject != null) {
            return requestPublishSubject.subscribe(observer);
        }

        requestPublishSubject = PublishSubject.create();
        requestPublishSubject.subscribe(new EndObserver<Bitmap>() {
            @Override
            public void onEnd() {
                requestPublishSubject = null;
            }

            @Override
            public void onNext(Bitmap data) {
                // noting
            }
        });

        Subscription subscription = requestPublishSubject.subscribe(observer);
        generateBitmap(address, amount)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(requestPublishSubject);

        return subscription;
    }

    public Subscription contactAction(Observer<Object> observer, String contact_id, ContactAction action)
    {
        if(contactActionPublishSubject != null) {
            return contactActionPublishSubject.subscribe(observer); // join it
        }

        contactActionPublishSubject = PublishSubject.create();
        contactActionPublishSubject.subscribe(new Observer<Object>(){
            @Override
            public void onCompleted(){
                contactActionPublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof RetrofitError) {
                    RetroError retroError;
                    if (((RetrofitError) throwable).isNetworkError()) {
                        retroError = new RetroError(application.getString(R.string.error_generic_server_down), 404);
                        contactActionPublishSubject.onError(retroError);
                    } else {
                        RetrofitError error = (RetrofitError) throwable;
                        retroError = Parser.parseRetrofitError(error);
                        contactActionPublishSubject.onError(retroError);
                    }
                }
            }

            @Override
            public void onNext(Object o){
            }
        });

        Subscription subscription = contactActionPublishSubject.subscribe(observer);
        switch (action) {
            case RELEASE:
                releaseContactObservable(contact_id)
                        .onErrorResumeNext(refreshTokenAndRetry(releaseContactObservable(contact_id)))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(contactActionPublishSubject);
                break;
            case CANCEL:
                cancelContactObservable(contact_id)
                        .onErrorResumeNext(refreshTokenAndRetry(cancelContactObservable(contact_id)))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(contactActionPublishSubject);
                break;
            case DISPUTE:
                disputeContactObservable(contact_id)
                        .onErrorResumeNext(refreshTokenAndRetry(disputeContactObservable(contact_id)))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(contactActionPublishSubject);
                break;
            case PAID:
                markPaidContactObservable(contact_id)
                        .onErrorResumeNext(refreshTokenAndRetry(markPaidContactObservable(contact_id)))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(contactActionPublishSubject);
                break;
            case FUND:
                fundContactObservable(contact_id)
                        .onErrorResumeNext(refreshTokenAndRetry(fundContactObservable(contact_id)))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(contactActionPublishSubject);
                break;
        }

        return subscription;
    }
    
    /**
     * Update and then fetch new advertisement from service
     */
    public Subscription updateAdvertisement(Observer<Advertisement> observer, final Advertisement advertisement)
    {
        if(advertisementEditPublishSubject != null) {
            return advertisementEditPublishSubject.subscribe(observer); // join it
        }

        advertisementEditPublishSubject = PublishSubject.create();
        advertisementEditPublishSubject.subscribe(new EndObserver<Advertisement>(){
            @Override
            public void onEnd() {
                advertisementEditPublishSubject = null;
            }

            @Override
            public void onNext(Advertisement o){
            }
        });

        Subscription subscription = advertisementEditPublishSubject.subscribe(observer);
        updateAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(updateAdvertisementObservable(advertisement)))
                .map(new ResponseToJSONObject())
                .map(new Func1<JSONObject, String>() {
                    @Override
                    public String call(JSONObject response) {
                        // Check JSON response for errors
                        return advertisement.ad_id;
                    }
                })
                .flatMap(new Func1<String, Observable<Advertisement>>() {
                    @Override
                    public Observable<Advertisement> call(String ad_id) {
                        return getAdvertisementObservable(ad_id); // refresh updated advertisement
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(advertisementEditPublishSubject);
        
        return subscription;
    }

    public Subscription createAdvertisement(Observer<Advertisement> observer, final Advertisement advertisement)
    {
        if(advertisementPublishSubject != null) {
            return advertisementPublishSubject.subscribe(observer); // join it
        }

        advertisementPublishSubject = PublishSubject.create();
        advertisementPublishSubject.subscribe(new Observer<Advertisement>(){
            @Override
            public void onCompleted(){
                advertisementPublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable){
                if (throwable instanceof RetrofitError) {
                    RetroError retroError;
                    if (((RetrofitError) throwable).isNetworkError()) {
                        retroError = new RetroError(application.getString(R.string.error_generic_server_down), 404);
                        advertisementPublishSubject.onError(retroError);
                    } else {
                        RetrofitError error = (RetrofitError) throwable;
                        retroError = Parser.parseRetrofitError(error);
                        advertisementPublishSubject.onError(retroError);
                    }
                }
            }

            @Override
            public void onNext(Advertisement o){
            }
        });

        Subscription subscription = advertisementPublishSubject.subscribe(observer);
        // TODO add type for online or local
        createAdvertisementObservable(advertisement)
                .onErrorResumeNext(refreshTokenAndRetry(createAdvertisementObservable(advertisement)))
                .map(response -> advertisement)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(advertisementPublishSubject);

        return subscription;
    }

    public Subscription validatePinCode(Observer<String> observer, final String pinCode)
    {
        if(pinCodePublishSubject != null) {
            return pinCodePublishSubject.subscribe(observer); // join it
        }

        pinCodePublishSubject = PublishSubject.create();
        pinCodePublishSubject.subscribe(new Observer<String>(){
            @Override
            public void onCompleted() {
                pinCodePublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) {
                pinCodePublishSubject = null;
            }

            @Override
            public void onNext(String s) {
            }
        });

        Subscription subscription = pinCodePublishSubject.subscribe(observer);
        validatePinCode(pinCode)
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<String>>() {
                    @Override
                    public Observable<String> call(JSONObject jsonObject) {
                        try {
                            JSONObject object = jsonObject.getJSONObject("data");
                            Boolean valid = (object.getString("pincode_ok").equals("true"));
                            Timber.d("IS OK: " + valid);
                            if(valid) {
                                return Observable.just(pinCode);
                            } else {
                                Timber.d(object.toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        return Observable.error(new Throwable(application.getString(R.string.toast_pin_code_invalid)));
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pinCodePublishSubject);

        return subscription;
    }
    
    public Subscription sendPinCodeMoney(Observer<String> observer, final String pinCode, String address, String amount)
    {
        if(sendMoneyPublishSubject != null) {
            return sendMoneyPublishSubject.subscribe(observer); // join it
        }

        sendMoneyPublishSubject = PublishSubject.create();
        sendMoneyPublishSubject.subscribe(new Observer<String>(){
            @Override
            public void onCompleted() {
                sendMoneyPublishSubject = null;
            }

            @Override
            public void onError(Throwable throwable) {
                sendMoneyPublishSubject = null;
            }

            @Override
            public void onNext(String s) {
            }
        });

        Subscription subscription = sendMoneyPublishSubject.subscribe(observer);
        sendMoneyWithPinCode(pinCode, address, amount)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sendMoneyPublishSubject);

        return subscription;
    }

    private Observable<Response> validatePinCode(final String pin_code)
    {
        String access_token = getAccessToken();
        return localBitcoins.checkPinCode(pin_code, access_token);
    }

    private Observable<String> sendMoneyWithPinCode(String pin_code, String address, String amount)
    {
        String access_token = getAccessToken();
        return localBitcoins.walletSendPin(pin_code, address, amount, access_token)
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<String>>() {
                    @Override
                    public Observable<String> call(JSONObject jsonObject) {
                        return Observable.just(jsonObject.toString());
                    }
                });
    }

    private Observable<Response> createAdvertisementObservable(final Advertisement advertisement)
    {
        String access_token = getAccessToken();
        return localBitcoins.createAdvertisement(advertisement.ad_id, access_token, advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, advertisement.trade_type.name(), advertisement.online_provider,
                String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                advertisement.city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount),
                String.valueOf(advertisement.trusted_required));
    }
    
    public Subscription getAdvertisement(Observer<Advertisement> observer, final String adId)
    {
        if(advertisementPublishSubject != null) {
            return advertisementPublishSubject.subscribe(observer); // join it
        }

        advertisementPublishSubject = PublishSubject.create();
        advertisementPublishSubject.subscribe(new Observer<Advertisement>(){
            @Override
            public void onCompleted()
            {
                advertisementPublishSubject = null;
            }

            @Override
            public void onError(Throwable e)
            {
                advertisementPublishSubject = null;
            }

            @Override
            public void onNext(Advertisement o){
            }
        });

        Subscription subscription = advertisementPublishSubject.subscribe(observer);
        getAdvertisementObservable(adId)
                .onErrorResumeNext(refreshTokenAndRetry(getAdvertisementObservable(adId)))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(advertisementPublishSubject);

        return subscription;
    }

    public Subscription createContact(Observer<ContactRequest> observer, String id, String amount, String message)
    {
        if(contactRequestPublishSubject != null) {
            return contactRequestPublishSubject.subscribe(observer); // join it
        }

        contactRequestPublishSubject = PublishSubject.create();
        contactRequestPublishSubject.subscribe(new EndObserver<ContactRequest>(){
            @Override
            public void onEnd()
            {
                contactRequestPublishSubject = null;
            }

            @Override
            public void onNext(ContactRequest results) {
               Timber.d("Contact Id: " + results.contact_id);
            }
        });

        //{"error": {"message": "An unspecified error has occurred.", "errors": {"amount": "* This field is required."}, "error_code": 9, "error_lists": {"amount": ["This field is required."]}}}
        Subscription subscription = contactRequestPublishSubject.subscribe(observer);
        createContactObservable(id, amount, message)
                .onErrorResumeNext(refreshTokenAndRetry(createContactObservable(id, amount, message)))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactRequestPublishSubject);

        return subscription;
    }
    
    /*
    .onErrorReturn(new Func1<Throwable, ContactRequest>() {
                    @Override
                    public ContactRequest call(Throwable throwable){
                        if (throwable instanceof RetrofitError) {
                            RetroError retroError;
                            if (((RetrofitError) throwable).isNetworkError()) {
                                retroError = new RetroError(application.getString(R.string.error_generic_server_down), 404);
                                contactRequestPublishSubject.onError(retroError);
                            } else {
                                RetrofitError error = (RetrofitError) throwable;
                                retroError = Parser.parseRetrofitError(error);
                                contactRequestPublishSubject.onError(retroError);
                            }
                        }

                        return null;
                    }
                });
     */


    public Subscription getCurrencies(final Observer<List<Currency>> observer)
    {
        if(currencyRequest != null) { // join the request
            currencyRequest.subscribe(observer);
        }

        currencyRequest = PublishSubject.create();
        currencyRequest.subscribe(new Observer<List<Currency>>() {
            @Override
            public void onCompleted()
            {
                currencyRequest = null;
            }

            @Override
            public void onError(Throwable throwable) {
                currencyRequest = null;
                /*if (throwable instanceof RetrofitError) {
                    if (((RetrofitError) throwable).isNetworkError()) {
                        Toast.makeText(application, application.getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show();
                    } else  {
                        Timber.e(throwable.getMessage());
                    }
                }*/
            }

            @Override
            public void onNext(List<Currency> exchanges)
            {
                // cache exchanges
            }
        });

        Subscription subscription = currencyRequest.subscribe(observer);
        bitcoinAverage.tickers()
                .map(new ResponseToCurrencyList())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(currencyRequest);

        return subscription;
    }

    // ---- OBSERVABLES -----

    public Observable<Response> postMessage(String contact_id, final String message)
    {
        String access_token = getAccessToken();
        return localBitcoins.contactMessagePost(contact_id, access_token, message)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Response> updateAdvertisementObservable(final Advertisement advertisement)
    {
        Timber.d("Update Advertisement Id: " + advertisement.ad_id);
        String access_token = getAccessToken();
        return localBitcoins.updateAdvertisement(advertisement.ad_id, access_token, String.valueOf(advertisement.visible), advertisement.min_amount,
                advertisement.max_amount, advertisement.price_equation, String.valueOf(advertisement.lat), String.valueOf(advertisement.lon),
                advertisement.city, advertisement.location, advertisement.country_code, advertisement.account_info, advertisement.bank_name,
                String.valueOf(advertisement.sms_verification_required), String.valueOf(advertisement.track_max_amount), String.valueOf(advertisement.trusted_required));

    }

    private Observable<Response> releaseContactObservable(String contact_id)
    {
        String access_token = getAccessToken();
        return localBitcoins.releaseContact(contact_id, access_token);
    }

    private Observable<Response> cancelContactObservable(String contact_id)
    {
        String access_token = getAccessToken();
        return localBitcoins.contactCancel(contact_id, access_token);
    }

    private Observable<Response> disputeContactObservable(String contact_id)
    {
        String access_token = getAccessToken();
        return localBitcoins.contactDispute(contact_id, access_token);
    }

    private Observable<Response> fundContactObservable(String contact_id)
    {
        String access_token = getAccessToken();
        return localBitcoins.contactFund(contact_id, access_token);
    }

    private Observable<Response> markPaidContactObservable(String contact_id)
    {
        String access_token = getAccessToken();
        return localBitcoins.markAsPaid(contact_id, access_token);
    }

    private Observable<ContactRequest> createContactObservable(final String id, final String amount, final String message)
    {
        String access_token = getAccessToken();
        return localBitcoins.createContact(id, access_token, amount, message)
                .map(new ResponseToContactRequest());
    }

    private Observable<User> getUserObserver(final String username)
    {
        return localBitcoins.getAccountInfo(username)
                .map(new ResponseToUser());
    }

    private Observable<Advertisement> getAdvertisementObservable(final String ad_id)
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseAdvertisement(MockData.ADVERTISEMENT_LOCAL_SELL));
        }

        String access_token = getAccessToken();
        return localBitcoins.getAdvertisement(ad_id, access_token)
                .map(new ResponseToAd());
    }

    public Observable<List<Method>> getOnlineProviders(final String countryName, final String countryCode)
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseMethods(MockData.METHODS));
        }
        
        if(paymentMethods != null) {
            return Observable.just(paymentMethods);
        }

        if(methods != null) {
            paymentMethods = setPaymentMethods(methods, countryName, countryCode);
            return Observable.just(paymentMethods);
        }

        return localBitcoins.getOnlineProviders()
                .map(new ResponseToMethod())
                .flatMap(new Func1<List<Method>, Observable<List<Method>>>() {
                    @Override
                    public Observable<List<Method>> call(List<Method> results) {
                        paymentMethods = setPaymentMethods(results, countryName, countryCode);
                        return Observable.just(paymentMethods);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    private List<Method> setPaymentMethods(List<Method> methods, String countryName, String countryCode)
    {
        Timber.d("Adding ALL");
        
        Method method = new Method();
        method.code = "ALL";
        method.name = ("All in " + countryName);
        method.key = "all";
        methods.add(0, method);

        for (Method m : methods) {
            m.countryCode = countryCode;
            m.countryName = countryName;  
        }

        return methods;
    }

    public Observable<List<Method>> getOnlineProviders()
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseMethods(MockData.METHODS));
        }

        if(paymentMethods != null)
            return Observable.just(paymentMethods);

        if(methods != null)
            return Observable.just(methods);

        return localBitcoins.getOnlineProviders()
                .map(new ResponseToMethod())
                .flatMap(new Func1<List<Method>, Observable<List<Method>>>() {
                    @Override
                    public Observable<List<Method>> call(List<Method> results) {
                        methods = results;
                        return  Observable.just(methods);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public Observable<JSONObject> deleteAdvertisement(final String ad_id)
    {
        String access_token = getAccessToken();
        return localBitcoins.deleteAdvertisement(ad_id, access_token)
               .onErrorResumeNext(refreshTokenAndRetry(localBitcoins.deleteAdvertisement(ad_id, access_token)))
               .subscribeOn(Schedulers.newThread())
               .observeOn(AndroidSchedulers.mainThread())
               .map(new ResponseToJSONObject());
    }

    private Observable<Wallet> getWalletObservable()
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseWallet(MockData.WALLET))
                    .flatMap(new Func1<Wallet, Observable<Wallet>>(){
                        @Override
                        public Observable<Wallet> call(Wallet wallet)
                        {
                            return getWalletBitmap(wallet);
                        }
                    });
        }

        String access_token = getAccessToken();
 
        return localBitcoins.getWallet(access_token)
                .map(new ResponseToWallet())
                .flatMap(new Func1<Wallet, Observable<Wallet>>() {
                    @Override
                    public Observable<Wallet> call(Wallet wallet)
                    {
                        return getWalletBitmap(wallet);
                    }
                })
                .flatMap(new Func1<Wallet, Observable<Wallet>>() {
                    @Override
                    public Observable<Wallet> call(Wallet wallet)
                    {
                        return getBitstamp(wallet);
                    }
                });
    }

    /*private Observable<Dashboard> getWalletBalance(final Bitstamp bitstamp)
    {
        if(Constants.USE_MOCK_DATA) return Observable.just(Parser.parseWalletBalance(MockData.WALLET_BALANCE))
                .flatMap(this::getWalletBitmap)
                .map(walletBalance -> {
                    walletBalance.bitstamp = bitstamp;
                    Dashboard dashboard = new Dashboard();
                    dashboard.walletBalance = walletBalance;
                    return dashboard;
                });

        String access_token = getAccessToken();
        return localBitcoins.getWalletBalance(access_token)
                .map(new ResponseToWalletBalance())
                .flatMap(this::getWalletBitmap)
                .map(walletBalance -> {
                    walletBalance.bitstamp = bitstamp;
                    Dashboard dashboard = new Dashboard();
                    dashboard.walletBalance = walletBalance;
                    return dashboard;
                });
    }*/

    private Observable<Wallet> getWalletBalanceObservable()
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseWalletBalance(MockData.WALLET_BALANCE))
                    .flatMap(this::getWalletBitmap);
        }

        String access_token = getAccessToken();
        return localBitcoins.getWalletBalance(access_token)
                .map(new ResponseToWalletBalance())
                .flatMap(this::getWalletBitmap)
                .flatMap(new Func1<Wallet, Observable<? extends Wallet>>()
                {
                    @Override
                    public Observable<? extends Wallet> call(Wallet wallet)
                    {
                        return getBitstamp(wallet);
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
                subscriber.onNext(WalletUtils.encodeAsBitmap(address, application));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }

    private Observable<Bitmap> generateBitmap(final String address, String amount)
    {
        return Observable.create((Subscriber<? super Bitmap> subscriber) -> {
            try {
                subscriber.onNext(WalletUtils.encodeAsBitmap(address, amount, application));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }

    private Observable<List<Contact>> getDashboardContacts(DashboardType dashboardType)
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseContacts(MockData.DASHBOARD));
        }

        String access_token = getAccessToken();
        if(dashboardType == DashboardType.RELEASED) {
            return localBitcoins.getDashboard(access_token, "released")
                    .map(new ResponseToContacts())
                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                        @Override
                        public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                            
                            if(contacts.isEmpty()) {
                                return Observable.just(contacts);
                            }
                            
                            return getContactsMessageObservable(contacts);
                        }
                    }); 
        } else if (dashboardType == DashboardType.CANCELED) {
            return localBitcoins.getDashboard(access_token, "canceled")
                    .map(new ResponseToContacts())
                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                        @Override
                        public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                            if(contacts.isEmpty()) {
                                return Observable.just(contacts);
                            }
                            return getContactsMessageObservable(contacts);
                        }
                    });
        } else if (dashboardType == DashboardType.CLOSED) {
            return localBitcoins.getDashboard(access_token, "closed")
                    .map(new ResponseToContacts())
                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                        @Override
                        public Observable<? extends List<Contact>> call(final List<Contact> contacts){
                            if(contacts.isEmpty()) {
                                return Observable.just(contacts);
                            }
                            return getContactsMessageObservable(contacts);
                        }
                    });
        } else {
            return localBitcoins.getDashboard(access_token)
                    .map(new ResponseToContacts())
                    .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                        @Override
                        public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                            if(contacts.isEmpty()) {
                                return Observable.just(contacts);
                            }
                            return getContactsMessageObservable(contacts);
                        }
                    });
        }
    }

    private Observable<List<Contact>> getContactsMessageObservable(final List<Contact> contacts)
    {
        String access_token = getAccessToken();
        return Observable.just(Observable.from(contacts)
                .flatMap(new Func1<Contact, Observable<? extends List<Contact>>>() {
                    @Override
                    public Observable<? extends List<Contact>> call(final Contact contact) {
                        return localBitcoins.contactMessages(contact.contact_id, access_token)
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, List<Contact>>() {
                                    @Override
                                    public List<Contact> call(List<Message> messages) {
                                        contact.messages = messages;
                                        return contacts;
                                    }
                                });
                    }
                }).toBlockingObservable().last());
    }

    private Observable<Dashboard> getDashboardObservable()
    {
        return getBitstamp()
                .flatMap(new Func1<DefaultExchange, Observable<Dashboard>>() {
                    @Override
                    public Observable<Dashboard> call(DefaultExchange exchange) {
                        return getDashboardActiveContactsObservable(exchange);
                    }
                })
                .flatMap(new Func1<Dashboard, Observable<Dashboard>>() {
                    @Override
                    public Observable<Dashboard> call(Dashboard dashboard) {
                        if(dashboard.contacts.isEmpty()) {
                            return Observable.just(dashboard);
                        }
                        
                        return Observable.just(getDashboardMessageObservable(dashboard));
                    }
                })
                .flatMap(new Func1<Dashboard, Observable<Dashboard>>() {
                    @Override
                    public Observable<Dashboard> call(Dashboard dashboard) {
                        return getAdvertisementsObservable(dashboard);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Dashboard getDashboardMessageObservable(final Dashboard dashboard)
    {
        assert dashboard != null;

        if(Constants.USE_MOCK_DATA) {
            for (Contact contact :dashboard.contacts) {
                List<Message> messages = Parser.parseMessages(MockData.MESSAGES);
                if(messages.size() > 0) {
                    contact.messages = messages;
                }
            }
            return dashboard;
        }

         String access_token = getAccessToken();
         return Observable.from(dashboard.contacts)
               .flatMap(new Func1<Contact, Observable<? extends Dashboard>>() {
                   @Override
                   public Observable<? extends Dashboard> call(final Contact contact)
                   {
                       return localBitcoins.contactMessages(contact.contact_id, access_token)
                               .map(new ResponseToMessages())
                               .map(new Func1<List<Message>, Dashboard>() {
                                   @Override
                                   public Dashboard call(List<Message> messages) {
                                       Timber.d("Messages: " + messages.size());
                                       contact.messages = messages;
                                       return dashboard;
                                   }
                               });
                   }
               }).toBlockingObservable().last();
    }

    private Observable<Contact> getContactObservable(String contact_id)
    {
        if(Constants.USE_MOCK_DATA) {
            Contact contact = Parser.parseContact(MockData.CONTACT_LOCAL_SELL);
            contact.messages = Parser.parseMessages(MockData.MESSAGES);
            return Observable.just(contact);
        }

        final String access_token = getAccessToken();
        return localBitcoins.getContact(contact_id, access_token)
                .map(new ResponseToContact())
                .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                    @Override
                    public Observable<? extends Contact> call(Contact contact) {
                        return localBitcoins.contactMessages(contact.contact_id, access_token)
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, Contact>() {
                                    @Override
                                    public Contact call(List<Message> messages) {
                                        contact.messages = messages;
                                        return contact;
                                    }
                                });
                    }
                });
    }

    private Observable<Dashboard> getDashboardActiveContactsObservable(final DefaultExchange exchange)
    {
        assert exchange != null;
        
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseContacts(MockData.DASHBOARD))
                    .map(contacts -> {
                        Dashboard dashboard = new Dashboard();
                        dashboard.exchange = exchange;
                        dashboard.contacts = contacts;
                        return dashboard;
                    });
        }

        String access_token = getAccessToken();
        return localBitcoins.getDashboard(access_token)
                .map(new ResponseToContacts())
                .map(contacts -> {
                    Timber.d("We have contact: " + contacts.size());
                    Dashboard dashboard = new Dashboard();
                    dashboard.exchange = exchange;
                    dashboard.contacts = contacts;
                    return dashboard;
                });
    }
    
    private Observable<List<Advertisement>> getAdvertisementsObservable()
    {
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseAdvertisements(MockData.ADVERTISEMENT_LIST_SUCCESS));
        }

        String access_token = getAccessToken();
        return localBitcoins.getAds(access_token)
                .map(new ResponseToAds());
    }

    private Observable<Dashboard> getAdvertisementsObservable(final Dashboard dashboard)
    {
        assert dashboard != null;
        
        if(Constants.USE_MOCK_DATA) {
            return Observable.just(Parser.parseAdvertisements(MockData.ADVERTISEMENT_LIST_SUCCESS))
                    .map(ads -> {
                        dashboard.advertisements = ads;
                        return dashboard;
                    });
        }

        String access_token = getAccessToken();
        return localBitcoins.getAds(access_token)
                .map(new ResponseToAds())
                .map(ads -> {
                    dashboard.advertisements = ads;
                    return dashboard;
                });
    }

    private Observable<DefaultExchange> getBitstamp()
    {
        return bitstampExchange.ticker()
               .map(new ResponseBitstampToExchange())
               .onErrorResumeNext(getBitfinex());
    }

    private Observable<Wallet> getBitstamp(final Wallet wallet)
    {
        return bitstampExchange.ticker()
               .map(new ResponseBitstampToExchange())
               .onErrorResumeNext(getBitfinex())
               .map(new Func1<DefaultExchange, Wallet>() {
                   @Override
                   public Wallet call(DefaultExchange defaultExchange) {
                       if (defaultExchange != null) {
                           wallet.exchange = defaultExchange;
                       }
                       return wallet;
                   }
               });
    }

    private Observable<DefaultExchange> getBitfinex()
    {
        return bitfinexExchange.ticker()
                .map(new ResponseBitfinexToExchange())
                .onErrorReturn(new Func1<Throwable, DefaultExchange>() {
                    @Override
                    public DefaultExchange call(Throwable throwable) {
                        return new DefaultExchange();
                    }
                });
    }
    
    // ---- Schedulers ----- 

    public boolean dataRefreshable()
    {
        return (needToRefreshExchanges());
    }

    public void setSelectedExchange(String name)
    {
        StringPreference preference = new StringPreference(sharedPreferences, PREFS_SELECTED_EXCHANGE, "Bitstamp");
        preference.set(name);
    }

    private void setExchangeExpireTime()
    {
        synchronized (this) {
            long expire = System.currentTimeMillis() + CHECK_EXCHANGE_DATA; // 1 hours
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(PREFS_EXPIRE_TIME, expire);
            editor.apply();
        }
    }

    private boolean needToRefreshExchanges()
    {
        synchronized (this) {
            long expiresAt = sharedPreferences.getLong(PREFS_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    // ---- AUTHORIZATION ------

    // retry method after refreshing tokens
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

    private boolean needToRefreshTokens()
    {
        synchronized (this) {
            long expiresAt = sharedPreferences.getLong(PREFS_TOKENS_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    public boolean isLoggedIn()
    {
        /*IntPreference preference = new IntPreference(sharedPreferences, PREFS_LOGGED_IN);
        int loggedIn = preference.get();
        Timber.e("Preference: " + loggedIn);
        return (loggedIn > 0);*/
        
        Authorization authorization = getAuthorization();
        return (authorization != null);
    }
    
    public void logOut()
    {
        IntPreference preference = new IntPreference(sharedPreferences, PREFS_LOGGED_IN);
        preference.delete();

        LongPreference longPreference = new LongPreference(sharedPreferences, PREFS_TOKENS_EXPIRE_TIME);
        longPreference.delete();

        StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER);
        stringPreference.delete();
        
        deleteAuthorization();
    }

    public Subscription getAuthorization(final Observer<Authorization> observer, String code)
    {
        if(authorizationRequest != null) {
            return authorizationRequest.subscribe(observer);
        }

        authorizationRequest = PublishSubject.create();
        authorizationRequest.subscribe(new Observer<Authorization>() {
            @Override
            public void onCompleted()
            {
                authorizationRequest = null;
            }

            @Override
            public void onError(Throwable e)
            {
                //Timber.e("Error: " + e.toString());
                authorizationRequest = null;
            }

            @Override
            public void onNext(Authorization authorization) {
                Timber.d("Access Token: " + authorization.access_token);
                Timber.d("Refresh Token: " + authorization.refresh_token);
                Timber.d("Expires: " + authorization.expires_in);
                
                saveAuthorization(authorization);
            }
        });

        Subscription subscription = authorizationRequest.subscribe(observer);
        getAuthorizationObservable(code)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(authorizationRequest);

        return subscription;
    }

    public Subscription getMyself(final Observer<User> observer, String token)
    {
        if(myselfRequest != null) {
            return myselfRequest.subscribe(observer);
        }

        myselfRequest = PublishSubject.create();
        myselfRequest.subscribe(new Observer<User>() {
            @Override
            public void onCompleted() {
                myselfRequest = null;
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("Error: " + e.toString());
                myselfRequest = null;
            }

            @Override
            public void onNext(User user) {
                Timber.d("User: " + user.username);
                StringPreference stringPreference = new StringPreference(sharedPreferences, PREFS_USER);
                stringPreference.set(user.username);
            }
        });

        Subscription subscription = myselfRequest.subscribe(observer);
        getMyselfObserver(token)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(myselfRequest);

        return subscription;
    }

    private Observable<User> getMyselfObserver(String access_token)
    {
        return localBitcoins.getMyself(access_token)
                .map(new ResponseToUser());
    }

    private Observable<Authorization> getAuthorizationObservable(String code)
    {
        return localBitcoins.getAuthorization("authorization_code", code, Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                .map(new ResponseToAuthorize());
    }

    public String getAccessToken()
    {
        Authorization authorization = getAuthorization();
        if(authorization == null) return null;
        
        return authorization.access_token;
    }

    private Observable<String> refreshTokens()
    {
        Authorization authorization = getAuthorization();
        if(authorization == null) return Observable.just(null);
        
        Timber.e("Get Refresh token: " + authorization.refresh_token);

        return localBitcoins.refreshToken("refresh_token", authorization.refresh_token, Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                .map(new ResponseToAuthorize())
                .flatMap(new Func1<Authorization, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Authorization authorization) {

                        Timber.d("New Access tokens: " + authorization.access_token);
                        
                        saveAuthorization(authorization);
                        return Observable.just(authorization.access_token);
                    }
                });
    }

    private void saveAuthorization(final Authorization authorization)
    {
        Timber.e("Save Authorization: " + authorization.access_token);

        synchronized (this) {
    
            ContentValues values = new ContentValues(1);
            values.put("access_token", authorization.access_token);
            values.put("refresh_token", authorization.refresh_token);
            values.put("expires_in", authorization.expires_in);
            
            Authorization oldAuth = getAuthorization();
            if(oldAuth != null) {
                // save to cupboard
                String id = String.valueOf(oldAuth._id);
                application.getContentResolver().update(CupboardProvider.TOKEN_URI, values, "_id =", new String[] { id });
                //cupboard().withContext(application).update(CupboardProvider.TOKEN_URI, values);
            } else {
                cupboard().withContext(application).put(CupboardProvider.TOKEN_URI, authorization);
            }
        }
    }
    
    private void deleteAuthorization()
    {
        synchronized (this) {
            List<Authorization> list = cupboard().withContext(application.getApplicationContext()).query(CupboardProvider.TOKEN_URI, Authorization.class).list();
            if (list != null && list.size() > 0) {
                Authorization authorization = list.get(0);
                //Uri uri = ContentUris.withAppendedId(CupboardProvider.TOKEN_URI, authorization._id);
                cupboard().withContext(application.getApplicationContext()).delete(CupboardProvider.TOKEN_URI, authorization);
            }
        }
    }

    private void setTokenExpire(String expires_in)
    {
        synchronized (this) {
            long expireMilliseconds = Long.parseLong(expires_in) * 1000;
            long expire = System.currentTimeMillis() + expireMilliseconds;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(PREFS_TOKENS_EXPIRE_TIME, expire);
            editor.apply();
        }
    }

    private Authorization getAuthorization()
    {
        synchronized (this) {
            List<Authorization> list = cupboard().withContext(application.getApplicationContext()).query(CupboardProvider.TOKEN_URI, Authorization.class).list();   
            if (list != null && list.size() > 0) {
                Authorization authorization = list.get(0);
                //setTokenExpire(authorization.expires_in);
                return authorization;
            }

            return null;
        }
    }
}
