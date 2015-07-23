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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAuthorize;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContact;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToContacts;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToMessages;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToWalletBalance;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.DbOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.MessageItem;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.prefs.StringPreference;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private Subscription walletSubscription;
    private Subscription contactsSubscription;
    private Subscription advertisementsSubscription;
    private Subscription tokensSubscription;

    ContentResolver contentResolver;
    BriteContentResolver briteContentResolver;
    SQLiteOpenHelper dbOpenHelper;
    private LocalBitcoins localBitcoins;
    private DbManager dbManager;
    private NotificationService notificationService;
    private SharedPreferences sharedPreferences;

    private Handler handler;
    
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);

        sharedPreferences = getContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        
        notificationService = new NotificationService(context, sharedPreferences);
        
        localBitcoins = initLocalBitcoins();
        
        dbOpenHelper = new DbOpenHelper(context.getApplicationContext());

        SqlBrite sqlBrite = SqlBrite.create();
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(dbOpenHelper);
        
        contentResolver = context.getContentResolver();
        briteContentResolver = sqlBrite.wrapContentProvider(contentResolver);
        
        dbManager = new DbManager(db, briteContentResolver, contentResolver);

        handler = new Handler();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        Timber.d("onPerformSync");

        dbManager.isLoggedIn()
        .subscribe(new Action1<Boolean>()
        {
            @Override
            public void call(Boolean isLoggedIn)
            {
                if (isLoggedIn) {
                    // refresh handler to give a little room on initial install
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateContacts();
                            updateWalletBalance();
                            //updateAdvertisements();
                        }
                    }, 10000);
                }
            }
        });
    }

    @Override
    public void onSyncCanceled()
    {
        super.onSyncCanceled();

        if(walletSubscription != null)
            walletSubscription.unsubscribe();
        
        if(contactsSubscription != null)
            contactsSubscription.unsubscribe();

        if(tokensSubscription != null)
            tokensSubscription.unsubscribe();
        
        if(advertisementsSubscription != null)
            advertisementsSubscription.unsubscribe();
    }

    private void updateContacts()
    {
        if(contactsSubscription != null)
            return;
        
        Timber.d("UpdateContacts");
        
        contactsSubscription = getContactsObservable()
                .subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> contacts)
                    {
                        contactsSubscription = null;
                        updateMessages(contacts);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        contactsSubscription = null;
                        handleError(throwable);
                    }
                });
    }

    private void updateAdvertisements()
    {
        if(advertisementsSubscription != null)
            return;

        Timber.d("UpdateAdvertisements");

        advertisementsSubscription = getAdvertisementObservable()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Advertisement>>()
                {
                    @Override
                    public void call(List<Advertisement> advertisements)
                    {
                        advertisementsSubscription = null;

                        updateAdvertisements(advertisements);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        advertisementsSubscription = null;

                        handleError(throwable);
                    }
                });
    }
    
    private void updateWalletBalance()
    {
        if(walletSubscription != null)
            return;
        
        Timber.d("UpdateWalletBalance");
        
        walletSubscription = getWalletBalance()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Wallet>()
                {
                    @Override
                    public void call(Wallet wallet)
                    {
                        walletSubscription = null;
                        
                        updateWalletBalance(wallet);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        walletSubscription = null;
                        
                        handleError(throwable);
                    }
                });
    }
    
    private void refreshAccessTokens()
    {
        if(tokensSubscription != null)
            return;

        Timber.d("RefreshAccessTokens");
        
        tokensSubscription = refreshTokens()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Authorization>() {
                    @Override
                    public void call(Authorization authorization)
                    {
                        tokensSubscription = null;
                        
                        updateTokens(authorization);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        
                        tokensSubscription = null;
                        
                        reportError(throwable);
                    }
                });
    }
    
    protected void reportError(Throwable throwable)
    {
        if(throwable != null && throwable.getLocalizedMessage() != null)
            Timber.e("Sync Data Error: " + throwable.getLocalizedMessage());
    }

    protected void handleError(Throwable throwable)
    {
        reportError(throwable);
        
        if(DataServiceUtils.isHttp403Error(throwable) || DataServiceUtils.isHttp400GrantError(throwable)) {
            refreshAccessTokens();
        }
    }
    
    private void updateAdvertisements(final List<Advertisement> advertisements)
    {
        final ArrayList<Advertisement> newAdvertisements = new ArrayList<Advertisement>();
        final ArrayList<String> deletedAdvertisements = new ArrayList<String>();
        final ArrayList<Advertisement> updateAdvertisements = new ArrayList<Advertisement>();

        final HashMap<String, Advertisement> entryMap = new HashMap<String, Advertisement>();
        for (Advertisement item : advertisements) {
            entryMap.put(item.ad_id, item);
        }

        Subscription subscription = briteContentResolver.createQuery(SyncProvider.ADVERTISEMENT_TABLE_URI, null, null, null, null, false)
                .map(AdvertisementItem.MAP)
                .subscribe(new Action1<List<AdvertisementItem>>()
                {
                    @Override
                    public void call(List<AdvertisementItem> advertisementItems)
                    {
                        Timber.d("Advertisements Items in Database: " + advertisementItems.size());

                        for (AdvertisementItem advertisementItem : advertisementItems) {

                            Advertisement match = entryMap.get(advertisementItem.ad_id());

                            if (match != null) {

                                entryMap.remove(advertisementItem.ad_id());

                                if ((match.price_equation != null && !match.price_equation.equals(advertisementItem.price_equation())) ||
                                        (match.temp_price != null && !match.temp_price.equals(advertisementItem.temp_price())) ||
                                        (match.city != null && !match.city.equals(advertisementItem.city())) ||
                                        (match.country_code != null && !match.country_code.equals(advertisementItem.country_code())) ||
                                        (match.bank_name != null && !match.bank_name.equals(advertisementItem.bank_name())) ||
                                        (match.currency != null && !match.currency.equals(advertisementItem.currency())) ||
                                        (match.lat != advertisementItem.lat()) || (match.lon != advertisementItem.lon()) ||
                                        (match.location != null && !match.location.equals(advertisementItem.location_string())) ||
                                        (match.max_amount != null && !match.max_amount.equals(advertisementItem.max_amount())) ||
                                        (match.min_amount != null && !match.min_amount.equals(advertisementItem.min_amount())) ||
                                        (match.max_amount_available != null && !match.max_amount_available.equals(advertisementItem.max_amount_available())) ||
                                        (match.online_provider != null && !match.online_provider.equals(advertisementItem.online_provider())) ||
                                        (match.account_info != null && !match.account_info.equals(advertisementItem.account_info())) ||
                                        (match.sms_verification_required != advertisementItem.sms_verification_required()) ||
                                        (match.visible != advertisementItem.visible() ||
                                                (match.trusted_required != advertisementItem.trusted_required()) ||
                                                (match.track_max_amount != advertisementItem.track_max_amount()) ||
                                                (match.message != null && !match.message.equals(advertisementItem.message())))) {

                                    updateAdvertisements.add(match);
                                }

                            } else {

                                deletedAdvertisements.add(advertisementItem.ad_id());
                            }
                        }

                        for (Advertisement advertisement : entryMap.values()) {
                            newAdvertisements.add(advertisement);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();

        for (Advertisement item : newAdvertisements) {
            AdvertisementItem.Builder builder = AdvertisementItem.createBuilder(item);
            contentResolver.insert(SyncProvider.ADVERTISEMENT_TABLE_URI, builder.build());
        }

        for (Advertisement item : updateAdvertisements) {
            AdvertisementItem.Builder builder = AdvertisementItem.createBuilder(item);
            contentResolver.update(SyncProvider.ADVERTISEMENT_TABLE_URI, builder.build(), AdvertisementItem.AD_ID + " = ?", new String[]{item.ad_id});
        }

        for (String id : deletedAdvertisements) {
            contentResolver.delete(SyncProvider.ADVERTISEMENT_TABLE_URI, AdvertisementItem.AD_ID + " = ?", new String[]{id});
        }
    }

    private void updateWalletBalance(final Wallet wallet)
    {
        Timber.d("Update Wallet");
        
        Subscription subscription = briteContentResolver.createQuery(SyncProvider.WALLET_TABLE_URI, null, null, null, null, false)
                .map(WalletItem.MAP)
                .subscribe(new Action1<WalletItem>()
                {
                    @Override
                    public void call(WalletItem walletItem)
                    {
                        // TODO This has to be in some type of async
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

                        if (walletItem != null) {

                            if (!walletItem.address().equals(wallet.address)
                                    || !walletItem.balance().equals(wallet.balance)
                                    || !walletItem.receivable().equals(wallet.received)
                                    || !walletItem.sendable().equals(wallet.sendable)) {

                                WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                                contentResolver.update(SyncProvider.WALLET_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(walletItem.id())});
                            }

                        } else {

                            WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, builder.build());
                        }

                        if (walletItem == null) {

                            notificationService.balanceUpdateNotification("Bitcoin Balance", "Bitcoin balance...", "You have " + wallet.balance + " BTC");

                        } else {

                            double newBalance = Doubles.convertToDouble(wallet.balance);
                            double oldBalance = Doubles.convertToDouble(walletItem.balance());
                            String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);

                            if (newBalance > oldBalance) {
                                notificationService.balanceUpdateNotification("Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC");
                            }
                        }

                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
    }
    
    private void getDeletedContactsInfo(List<String> contacts)
    {
        Timber.e("deleted contacts: " + contacts.size());
        
        getContactInfo(contacts)
                .onErrorResumeNext(getContactInfo(contacts))
                .subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> contacts)
                    {
                        Timber.e("List of Deleted Contacts Size: " + contacts.size());
                        
                        if(!contacts.isEmpty())
                            notificationService.contactDeleteNotification(contacts);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });
    }

    private Observable<Wallet> getWalletBalance()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Wallet>>()
                {
                    @Override
                    public Observable<Wallet> call(final SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;
                        Timber.d("Access Token: " + sessionItem.access_token());
                        return localBitcoins.getWalletBalance(sessionItem.access_token())
                                .map(new ResponseToWalletBalance())
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

    // TODO move this to util as its used in other locations
    private Observable<Bitmap> generateBitmap(final String address)
    {
        return Observable.create(new Observable.OnSubscribe<Bitmap>()
        {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber)
            {
                try {
                    subscriber.onNext(WalletUtils.encodeAsBitmap(address, getContext()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private Observable<List<Contact>> getContactInfo(final List<String> contactIds)
    {
        final List<Contact> contactList = Collections.emptyList();
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(final SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;
                        //Timber.d("Access Token: " + sessionItem.access_token());
                        return Observable.just(Observable.from(contactIds)
                                .flatMap(new Func1<String, Observable<? extends List<Contact>>>()
                                {
                                    @Override
                                    public Observable<? extends List<Contact>> call(final String contactId)
                                    {
                                        return localBitcoins.getContact(contactId, sessionItem.access_token())
                                                .map(new ResponseToContact())
                                                .map(new Func1<Contact, List<Contact>>()
                                                {
                                                    @Override
                                                    public List<Contact> call(Contact contactResult)
                                                    {
                                                        if(contactResult != null && (TradeUtils.isCanceledTrade(contactResult) || TradeUtils.isReleased(contactResult))) {
                                                           contactList.add(contactResult);
                                                        }

                                                        return contactList;
                                                    }
                                                });
                                    }
                                }).toBlocking().lastOrDefault(contactList));
                    }
                });
    }

    private Observable<List<Advertisement>> getAdvertisementObservable()
    {
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Advertisement>>>()
                {
                    @Override
                    public Observable<List<Advertisement>> call(final SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;
                        Timber.d("Access Token: " + sessionItem.access_token());
                        return localBitcoins.getAds(sessionItem.access_token())
                                .map(new ResponseToAds());

                    }
                });
    }

    private Observable<List<Contact>> getContactsObservable()
    {
        /*List<Message> messages = Parser.parseMessages(MockData.MESSAGES);
        List<Contact> contacts = Parser.parseContacts(MockData.DASHBOARD);
        
        Contact contact = contacts.get(0);
        contact.messages = messages;
        
        return Observable.just(contacts);*/
        
        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<List<Contact>>>()
                {
                    @Override
                    public Observable<List<Contact>> call(final SessionItem sessionItem)
                    {
                        if (sessionItem == null) return null;
                        Timber.d("Access Token: " + sessionItem.access_token());
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
                                        return getContactsMessageObservable(contacts, sessionItem.access_token());
                                    }
                                });
                    }
                });
    }

    private Observable<List<Contact>> getContactsMessageObservable(final List<Contact> contacts, final String access_token)
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
                                        for (Message message : messages) {
                                            message.contact_id = contact.contact_id;
                                        }

                                        contact.messages = messages;
                                        return contacts;
                                    }
                                });
                    }
                }).toBlocking().last());
    }


    public Observable<SessionItem> getTokens()
    {
        return dbManager.getTokens();
    }
    
    public void updateTokens(Authorization authorization)
    {
        Timber.d("Access Token: " + authorization.access_token);
        Timber.d("Refresh Token : " + authorization.refresh_token);

        final SessionItem.Builder builder = new SessionItem.Builder()
                .access_token(authorization.access_token)
                .refresh_token(authorization.refresh_token);

        Subscription subscription = briteContentResolver.createQuery(SyncProvider.SESSION_TABLE_URI, null, null, null, null, false)
                .map(SessionItem.MAP)
                .subscribe(new Action1<SessionItem>()
                {
                    @Override
                    public void call(SessionItem sessionItem)
                    {
                        if (sessionItem != null) {
                            contentResolver.update(SyncProvider.SESSION_TABLE_URI, builder.build(), SessionItem.ID + " = ?", new String[]{String.valueOf(sessionItem.id())});
                        } else {
                            contentResolver.insert(SyncProvider.SESSION_TABLE_URI, builder.build());
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
    }
    
    LocalBitcoins initLocalBitcoins()
    {
        OkHttpClient okHttpClient = new OkHttpClient();
        OkClient client = new OkClient(okHttpClient);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(Constants.BASE_URL)
                .build();
        return restAdapter.create(LocalBitcoins.class);
    }

    private Observable<Authorization> refreshTokens()
    {
        Timber.d("refreshTokens");

        return getTokens()
                .flatMap(new Func1<SessionItem, Observable<Authorization>>() {
                    @Override
                    public Observable<Authorization> call(SessionItem sessionItem)
                    {
                        if(sessionItem == null) 
                            return null;
                        
                        Timber.d("Refresh Token: " + sessionItem.refresh_token());
                        return localBitcoins.refreshToken("refresh_token", sessionItem.refresh_token(), Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                                .map(new ResponseToAuthorize());
                    }
                });
    }
    
    private void updateContactsData(final HashMap<String, Contact> entryMap)
    {
        Timber.d("Update Contacts Data Size: " + entryMap.size());
        
        final ArrayList<Contact> newContacts = new ArrayList<Contact>();
        final ArrayList<String> deletedContacts = new ArrayList<String>();
        final ArrayList<Contact> updatedNotifyContacts = new ArrayList<Contact>();
        final ArrayList<Contact> updatedContacts = new ArrayList<Contact>();
        
        Subscription subscription = briteContentResolver.createQuery(SyncProvider.CONTACT_TABLE_URI, null, null, null, null, false)
                .map(ContactItem.MAP)
                .subscribe(new Action1<List<ContactItem>>()
                {
                    @Override
                    public void call(List<ContactItem> contactItems)
                    {
                        Timber.d("Contact Items in Database: " + contactItems.size());
                        for (ContactItem contactItem : contactItems) {

                            Contact match = entryMap.get(contactItem.contact_id());

                            if (match != null) {

                                entryMap.remove(contactItem.contact_id());

                                if ((match.payment_completed_at != null && !match.payment_completed_at.equals(contactItem.payment_completed_at()))
                                        || (match.closed_at != null && !match.closed_at.equals(contactItem.closed_at()))
                                        || (match.disputed_at != null && !match.disputed_at.equals(contactItem.disputed_at()))
                                        || (match.escrowed_at != null && !match.escrowed_at.equals(contactItem.escrowed_at()))
                                        || (match.funded_at != null && !match.funded_at.equals(contactItem.funded_at()))
                                        || (match.released_at != null && !match.released_at.equals(contactItem.released_at()))
                                        || (match.canceled_at != null && !match.canceled_at.equals(contactItem.canceled_at()))
                                        || (match.actions.fund_url != null && !match.actions.fund_url.equals(contactItem.funded_at()))
                                        || (match.is_funded != contactItem.is_funded())) {

                                    updatedNotifyContacts.add(match);
                                    updatedContacts.add(match);

                                } else if ((match.seller.last_online != null && !match.seller.last_online.equals(contactItem.seller_last_online()))
                                        || (match.hasUnseenMessages != contactItem.hasUnseenMessages())
                                        || (match.messageCount != contactItem.messageCount())
                                        || (match.buyer.last_online != null && !match.buyer.last_online.equals(contactItem.buyer_last_online()))) {
                                    updatedContacts.add(match);
                                }
                            } else {

                                deletedContacts.add(contactItem.contact_id());
                            }
                        }

                        for (Contact contact : entryMap.values()) {
                            newContacts.add(contact);
                        }
                        
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();
        
        Timber.d("New Contact Items: " + newContacts.size());

        for (Contact item : newContacts) {
            Timber.d("New Contact Id: " + item.contact_id);
            int messageCount = item.messages.size();
            contentResolver.insert(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(item, messageCount, true).build());
        }

        Timber.d("Updated Contact Items: " + updatedContacts.size());
        
        for (Contact item : updatedContacts) {
            Timber.d("Update Contact Id: " + item.contact_id);
            int messageCount = item.messages.size();
            contentResolver.update(SyncProvider.CONTACT_TABLE_URI, ContactItem.createBuilder(item, messageCount, item.hasUnseenMessages).build(), ContactItem.CONTACT_ID + " = ?", new String[]{item.contact_id});
        }

        Timber.d("Delete Contacts: " + deletedContacts.size());
        
        for (String id : deletedContacts) {
            Timber.d("Delete Contact Id: " + id);
            contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{id});
            contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{id});
        }
        
        if(!newContacts.isEmpty())
            notificationService.contactNewNotification(newContacts);

        if(!updatedNotifyContacts.isEmpty())
            notificationService.contactUpdateNotification(updatedNotifyContacts);

        // look up deleted trades and find the reason
        if (!deletedContacts.isEmpty()) {
            getDeletedContactsInfo(deletedContacts);
        }
    }
    
    public void updateMessages(final List<Contact> contacts)
    {
        final ArrayList<String> deletedMessages = new ArrayList<String>();
        final ArrayList<Message> newMessages = new ArrayList<Message>();
        final ArrayList<Message> deleteMessages = new ArrayList<Message>();
        final HashMap<String, Contact> contactMap = new HashMap<String, Contact>();
        final HashMap<String, Message> entryMap = new HashMap<String, Message>();
        
        for (Contact contact : contacts) {

            //contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{contact.contact_id});
            //contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{contact.contact_id});

            contact.messageCount = contact.messages.size(); // update item message count
            contactMap.put(contact.contact_id, contact);

            Timber.d("Message Contact Id: " + contact.contact_id);
            Timber.d("Contact Message Count: " + contact.messageCount);

            for (Message message : contact.messages) {
                message.id = contact.contact_id + "_" + message.created_at;
                message.contact_id = contact.contact_id;
                entryMap.put(message.id, message);
            }
        }

        // get all the current messages
        Subscription subscription = briteContentResolver.createQuery(SyncProvider.MESSAGE_TABLE_URI, null, null, null, null, false)
                .map(MessageItem.MAP)
                .subscribe(new Action1<List<MessageItem>>()
                {
                    @Override
                    public void call(List<MessageItem> messageItems)
                    {
                        for (MessageItem messageItem : messageItems) {
                            String id = messageItem.contact_id() + "_" + messageItem.create_at();
                            Message match = entryMap.get(id);
                            if (match != null) {
                                entryMap.remove(id);
                            } else {
                                deletedMessages.add(String.valueOf(messageItem.contact_id()));
                            }
                        }

                        for (Message message : entryMap.values()) {
                            newMessages.add(message);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });

        subscription.unsubscribe();

        StringPreference stringPreference = new StringPreference(sharedPreferences, DbManager.PREFS_USER);
        String username = stringPreference.get();
        
        for (Message item : newMessages) {
            
            contentResolver.insert(SyncProvider.MESSAGE_TABLE_URI, MessageItem.createBuilder(item).build());
            Contact contact = contactMap.get(item.contact_id);
            
            if(!item.sender.username.equals(username)) {
                contact.hasUnseenMessages = true; 
            }
        }

        if(!newMessages.isEmpty())
            notificationService.messageNotifications(newMessages);

        for (String id : deletedMessages) {
            contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{id});
        }
        
        updateContactsData(contactMap); // let's update contacts now
    }
}
