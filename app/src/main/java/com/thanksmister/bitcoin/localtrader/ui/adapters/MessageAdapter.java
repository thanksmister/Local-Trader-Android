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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.MessageItem;
import com.thanksmister.bitcoin.localtrader.utils.DateUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends BaseAdapter {
    private List<MessageItem> data = Collections.emptyList();
    private Context context;
    private final LayoutInflater inflater;

    public MessageAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public MessageItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void replaceWith(List<MessageItem> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_message_list, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        /*if (position % 2 == 0)
            holder.row.setBackgroundColor(context.getResources().getColor(R.color.white));
        else
            holder.row.setBackgroundColor(context.getResources().getColor(R.color.list_gray_color));*/

        MessageItem message = getItem(position);

        holder.senderName.setText(message.sender_username());
        Date date = DateUtils.parseLocalDateISO(message.create_at());
        holder.createdAt.setText(android.text.format.DateUtils.getRelativeTimeSpanString(date.getTime()));
        holder.messageBody.setText(message.message());

        if (!Strings.isBlank(message.attachment_name())) {
            holder.attachmentLayout.setVisibility(View.VISIBLE);
            holder.attachmentName.setText(message.attachment_name());
        } else {
            holder.attachmentLayout.setVisibility(View.GONE);
            holder.attachmentName.setText("");
        }

        return view;
    }

    static class ViewHolder {
        View row;
        TextView senderName;
        TextView createdAt;
        TextView messageBody;
        View attachmentLayout;
        TextView attachmentName;

        public ViewHolder(View view){
        }
    }
}


