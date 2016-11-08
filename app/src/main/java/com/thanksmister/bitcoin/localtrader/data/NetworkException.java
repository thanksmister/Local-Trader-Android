/*
 * Copyright (c) 2016. DusApp
 */

package com.thanksmister.bitcoin.localtrader.data;

public class NetworkException extends Exception
{
    private String message;
    private int code;

    @Override
    public String getMessage()
    {
        return message;
    }

    public int getCode()
    {
        return code;
    }

    public NetworkException(String detailMessage, int code)
    {
        super(detailMessage);
        this.message = detailMessage;
        this.code = code;
    }
}
