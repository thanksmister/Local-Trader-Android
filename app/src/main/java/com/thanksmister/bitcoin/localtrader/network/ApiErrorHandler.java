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
    public static Exception handleError(Context context, Throwable throwable) {
        ApiError apiError = new ApiError(throwable);
        if(throwable.getCause() instanceof UnknownHostException) {
            return new NetworkException(context.getString(R.string.error_no_internet), apiError.getStatus());
        }
        if (apiError.getStatus() == 500) {
            return new NetworkException(context.getString(R.string.error_generic_error), apiError.getStatus());
        } else if (apiError.getStatus() == 404) {
            return new AuthenticationException(context.getString(R.string.error_no_internet), apiError.getCode(), apiError.getStatus());
        } else if (apiError.getStatus() == 403) {
            return new AuthenticationException(apiError.getMessage(), apiError.getCode(), apiError.getStatus());
        } else if (apiError.getStatus() == 400 && apiError.getCode() == 3) {
           return new AuthenticationException(apiError.getMessage(), apiError.getCode(), apiError.getStatus());
        } else if (apiError.getStatus() == 400) {
            return new AuthenticationException(context.getString(R.string.error_service_error), apiError.getCode(), apiError.getStatus());
        } else {
            return new NetworkException(context.getString(R.string.error_unknown_error), apiError.getCode(), apiError.getStatus());
        }
    }
}