
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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class WalletContract extends BaseContract
{
    private WalletContract()
    {
    }

    private static final String PATH = "wallets";

    public static final String[] PROJECTION= new String[] {
            Wallet._ID,
            Wallet.COLUMN_WALLET_MESSAGE,
            Wallet.COLUMN_WALLET_BALANCE,
            Wallet.COLUMN_WALLET_SENDABLE,
            Wallet.COLUMN_WALLET_ADDRESS,
            Wallet.COLUMN_WALLET_ADDRESS_RECEIVABLE,
            Wallet.COLUMN_WALLET_QRCODE};


    public static class Wallet implements BaseColumns 
    {
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.localtrader.wallets";
        
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.localtrader.wallet";
        
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();

        public static final String TABLE_NAME = "wallet_table";

        public static final String COLUMN_WALLET_MESSAGE = "wallet_message";
        public static final String COLUMN_WALLET_BALANCE = "wallet_balance";
        public static final String COLUMN_WALLET_SENDABLE = "wallet_sendable";
        public static final String COLUMN_WALLET_ADDRESS = "wallet_address";
        public static final String COLUMN_WALLET_ADDRESS_RECEIVABLE = "wallet_address_receivable";
        public static final String COLUMN_WALLET_QRCODE = "wallet_qrcode";

        public static final int COLUMN_INDEX_ID = 0;
        public static final int COLUMN_INDEX_WALLET_MESSAGE = 1;
        public static final int COLUMN_INDEX_WALLET_BALANCE  = 2;
        public static final int COLUMN_INDEX_WALLET_SENDABLE  = 3;
        public static final int COLUMN_INDEX_WALLET_ADDRESS = 4;
        public static final int COLUMN_INDEX_WALLET_ADDRESS_RECEIVABLE = 5;
        public static final int COLUMN_INDEX_WALLET_QRCODE = 6;
    }
}