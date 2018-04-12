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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.network.api.model.AdvertisementType;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AdvertisementAdapter extends BaseAdapter implements Filterable {
    protected List<AdvertisementItem> data = Collections.emptyList();
    protected List<MethodItem> methods = Collections.emptyList();
    protected Context context;
    protected final LayoutInflater inflater;
    private ValueFilter valueFilter;
    private List<AdvertisementItem> dataFiltered;

    public AdvertisementAdapter(Context context) {
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
    public AdvertisementItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void replaceWith(List<AdvertisementItem> data, List<MethodItem> methods) {
        this.data = data;
        this.methods = methods;
        dataFiltered = data;
        notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_advertisement_list, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        AdvertisementItem advertisement = getItem(position);

        if (advertisement.visible())
            holder.row.setBackgroundColor(context.getResources().getColor(R.color.white));
        else
            holder.row.setBackgroundColor(context.getResources().getColor(R.color.list_gray_color));

        TradeType tradeType = TradeType.valueOf(advertisement.trade_type());
        String type = "";
        switch (tradeType) {
            case LOCAL_BUY:
                type = context.getString(R.string.text_advertisement_item_local_buy);
                break;
            case LOCAL_SELL:
                type = context.getString(R.string.text_advertisement_item_local_sale);
                break;
            case ONLINE_BUY:
                type = context.getString(R.string.text_advertisement_item_online_buy);
                break;
            case ONLINE_SELL:
                type = context.getString(R.string.text_advertisement_item_local_sale);
                break;
        }

        String price = advertisement.temp_price() + " " + advertisement.currency();
        String location = advertisement.location_string();

        if (TradeUtils.isLocalTrade(advertisement)) {
            if (TradeUtils.isAtm(advertisement)) {
                holder.advertisementType.setText(R.string.text_atm);
                holder.advertisementDetails.setText(context.getText(R.string.text_atm) + " " + context.getString(R.string.text_in, advertisement.city()));
            } else {
                holder.advertisementType.setText(type + " " + price);
                holder.advertisementDetails.setText(context.getString(R.string.text_in_caps, location));
            }

        } else {
            String paymentMethod = TradeUtils.getPaymentMethodFromItems(advertisement, methods);
            holder.advertisementType.setText(type + price);
            holder.advertisementDetails.setText(context.getString(R.string.text_with_in, paymentMethod, advertisement.city()));
        }

        if (advertisement.visible()) {
            holder.icon.setImageResource(R.drawable.ic_action_visibility);
        } else {
            holder.icon.setImageResource(R.drawable.ic_action_visibility_off);
        }

        return view;
    }

    @Override
    public Filter getFilter() {
        if (valueFilter == null) {
            valueFilter = new ValueFilter();
        }

        return valueFilter;
    }

    private class ValueFilter extends Filter {
        //Invoked in a worker thread to filter the data according to the constraint.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint != null && constraint.length() > 0) {
                ArrayList<AdvertisementItem> filterList = new ArrayList<AdvertisementItem>();

                for (AdvertisementItem advertisement : dataFiltered) {

                    if (constraint.equals(AdvertisementType.INACTIVE.name())) {

                        if (!advertisement.visible()) {
                            filterList.add(advertisement);
                        }

                    } else if (constraint.equals(AdvertisementType.ACTIVE.name())) {

                        if (advertisement.visible()) {
                            filterList.add(advertisement);
                        }

                    } else {
                        filterList.add(advertisement);
                    }
                }
                results.count = filterList.size();
                results.values = filterList;
            } else {
                results.count = dataFiltered.size();
                results.values = dataFiltered;
            }
            return results;
        }

        //Invoked in the UI thread to publish the filtering results in the user interface.
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            data = (ArrayList<AdvertisementItem>) results.values;
            notifyDataSetChanged();
        }
    }

    protected static class ViewHolder {
        @BindView(android.R.id.background)
        public View row;
        @BindView(R.id.advertisementType)
        public TextView advertisementType;
        @BindView(android.R.id.icon)
        public ImageView icon;
        @BindView(R.id.advertisementDetails)
        public TextView advertisementDetails;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public static class ContactAdapter extends BaseAdapter {
        protected List<Contact> contacts = Collections.emptyList();
        protected Context context;
        protected final LayoutInflater inflater;

        public ContactAdapter(Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public int getCount() {
            return contacts.size();
        }

        @Override
        public Contact getItem(int position) {
            return contacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void replaceWith(List<Contact> data) {
            contacts = data;
            notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view != null) {
                holder = (ViewHolder) view.getTag();
            } else {
                view = inflater.inflate(R.layout.adapter_contact_list, parent, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }

            Contact contact = getItem(position);

            String type = "";
            switch (contact.advertisement.trade_type) {
                case LOCAL_BUY:
                case LOCAL_SELL:
                    type = (contact.is_buying) ? context.getString(R.string.text_buying_locally) : context.getString(R.string.text_selling_locally);
                    break;
                case ONLINE_BUY:
                case ONLINE_SELL:
                    type = (contact.is_buying) ? context.getString(R.string.text_buying_online) : context.getString(R.string.text_selling_online);
                    break;
                default:
                    type = context.getString(R.string.text_trade);
                    break;
            }

            String amount = contact.amount + " " + contact.currency;
            String person = (contact.is_buying) ? contact.seller.username : contact.buyer.username;
            String date = Dates.parseLocalDateStringAbbreviatedTime(contact.created_at);

            holder.tradeType.setText(type + " - " + amount);
            holder.tradeDetails.setText(context.getString(R.string.text_with, person + " (" + date + ")"));
            holder.contactMessageCount.setText(String.valueOf(contact.messages.size()));

            return view;
        }

        static class ViewHolder {
            @BindView(R.id.tradeType)
            TextView tradeType;
            @BindView(android.R.id.icon)
            ImageView icon;
            @BindView(R.id.tradeDetails)
            TextView tradeDetails;
            @BindView(R.id.contactMessageCount)
            TextView contactMessageCount;

            public ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }
    }
}


