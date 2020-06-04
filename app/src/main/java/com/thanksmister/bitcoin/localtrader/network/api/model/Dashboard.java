/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Michael Ritchie
 * Updated: 12/17/15
 */
public class Dashboard {
    
    @SerializedName("contact_list")
    @Expose
    public List<Contact> items = new ArrayList<Contact>();

    @SerializedName("contact_count")
    @Expose
    public Integer count;

    public Dashboard(@NonNull final List<Contact> items) {
        this.items = items;
        count = items.size();
    }

    @NonNull
    public List<Contact> getItems() {
        return items;
    }

    public int getCount() {
        return count;
    }

}
