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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementContract;
import com.thanksmister.bitcoin.localtrader.data.database.BaseContract;
import com.thanksmister.bitcoin.localtrader.data.database.ContactContract;
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseHelper;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeContract;
import com.thanksmister.bitcoin.localtrader.data.database.MessagesContract;
import com.thanksmister.bitcoin.localtrader.data.database.WalletContract;
import com.thanksmister.bitcoin.localtrader.utils.SelectionBuilder;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 */
public class SyncProvider extends ContentProvider
{
    private static final String CLASS_NAME = SyncProvider.class.getSimpleName() + " ";

    DatabaseHelper databaseHelper;

    /**
     * Content authority for this provider.
     */
    private static final String AUTHORITY = BaseContract.CONTENT_AUTHORITY;

    public static final int PATH_CONTACTS = 1;
    public static final int PATH_CONTACTS_ID = 2;
    
    public static final int PATH_WALLET = 3;
    public static final int PATH_WALLET_ID = 4;
    
    public static final int PATH_ADVERTISEMENTS = 5;
    public static final int PATH_ADVERTISEMENTS_ID = 6;

    public static final int PATH_MESSAGES = 7;
    public static final int PATH_MESSAGES_ID = 8;

    public static final int PATH_SESSION = 9;
    public static final int PATH_SESSION_ID = 10;

    public static final int PATH_EXCHANGES = 11;
    public static final int PATH_EXCHANGES_ID = 12;

    // Uri Matcher for the content provider
    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        
        sUriMatcher.addURI(AUTHORITY, "wallets", PATH_WALLET);
        sUriMatcher.addURI(AUTHORITY, "wallets/*", PATH_WALLET_ID);

        sUriMatcher.addURI(AUTHORITY, "advertisements", PATH_ADVERTISEMENTS);
        sUriMatcher.addURI(AUTHORITY, "advertisements/*", PATH_ADVERTISEMENTS_ID);
        
        sUriMatcher.addURI(AUTHORITY, "contacts", PATH_CONTACTS);
        sUriMatcher.addURI(AUTHORITY, "contacts/*", PATH_CONTACTS_ID);
        
        sUriMatcher.addURI(AUTHORITY, "messages", PATH_MESSAGES);
        sUriMatcher.addURI(AUTHORITY, "messages/*", PATH_MESSAGES_ID);

        sUriMatcher.addURI(AUTHORITY , "session", PATH_SESSION);
        sUriMatcher.addURI(AUTHORITY , "session/*", PATH_SESSION_ID);

