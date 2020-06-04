/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.network.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.thanksmister.bitcoin.localtrader.R;

import javax.inject.Inject;

import static android.content.Context.ACCOUNT_SERVICE;

/**
 * Static helper methods for working with the sync framework.
 */
public class SyncUtils {

    // Sync interval constants
    private static final String CONTENT_AUTHORITY = "com.thanksmister.bitcoin.localtrader.provider";
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SYNC_INTERVAL_IN_MINUTES = 3L;
    private static final long SYNC_FREQUENCY = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;

    /** Injectable constructor */
    @Inject
    public SyncUtils() {}
    
    /**
     * Request immediate sync.
     * @param context
     */
    public static void requestSyncNow(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
        ContentResolver.requestSync(getSyncAccount(context), CONTENT_AUTHORITY, bundle);
    }

    /**
     * Creates the standard sync account for our app.
     * @param context {@link Context}
     */
    public static void createSyncAccount(Context context) {
        Account account = SyncUtils.getSyncAccount(context);
        ContentResolver.setIsSyncable(account,  CONTENT_AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, Bundle.EMPTY, SyncUtils.SYNC_FREQUENCY);
    }

    /**
     * Cancel any ongoing syncs.
     * @param context
     */
    public static void cancelSync(Context context) {
        ContentResolver.cancelSync(getSyncAccount(context), CONTENT_AUTHORITY);
    }

    /**
     * Get existing account or create a new account if non exists.
     * @param context
     * @return
     */
    public static Account getSyncAccount(Context context) {
        String acctType = "com.thanksmister.bitcoin.localtrader.sync";
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        Account account = new Account(context.getString(R.string.app_name), acctType);
        if (accountManager != null) {
            accountManager.addAccountExplicitly(account, null, null);
        }
        return account;
    }
}
