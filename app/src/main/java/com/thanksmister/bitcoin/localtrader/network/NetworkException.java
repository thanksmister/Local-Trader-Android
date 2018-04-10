package com.thanksmister.bitcoin.localtrader.network;

/**
 * Created by Michael Ritchie on 4/10/18.
 */
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

    public NetworkException(String msg, int code) {
        super(msg);
        this.message = msg;
        this.code = code;
    }
}
