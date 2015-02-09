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

package com.thanksmister.bitcoin.localtrader.ui.misc;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Transaction;
import com.thanksmister.bitcoin.localtrader.data.api.model.TransactionType;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TransactionsAdapter extends BaseAdapter
{
    private List<Transaction> data = Collections.emptyList();
    private Context context;
    private final LayoutInflater inflater;
    
    public TransactionsAdapter(Context context)
    {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public boolean isEnabled(int position)
    {
        return true;
    }

    @Override
    public int getCount()
    {
        return data.size();
    }

    @Override
    public Transaction getItem(int position)
    {
        return data.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public void replaceWith(List<Transaction> data)
    {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent)
    {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_transaction_list, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        /*if (position % 2 == 0)
            holder.row.setBackgroundColor(context.getResources().getColor(R.color.white));
        else
            holder.row.setBackgroundColor(context.getResources().getColor(R.color.list_gray_color));*/
        
        Transaction transaction = getItem(position);
        String amount = Conversions.formatBitcoinAmount(Doubles.convertToDouble(transaction.amount));

        if(transaction.type == TransactionType.RECEIVED) {
            String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description);
            String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
            holder.descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_received_description, blockUrl, blockAddress)));
            holder.descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        } else if(transaction.type == TransactionType.SENT) {
            String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description);
            String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
            holder.descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_description, blockUrl, blockAddress)));
            holder.descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount))); 
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
        } else if ((transaction.type == TransactionType.FEE)) {
            holder.descriptionText.setText(context.getString(R.string.transaction_fee_description, amount));
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount)));
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
        } else if ((transaction.type == TransactionType.CONTACT_SENT)) {
            //String contactId = Constants.BLOCKCHAIN_INFO_ADDRESS + WalletUtils.parseContactIdFromTransaction(transaction);
            holder.descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_contact_sent, transaction.description)));
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount)));
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
        } else if ((transaction.type == TransactionType.CONTACT_RECEIVE)) {
            holder.descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_contact_received, transaction.description)));
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        } else if ((transaction.type == TransactionType.AFFILIATE)) {
            holder.descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_affiliate, transaction.description)));
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        } else if ((transaction.type == TransactionType.INTERNAL)) {
            String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description);
            String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
            holder.descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_internal, blockUrl, blockAddress)));
            holder.btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            holder.transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        }

        if(transaction.created_at != null) {
            holder.dateText.setText(Dates.parseLocalDateStringShort(transaction.created_at));
        }

        return view;
    }

    static class ViewHolder
    {  
        @InjectView(R.id.descriptionText) TextView descriptionText;
        @InjectView(R.id.btcText) TextView btcText;
        @InjectView(R.id.dateText) TextView dateText;
        @InjectView(R.id.transactionIcon)
        ImageView transactionIcon;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}


