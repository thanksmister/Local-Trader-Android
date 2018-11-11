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
