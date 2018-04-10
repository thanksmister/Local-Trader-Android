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

package com.thanksmister.bitcoin.localtrader.network;


import android.content.Context;

import com.thanksmister.bitcoin.localtrader.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Updated: 12/18/15
 */
public class RetrofitErrorHandler
{

    private Context mContext;

    ///api/ads/, 42, Given nonce was too small.

    public RetrofitErrorHandler(Context context)
    {
        mContext = context;
    }

    /*
    {"message":"HMAC authentication key and signature was given, but they are invalid.","error_code":41}
     */
    private ErrorResponse parseErrorMessage(String json)
    {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.message = "";
        errorResponse.code = 0;

        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(json);
            JSONObject errorObj = new JSONObject();
            int error_code = 0;
            String error_message = "";

            if (jsonObject.has("error")) {
                errorObj = jsonObject.getJSONObject("error");
                error_message = errorObj.getString("message");
            } else if (jsonObject.has("message")) {
                error_message = jsonObject.getString("message");
            }

            if (errorObj.has("error_code")) {
                error_code = errorObj.getInt("error_code");
            } else if (jsonObject.has("error_code")) {
                error_code = jsonObject.getInt("error_code");
            }

            if (errorObj.has("errors")) {
                error_message = "";
                JSONObject errors = errorObj.getJSONObject("errors");
                Iterator<?> keys = errors.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String message = errors.getString(key);
                    error_message += key + " " + message + " ";
                }
            }

            errorResponse.message = error_message;
            errorResponse.code = error_code;

        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        return errorResponse;
    }

    private ErrorResponse parseError(String response)
    {
        //Timber.e("Error Json Response: " + response);

        ErrorResponse errorResponse = new ErrorResponse();
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(response);
            JSONObject errorObj = jsonObject.getJSONObject("error");

            int error_code = 0;
            if (errorObj.has("error_code")) {
                error_code = errorObj.getInt("error_code");
            }

            String error_message = errorObj.getString("message"); // TODO this is too generic

            if (errorObj.has("errors")) {
                error_message = "";
                JSONObject errors = errorObj.getJSONObject("errors");
                Iterator<?> keys = errors.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String message = errors.getString(key);
                    error_message += key + " " + message + " ";
                }
            }

            errorResponse.message = error_message;
            errorResponse.code = error_code;

        } catch (JSONException e) {
            Timber.e(e.getMessage());

            errorResponse.message = "Network error.";
            errorResponse.code = 0;
        }

        return errorResponse;
    }

    /*// Unable to resolve host "api.bitcoinaverage.com": No address associated with hostname
    public NetworkException create(Throwable throwable)
    {
        if (throwable instanceof NetworkException) {
            return (NetworkException) throwable;
        } else if (throwable instanceof SocketTimeoutException) {
            return new NetworkException(mContext.getString(R.string.error_no_internet), 500);
        } else if (throwable instanceof NetworkConnectionException) {
            return new NetworkException(mContext.getString(R.string.error_no_internet), 500);
        } else if (throwable instanceof UnknownHostException) {
            return new NetworkException(mContext.getString(R.string.error_authentication), 403);
        } else if (throwable instanceof Exception) {
            return new NetworkException(mContext.getString(R.string.error_unknown_error), 500);
        }

        if (isHttp403Error(throwable) || isHttp400Error(throwable)) {

            try {

                String json = new JSONObject(((HttpException) throwable).response().errorBody().source().readUtf8()).getString("error");

                Timber.e("JSON: " + json);

                RetrofitErrorHandler.ErrorResponse errorResponse = parseErrorMessage(json);

                Timber.e("Error: " + errorResponse.message);
                Timber.e("Code: " + errorResponse.code);

                return new NetworkException(errorResponse.message, errorResponse.code);

            } catch (IOException | JSONException e) {

                Timber.e("JSON Parsing Error: " + e.getMessage());

                int code = ((HttpException) throwable).code();

                return new NetworkException(throwable.getMessage(), code);
            }
        }

        if (isHttp401Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_no_internet), 401);
        }

        if (isHttp1007Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_no_internet), 500);
        }

        if (isHttp500Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_service_unreachable_error), 500);
        }

        if (isHttp404Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_service_error), 404);
        }

        if (isHttp400Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_service_error), 400);
        }

        if (isHttp409Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_service_error), 409);
        }

        if (isHttp503Error(throwable)) {
            return new NetworkException(mContext.getString(R.string.error_service_error), 503);
        }

        return new NetworkException(mContext.getString(R.string.error_unknown_error), 0);
    }

    public static boolean isHttp503Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 503);
    }

    // authorization error
    public static boolean isHttp403Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 403);
    }

    // bad request
    public static boolean isHttp400Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 400);
    }

    // network error
    public static boolean isHttp401Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 401);
    }

    public static boolean isHttp1007Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 1007);
    }

    // server error
    public static boolean isHttp500Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 500);
    }

    public static boolean isHttp404Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 404);
    }

    public static boolean isHttp409Error(Throwable throwable)
    {
        return (((HttpException) throwable).code() == 409);
    }*/

    public static boolean isInvalidNonce(int code)
    {
        return (code == 42);
    }

    public static class ErrorResponse
    {
        public int code;
        public String message;
    }
}
