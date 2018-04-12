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
    public Throwable handleError(RetrofitError cause) {
        String errorDescription = "";
        if (cause.isNetworkError()) {
            errorDescription = context.getString(R.string.error_no_internet);
        } else {
            if (cause.getResponse() == null) {
                errorDescription = context.getString(R.string.error_service_unreachable_error);
            } else {
                try {
                    ErrorResponse errorResponse = (ErrorResponse) cause.getBodyAs(ErrorResponse.class);
                    List<String> errorList = errorResponse.getError().getErrorLists().getAll();
                    if (!errorList.isEmpty()) {
                        errorDescription = errorList.get(0);
                    } else {
                        errorDescription = context.getString(R.string.error_unknown_error);
                    }
                } catch (Exception ex) {
                    Timber.w("Error handler error: " + ex.getMessage());
                    try {
                        errorDescription = "Http Error: " + cause.getResponse().getStatus();
                    } catch (Exception ex2) {
                        Timber.w("handleError: " + ex2.getMessage());
                        errorDescription = context.getString(R.string.error_unknown_error);
                    }
                }
            }
        }
        Timber.w("handleError: " + errorDescription);
        return new Exception(errorDescription);
    }
}