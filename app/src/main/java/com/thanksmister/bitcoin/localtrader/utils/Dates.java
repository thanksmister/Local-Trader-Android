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


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class Dates {

    public static Date parseLocalDateISO(String dateTime) {
        try {
            if (dateTime.contains(".")) {
                int period = dateTime.indexOf(".");
                Timber.d("Date: " + dateTime);
                Timber.d("period: " + period);

                if (period > 0) {
                    String replace = dateTime.substring(period, period + 5);
                    Timber.d("replace: " + replace);
                    dateTime = dateTime.replace(replace, "");
                }
            }
            return ISO8601.toCalendar(dateTime).getTime();
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return new Date();
    }

    public static String parseLocalDateStringAbbreviatedTime(String dateTime) {

        String dateString;
        // Date
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }

        return dateString;
    }

    public static String parseLocaleDate(String dateTime) {
        String dateString;

        // Date
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }
        return dateString;
    }

    public static String parseLocaleDateTime(String dateTime) {
        String dateString;

        // Date
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }
        return dateString;
    }

    public static Date parseLastSeenDate(String dateTime) {
        try {
            return ISO8601.toCalendar(dateTime).getTime();
        } catch (ParseException e) {
            Timber.d("Error parsing last seen date");
        }

        return new Date();
    }

    public static Date parseDate(String dateString) {
        return parseISODate(dateString);
    }

    // Parsing this shit 2016-07-13T16:00:58+00:00
    public static Date parseISODate(String dateTime) {
        try {
            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            return df1.parse(dateTime);
        } catch (ParseException e) {
            throw new IllegalStateException("Unsupported date format." + e.getMessage());
        }
    }

    public static String createISODate() {
        return ISO8601.now();
    }

}
