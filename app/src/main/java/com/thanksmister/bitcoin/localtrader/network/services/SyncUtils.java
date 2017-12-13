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

package com.thanksmister.bitcoin.localtrader.network.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.thanksmister.bitcoin.localtrader.R;

import static android.content.Context.ACCOUNT_SERVICE;


/**
 * Static helper methods for working with the sync framework.
 */
public class SyncUtils {
    
    public static final long SYNC_FREQUENCY = 5 * 60;  // 5 minutes in seconds

    /**
     * Request immediate sync
     *
     * @param context
     */
    public static void requestSyncNow(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
        ContentResolver.requestSync(getSyncAccount(context), SyncProvider.CONTENT_AUTHORITY, bundle);
    }

    /**
     * Cancel any ongoing syncs
     *
     * @param context
     */
    public static void cancelSync(Context context) {
        ContentResolver.cancelSync(getSyncAccount(context), SyncProvider.CONTENT_AUTHORITY);
    }

    /**
     * Get existing account or create a new account if non exists
     *
     * @param context
     * @return
     */
    public static Account getSyncAccount(Context context) {
        String acctType = "com.thanksmister.bitcoin.localtrader.sync";
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        Account account = new Account(context.getString(R.string.app_name), acctType);
        accountManager.addAccountExplicitly(account, null, null);
        return account;
    }
}
