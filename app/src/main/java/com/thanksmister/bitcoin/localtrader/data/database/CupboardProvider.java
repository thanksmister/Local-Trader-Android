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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Authorization;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactSync;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class CupboardProvider extends ContentProvider
{
    public static final int TOKEN = 0;
    public static final int TOKEN_ID = 1;

    public static final int CONTACT = 2;
    public static final int CONTACT_ID = 3;
    
    private static final String BASE_TOKEN = "tokens";
    private static final String BASE_CONTACT= "contacts";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + Constants.CONTENT_AUTHORITY);
    
    public static final Uri TOKEN_URI = BASE_CONTENT_URI.buildUpon().appendPath(BASE_TOKEN).build();
    public static final Uri CONTACT_URI = BASE_CONTENT_URI.buildUpon().appendPath(BASE_CONTACT).build();

    private CupboardSQLiteOpenHelper cupboardHelper;
    private static UriMatcher sMatcher;
    private static final Object LOCK = new Object();

    static {
        // register our models
        cupboard().register(Authorization.class);
        cupboard().register(ContactSync.class);
    }

    @Override
    public boolean onCreate()
    {
        if (sMatcher == null) {
            sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
            sMatcher.addURI(Constants.CONTENT_AUTHORITY, BASE_TOKEN, TOKEN);
            sMatcher.addURI(Constants.CONTENT_AUTHORITY, BASE_TOKEN + "/*", TOKEN_ID);  
            sMatcher.addURI(Constants.CONTENT_AUTHORITY, BASE_CONTACT, CONTACT);
            sMatcher.addURI(Constants.CONTENT_AUTHORITY, BASE_CONTACT + "/*", CONTACT_ID);
        }
        
        cupboardHelper = new CupboardSQLiteOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        synchronized (LOCK) {
            SQLiteDatabase db = cupboardHelper.getWritableDatabase();
            switch (sMatcher.match(uri)) {
                case TOKEN:
                case TOKEN_ID:
                    return cupboard().withDatabase(db).query(Authorization.class).
                            withProjection(projection).
                            withSelection(selection, selectionArgs).
                            orderBy(sortOrder).
                            getCursor();

                case CONTACT:
                case CONTACT_ID:
                    return cupboard().withDatabase(db).query(ContactSync.class).
                            withProjection(projection).
                            withSelection(selection, selectionArgs).
                            orderBy(sortOrder).
                            getCursor();
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
    }

    @Override
    public String getType(Uri uri)
    {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        synchronized (LOCK) {
            SQLiteDatabase db = cupboardHelper.getWritableDatabase();
            Class clz;
            long id = Long.getLong(uri.getLastPathSegment(), 0);
            switch (sMatcher.match(uri)) {
                case TOKEN:
                case TOKEN_ID:
                    if (id == 0) {
                        id = cupboard().withDatabase(db).put(Authorization.class, values);
                    } else {
                        id = cupboard().withDatabase(db).update(Authorization.class, values);
                    }
                    return Uri.parse(getContext().getString(R.string.authority) + "/" + BASE_TOKEN + "/" + id);

                case CONTACT:
                case CONTACT_ID:
                    if (id == 0) {
                        id = cupboard().withDatabase(db).put(ContactSync.class, values);
                    } else {
                        id = cupboard().withDatabase(db).update(ContactSync.class, values);
                    }
                    return Uri.parse(getContext().getString(R.string.authority) + "/" + BASE_CONTACT + "/" + id);
                
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        synchronized (LOCK) {
            SQLiteDatabase db = cupboardHelper.getWritableDatabase();
            switch (sMatcher.match(uri)) {
                case TOKEN:
                case TOKEN_ID:
                    return cupboard().withDatabase(db).delete(Authorization.class, selection, selectionArgs);

                case CONTACT:
                case CONTACT_ID:
                    return cupboard().withDatabase(db).delete(ContactSync.class, selection, selectionArgs);
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        synchronized (LOCK) {
            SQLiteDatabase db = cupboardHelper.getWritableDatabase();
            switch (sMatcher.match(uri)) {
                case TOKEN:
                case TOKEN_ID:
                    return cupboard().withDatabase(db).update(Authorization.class, values, selection, selectionArgs);
                case CONTACT:
                case CONTACT_ID:
                    return cupboard().withDatabase(db).update(ContactSync.class, values, selection, selectionArgs);
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
    }
}
