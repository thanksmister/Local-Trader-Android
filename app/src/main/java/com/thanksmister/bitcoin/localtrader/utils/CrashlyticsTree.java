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

package com.thanksmister.bitcoin.localtrader.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BuildConfig;

import timber.log.Timber;

/**
 * A logging implementation which reports 'info', 'warning', and 'error' logs to Crashlytics.
 */
public class CrashlyticsTree extends Timber.Tree {
    public CrashlyticsTree() {
    }

    @Override
    public void d(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.println(Log.DEBUG, "LocalTrader", message);
        }
        //logMessage(Log.DEBUG, message, args);
    }

    @Override
    public void i(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.INFO, message, args);
        }
    }

    @Override
    public void i(Throwable t, String message, Object... args) {
        //logMessage(Log.INFO, message, args);
        if (BuildConfig.DEBUG) {
            logMessage(Log.INFO, message, args);
        }
        // NOTE: We are explicitly not sending the exception to Crashlytics here.
    }

    @Override
    public void w(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.WARN, message, args);
        }
    }

    @Override
    public void w(Throwable t, String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.WARN, message, args);
        }
        // NOTE: We are explicitly not sending the exception to Crashlytics here.
    }

    @Override
    public void e(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.ERROR, message, args);
        }
    }

    @Override
    public void e(Throwable t, String message, Object... args) {
        logMessage(Log.ERROR, message, args);
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority == Log.ERROR) {
            try {
                if (tag != null && tag.length() > 0) {
                    Crashlytics.log(priority, tag, String.format(message, tag));
                } else {
                    Crashlytics.log(priority, tag, message);
                }

                //Crashlytics.logException(t);

            } catch (Exception e) {
                //Timber.e(e.getMessage());
            }
        }
    }

    private void logMessage(int priority, String message, Object... args) {
        try {
            if (args.length > 0) {

                //Crashlytics.log(priority, "LocalTrader", String.format(message, args));

                if (priority == Log.ERROR)
                    Crashlytics.logException(new Throwable(String.format(message, args)));

            } else {
                //Crashlytics.log(priority, "LocalTrader", message);

                if (priority == Log.ERROR)
                    Crashlytics.logException(new Throwable(message));
            }

        } catch (Exception e) {
            //Timber.e(e.getMessage());
        }
    }
}
