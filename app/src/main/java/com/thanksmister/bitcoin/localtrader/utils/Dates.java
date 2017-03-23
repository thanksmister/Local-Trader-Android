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

package com.thanksmister.bitcoin.localtrader.utils;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

public class Dates
{
   
    public static Date parseLocalDate(String dateTime)
    {
        Date date = new Date();

        if(dateTime == null) {
            return date;
        }

        // Date
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy,'at' kk:mm");
            date = dateFormat.parse(dateTime);
        } catch (ParseException e) {
            return  date;
        }
        return  date;
    }

    public static Date parseLocalDateISO(String dateTime)
    {
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

    public static String parseLocalDateStringShort(String dateTime)
    {
        String dateString;

        // Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }
        return dateString ;
    }

    public static String parseLocalDateString(String dateTime)
    {
        String dateString;
        // Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy, hh:mma");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }

        return dateString ;
    }

    public static String parseLocalDateStringAbbreviatedTime(String dateTime)
    {

        String dateString;
        // Date
        //SimpleDateFormat dateFormat = new SimpleDateFormat("MMM M, kk:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, kk:mm");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }

        return dateString ;
    }

    public static String parseLocaleDate(String dateTime)
    {
        String dateString;

        // Date
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }
        return dateString ;
    }

    public static String parseLocaleDateTime(String dateTime)
    {
        String dateString;

        // Date
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }
        return dateString ;
    }

    public static String parseLocalDateStringAbbreviatedDate(String dateTime)
    {
        String dateString;

        // Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }
        return dateString ;
    }

    public static String getLocalDateMilitaryTime()
    {
        String dateTime = createISODate();
        String dateString;
        // Date
        //yyyy-MM-dd kk:mm:ss
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }

        return dateString ;
    }

    public static String getLocalDateMilitaryTimeShort()
    {
        String dateTime = createISODate();
        String dateString;
        // Date
        //yyyy-MM-dd kk:mm:ss
        SimpleDateFormat dateFormat = new SimpleDateFormat("kk:mm");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            Date date = new Date();
            dateString = (dateFormat.format(date.getTime()));
        }

        return dateString ;
    }

    public static Date parseLastSeenDate(String dateTime)
    {
        try {
            return ISO8601.toCalendar(dateTime).getTime();
        } catch (ParseException e) {
            Timber.d("Error parsing last seen date");
        }

        Date date = new Date();
        return date;
    }

    public static String parseFileTimeStamp()
    {
        String dateTime = createISODate();
        String dateString;

        // Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_dd_yyyy_kk_mm");
        try {
            dateString = (dateFormat.format(ISO8601.toCalendar(dateTime).getTime()));
        } catch (ParseException e) {
            dateString = null;
        }
        return dateString ;
    }

    public static String createISODate()
    {
        return  ISO8601.now();
    }

}
