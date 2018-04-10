package com.thanksmister.bitcoin.localtrader.network;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.network.api.model.ErrorResponse;
import com.thanksmister.bitcoin.localtrader.network.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.utils.Parser;

import java.util.List;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Converts the complex error structure into a single string you can get with error.getLocalizedMessage() in Retrofit error handlers.
 * Also deals with there being no network available
 *
 * Uses a few string IDs for user-visible error messages
 */
public class CustomErrorHandler implements ErrorHandler {
    private final Context ctx;

    public CustomErrorHandler(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public Throwable handleError(RetrofitError cause) {
        String errorDescription = "";
        if (cause.isNetworkError()) {
            errorDescription = ctx.getString(R.string.error_no_internet);
        } else {
            if (cause.getResponse() == null) {
                errorDescription = ctx.getString(R.string.error_service_unreachable_error);
            } else {
                try {
                    ErrorResponse errorResponse = (ErrorResponse) cause.getBodyAs(ErrorResponse.class);
                    List<String> errorList =  errorResponse.getError().getErrorLists().getAll();
                    String message = "";
                    if(!errorList.isEmpty()) {
                        errorDescription = errorList.get(0);
                    } else {
                        errorDescription = ctx.getString(R.string.error_unknown_error);
                    }
                } catch (Exception ex) {
                    Timber.e("Error handler error: " + ex.getMessage());
                    try {
                        errorDescription = "Http Error: " + cause.getResponse().getStatus();
                    } catch (Exception ex2) {
                        Timber.e("handleError: " + ex2.getLocalizedMessage());
                        errorDescription = ctx.getString(R.string.error_unknown_error);
                    }
                }
            }
        }
        return new Exception(errorDescription);
    }
}