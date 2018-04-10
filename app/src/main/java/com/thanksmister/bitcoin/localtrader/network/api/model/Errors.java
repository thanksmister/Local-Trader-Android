package com.thanksmister.bitcoin.localtrader.network.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Michael Ritchie on 4/10/18.
 */
public class Errors {

    @SerializedName("__all__")
    @Expose
    private String all;

    public String getAll() {
        return all;
    }

    public void setAll(String all) {
        this.all = all;
    }

}