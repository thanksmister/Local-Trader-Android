/*
 * Copyright (c) 2017 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.RecentMessageItem;
import com.thanksmister.bitcoin.localtrader.utils.Dates;

import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder>
{
    private static final int TYPE_EMPTY = R.layout.view_empty_dashboard;
    private static final int TYPE_PROGRESS = R.layout.view_progress_dashboard;
    private static final int TYPE_ITEM = R.layout.adapter_dashboard_message_list;

    protected List<RecentMessageItem> items;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public static interface OnItemClickListener
    {
        public void onSearchButtonClicked();
        public void onAdvertiseButtonClicked();
    }

    public MessagesAdapter(Context context)
    {
        this.context = context;
    }

    public MessagesAdapter(Context context, OnItemClickListener onItemClickListener)
    {
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    public void replaceWith(List<RecentMessageItem> data)
    {
        this.items = data;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false);

        if (viewType == TYPE_ITEM) {
            return new ItemViewHolder(itemLayoutView);
        } else if (viewType == TYPE_EMPTY) {
            return new EmptyViewHolder(itemLayoutView);
        } 

        return new ProgressViewHolder(itemLayoutView);
    }

    @Override
    public int getItemViewType(int position)
    {
        if (items == null) {
            return TYPE_PROGRESS;
        } else if (items.size() == 0) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM;
    }

    @Override
    public int getItemCount()
    {
        if (items == null)
            return -1;

        return items.size() > 0? items.size():1;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position)
    {
        if (viewHolder instanceof ItemViewHolder) {
            RecentMessageItem messageItem = items.get(position);
            ((ItemViewHolder) viewHolder).messageBody.setText("Message from " + messageItem.sender_username());
            ((ItemViewHolder) viewHolder).contactId.setText("Contact #" + messageItem.contact_id());
            Date date = Dates.parseLocalDateISO(messageItem.create_at());
            ((ItemViewHolder) viewHolder).createdAt.setText(DateUtils.getRelativeTimeSpanString(date.getTime()));
            if (messageItem.seen()) {
                ((ItemViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_communication_messenger);
            } else {
                ((ItemViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_communication_messenger_active);
            }
        }
    }

    public RecentMessageItem getItemAt(int position)
    {
        if(items != null && !items.isEmpty() && items.size() > position) {
            return items.get(position);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public ViewHolder(View itemView)
        {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class ItemViewHolder extends ViewHolder
    {
        @BindView(R.id.messageBody)
        public TextView messageBody;

        @BindView(R.id.itemIcon)
        public ImageView icon;

        @BindView(R.id.contactId)
        public TextView contactId;

        @BindView(R.id.createdAt)
        public TextView createdAt;

        public ItemViewHolder(View itemView)
        {
            super(itemView);
        }
    }

    public class EmptyViewHolder extends ViewHolder
    {
        @OnClick(R.id.advertiseButton)
        public void advertiseButtonClicked()
        {
            onItemClickListener.onAdvertiseButtonClicked();
        }

        @OnClick(R.id.searchButton)
        public void searchButtonClicked()
        {
            onItemClickListener.onSearchButtonClicked();
        }

        public EmptyViewHolder(View itemView)
        {
            super(itemView);
        }
    }

    public class ProgressViewHolder extends ViewHolder
    {
        public ProgressViewHolder(View itemView)
        {
            super(itemView);
        }
    }
}

