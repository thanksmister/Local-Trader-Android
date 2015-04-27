/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.ui.misc.LinearListView;
import com.thanksmister.bitcoin.localtrader.utils.Dates;

public class DashboardContactAdapter extends ContactAdapter
{
    public DashboardContactAdapter(Context context)
    {
        super(context);
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent)
    {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_dashboard_contact_list, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        ContactItem contact = getItem(position);

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
        String person = (contact.is_buying())? contact.seller_username():contact.buyer_username();
        String date = Dates.parseLocalDateStringAbbreviatedTime(contact.created_at());

        holder.tradeType.setText(type + " - " + amount);
        holder.tradeDetails.setText("With " + person + " (" + date + ")");
 
        if(contact.unseen_messages()) {
            holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_communication_messenger_active));
        } else {
            holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_communication_messenger));
        }

        holder.contactMessageCount.setText(String.valueOf(contact.message_count()));
        
        holder.contactButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                ((LinearListView) parent).performItemClick(v, position, 0);
            }
        });
        
        return view;
    }
}


