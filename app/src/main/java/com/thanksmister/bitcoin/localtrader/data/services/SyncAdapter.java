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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.Db;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.DbOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.MessageItem;
import com.thanksmister.bitcoin.localtrader.data.database.NotificationItem;
import com.thanksmister.bitcoin.localtrader.data.database.RecentMessageItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Singleton;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

@Singleton
public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private static final String BITCOIN_AVERAGE_ENDPOINT = "https://api.bitcoinaverage.com";
    public static final String BASE_URL = "https://localbitcoins.com/";
    
    private DataService dataService;
    private DbManager dbManager;
    private NotificationService notificationService;
    private SharedPreferences sharedPreferences;
    private ContentResolver contentResolver;
    private BriteContentResolver briteContentResolver;
   
    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        
        sharedPreferences = getContext().getApplicationContext().getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
        notificationService = new NotificationService(context, sharedPreferences);
        DbOpenHelper dbOpenHelper = new DbOpenHelper(context.getApplicationContext());
        SqlBrite sqlBrite = SqlBrite.create();
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(dbOpenHelper);
        contentResolver = context.getContentResolver();
        briteContentResolver = sqlBrite.wrapContentProvider(contentResolver);
        
        dbManager = new DbManager(db, briteContentResolver, contentResolver);
        LocalBitcoins localBitcoins = initLocalBitcoins();
        BitcoinAverage bitcoinAverage = initBitcoinAverage();
        dataService = new DataService(dbManager, (BaseApplication) context.getApplicationContext(), sharedPreferences, localBitcoins, bitcoinAverage);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
    {
        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        Timber.d("onPerformSync: " + hasCredentials);
        getContacts();
    }

    @Override
    public void onSyncCanceled()
    {
        super.onSyncCanceled();
    }
    
    private void getContacts()
    {
        Timber.d("getContacts");

        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        if (!hasCredentials) {
            return;
        }
        
        dataService.getContacts(DashboardType.ACTIVE)
                .subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> contacts)
                    {
                        dbManager.updateContacts(contacts);
                        updateContacts(contacts);
                        getNotifications();
                    }

                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable);
                        getNotifications();
                    }
                });
    }

    private void getWalletBalance()
    {
        Timber.d("getWalletBalance");

        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        if (!hasCredentials) {
            return;
        }
        
        dataService.getWalletBalance()
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
                                        reportError(throwable);
                                        return wallet;
                                    }
                                });
                    }
                })
                .subscribe(new Action1<Wallet>()
                {
                    @Override
                    public void call(Wallet wallet)
                    {
                        updateWalletBalance(wallet);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable);
                    }
                });
    }

    @Deprecated
    private void getRecentMessages()
    {
        Timber.d("getRecentMessages");

        dataService.getRecentMessages()
                .subscribe(new Action1<List<Message>>()
                {
                    @Override
                    public void call(List<Message> messages)
                    {
                        updateRecentMessages(messages);
                        getWalletBalance();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable);
                        getWalletBalance();
                    }
                });
    }

    private void getNotifications()
    {
        Timber.d("getNotifications");

        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        if (!hasCredentials) {
            return;
        }

        dataService.getNotifications()
                .subscribe(new Action1<List<Notification>>()
                {
                    @Override
                    public void call(List<Notification> notifications)
                    {
                        updateNotifications(notifications);
                        getWalletBalance();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable);
                        getWalletBalance();
                    }
                });
    }

    protected void reportError(Throwable throwable)
    {
        if (throwable != null && throwable.getMessage() != null) {
            Timber.e("Sync Data Error: " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    protected void handleError(Throwable throwable)
    {
        reportError(throwable);
    }

    /**
     * Updates the notifications list by adding only the newest
     * notifications, updating the current notifications status
     */
    private void updateNotifications(final List<Notification> notifications)
    {
        Timber.d("updateNotifications : " + notifications.size());

        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        if (!hasCredentials) {
            return;
        }
        
        final HashMap<String, Notification> entryMap = new HashMap<>();
        for (Notification notification : notifications) {
            entryMap.put(notification.notification_id, notification);
        }

       
        Cursor cursor = contentResolver.query(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, NotificationItem.ID);
                String notificationId = Db.getString(cursor, NotificationItem.NOTIFICATION_ID);
                boolean notificationRead = Db.getBoolean(cursor, NotificationItem.READ);
                Notification match = entryMap.get(notificationId);
                if (match != null) {
                    entryMap.remove(notificationId);
                    if(match.read != notificationRead) {
                        NotificationItem.Builder builder = NotificationItem.createBuilder(match);
                        contentResolver.update(SyncProvider.NOTIFICATION_TABLE_URI, builder.build(), NotificationItem.ID + " = ?", new String[]{String.valueOf(id)});
                    }
                } else {
                    contentResolver.delete(SyncProvider.NOTIFICATION_TABLE_URI, NotificationItem.ID + " = ?", new String[]{String.valueOf(id)});
                }
            }
            cursor.close();
        }
        
        List<Notification> newNotifications = new ArrayList<>();
        if (!entryMap.isEmpty()) {
            for (Notification notification : entryMap.values()) {
                newNotifications.add(notification);
                contentResolver.insert(SyncProvider.NOTIFICATION_TABLE_URI, NotificationItem.createBuilder(notification).build());
            }
        }

        Timber.d("updateNotifications newNotifications: " + newNotifications.size());
        if(!newNotifications.isEmpty()) {
            notificationService.createNotifications(newNotifications);
        }
    }

    /**
     * Updates the recent messages list by adding only the newest
     * messages, retaining the state of the current messages
     */
    private void updateRecentMessages(final List<Message> messages)
    {
        Timber.d("updateRecentMessages");

        // build a map of the ids to check against current records in database
        String userName = AuthUtils.getUsername(sharedPreferences);
        final HashMap<String, Message> entryMap = new HashMap<>();
        for (Message message : messages) {
            if(!userName.equals(message.sender.username)) {
                String messageId = message.contact_id + message.created_at;
                entryMap.put(messageId, message);
            }
        }

        List<Message> newMessages = new ArrayList<>();
        Cursor cursor = contentResolver.query(SyncProvider.RECENT_MESSAGE_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = Db.getLong(cursor, RecentMessageItem.ID);
                String contactId = Db.getString(cursor, RecentMessageItem.CONTACT_ID);
                String createdAt = Db.getString(cursor, RecentMessageItem.CREATED_AT);
                String messageId = String.valueOf(contactId) + String.valueOf(createdAt);
                Message match = entryMap.get(messageId);
                if (match != null) {
                    entryMap.remove(messageId);
                } else {
                    contentResolver.delete(SyncProvider.RECENT_MESSAGE_TABLE_URI, RecentMessageItem.ID + " = ?", new String[]{String.valueOf(id)});
                }
            }
            cursor.close();
        } 
        
        if (!entryMap.isEmpty()) {
            for (Message message : entryMap.values()) {
                newMessages.add(message);
                contentResolver.insert(SyncProvider.RECENT_MESSAGE_TABLE_URI, RecentMessageItem.createBuilder(message).build());
            }
        }

        if(!newMessages.isEmpty()) {
            notificationService.messageNotifications(newMessages);
        }
    }
    
    private void updateWalletBalance(final Wallet wallet)
    {
        Timber.d("updateWalletBalance");

        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        if (!hasCredentials) {
            return;
        }
        
        Cursor cursor = contentResolver.query(SyncProvider.WALLET_TABLE_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            long id = Db.getLong(cursor, WalletItem.ID);
            String address = Db.getString(cursor, WalletItem.ADDRESS);
            String balance = Db.getString(cursor, WalletItem.BALANCE);
            if (!address.equals(wallet.address) || !balance.equals(wallet.balance)){
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
                WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                contentResolver.update(SyncProvider.WALLET_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(id)});
                try {
                    double newBalance = Doubles.convertToDouble(wallet.balance);
                    double oldBalance = Doubles.convertToDouble(balance);
                    String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);
                    Timber.d("updateWalletBalance newBalance: " + newBalance);
                    Timber.d("updateWalletBalance oldBalance: " + oldBalance);
                    if (newBalance > oldBalance) {
                        notificationService.balanceUpdateNotification("Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC");
                    }
                } catch (Exception e) {
                    reportError(e);
                }
            }
            cursor.close();
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
            WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, builder.build());
            Timber.d("updateWalletBalance Init Balance: " + wallet.balance);
            notificationService.balanceUpdateNotification("Bitcoin Balance", "Bitcoin balance...", "You have " + wallet.balance + " BTC");
        }
        
        /*Subscription subscription = briteContentResolver.createQuery(SyncProvider.WALLET_TABLE_URI, null, null, null, null, false)
                .map(WalletItem.MAP)
                .subscribe(new Action1<WalletItem>()
                {
                    @Override
                    public void call(WalletItem walletItem)
                    {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        wallet.qrImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

                        if (walletItem != null) {

                            if (!walletItem.address().equals(wallet.address) || !walletItem.balance().equals(wallet.balance)){

                                WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                                contentResolver.update(SyncProvider.WALLET_TABLE_URI, builder.build(), WalletItem.ID + " = ?", new String[]{String.valueOf(walletItem.id())});
                            }

                        } else {

                            WalletItem.Builder builder = WalletItem.createBuilder(wallet, baos);
                            contentResolver.insert(SyncProvider.WALLET_TABLE_URI, builder.build());
                        }

                        if (walletItem == null) {

                            Timber.d("updateWalletBalance Init Balance: " + wallet.balance);
                         
                            notificationService.balanceUpdateNotification("Bitcoin Balance", "Bitcoin balance...", "You have " + wallet.balance + " BTC");

                        } else {

                            try {
                                double newBalance = Doubles.convertToDouble(wallet.balance);
                                double oldBalance = Doubles.convertToDouble(walletItem.balance());
                                String diff = Conversions.formatBitcoinAmount(newBalance - oldBalance);

                                Timber.d("updateWalletBalance newBalance: " + newBalance);
                                Timber.d("updateWalletBalance oldBalance: " + oldBalance);

                                if (newBalance > oldBalance) {
                                    notificationService.balanceUpdateNotification("Bitcoin Received", "Bitcoin received...", "You received " + diff + " BTC");
                                }
                                
                            } catch (Exception e) {
                                reportError(e);
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

        subscription.unsubscribe();*/
    }

    private void getDeletedContactsInfo(List<String> contacts)
    {
        getContactInfo(contacts)
                .subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> contacts)
                    {
                        if (!contacts.isEmpty())
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
        return Observable.just(Observable.from(contactIds)
                .flatMap(new Func1<String, Observable<? extends List<Contact>>>()
                {
                    @Override
                    public Observable<? extends List<Contact>> call(final String contactId)
                    {
                        return dataService.getContact(contactId)
                                .map(new Func1<Contact, List<Contact>>()
                                {
                                    @Override
                                    public List<Contact> call(Contact contactResult)
                                    {
                                        if (contactResult != null) {
                                            contactList.add(contactResult);
                                        }

                                        return contactList;
                                    }
                                });
                    }
                }).toBlocking().last());
    }

    private BitcoinAverage initBitcoinAverage()
    {
        OkHttpClient okHttpClient = new OkHttpClient();
        OkClient client = new OkClient(okHttpClient);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(BITCOIN_AVERAGE_ENDPOINT)
                .build();
        return restAdapter.create(BitcoinAverage.class);
    }

    private LocalBitcoins initLocalBitcoins()
    {
        OkHttpClient okHttpClient = new OkHttpClient();
        OkClient client = new OkClient(okHttpClient);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(BASE_URL)
                .build();
        return restAdapter.create(LocalBitcoins.class);
    }

    private void updateContacts(List<Contact> contacts)
    {
        Timber.d("Update Contacts Data Size: " + contacts.size());

        boolean hasCredentials = AuthUtils.hasCredentials(sharedPreferences);
        if (!hasCredentials) {
            return;
        }

        final HashMap<String, Contact> entryMap = new HashMap<String, Contact>();
        for (Contact contact : contacts) {
            entryMap.put(contact.contact_id, contact);
        }

        final ArrayList<Contact> newContacts = new ArrayList<Contact>();
        final ArrayList<String> deletedContacts = new ArrayList<String>();
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
            Timber.d("We have deleted contacts!");
            Timber.d("Delete Contact Id: " + id);
            contentResolver.delete(SyncProvider.CONTACT_TABLE_URI, ContactItem.CONTACT_ID + " = ?", new String[]{id});
            contentResolver.delete(SyncProvider.MESSAGE_TABLE_URI, MessageItem.CONTACT_LIST_ID + " = ?", new String[]{id});
        }
    }
}
