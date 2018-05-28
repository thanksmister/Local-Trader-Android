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
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.persistence.Notification;
import com.thanksmister.bitcoin.localtrader.utils.Dates;

import java.util.Date;
import java.util.List;


public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private static final int TYPE_EMPTY = R.layout.view_empty_dashboard;
    private static final int TYPE_PROGRESS = R.layout.view_progress_dashboard;
    private static final int TYPE_ITEM = R.layout.adapter_dashboard_notification_list;

    protected List<Notification> items;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public static interface OnItemClickListener {
        public void onSearchButtonClicked();

        public void onAdvertiseButtonClicked();
    }

    public NotificationAdapter(Context context, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    public void replaceWith(List<Notification> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public NotificationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public int getItemViewType(int position) {
        if (items == null) {
            return TYPE_PROGRESS;
        } else if (items.size() == 0) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        if (items == null)
            return -1;

        return items.size() > 0 ? items.size() : 1;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        if (viewHolder instanceof ItemViewHolder) {
            Notification item = items.get(position);
            ((ItemViewHolder) viewHolder).messageBody.setText(item.getMsg().trim());
            ((ItemViewHolder) viewHolder).contactId.setVisibility(View.VISIBLE);
            if (item.getContactId() != null) {
                ((ItemViewHolder) viewHolder).contactId.setText("Contact #" + item.getContactId());
            } else if (item.getAdvertisementId() != null) {
                ((ItemViewHolder) viewHolder).contactId.setText("Advertisement #" + item.getAdvertisementId());
            } else {
                ((ItemViewHolder) viewHolder).contactId.setVisibility(View.GONE);
            }

            Date date = Dates.parseLocalDateISO(item.getCreatedAt());
            ((ItemViewHolder) viewHolder).createdAt.setText(DateUtils.getRelativeTimeSpanString(date.getTime()));

            if (item.getRead()) {
                ((ItemViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_action_notification);
            } else {
                ((ItemViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_action_notification_new);
            }
        }
    }

    public Notification getItemAt(int position) {
        if (items != null && !items.isEmpty() && items.size() > position) {
            return items.get(position);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ItemViewHolder extends ViewHolder {
        public TextView messageBody;

        public ImageView icon;

        public TextView contactId;

        public TextView createdAt;

        public ItemViewHolder(View itemView) {
            super(itemView);
        }
    }


    public class EmptyViewHolder extends ViewHolder {
        public void advertiseButtonClicked() {
            onItemClickListener.onAdvertiseButtonClicked();
        }

        public void searchButtonClicked() {
            onItemClickListener.onSearchButtonClicked();
        }

        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ProgressViewHolder extends ViewHolder {
        public ProgressViewHolder(View itemView) {
            super(itemView);
        }
    }
}

