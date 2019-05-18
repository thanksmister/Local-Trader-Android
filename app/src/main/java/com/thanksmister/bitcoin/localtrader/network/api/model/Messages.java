/*
 * Copyright (c) 2019 ThanksMister LLC
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
import com.thanksmister.bitcoin.localtrader.utils.Dates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Messages {
    @SerializedName("message_list")
    @Expose
    private List<Message> items = new ArrayList<Message>();

    @SerializedName("message_count")
    @Expose
    private int count;

    public Messages(@NonNull final List<Message> items) {
        this.items = items;
        this.count = items.size();
    }

    public List<Message> getItems() {
        Collections.sort(items, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                Date d1 = Dates.parseDate(m1.getCreatedAt());
                Date d2 = Dates.parseDate(m2.getCreatedAt());
                return (d2.getTime() < d1.getTime() ? -1 : 1);     //descending
            }
        });

        return items;
    }

    public int getCount() {
        return count;
    }
}