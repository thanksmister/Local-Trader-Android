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

public class Places {
    
    @SerializedName("places")
    @Expose
    public List<Place> items = new ArrayList<Place>();

    @SerializedName("place_count")
    @Expose
    public Integer count;

    public Places(@NonNull final List<Place> items)
    {
        this.items = items;
    }

    public List<Place> getItems()
    {
        return items;
    }

    public int getCount()
    {
        return count;
    }
}