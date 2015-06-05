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

package com.thanksmister.bitcoin.localtrader.data.database;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.data.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.data.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.services.SqlBriteContentProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

@Module(complete = false, library = true)
public final class DbModule
{
    @Provides
    @Singleton
    ContentResolver provideContentResolver(BaseApplication application) {
        return application.getContentResolver();
    }

    @Provides
    @Singleton
    SqlBriteContentProvider provideSqlBriteContentProvider(ContentResolver contentResolver)
    {
        return SqlBriteContentProvider.create(contentResolver);
    }
    
    @Provides
    @Singleton
    DbManager provideDbManager(SqlBrite sqlBrite, SqlBriteContentProvider sqlBriteContentProvider, ContentResolver contentResolver)
    {
        return new DbManager(sqlBrite, sqlBriteContentProvider, contentResolver);
    }

    @Provides
    @Singleton
    SQLiteOpenHelper provideOpenHelper(BaseApplication application)
    {
        return new DbOpenHelper(application);
    }

    @Provides
    @Singleton
    SqlBrite provideSqlBrite(SQLiteOpenHelper openHelper)
    {
        SqlBrite db = SqlBrite.create(openHelper);

        if (BuildConfig.DEBUG) {
            db.setLogger(new SqlBrite.Logger()
            {
                @Override
                public void log(String message)
                {
                    Timber.tag("Database").v(message);
                }
            });
            db.setLoggingEnabled(true);
        }

        return db;
    }
}