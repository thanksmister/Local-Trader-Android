/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.exceptions;

public class NetworkException extends Exception {

    private String message;
    private int code;

    @Override
    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public NetworkException(String detailMessage, int code) {
        super(detailMessage);
        this.message = detailMessage;
        this.code = code;
    }
}