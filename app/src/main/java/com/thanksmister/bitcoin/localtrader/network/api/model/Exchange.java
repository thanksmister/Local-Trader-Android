/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */
package com.thanksmister.bitcoin.localtrader.network.api.model;


public class Exchange {

    private String display_name;
    private String ask;
    private String bid;
    private String last;
    private String source;
    private String created_at;

    public Exchange(String name, String ask, String bid, String last, String source, String date) {
        this.display_name = name;
        this.ask = ask;
        this.bid = bid;
        this.last = last;
        this.source = source;
        this.created_at = date;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public String getAsk() {
        return ask;
    }

    public String getBid() {
        return bid;
    }

    public String getLast() {
        return last;
    }

    public String getSource() {
        return source;
    }

    public String getCreated_at() {
        return created_at;
    }

}
