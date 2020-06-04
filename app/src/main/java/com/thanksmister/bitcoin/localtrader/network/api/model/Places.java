/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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