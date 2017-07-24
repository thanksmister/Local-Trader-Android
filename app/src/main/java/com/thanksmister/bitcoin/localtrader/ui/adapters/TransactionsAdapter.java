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
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.TransactionType;
import com.thanksmister.bitcoin.localtrader.data.database.TransactionItem;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
    
    private static final int TYPE_TRANSACTION = R.layout.adapter_transaction_list;

    private List items = Collections.emptyList();
    private Context context;

    public TransactionsAdapter(Context context) {
        this.context = context;
    }

    public void replaceWith(List data) {
        this.items = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size() > 0 ? items.size() : 0;
    }

    public Object getItemAt(int position) {
        try {
            return items.get(position);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public TransactionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new TransactionViewHolder(itemLayoutView);
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_TRANSACTION;
    }
    
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        
        if (items == null || items.isEmpty()) {
            return;
        }
        
        TransactionItem transaction = (TransactionItem) items.get(position);
        String amount = Conversions.formatBitcoinAmount(transaction.amount());
        if (transaction.tx_type() == TransactionType.RECEIVED) {
            String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description());
            String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
            ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_received_description, blockUrl, blockAddress)));
            ((TransactionViewHolder) viewHolder).descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        } else if (transaction.tx_type() == TransactionType.SENT) {
            String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description());
            String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
            ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_description, blockUrl, blockAddress)));
            ((TransactionViewHolder) viewHolder).descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
        } else if ((transaction.tx_type() == TransactionType.FEE)) {
            ((TransactionViewHolder) viewHolder).descriptionText.setText(context.getString(R.string.transaction_fee_description));
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
        } else if ((transaction.tx_type() == TransactionType.CONTACT_SENT)) {
            //String contactId = Constants.BLOCKCHAIN_INFO_ADDRESS + WalletUtils.parseContactIdFromTransaction(transaction);
            ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_contact_sent, transaction.description())));
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
        } else if ((transaction.tx_type() == TransactionType.CONTACT_RECEIVE)) {
            ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_contact_received, transaction.description())));
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        } else if ((transaction.tx_type() == TransactionType.AFFILIATE)) {
            ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_affiliate, transaction.description())));
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        } else if ((transaction.tx_type() == TransactionType.INTERNAL)) {
            String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description());
            String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
            ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_internal, blockUrl, blockAddress)));
            ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
            ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
        }

        if (!TextUtils.isEmpty(transaction.created_at())) {
            ((TransactionViewHolder) viewHolder).dateText.setText(Dates.parseLocalDateStringShort(transaction.created_at()));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }

    class TransactionViewHolder extends ViewHolder {
        @InjectView(R.id.descriptionText)
        TextView descriptionText;

        @InjectView(R.id.btcText)
        TextView btcText;

        @InjectView(R.id.dateText)
        TextView dateText;

        @InjectView(R.id.transactionIcon)
        ImageView transactionIcon;

        TransactionViewHolder(View itemView) {
            super(itemView);
        }
    }
}