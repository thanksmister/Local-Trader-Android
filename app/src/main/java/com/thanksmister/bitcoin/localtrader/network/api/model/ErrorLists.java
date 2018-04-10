package com.thanksmister.bitcoin.localtrader.network.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Michael Ritchie on 4/10/18.
 */
public class ErrorLists {

    @SerializedName("__all__")
    @Expose
    private List<String> all = null;

    public List<String> getAll() {
        return all;
    }

    public void setAll(List<String> all) {
        this.all = all;
    }
}
