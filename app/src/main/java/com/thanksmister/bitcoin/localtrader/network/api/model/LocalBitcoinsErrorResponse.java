/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Michael Ritchie on 4/19/18.
 */
public class LocalBitcoinsErrorResponse {

    @SerializedName("error")
    @Expose
    private LocalBitcoinsError error;

    public LocalBitcoinsError getError() {
        return error;
    }

    public void setError(LocalBitcoinsError error) {
        this.error = error;
    }
}
