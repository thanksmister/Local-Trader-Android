/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.utils;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;

import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import timber.log.Timber;

public class DataServiceUtils
{
    // authorization error
    public static boolean isHttp403Error(Throwable throwable)
    {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            Response response = retroError.getResponse();
            return (response.getStatus() == 403);
        } 

        return false;
    }

    // bad request
    public static boolean isHttp400Error(Throwable throwable)
    {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            Response response = retroError.getResponse();
            return (response.getStatus() == 400);
        }

        return false;
    }

    // network error
    public static boolean isHttp401Error(Throwable throwable)
    {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            Response response = retroError.getResponse();
            return (response.getStatus() == 401);
        }

        return false;
    }

    // server error
    public static boolean isHttp500Error(Throwable throwable)
    {
        if (throwable instanceof RetrofitError) {
            RetrofitError retroError = (RetrofitError) throwable;
            Response response = retroError.getResponse();
            return (response.getStatus() == 500);
        }

        return false;
    }
    
    public static RetroError convertRetroError(Throwable throwable, Context context)
    {
        RetroError retroError;
        if (throwable instanceof RetrofitError) {
            
            if (((RetrofitError) throwable).isNetworkError()) {
                retroError = new RetroError(context.getString(R.string.error_no_internet), 404);
            
            } else {
                RetrofitError error = (RetrofitError) throwable;
                retroError = Parser.parseRetrofitError(error);
                if(retroError.getCode() == 4) {
                    retroError = new RetroError(context.getString(R.string.error_generic_permission), 4);
                }
            }
        } else if(isHttp403Error(throwable)) {
            
            retroError = new RetroError(context.getString(R.string.error_authentication), 403);

        } else if(isHttp401Error(throwable)) {

            retroError = new RetroError(context.getString(R.string.error_no_internet), 401);

        } else if(isHttp400Error(throwable)) {

            try {
                String response =  new String(((TypedByteArray) ((RetrofitError) throwable).getResponse().getBody()).getBytes());
                retroError = new RetroError(response, 400);
            } catch (ClassCastException error) {
                retroError = new RetroError(context.getString(R.string.error_service_error), 400);
            }

        } else if(isHttp500Error(throwable)) {

            retroError = new RetroError(context.getString(R.string.error_service_error), 500);

        } else {
            
            retroError = new RetroError(context.getString(R.string.error_generic_error), 0);
        }
        
        return retroError;
    }
}
