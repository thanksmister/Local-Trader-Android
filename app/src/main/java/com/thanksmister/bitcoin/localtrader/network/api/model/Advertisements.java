/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.network.api.model;

import android.support.annotation.NonNull;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Advertisements {
    @SerializedName("ad_list")
    @Expose
    public List<Advertisement> items = new ArrayList<Advertisement>();

    @SerializedName("ad_count")
    @Expose
    public Integer count;

    public Advertisements(@NonNull final List<Advertisement> items) {
        this.items = items;
    }

    public List<Advertisement> getItems() {
        Collections.sort(items, new Comparator<Advertisement>() {
            @Override
            public int compare(Advertisement a1, Advertisement a2) {
                boolean b1 = a1.getVisible();
                boolean b2 = a2.getVisible();
                if (b1 && !b2) {
                    return +1;
                }
                if (!b1 && b2) {
                    return -1;
                }
                return 0;
            }
        });

        return items;
    }

    public int getCount() {
        return count;
    }
}
