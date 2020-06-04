/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model;

import java.lang.Error;

public class RetroError extends Error {
    private String message;
    private int code;

    @Override
    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public RetroError(String detailMessage) {
        super(detailMessage);
        this.message = detailMessage;
    }

    public RetroError(String detailMessage, int code) {
        super(detailMessage);
        this.message = detailMessage;
        this.code = code;
    }

    public boolean isAuthenticationError() {
        return (code == 403 || code == 4);
    }

    public boolean isNetworkError() {
        return (code == 404);
    }
}
