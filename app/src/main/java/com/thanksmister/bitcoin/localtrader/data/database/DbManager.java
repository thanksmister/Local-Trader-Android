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

package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseBitfinexToExchange;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToJSONObject;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMethod;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToUser;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWallet;
import com.thanksmister.bitcoin.localtrader.data.mock.MockData;
import com.thanksmister.bitcoin.localtrader.data.prefs.LongPreference;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import timber.log.Timber;

public class DbManager 
{
    private static final String PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire";
    public static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    public static final String PREFS_ADVERTISEMENT_EXPIRE_TIME = "pref_ads_expire";
    public static final String PREFS_USER = "pref_user";

    public static final int CHECK_EXCHANGE_DATA = 3 * 60 * 1000;// 5 minutes
    private static final int CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
    public static final int CHECK_ADVERTISEMENT_DATA = 15 * 60 * 1000;// 15 mintues
    
    private SqlBrite db;
    private LocalBitcoins localBitcoins;
    private SharedPreferences sharedPreferences;
    private BitfinexExchange bitfinexExchange;
    private BaseApplication baseApplication;
    
    @Inject
    public DbManager(SqlBrite db, LocalBitcoins localBitcoins, SharedPreferences sharedPreferences, BitfinexExchange bitfinexExchange, BaseApplication application)
    {
        this.db = db;
        this.localBitcoins = localBitcoins;
        this.bitfinexExchange = bitfinexExchange;
        this.sharedPreferences = sharedPreferences;
        this.baseApplication = baseApplication;
    }

    /**
     * Resets Db manager and clear all preferences
     */
    public void clearDbManager()
    {
        db.delete(WalletItem.TABLE, null, null);
        db.delete(SessionItem.TABLE, null, null);
        db.delete(ContactItem.TABLE, null, null);
        db.delete(MessageItem.TABLE, null, null);
        db.delete(AdvertisementItem.TABLE, null, null);

        resetAdvertisementsExpireTime();
        resetExchangeExpireTime();
        resetMethodsExpireTime();
    }

