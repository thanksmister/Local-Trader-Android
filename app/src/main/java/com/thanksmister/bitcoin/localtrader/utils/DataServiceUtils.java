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
    
    //{"error": {"required_scope": ["money_pin"], "message": "This token does not have access to this API. This API requires one of ['money_pin']. This token has ['read', 'write']. Learn how to grant more access at https://localbitcoins.com/api-docs/", "error_code": 4}}
    public static RetroError convertRetroError(Throwable throwable, Context context)
    {
        RetroError retroError;
        if (throwable instanceof RetrofitError) {
            
            if (((RetrofitError) throwable).isNetworkError()) {
                retroError = new RetroError(context.getString(R.string.error_generic_server_down), 404);
            } else {
                RetrofitError error = (RetrofitError) throwable;
                retroError = Parser.parseRetrofitError(error);
                if(retroError.getCode() == 4) {
                    retroError = new RetroError(context.getString(R.string.error_generic_permission), 4);
                }
            }
        } else {
            retroError = new RetroError(throwable.getMessage());
        }
        
        return retroError;
    }
}
