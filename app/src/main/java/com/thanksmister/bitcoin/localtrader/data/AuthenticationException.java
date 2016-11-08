/*
 * Copyright (c) 2016. DusApp
 */

package com.thanksmister.bitcoin.localtrader.data;

public class AuthenticationException extends Exception
{
    public AuthenticationException() {
        super();
    }

    public AuthenticationException(final String message) {
        super(message);
    }

    public AuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(final Throwable cause) {
        super(cause);
    }
}
