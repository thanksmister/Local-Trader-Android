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

public class Notifications {

    @SerializedName("data")
    @Expose
    public List<Notification> items = new ArrayList<Notification>();

    public Notifications(@NonNull final List<Notification> items) {
        this.items = items;
    }

    public List<Notification> getItems() {
        if(items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
}
