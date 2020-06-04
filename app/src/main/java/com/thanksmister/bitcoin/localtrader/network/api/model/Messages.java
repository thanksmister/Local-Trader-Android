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