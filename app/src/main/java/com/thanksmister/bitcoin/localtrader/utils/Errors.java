package com.thanksmister.bitcoin.localtrader.utils;

import android.content.Context;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;

import retrofit.RetrofitError;

/**
 * Author: Michael Ritchie
 * Date: 2/10/15
 * Copyright 2015, ThanksMister LLC
 */
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
