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

package com.thanksmister.bitcoin.localtrader.network;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.R;

import java.net.UnknownHostException;

import timber.log.Timber;

public class ApiErrorHandler {

    public static int CODE_THREE = 3; // authorization failed
    public static int STATUS_403 = 403; // not authenticated
    private static int STATUS_400 = 400; // service error
    private static int STATUS_500 = 500; // service error

    public static NetworkException handleError(Context context, Throwable throwable) {
        ApiError apiError = new ApiError(throwable);

        Timber.d("handleError status: " + apiError.getStatus());
        Timber.d("handleError code: " + apiError.getCode());
        Timber.d("handleError UnknownHostException: " + (throwable.getCause() instanceof UnknownHostException));

        if(throwable.getCause() instanceof UnknownHostException) {
            Timber.d("NetworkException UnknownHostException");
            return new NetworkException(context.getString(R.string.error_no_internet), apiError.getStatus(), throwable.getCause());
        } else if (apiError.getStatus() == STATUS_500) {
            Timber.d("NetworkException STATUS_500");
            return new NetworkException(context.getString(R.string.error_generic_error), apiError.getStatus(), throwable.getCause());
        } else if (apiError.getStatus() == 404) {
            return new NetworkException(context.getString(R.string.error_no_internet), apiError.getCode(), apiError.getStatus(), throwable.getCause());
        } else if (apiError.getStatus() == STATUS_403) {
            Timber.d("NetworkException STATUS_403");
            return new NetworkException(apiError.getMessage(), apiError.getCode(), apiError.getStatus(), throwable.getCause());
        } else if (apiError.getStatus() == STATUS_400 && apiError.getCode() == CODE_THREE) {
            Timber.d("NetworkException STATUS_400 CODE_THREE");
           return new NetworkException(apiError.getMessage(), apiError.getCode(), apiError.getStatus(), throwable.getCause());
        } else if (apiError.getStatus() == STATUS_400) {
            Timber.d("NetworkException STATUS_400");
            NetworkException networkException = new NetworkException(context.getString(R.string.error_service_error), apiError.getCode(), apiError.getStatus(), throwable.getCause());
            Timber.d("networkException status: " + networkException.getStatus());
            Timber.d("networkException code: " + networkException.getCode());
            return networkException;
        } else {
            return new NetworkException(context.getString(R.string.error_unknown_error), apiError.getCode(), apiError.getStatus(), throwable.getCause());
        }
    }

    public static Boolean isAuthenticationError(NetworkException exception) {
        Timber.d("isAuthenticationError status: " + exception.getStatus());
        Timber.d("isAuthenticationError code: " + exception.getCode());
        return exception.getStatus() == STATUS_403 || (exception.getStatus() == STATUS_400 && exception.getCode() == CODE_THREE);
    }

    public static Boolean isNetworkError(NetworkException exception) {
        return (exception.getCause() instanceof UnknownHostException);
    }
}