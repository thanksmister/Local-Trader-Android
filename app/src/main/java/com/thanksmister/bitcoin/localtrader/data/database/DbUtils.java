/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.data.database;

import timber.log.Timber;

/**
 * TODO: Add a class header comment!
 */
public class DbUtils {

    /**
     * Print out the query to place in Stetho to make query calls on the data
     *
     * @param query Query string
     * @param query Argument
     */
    public static void printQueryText(String query, int arg1) {
        query = query.replace("?", String.valueOf(arg1));
        Timber.d("Query Text: " + query);
    }

    /**
     * Print out the query to place in Stetho to make query calls on the data
     *
     * @param query Query string
     * @param query Argument
     */
    public static void printQueryText(String query, String[] argArray) {
        if (argArray != null) {
            int len = argArray.length;
            for (int i = 0; i < len; i++) {
                query = query.replaceFirst("\\?", argArray[i]);
            }
        }

        Timber.d("Query Text: " + query);
    }

    /**
     * Print out the query to place in Stetho to make query calls on the data
     *
     * @param query
     * @param arg1
     * @param arg2
     */
    public static void printQueryText(String query, int arg1, int arg2) {
        query = query.replaceFirst("\\?", String.valueOf(arg1));
        query = query.replaceFirst("\\?", String.valueOf(arg2));
        Timber.d("Query Text: " + query);
    }
}
