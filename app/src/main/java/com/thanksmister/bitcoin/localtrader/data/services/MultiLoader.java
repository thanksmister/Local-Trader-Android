package com.thanksmister.bitcoin.localtrader.data.services;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;

import com.thanksmister.bitcoin.localtrader.data.database.ContactContract;
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseHelper;
import com.thanksmister.bitcoin.localtrader.data.database.DatabaseManager;

import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 2/28/15
 * Copyright 2013, ThanksMister LLC
 */
public class MultiLoader extends AsyncTaskLoader
{
    Context context;
    DatabaseHelper databaseHelper;
    DatabaseManager databaseManager;

    public MultiLoader(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Cursor loadInBackground() {
        
        Timber.d("loadInBackground");

        databaseHelper = new DatabaseHelper(context);
        databaseManager = new DatabaseManager();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        
        /*** create a custom cursor whether it is join of multiple tables or complex query**/
        //Cursor cursor = db.query(<TableName>, null,null, null, null, null, null, null);

        Cursor cursor = db.rawQuery("SELECT * FROM contacts_table, advertisement_table, exchange_table" +
                "GROUP BY Table1.data1", null);

        final String MY_QUERY = "SELECT * FROM contacts_table a INNER JOIN advertisement_table b INNER JOIN exchange_table c ";

        db.rawQuery(MY_QUERY, null);
        
        return cursor;
    }
    
    public Cursor getContactCursor()
    {
        return databaseManager.getContactsCursor(context);
    }

    public Cursor getAdvertisementCursor()
    {
        return databaseManager.getAdvertisementsCursor(context);
    }

    public Cursor getExchangeCursor()
    {
        return databaseManager.getCursorExchange(context);
    }
}