/*
 * Copyright 2007 ZXing authors
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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.thanksmister.bitcoin.localtrader.data.database.DbOpenHelper;
import com.thanksmister.bitcoin.localtrader.data.database.RecentMessageItem;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 */
public class SyncProvider extends ContentProvider
{
    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "com.thanksmister.bitcoin.localtrader.provider";

    /**
     * Base URI. (content://com.thanksmister.bitcoin.localtrader.provider)
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    

    public static final Uri METHOD_TABLE_URI = CONTENT_URI.buildUpon().appendPath("method_item").build();
    public static final Uri EXCHANGE_TABLE_URI = CONTENT_URI.buildUpon().appendPath("exchange_item").build();
    public static final Uri CONTACT_TABLE_URI = CONTENT_URI.buildUpon().appendPath("contact_item").build();
    public static final Uri ADVERTISEMENT_TABLE_URI = CONTENT_URI.buildUpon().appendPath("advertisement_item").build();
    public static final Uri MESSAGE_TABLE_URI = CONTENT_URI.buildUpon().appendPath("message_item").build();
    public static final Uri RECENT_MESSAGE_TABLE_URI = CONTENT_URI.buildUpon().appendPath(RecentMessageItem.TABLE).build();
    public static final Uri WALLET_TABLE_URI = CONTENT_URI.buildUpon().appendPath("wallet_item").build();
   
    DbOpenHelper dbOpenHelper;
    ContentResolver contentResolver;

    @Override
    public boolean onCreate()
    {
        dbOpenHelper = new DbOpenHelper(getContext());
        contentResolver = getContext().getContentResolver();
        return true;
    }

    @Override
    public String getType(Uri arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int delete(Uri uri, String where, String[] args)
    {
        String table = getTableName(uri);
        SQLiteDatabase dataBase = dbOpenHelper.getWritableDatabase();
        contentResolver.notifyChange(uri, null);
        return dataBase.delete(table, where, args);
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues)
    {
        String table = getTableName(uri);
        SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
        long value = database.insert(table, null, initialValues);
        contentResolver.notifyChange(uri, null);
        return Uri.withAppendedPath(CONTENT_URI, String.valueOf(value));
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        String table = getTableName(uri);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        return database.query(table, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs)
    {
        String table = getTableName(uri);
        SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
        contentResolver.notifyChange(uri, null);
        return database.update(table, values, whereClause, whereArgs);
    }

    public static String getTableName(Uri uri)
    {
        String value = uri.getPath();
        value = value.replace("/", "");//we need to remove '/'
        return value;
    }
}
