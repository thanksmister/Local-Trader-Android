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

package com.thanksmister.bitcoin.localtrader.network.services;

import com.thanksmister.bitcoin.localtrader.network.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.network.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.utils.Parser;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class DataServiceUtils {

    public static int CODE_MINUS_ONE = -1; // authorization failed
    public static int CODE_THREE = 3; // authorization failed
    public static int CODE_FORTY_ONE = 41; // bad hmac signature
    public static int CODE_FORTY_TWO = 42; // nonce too small

    public static boolean isNetworkError(Throwable throwable) {
        if (throwable instanceof UnknownHostException || throwable instanceof NetworkConnectionException) {
            return true;
        } else if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 503);
        }

        return false;
    }

    public static boolean isTimeoutError(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return true;
        }

        return false;
    }

    public static boolean isConnectionError(Throwable throwable) {
        if (throwable instanceof ConnectException) {
            return true;
        }

        return false;
    }

    public static boolean isHttp41Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetroError retroError = createRetroError(throwable);
            return (retroError.getCode() == CODE_FORTY_ONE);
        }

        return false;
    }

    public static boolean isHttp42Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetroError retroError = createRetroError(throwable);
            return (retroError.getCode() == CODE_FORTY_TWO);
        }

        return false;
    }

    // bad gateway eror
    public static boolean isHttp502Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 502);
        }

        return false;
    }

    // authorization error
    public static boolean isHttp403Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 403);
        }

        return false;
    }

    // bad request
    public static boolean isHttp400Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 400);
        }

        return false;
    }

    // network error
    public static boolean isHttp401Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 401);
        }

        return false;
    }

    // server error
    public static boolean isHttp500Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 500);
        }

        return false;
    }

    public static boolean isHttp404Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 404);
        }

        return false;
    }

    public static boolean isHttp504Error(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError) == 504);
        }

        return false;
    }

    /*
   Added because service now always returns 400 error and have to check valid code
   {"error": {"message": "Invalid or expired access token for scope 2. 
    */
    public static RetroError createRetroError(Throwable throwable) {
        RetrofitError retroError = (RetrofitError) throwable;
        Response response = retroError.getResponse();
        String json = null;
        try {
            json = Parser.parseRetrofitResponse(response);
            Timber.d("JSON: " + json);
        } catch (Throwable e) {
            return new RetroError(e.getMessage());
        }

        RetroError err = Parser.parseError(json);
        Timber.e("Error: " + err.getMessage());
        Timber.e("Code: " + err.getCode());
        return err;
    }

    public static int getStatusCode(RetrofitError error) {
        try {
            Timber.w("Status Kind: " + error.getKind());
        } catch (Throwable e) {
            Timber.w("Error Status: " + e.getMessage());
        }

        try {
            if (error.getKind() == RetrofitError.Kind.NETWORK) {
                return 503; // Use another code if you'd prefer
            }
        } catch (Exception e) {
            Timber.w(e.getLocalizedMessage());
            return 503; // Use another code if you'd prefer
        }

        try {
            if (error.getResponse() != null) {
                Timber.w("Error Code: " + error.getResponse().getStatus());
                return error.getResponse().getStatus();
            }
        } catch (Throwable e) {
            Timber.w("Error Status: " + e.getMessage());
        }

        return 0;
    }

    public static int getStatusCode(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            return (getStatusCode(retroError));
        }

        return 0;
    }
}