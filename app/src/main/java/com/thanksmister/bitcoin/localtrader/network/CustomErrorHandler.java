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
import com.thanksmister.bitcoin.localtrader.network.api.model.ErrorResponse;
import com.thanksmister.bitcoin.localtrader.network.api.model.LocalBitcoinsErrorResponse;

import java.net.UnknownHostException;
import java.util.List;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Converts the complex error structure into a single string you can get with error.getLocalizedMessage() in Retrofit error handlers.
 * Also deals with there being no network available
 * <p>
 * Uses a few string IDs for user-visible error messages
 */
public class CustomErrorHandler implements ErrorHandler {
    private final Context context;

    public CustomErrorHandler(Context ctx) {
        this.context = ctx;
    }

    @Override
    public Exception handleError(RetrofitError cause) {
        String errorDescription = "";
        int status = 0;

        if(cause.getCause() instanceof UnknownHostException) {
            Timber.d("UnknownHostException");
            errorDescription = context.getString(R.string.error_no_internet);
            return new NetworkConnectionException(errorDescription, status, cause);
        }

        try {
            status = cause.getResponse().getStatus();
        } catch (Exception e) {
            status = 0;
        }

        if (cause.isNetworkError()) {
            errorDescription = context.getString(R.string.error_no_internet);
            return new NetworkConnectionException(errorDescription, status, cause);
        } else {
            if (cause.getResponse() == null) {
                errorDescription = context.getString(R.string.error_service_unreachable_error);
                return new NetworkConnectionException(errorDescription, status, cause);
            } else {
                try {
                    //{"error": {"message": "Invalid parameters.", "errors": {"__all__": "* You need to do identity verification with LocalBitcoins to advertise this payment method."}, "error_code": 9, "error_lists": {"__all__": ["You need to do identity verification with LocalBitcoins to advertise this payment method."]}}}
                    ErrorResponse errorResponse = (ErrorResponse) cause.getBodyAs(ErrorResponse.class);
                    List<String> errorList = errorResponse.getError().getErrorLists().getAll();
                    int errorCode = errorResponse.getError().getErrorCode();
                    Timber.d("Error code: " + errorCode);
                    if (!errorList.isEmpty()) {
                        errorDescription = errorList.get(0);
                    } else {
                        errorDescription = context.getString(R.string.error_unknown_error);
                    }
                    Timber.d("Error description: " + errorDescription);
                    return new NetworkException(errorDescription, errorCode, status, cause);
                } catch (Exception ex) {
                    //{"error": {"message": "Invalid or expired access token for scope 2. Learn how to renew an access token at https://localbitcoins.com/api-docs/", "error_code": 3}}
                    Timber.w("Error handler error: " + ex.getMessage());
                    try {
                        LocalBitcoinsErrorResponse error = (LocalBitcoinsErrorResponse) cause.getBodyAs(LocalBitcoinsErrorResponse.class);
                        Timber.d("Error message " + error.getError().getMessage());
                        Timber.d("Error code " + error.getError().getErrorCode());
                        return new NetworkException(error.getError().getMessage(), error.getError().getErrorCode(), status, cause);
                    } catch (Exception ex2) {
                        Timber.w("Error handler error2: " + ex2.getMessage());
                        try {
                            return new NetworkException(ex.getMessage(), status, cause);
                        } catch (Exception ex3) {
                            Timber.w("handleError: " + ex3.getMessage());
                            errorDescription = context.getString(R.string.error_unknown_error);
                            return new NetworkException(errorDescription, status, cause);
                        }
                    }
                }
            }
        }
    }
}