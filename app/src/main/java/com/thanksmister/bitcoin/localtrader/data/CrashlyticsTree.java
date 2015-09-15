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

/*
 * Jake Wharton
 * https://github.com/JakeWharton/timber/issues/10
 */

package com.thanksmister.bitcoin.localtrader.data;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

/**
 * A logging implementation which reports 'info', 'warning', and 'error' logs to Crashlytics.
 */
public class CrashlyticsTree extends Timber.Tree
{
    @Override
    public void i(String message, Object... args)
    {
        logMessage(message, args);
    }

    @Override
    public void i(Throwable t, String message, Object... args)
    {
        logMessage("INFO: " + message, args);
        // NOTE: We are explicitly not sending the exception to Crashlytics here.
    }

    @Override
    public void w(String message, Object... args)
    {
        logMessage("WARN: " + message, args);
    }

    @Override
    public void w(Throwable t, String message, Object... args)
    {
        logMessage("WARN: " + message, args);
        // NOTE: We are explicitly not sending the exception to Crashlytics here.
    }

    @Override
    public void e(String message, Object... args)
    {
        logMessage("ERROR: " + message, args);
    }

    @Override
    public void e(Throwable t, String message, Object... args)
    {
        logMessage("ERROR: " + message, args);
        Crashlytics.logException(t);
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t)
    {
        if(priority == Log.ERROR) {
            Crashlytics.log(String.format(message, tag)); 
        }
    }

    private void logMessage(String message, Object... args)
    {
        Crashlytics.log(String.format(message, args));
    }
}