    public boolean isLoggedIn()
    {
        Cursor cursor = db.query(SessionItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String access_token = Db.getString(cursor, SessionItem.ACCESS_TOKEN);
                Timber.d("Access Token: " + access_token);
                return (!Strings.isBlank(access_token));
            }
            return false;
        } finally {
            cursor.close();
        }
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
                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize())
                                .flatMap(new Func1<Authorization, Observable<? extends String>>()
                                {
                                    @Override
                                    public Observable<? extends String> call(Authorization authorization)
                                    {
                                        Timber.d("New Access tokens: " + authorization.access_token);
                                        db.insert(SessionItem.TABLE, new SessionItem.Builder().access_token(authorization.access_token).refresh_token(authorization.refresh_token).build());
                                        return Observable.just(authorization.access_token);
                                    }
                                });
                    }
                });
    }

    public Observable<User> getMyself(String token)
    {
        return localBitcoins.getMyself(token)
                .map(new ResponseToUser());
    }

    public Observable<SessionItem> getTokens()
    {
        return db.createQuery(SessionItem.TABLE, SessionItem.QUERY)
                .map(SessionItem.MAP);
    }

    public Observable<ContactItem> contactQuery(String contactId)
    {
        return db.createQuery(ContactItem.TABLE, ContactItem.QUERY_ITEM_WITH_MESSAGES, String.valueOf(contactId))
                .map(ContactItem.MAP)
                .flatMap(new Func1<List<ContactItem>, Observable<ContactItem>>()
                {
                    @Override
                    public Observable<ContactItem> call(List<ContactItem> contactItems)
                    {
                        if (contactItems.size() > 0) {
                            return Observable.just(contactItems.get(0));
                        }

                        return null;
                    }
                });
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
    
    public void updateContacts(List<Contact> contacts)
    {
        for (Contact contact : contacts) {
            updateContact(contact);
            updateMessages(contact.messages);
        }
    }

    public void updateContact(Contact contact)
    {
        ContactItem.Builder builder = new ContactItem.Builder()
                .contact_id(contact.contact_id)
                .created_at(contact.created_at)
                .amount(contact.amount)
                .amount_btc(contact.amount_btc)
                .currency(contact.currency)
                .reference_code(contact.reference_code)

                .payment_completed_at(contact.payment_completed_at)
                .contact_id(contact.contact_id)
                .disputed_at(contact.disputed_at)
                .funded_at(contact.funded_at)
                .escrowed_at(contact.escrowed_at)
                .released_at(contact.released_at)
                .canceled_at(contact.canceled_at)
                .closed_at(contact.closed_at)
                .disputed_at(contact.disputed_at)
                .is_funded(contact.is_funded)

                .fund_url(contact.actions.fund_url)
                .release_url(contact.actions.release_url)
                .advertisement_public_view(contact.actions.advertisement_public_view)
                .message_url(contact.actions.messages_url)
                .message_post_url(contact.actions.message_post_url)
                .mark_as_paid_url(contact.actions.mark_as_paid_url)
                .dispute_url(contact.actions.dispute_url)
                .cancel_url(contact.actions.cancel_url)

                .buyer_last_online(contact.buyer.name)
                .buyer_last_online(contact.buyer.username)
                .buyer_last_online(contact.buyer.trade_count)
                .buyer_last_online(contact.buyer.feedback_score)
                .buyer_last_online(contact.buyer.last_online)

                .seller_last_online(contact.seller.name)
                .seller_last_online(contact.seller.username)
                .seller_last_online(contact.seller.trade_count)
                .seller_last_online(contact.seller.feedback_score)
                .seller_last_online(contact.seller.last_online)

                .account_receiver_email(contact.account_details.email)
                .account_receiver_name(contact.account_details.receiver_name)
                .account_iban(contact.account_details.iban)
                .account_swift_bic(contact.account_details.swift_bic)
                .account_reference(contact.account_details.reference)

                .advertisement_id(contact.advertisement.id)
                .advertisement_trade_type(contact.advertisement.trade_type.name())
                .advertisement_payment_method(contact.advertisement.payment_method)

                .advertiser_name(contact.advertisement.advertiser.name)
                .advertiser_username(contact.advertisement.advertiser.username)
                .advertiser_trade_count(contact.advertisement.advertiser.trade_count)
                .advertiser_feedback_score(contact.advertisement.advertiser.feedback_score)
                .advertiser_last_online(contact.advertisement.advertiser.last_online);
        
        Cursor cursor = db.query(ContactItem.QUERY_ITEM, String.valueOf(contact.contact_id));
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, ContactItem.ID);
                db.update(ContactItem.TABLE, builder.build(), ContactItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(ContactItem.TABLE, builder.build());
            }
        } finally {
            cursor.close();
        }
    }
    
    public Observable<List<ContactItem>> contactsQuery()
    {
        return db.createQuery(ContactItem.TABLE, ContactItem.QUERY)
                .map(ContactItem.MAP);
    }
    
    /*public Observable<List<ContactItem>> contactsQueryWithMessages()
    {
        return db.createQuery(ContactItem.TABLE, ContactItem.QUERY_WITH_MESSAGES)
                .map(ContactItem.MAP);
    }*/

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

    public Observable<List<MessageItem>> queryMessages(long contactId)
    {
        return  db.createQuery(MessageItem.TABLE, MessageItem.QUERY, String.valueOf(contactId))
                .map(MessageItem.MAP);
    }

    public Observable<Integer> messageCountQuery(long contactId)
    {
        return db.createQuery(MessageItem.TABLE, MessageItem.COUNT_QUERY, String.valueOf(contactId))
                .map(new Func1<SqlBrite.Query, Integer>()
                {
                    @Override
                    public Integer call(SqlBrite.Query query)
                    {
                        Cursor cursor = query.run();
                        try {
                            if (!cursor.moveToNext()) {
                                throw new AssertionError("No messages");
                            }
                            return cursor.getInt(0);
                        } finally {
                            cursor.close();
                        }
                    }
                });
    }

    public void updateMessages(final List<Message> messages)
    {
        HashMap<String, Message> entryMap = new HashMap<String, Message>();
        for (Message message : messages) {
            entryMap.put(message.created_at, message);
        }

        Cursor cursor = db.query(MessageItem.QUERY);

        try {
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, MessageItem.ID);
                String createdAt = Db.getString(cursor, MessageItem.CREATED_AT);
                Message match = entryMap.get(createdAt);

                if (match != null) {
                    // Entry exists. Do not update message
                    entryMap.remove(createdAt);

                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(MessageItem.TABLE, String.valueOf(id));
                }
            }
        } finally {
            cursor.close();
        }
        
        // Add new items
        for (Message item : entryMap.values()) {
            
            MessageItem.Builder builder = new MessageItem.Builder()
                    .contact_list_id(Long.valueOf(item.contact_id))
                    .message(item.msg)
                    .seen(true)
                    .created_at(item.created_at)
                    .sender_id(item.sender.id)
                    .sender_name(item.sender.name)
                    .sender_username(item.sender.username)
                    .sender_trade_count(item.sender.trade_count)
                    .sender_last_online(item.sender.last_seen_on)
                    .is_admin(item.is_admin);

            db.insert(MessageItem.TABLE, builder.build());
        }
    }


    public Observable<List<MethodItem>> methodQuery()
    {
       return db.createQuery(MethodItem.TABLE, MethodItem.QUERY)
                .map(MethodItem.MAP);
    }
    
    public Observable<List<Method>> getMethods()
    {
        if(!needToRefreshMethods()) {
            return Observable.empty();
        }
        
        return localBitcoins.getOnlineProviders()
                .map(new ResponseToMethod());
    }

    public Observable<ExchangeItem> exchangeQuery()
    {
        return  db.createQuery(ExchangeItem.TABLE, ExchangeItem.QUERY)
                .map(ExchangeItem.MAP);
    }
 
    public Observable<Exchange> getExchange()
    {
        if(!needToRefreshExchanges()){
            return Observable.empty();
        }

        return bitfinexExchange.ticker()
                .map(new ResponseBitfinexToExchange());
    }
    
    public Observable<List<AdvertisementItem>> advertisementQuery()
    {
        return  db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY)
                .map(AdvertisementItem.MAP);
    }

    public Observable<AdvertisementItem> advertisementItemQuery(String adId)
    {
        return  db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY_ITEM, adId)
                .map(AdvertisementItem.MAP)
                .flatMap(new Func1<List<AdvertisementItem>, Observable<AdvertisementItem>>()
                {
                    @Override
                    public Observable<AdvertisementItem> call(List<AdvertisementItem> advertisementItems)
                    {
                        if (advertisementItems.size() > 0) {
                            return Observable.just(advertisementItems.get(0));
                        }

                        return null;
                    }
                });
    }

    public Observable<List<Advertisement>> getAdvertisements()
    {
        if(!needToRefreshAdvertisements()) {
            return Observable.just(new ArrayList<Advertisement>());
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
                        return localBitcoins.getAds(sessionItem.access_token())
                                .map(new ResponseToAds());
                    }
                });
    }

    public Observable<WalletItem> walletQuery()
    {
        return  db.createQuery(WalletItem.TABLE, WalletItem.QUERY)
                .map(WalletItem.MAP);
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
    
    public void updateWallet(Wallet wallet)
    {
        WalletItem.Builder builder = new WalletItem.Builder()
                .message(wallet.message)
                .balance(wallet.total.balance)
                .sendable(wallet.total.sendable)
                .address(wallet.address.address)
                .receivable(wallet.address.received);
        
            if(wallet.qrImage != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
                builder.qrcode(baos.toByteArray());
            }

        Cursor cursor = db.query(WalletItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, WalletItem.ID);
                db.update(WalletItem.TABLE, builder.build(), WalletItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(WalletItem.TABLE, builder.build());
            }
        } finally {
            cursor.close();
        }
    }
    
    public void updateExchange(final Exchange exchange)
    {
        ExchangeItem.Builder builder = new ExchangeItem.Builder()
                .ask(exchange.ask)
                .bid(exchange.bid)
                .last(exchange.last)
                .exchange(exchange.name);

        Cursor cursor = db.query(ExchangeItem.QUERY);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                long id = Db.getLong(cursor, ExchangeItem.ID);
                db.update(ExchangeItem.TABLE, builder.build(), ExchangeItem.ID + " = ?", String.valueOf(id));
            } else {
                db.insert(ExchangeItem.TABLE, builder.build());
            }
        } finally {
            cursor.close();
        }
        
        setExchangeExpireTime();
    }
    
    public void updateMethods(final List<Method> methods)
    {
        HashMap<String, Method> entryMap = new HashMap<String, Method>();
        for (Method item : methods) {
            entryMap.put(item.key, item);
        }

        // Get list of all items
        Cursor cursor = db.query(MethodItem.QUERY);
        try {
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, MethodItem.ID);
                String key = Db.getString(cursor, MethodItem.KEY);
                
                Method match = entryMap.get(key);

                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later. Do not update
                    entryMap.remove(key);
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(MethodItem.TABLE, String.valueOf(id));
                }
            }
        } finally {
            cursor.close();
        }

        // Add new items
        for (Method item : entryMap.values()) {
            
            MethodItem.Builder builder =  new MethodItem.Builder()
                    .key(item.key)
                    .name(item.name)
                    .code(item.code)
                    .countryCode(item.countryCode)
                    .countryName(item.countryName);

            db.insert(MethodItem.TABLE, builder.build());
        }
        
        setMethodsExpireTime(); // reset time to expire*/
        
        //db.createQuery(MethodItem.TABLE, MethodItem.createInsertOrReplaceQuery(method));
    }

    public void updateAdvertisements(final List<Advertisement> advertisements)
    {
        // Build hash table of incoming entries
        HashMap<String, Advertisement> entryMap = new HashMap<String, Advertisement>();
        for (Advertisement item : advertisements) {
            entryMap.put(item.ad_id, item);
        }

        // Get list of all items
        Cursor cursor = db.query(AdvertisementItem.QUERY);

        try {
            
            while (cursor.moveToNext()) {

                long id = Db.getLong(cursor, AdvertisementItem.ID);
                String adId = Db.getString(cursor, AdvertisementItem.AD_ID);
                boolean visible = Db.getBoolean(cursor, AdvertisementItem.VISIBLE);

                String location = Db.getString(cursor, AdvertisementItem.LOCATION_STRING);
                String city = Db.getString(cursor, AdvertisementItem.CITY);
                String country_code = Db.getString(cursor, AdvertisementItem.COUNTRY_CODE);
                String trade_type = Db.getString(cursor, AdvertisementItem.TRADE_TYPE);
                String online_provider = Db.getString(cursor, AdvertisementItem.ONLINE_PROVIDER);
                String price_equation = Db.getString(cursor, AdvertisementItem.PRICE_EQUATION);
                String currency = Db.getString(cursor, AdvertisementItem.CURRENCY);
                String account_info = Db.getString(cursor, AdvertisementItem.ACCOUNT_INFO);

                double lat = Db.getDouble(cursor, AdvertisementItem.LAT);
                double lon = Db.getDouble(cursor, AdvertisementItem.LON);

                String min_amount = Db.getString(cursor, AdvertisementItem.MIN_AMOUNT);
                String max_amount = Db.getString(cursor, AdvertisementItem.MAX_AMOUNT);
                String temp_price = Db.getString(cursor, AdvertisementItem.TEMP_PRICE);
                String bank_name = Db.getString(cursor, AdvertisementItem.BANK_NAME);
                String message = Db.getString(cursor, AdvertisementItem.MESSAGE);

                boolean smsRequired = Db.getBoolean(cursor, AdvertisementItem.SMS_VERIFICATION_REQUIRED);
                boolean trustRequired = Db.getBoolean(cursor, AdvertisementItem.TRUSTED_REQUIRED);
                boolean trackMaxAmount = Db.getBoolean(cursor, AdvertisementItem.TRACK_MAX_AMOUNT);

                Advertisement match = entryMap.get(adId);

                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later.
                    entryMap.remove(adId);

                    if ((match.price_equation != null && !match.price_equation.equals(price_equation)) ||
                            (match.temp_price != null && !match.temp_price.equals(temp_price)) ||
                            (match.city != null && !match.city.equals(city)) ||
                            (match.country_code != null && !match.country_code.equals(country_code)) ||
                            (match.bank_name != null && !match.bank_name.equals(bank_name)) ||
                            (match.currency != null && !match.currency.equals(currency)) ||
                            (match.lat != lat) || (match.lon != lon) ||
                            (match.location != null && !match.location.equals(location)) ||
                            (match.max_amount != null && !match.max_amount.equals(max_amount)) ||
                            (match.min_amount != null && !match.min_amount.equals(min_amount)) ||
                            (match.online_provider != null && !match.online_provider.equals(online_provider)) ||
                            (match.account_info != null && !match.account_info.equals(account_info)) ||
                            (match.trade_type.name() != null && !match.trade_type.name().equals(trade_type)) ||
                            (match.sms_verification_required != smsRequired) ||
                            (match.visible != visible ||
                                    (match.trusted_required != trustRequired) ||
                                    (match.track_max_amount != trackMaxAmount) ||
                                    (match.message != null && !match.message.equals(message)))) {


                        AdvertisementItem.Builder builder = new AdvertisementItem.Builder()
                                .city(match.city)
                                .country_code(match.country_code)
                                .currency(match.currency)
                                .lat(match.lat)
                                .lon(match.lon)
                                .location_string(match.location)
                                .max_amount(match.max_amount)
                                .min_amount(match.min_amount)
                                .online_provider(match.online_provider)
                                .temp_price(match.temp_price)
                                .price_equation(match.price_equation)
                                .action_public_view(match.actions.public_view)
                                .trade_type(match.trade_type.name())
                                .visible(match.visible)
                                .account_info(match.account_info)
                                .profile_last_online(match.profile.last_online)
                                .profile_name(match.profile.name)
                                .profile_username(match.profile.username)
                                .bank_name(match.bank_name)
                                .trusted_required(match.trusted_required)
                                .message(match.message)
                                .track_max_amount(match.track_max_amount);

                        db.update(AdvertisementItem.TABLE, builder.build(), AdvertisementItem.ID + " = ?", String.valueOf(id));

                    }
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    db.delete(AdvertisementItem.TABLE, String.valueOf(id));
                }
            }
        } finally {
            cursor.close();
        }

        // Add new items
        for (Advertisement item : entryMap.values()) {

            AdvertisementItem.Builder builder = new AdvertisementItem.Builder()
                    .created_at(item.created_at)
                    .ad_id(item.ad_id)
                    .city(item.city)
                    .country_code(item.country_code)
                    .currency(item.currency)
                    .email(item.email)
                    .lat(item.lat)
                    .lon(item.lon)
                    .location_string(item.location)
                    .max_amount(item.max_amount)
                    .min_amount(item.min_amount)
                    .max_amount_available(item.max_amount_available)
                    .online_provider(item.online_provider)
                    .require_trade_volume(item.require_trade_volume)
                    .require_feedback_score(item.require_feedback_score)
                    .atm_model(item.atm_model)
                    .temp_price(item.temp_price)
                    .temp_price_usd(item.temp_price_usd)
                    .price_equation(item.price_equation)
                    .reference_type(item.reference_type)
                    .action_public_view(item.actions.public_view)
                    .sms_verification_required(item.sms_verification_required)
                    .trade_type(item.trade_type.name())
                    .visible(item.visible)
                    .account_info(item.account_info)
                    .profile_last_online(item.profile.last_online)
                    .profile_name(item.profile.name)
                    .profile_username(item.profile.username)
                    .profile_feedback_score(item.profile.feedback_score)
                    .profile_trade_count(item.profile.trade_count)
                    .bank_name(item.bank_name)
                    .message(item.message)
                    .track_max_amount(item.track_max_amount)
                    .trusted_required(item.trusted_required);
            
            db.insert(AdvertisementItem.TABLE, builder.build());
        }

        setAdvertisementsExpireTime();
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

    private boolean needToRefreshAdvertisements()
    {
        synchronized (this) {
            LongPreference preference = new LongPreference(sharedPreferences, PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
            return System.currentTimeMillis() >= preference.get();
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