        sUriMatcher.addURI(AUTHORITY, "exchanges", PATH_EXCHANGES);
        sUriMatcher.addURI(AUTHORITY, "exchanges/*", PATH_EXCHANGES_ID);
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri)
    {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PATH_CONTACTS:
                return ContactContract.ContactData.CONTENT_TYPE;
            case PATH_CONTACTS_ID:
                return ContactContract.ContactData.CONTENT_ITEM_TYPE; 
            case PATH_MESSAGES:
                return MessagesContract.Message.CONTENT_TYPE;
            case PATH_MESSAGES_ID:
                return MessagesContract.Message.CONTENT_ITEM_TYPE;
            case PATH_EXCHANGES:
                return ExchangeContract.Exchange.CONTENT_TYPE;
            case PATH_EXCHANGES_ID:
                return ExchangeContract.Exchange.CONTENT_ITEM_TYPE;
            case PATH_ADVERTISEMENTS:
                return AdvertisementContract.Advertisement.CONTENT_TYPE;
            case PATH_ADVERTISEMENTS_ID:
                return AdvertisementContract.Advertisement.CONTENT_ITEM_TYPE;
            case PATH_SESSION:
                return SessionContract.Session.CONTENT_TYPE;
            case PATH_SESSION_ID:
                return SessionContract.Session.CONTENT_ITEM_TYPE;
            case PATH_WALLET:
                return WalletContract.Wallet.CONTENT_TYPE;
            case PATH_WALLET_ID:
                return WalletContract.Wallet.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("URI " + uri + " is not supported.");
        }
    }

    /**
     * Perform a database query by URI.
     *
     * <p>Currently supports returning all entries (/entries) and individual entries by ID
     * (/entries/{ID}).
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        String id;
        Cursor c;
        Context ctx = getContext();
        switch (uriMatch) {
            case PATH_CONTACTS_ID :
                // Return a single entry, by ID.
                id = uri.getLastPathSegment();
                builder.where(ContactContract.ContactData._ID + "=?", id);
            case PATH_CONTACTS:
                // Return all known entries.
                builder.table(ContactContract.ContactData.TABLE_NAME).where(selection, selectionArgs);
                c = builder.query(db, projection, sortOrder);
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            case PATH_EXCHANGES_ID :
                // Return a single entry, by ID.
                id = uri.getLastPathSegment();
                builder.where(ExchangeContract.Exchange._ID + "=?", id);
            case PATH_EXCHANGES:
                // Return all known entries.
                builder.table(ExchangeContract.Exchange.TABLE_NAME).where(selection, selectionArgs);
                c = builder.query(db, projection, sortOrder);
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            case PATH_MESSAGES_ID :
                // Return a single entry, by ID.
                id = uri.getLastPathSegment();
                builder.where(MessagesContract.Message._ID + "=?", id);
            case PATH_MESSAGES:
                // Return all known entries.
                builder.table(MessagesContract.Message.TABLE_NAME).where(selection, selectionArgs);
                c = builder.query(db, projection, sortOrder);
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;

            case PATH_ADVERTISEMENTS_ID :
                // Return a single entry, by ID.
                id = uri.getLastPathSegment();
                builder.where(AdvertisementContract.Advertisement._ID + "=?", id);
            case PATH_ADVERTISEMENTS:
                // Return all known entries.
                builder.table(AdvertisementContract.Advertisement.TABLE_NAME).where(selection, selectionArgs);
                c = builder.query(db, projection, sortOrder);
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;

            case PATH_SESSION_ID :
                // Return a single entry, by ID.
                id = uri.getLastPathSegment();
                builder.where(SessionContract.Session._ID + "=?", id);
            case PATH_SESSION:
                // Return all known entries.
                builder.table(SessionContract.Session.TABLE_NAME).where(selection, selectionArgs);
                c = builder.query(db, projection, sortOrder);
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            case PATH_WALLET_ID:
                // Return a single entry, by ID.
                id = uri.getLastPathSegment();
                builder.where(WalletContract.Wallet._ID + "=?", id);
            case PATH_WALLET:
                // Return all known entries.
                builder.table(WalletContract.Wallet.TABLE_NAME).where(selection, selectionArgs);
                c = builder.query(db, projection, sortOrder);
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Insert a new data into the database.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values)
    {

        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        long id;
        switch (match) {
            case PATH_CONTACTS:
                id = db.insertOrThrow(ContactContract.ContactData.TABLE_NAME, null, values);
                result = Uri.parse(ContactContract.ContactData.CONTENT_URI + "/" + id);

                break;
            
            case PATH_MESSAGES:
                id = db.insertOrThrow(MessagesContract.Message.TABLE_NAME, null, values);
                result = Uri.parse(MessagesContract.Message.CONTENT_URI + "/" + id);

                break;
            case PATH_EXCHANGES:
                id = db.insertOrThrow(ExchangeContract.Exchange.TABLE_NAME, null, values);
                result = Uri.parse(ExchangeContract.Exchange.CONTENT_URI + "/" + id);
                break;
            case PATH_ADVERTISEMENTS:
                id = db.insertOrThrow(AdvertisementContract.Advertisement.TABLE_NAME, null, values);
                result = Uri.parse(AdvertisementContract.Advertisement.CONTENT_URI + "/" + id);
      
                break;
            case PATH_SESSION:
                id = db.insertOrThrow(SessionContract.Session.TABLE_NAME, null, values);
                result = Uri.parse(SessionContract.Session.CONTENT_URI + "/" + id);
                break;
            case PATH_WALLET:
                id = db.insertOrThrow(WalletContract.Wallet.TABLE_NAME, null, values);
                result = Uri.parse(WalletContract.Wallet.CONTENT_URI + "/" + id);
    
                break;
            case PATH_EXCHANGES_ID:
            case PATH_CONTACTS_ID:
            case PATH_MESSAGES_ID:
            case PATH_ADVERTISEMENTS_ID:
            case PATH_SESSION_ID:
            case PATH_WALLET_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return result;
    }

    /**
     * Delete an escrow by database by URI.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        String id;
        switch (match) {
            case PATH_CONTACTS:
                count = builder.table(ContactContract.ContactData.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_CONTACTS_ID:
                 id = uri.getLastPathSegment();
                count = builder.table(ContactContract.ContactData.TABLE_NAME)
                        .where(ContactContract.ContactData._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            
            case PATH_MESSAGES:
                count = builder.table(MessagesContract.Message.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_MESSAGES_ID:
                 id = uri.getLastPathSegment();
                count = builder.table(MessagesContract.Message.TABLE_NAME)
                        .where(MessagesContract.Message._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;

            case PATH_EXCHANGES:
                count = builder.table(ExchangeContract.Exchange.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_EXCHANGES_ID:
                id = uri.getLastPathSegment();
                count = builder.table(ExchangeContract.Exchange.TABLE_NAME)
                        .where(ExchangeContract.Exchange._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;

            
            case PATH_ADVERTISEMENTS:
                count = builder.table(AdvertisementContract.Advertisement.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_ADVERTISEMENTS_ID:
                id = uri.getLastPathSegment();
                count = builder.table(AdvertisementContract.Advertisement.TABLE_NAME)
                        .where(AdvertisementContract.Advertisement._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_SESSION:
                count = builder.table(SessionContract.Session.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_SESSION_ID:
                id = uri.getLastPathSegment();
                count = builder.table(SessionContract.Session.TABLE_NAME)
                        .where(SessionContract.Session._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_WALLET:
                count = builder.table(WalletContract.Wallet.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case PATH_WALLET_ID:
                id = uri.getLastPathSegment();
                count = builder.table(WalletContract.Wallet.TABLE_NAME)
                        .where(WalletContract.Wallet._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    /**
     * Update an data in the database by URI.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        String id;
        switch (match) {
            case PATH_CONTACTS:
                count = builder.table(ContactContract.ContactData.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case PATH_CONTACTS_ID:
                id = uri.getLastPathSegment();
                count = builder.table(ContactContract.ContactData.TABLE_NAME)
                        .where(ContactContract.ContactData._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;

            case PATH_EXCHANGES:
                count = builder.table(ExchangeContract.Exchange.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case PATH_EXCHANGES_ID:
                id = uri.getLastPathSegment();
                count = builder.table(ExchangeContract.Exchange.TABLE_NAME)
                        .where(ExchangeContract.Exchange._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;

            
            case PATH_MESSAGES:
                count = builder.table(MessagesContract.Message.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case PATH_MESSAGES_ID:
                id = uri.getLastPathSegment();
                count = builder.table(MessagesContract.Message.TABLE_NAME)
                        .where(MessagesContract.Message._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;

            case PATH_ADVERTISEMENTS:
                count = builder.table(AdvertisementContract.Advertisement.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case PATH_ADVERTISEMENTS_ID:
                id = uri.getLastPathSegment();
                count = builder.table(AdvertisementContract.Advertisement.TABLE_NAME)
                        .where(AdvertisementContract.Advertisement._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;

            case PATH_SESSION:
                count = builder.table(SessionContract.Session.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case PATH_SESSION_ID:
                id = uri.getLastPathSegment();
                count = builder.table(SessionContract.Session.TABLE_NAME)
                        .where(SessionContract.Session._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;

            case PATH_WALLET:
                count = builder.table(WalletContract.Wallet.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case PATH_WALLET_ID:
                id = uri.getLastPathSegment();
                count = builder.table(WalletContract.Wallet.TABLE_NAME)
                        .where(WalletContract.Wallet._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

}
