/*
 * Copyright (c) 2016. DusApp
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
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.thanksmister.bitcoin.localtrader.R.menu.contact;

public class AdvertisementsAdapter extends RecyclerView.Adapter<AdvertisementsAdapter.ViewHolder>
{
    private static final int TYPE_EMPTY = R.layout.view_empty_dashboard;
    private static final int TYPE_PROGRESS = R.layout.view_progress_dashboard;
    private static final int TYPE_ITEM = R.layout.adapter_dashboard_advertisement_list;

    protected List<AdvertisementItem> items;
    protected List<MethodItem> methods = Collections.emptyList();
    private Context context;
    private OnItemClickListener onItemClickListener;

    public static interface OnItemClickListener
    {
        public void onSearchButtonClicked();

        public void onAdvertiseButtonClicked();
    }

    public AdvertisementsAdapter(Context context)
    {
        this.context = context;
    }

    public AdvertisementsAdapter(Context context, OnItemClickListener onItemClickListener)
    {
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    public void replaceWith(List<AdvertisementItem> data, List<MethodItem> methods)
    {
        this.items = data;
        this.methods = methods;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdvertisementsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false);

        if (viewType == TYPE_ITEM) {
            return new AdvertisementViewHolder(itemLayoutView);
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
        if (viewHolder instanceof AdvertisementViewHolder) {

            AdvertisementItem advertisement = items.get(position);
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

            if (advertisement.trade_type().equals(TradeType.LOCAL_SELL.name()) || advertisement.trade_type().equals(TradeType.LOCAL_BUY.name())) {

                if (TradeUtils.isAtm(advertisement)) {
                    ((AdvertisementViewHolder) viewHolder).advertisementType.setText("ATM");
                } else {
                    ((AdvertisementViewHolder) viewHolder).advertisementType.setText(type + " " + price);
                    ((AdvertisementViewHolder) viewHolder).advertisementDetails.setText("In " + location);
                }

            } else {

                String adLocation = TradeUtils.isOnlineTrade(advertisement) ? advertisement.location_string() : advertisement.city();
                String paymentMethod = TradeUtils.getPaymentMethodFromItems(advertisement, methods);
                if (Strings.isBlank(paymentMethod)) {
                    ((AdvertisementViewHolder) viewHolder).advertisementDetails.setText("In " + adLocation);
                } else {
                    ((AdvertisementViewHolder) viewHolder).advertisementDetails.setText("With " + paymentMethod + " in " + adLocation);
                }

                ((AdvertisementViewHolder) viewHolder).advertisementType.setText(type + " " + price);
            }

            if (advertisement.visible()) {
                ((AdvertisementViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_action_visibility_dark);
            } else {
                ((AdvertisementViewHolder) viewHolder).icon.setImageResource(R.drawable.ic_action_visibility_off_dark);
            }

            String date = Dates.parseLocaleDate(advertisement.created_at());
            ((AdvertisementViewHolder) viewHolder).advertisementId.setText(advertisement.ad_id());
            ((AdvertisementViewHolder) viewHolder).advertisementDate.setText(date);
        }
    }

    public AdvertisementItem getItemAt(int position)
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
            ButterKnife.inject(this, itemView);
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

        @InjectView(R.id.advertisementId)
        public TextView advertisementId;

        @InjectView(R.id.advertisementDate)
        public TextView advertisementDate;

        public AdvertisementViewHolder(View itemView)
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

