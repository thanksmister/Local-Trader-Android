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

import android.content.Context;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;

import retrofit.RetrofitError;

public class Errors
{
    public static RetroError getError(Throwable throwable, Context context) {
        
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
            retroError = new RetroError(throwable.getMessage(), 0);
        }

        return retroError;
    }
}
