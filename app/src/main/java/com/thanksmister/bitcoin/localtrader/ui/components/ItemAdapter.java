/*
 * Copyright 2007 ZXing authors
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

package com.thanksmister.bitcoin.localtrader.ui.components;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>
{
    private static final int TYPE_EMPTY = R.layout.view_empty_dashboard;
    private static final int TYPE_PROGRESS = R.layout.view_progress_dashboard;
    private static final int TYPE_HEADER = R.layout.view_dashboard_header;
    private static final int TYPE_CONTACT = R.layout.adapter_dashboard_contact_list;
    private static final int TYPE_ADVERTISEMENT = R.layout.adapter_dashboard_advertisement_list;
    
    protected List items;
    protected List<MethodItem> methods = Collections.emptyList();
    private Context context;
    private OnItemClickListener onItemClickListener;

    public static interface OnItemClickListener
    {
        public void onSearchButtonClicked();
        public void onAdvertiseButtonClicked();
    }
   
    public ItemAdapter(Context context)
    {
        this.context = context;
    }

    public ItemAdapter(Context context, OnItemClickListener onItemClickListener)
    {
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }
  
    public void replaceWith(List data, List<MethodItem> methods)
    {
        this.items = data;
        this.methods = methods;
        notifyDataSetChanged();
    }
    
    // Create new views (invoked by the layout manager)
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false);
        
        if(viewType == TYPE_CONTACT) {
            return new ContactViewHolder(itemLayoutView);
        } else if (viewType == TYPE_ADVERTISEMENT) {
            return new AdvertisementViewHolder(itemLayoutView);
        } else if (viewType == TYPE_HEADER) {
            return new ExchangeViewHolder(itemLayoutView);
        } else if (viewType == TYPE_EMPTY) {
            return new EmptyViewHolder(itemLayoutView);
        } 

        return new ProgressViewHolder(itemLayoutView);
    }

    @Override
    public int getItemViewType(int position) {

        Timber.d("Position: " + position);
        
        if (items == null) {
            return TYPE_PROGRESS;
        }
        
        /*if(isPositionHeader(position)) {
            return TYPE_HEADER;
        }
*/
        if (items.size() == 0) {
            return TYPE_EMPTY;
        }
        
        return (items.get(position) instanceof ContactItem)? TYPE_CONTACT:TYPE_ADVERTISEMENT;
    }

    @Override
    public int getItemCount() {
        
        if (items == null)
            return -1;
        
        return items.size() > 0 ? items.size() : 1;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position)
    {
        if(viewHolder instanceof ContactViewHolder) {

            ContactItem contact = (ContactItem) items.get(position);
            TradeType tradeType = TradeType.valueOf(contact.advertisement_trade_type());
            String type = "";
            switch (tradeType) {
                case LOCAL_BUY:
                case LOCAL_SELL:
                    type = (contact.is_buying())? "Buying Locally":"Selling Locally";
                    break;
                case ONLINE_BUY:
                case ONLINE_SELL:
                    type = (contact.is_buying())? "Buying Online":"Selling Online";
                    break;
            }

            String amount =  contact.amount() + " " + contact.currency();
            //String btc =  contact.amount_btc() + context.getString(R.string.btc);
            String person = (contact.is_buying())? contact.seller_username():contact.buyer_username();
            String date = Dates.parseLocalDateStringAbbreviatedTime(contact.created_at());

            ((ContactViewHolder) viewHolder).tradeType.setText(type + " - " + amount);
            ((ContactViewHolder) viewHolder).tradeDetails.setText("With " + person + " (" + date + ")");
            ((ContactViewHolder) viewHolder).contactMessageCount.setText(String.valueOf(contact.messageCount()));
            
        } else if (viewHolder instanceof AdvertisementViewHolder) {
            
            AdvertisementItem advertisement = (AdvertisementItem) items.get(position);
            TradeType tradeType = TradeType.valueOf(advertisement.trade_type());
            String type = "";
            switch (tradeType) {
                case LOCAL_BUY:
                    type = "Local Buy -";
                    break;
                case LOCAL_SELL:
                    type = "Local Sale -";
                    break;
                case ONLINE_BUY:
                    type = "Online Buy -";
                    break;
                case ONLINE_SELL:
                    type = "Online Sale -";
                    break;
            }

            String price = advertisement.temp_price() + " " + advertisement.currency();
            String location = advertisement.location_string();

            if(advertisement.trade_type().equals(TradeType.LOCAL_SELL.name()) || advertisement.trade_type().equals(TradeType.LOCAL_BUY.name())) {

                if(TradeUtils.isAtm(advertisement)) {
                    ((AdvertisementViewHolder) viewHolder).advertisementType.setText("ATM");
                } else {
                    ((AdvertisementViewHolder) viewHolder).advertisementType.setText(type + " " + price);
                    ((AdvertisementViewHolder) viewHolder).advertisementDetails.setText("In " + location);
                }

            } else {

                String adLocation = TradeUtils.isOnlineTrade(advertisement)? advertisement.location_string(): advertisement.city();
                String paymentMethod = TradeUtils.getPaymentMethodFromItems(advertisement, methods);
                if(Strings.isBlank(paymentMethod)) {
                    ((AdvertisementViewHolder) viewHolder).advertisementDetails.setText("In " + adLocation);
                } else {
                    ((AdvertisementViewHolder) viewHolder).advertisementDetails.setText("With " +  paymentMethod + " in " + adLocation);
                }

                ((AdvertisementViewHolder) viewHolder).advertisementType.setText(type + " " + price);
            }

            if (advertisement.visible()) {
                ((AdvertisementViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_action_visibility_dark);
            } else {
                ((AdvertisementViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_action_visibility_off_dark);
            }
        } else if (viewHolder instanceof ExchangeViewHolder) {

            ExchangeItem exchangeItem = (ExchangeItem) items.get(position);
            String value = Calculations.calculateAverageBidAskFormatted(exchangeItem.bid(), exchangeItem.ask());
            ((ExchangeViewHolder) viewHolder).bitcoinTitle.setText("MARKET PRICE");
            ((ExchangeViewHolder) viewHolder).bitcoinPrice.setText("$" + value + " / BTC");
            ((ExchangeViewHolder) viewHolder).bitcoinValue.setText("Source " + exchangeItem.exchange());
            ((ExchangeViewHolder) viewHolder).itemView.refreshDrawableState();

        } else if (viewHolder instanceof EmptyViewHolder) {
            // do nothing
        } else if (viewHolder instanceof ProgressViewHolder) {
            // do nothing
        }
    }

    public Object getItemAt(int position)
    {
        try {
            return items.get(position);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public ViewHolder(View itemView)
        {
            super(itemView);
            
            ButterKnife.inject(this, itemView);
        }
    }

    // inner class to hold a reference to each item of RecyclerView 
    public class ContactViewHolder extends ViewHolder
    {
        @InjectView(R.id.tradeType)
        public TextView tradeType;
        
        @InjectView(R.id.itemIcon)
        public ImageView icon;
        
        @InjectView(R.id.tradeDetails)
        public TextView tradeDetails;
        
        @InjectView(R.id.contactMessageCount)
        public TextView contactMessageCount;

        public ContactViewHolder(View itemView)
        {
            super(itemView);
        }
    }

    public class AdvertisementViewHolder extends ViewHolder
    {
        @InjectView(android.R.id.background)
        public View row;
        
        @InjectView(R.id.advertisementType)
        public TextView advertisementType;
        
        @InjectView(R.id.itemIcon)
        public ImageView icon;
        
        @InjectView(R.id.advertisementDetails)
        public TextView advertisementDetails;
        
        public AdvertisementViewHolder(View itemView) 
        {
            super(itemView);
        }
    }

    public class ExchangeViewHolder extends ViewHolder
    {
        @InjectView(R.id.bitcoinTitle)
        TextView bitcoinTitle;

        @InjectView(R.id.bitcoinPrice)
        TextView bitcoinPrice;

        @InjectView(R.id.bitcoinValue)
        TextView bitcoinValue;

        public ExchangeViewHolder(View itemView)
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
        
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ProgressViewHolder extends ViewHolder
    {
        public ProgressViewHolder(View itemView) {
            super(itemView);
        }
    }
}

