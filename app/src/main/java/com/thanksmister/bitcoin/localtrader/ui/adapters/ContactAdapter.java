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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.utils.Dates;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder>
{
    private static final int TYPE_ITEM = R.layout.adapter_dashboard_contact_list;

    protected List<ContactItem> items;
    private Context context;
    
    public ContactAdapter(Context context) {
        this.context = context;
    }

    public void replaceWith(List<ContactItem> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ContactAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ItemViewHolder(itemLayoutView);
    }

    @Override
    public int getItemViewType(int position)
    {
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount()
    {
        if (items == null)
            return 0;

        return items.size() > 0? items.size():0;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position)
    {
        if (viewHolder instanceof ItemViewHolder) {
            ContactItem contact = items.get(position);
            TradeType tradeType = TradeType.valueOf(contact.advertisement_trade_type());
            String type = "";
            switch (tradeType) {
                case LOCAL_BUY:
                case LOCAL_SELL:
                    type = (contact.is_buying()) ? "Buying Locally" : "Selling Locally";
                    break;
                case ONLINE_BUY:
                case ONLINE_SELL:
                    type = (contact.is_buying()) ? "Buying Online" : "Selling Online";
                    break;
            }

            String amount = contact.amount() + " " + contact.currency();
            String person = (contact.is_buying()) ? contact.seller_username() : contact.buyer_username();
            String date = Dates.parseLocaleDateTime(contact.created_at());
            
            ((ItemViewHolder) viewHolder).tradeType.setText(type + " - " + amount);
            ((ItemViewHolder) viewHolder).tradeDetails.setText("With " + person);
            ((ItemViewHolder) viewHolder).contactId.setText(contact.contact_id());
            ((ItemViewHolder) viewHolder).contactDate.setText(date);
        }
    }

    public ContactItem getItemAt(int position)
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
        @BindView(R.id.tradeType)
        public TextView tradeType;

        @BindView(R.id.itemIcon)
        public ImageView icon;

        @BindView(R.id.tradeDetails)
        public TextView tradeDetails;

        @BindView(R.id.contactId)
        public TextView contactId;

        @BindView(R.id.contactDate)
        public TextView contactDate;

        public ItemViewHolder(View itemView)
        {
            super(itemView);
        }
    }
}

