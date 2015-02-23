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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.thanksmister.bitcoin.localtrader.data.database.BaseContract;

public class SessionContract extends BaseContract
{
    private SessionContract()
    {
    }

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    
    private static final String PATH_ITEMS= "session";

    public static final String[] PROJECTION= new String[] {
            Session._ID,
            Session.COLUMN_ACCESS_TOKEN,
            Session.COLUMN_REFRESH_TOKEN};


    public static class Session implements BaseColumns 
    {
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.localtrader.sessions";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.localtrader.session";
        
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ITEMS).build();

        public static final String TABLE_NAME = "session_table";
        
        public static final String COLUMN_ACCESS_TOKEN = "access_token";
        public static final String COLUMN_REFRESH_TOKEN = "refresh_token";
        
        public static final int COLUMN_ID = 0;
        public static final int COLUMN_INDEX_ACCESS_TOKEN = 1;
        public static final int COLUMN_INDEX_REFRESH_TOKEN   = 2;
    }
}