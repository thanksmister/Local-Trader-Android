/*
 * Copyright (c) 2015 ThanksMister LLC
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

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.TransactionType;
import com.thanksmister.bitcoin.localtrader.data.api.model.WalletAdapter;
import com.thanksmister.bitcoin.localtrader.data.database.TransactionItem;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder>
{
    private static final int TYPE_TRANSACTION = R.layout.adapter_transaction_list;
    private static final int TYPE_WALLET = R.layout.view_wallet_header;
    private static final int TYPE_PROGRESS = R.layout.view_progress_dashboard;
    
    private List items = Collections.emptyList();
    private Context context;
  
    public TransactionsAdapter(Context context)
    {
        this.context = context;
    }

    public void replaceWith(List data)
    {
        this.items = data;
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemCount() {
        
        return items.size() > 0 ? items.size() : 1;
    }

    public Object getItemAt(int position)
    {
        try {
            return items.get(position);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public TransactionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false);

        if(viewType == TYPE_WALLET) {
            return new WalletViewHolder(itemLayoutView);
        } else if (viewType == TYPE_TRANSACTION) {
            return new TransactionViewHolder(itemLayoutView);
        }

        return new ProgressViewHolder(itemLayoutView);
    }

    @Override
    public int getItemViewType(int position) {
        
        if (items.size() == 0) {
            return TYPE_PROGRESS;
        }
        
        if (isPositionHeader(position)) {
            return TYPE_WALLET;
        }

        return TYPE_TRANSACTION;
    }

    private boolean isPositionHeader(int position)
    {
        return position == 0;
    }
    

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position)
    {
        if(items == null) return;
        
        if(viewHolder instanceof WalletViewHolder) {

            WalletAdapter item = (WalletAdapter) items.get(position);
            if(item.address != null)
                ((WalletViewHolder) viewHolder).addressButton.setText(item.address);
            
            if(item.qrImage != null)
                ((WalletViewHolder) viewHolder).qrCodeImage.setImageBitmap(item.qrImage);
            
        } else if (viewHolder instanceof TransactionViewHolder) {

            TransactionItem transaction = (TransactionItem) items.get(position);
            
            String amount = "";
                    
            // TODO convert the data to the display format instead of converting, only convert when doing math
            try {
                amount = Conversions.formatBitcoinAmount(transaction.amount()); 
            } catch (Exception e) {
                Timber.e(e.getMessage());
            }
            
            if(transaction.tx_type() == TransactionType.RECEIVED) {
                String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description());
                String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
                ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_received_description, blockUrl, blockAddress)));
                ((TransactionViewHolder) viewHolder).descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
                ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_received_btc_amount, amount)));
                ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_right_bottom);
            } else if(transaction.tx_type() == TransactionType.SENT) {
                String blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description());
                String blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress;
                ((TransactionViewHolder) viewHolder).descriptionText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_description, blockUrl, blockAddress)));
                ((TransactionViewHolder) viewHolder).descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
                ((TransactionViewHolder) viewHolder).btcText.setText(Html.fromHtml(context.getString(R.string.transaction_sent_btc_amount, amount)));
                ((TransactionViewHolder) viewHolder).transactionIcon.setImageResource(R.drawable.ic_action_arrow_left_top);
            } else if ((transaction.tx_type() == TransactionType.FEE)) {
                ((TransactionViewHolder) viewHolder).descriptionText.setText(context.getString(R.string.transaction_fee_description, amount));
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

            if(transaction.created_at() != null) {
                ((TransactionViewHolder) viewHolder).dateText.setText(Dates.parseLocalDateStringShort(transaction.created_at()));
            }
            
        } else if (viewHolder instanceof ProgressViewHolder) {
            // do nothing
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

    public class TransactionViewHolder extends ViewHolder
    {
        @InjectView(R.id.descriptionText)
        TextView descriptionText;
        
        @InjectView(R.id.btcText)
        TextView btcText;
        
        @InjectView(R.id.dateText)
        TextView dateText;
        
        @InjectView(R.id.transactionIcon)
        ImageView transactionIcon;

        public TransactionViewHolder(View itemView)
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
    
    public class WalletViewHolder extends ViewHolder
    {
        @InjectView(R.id.codeImage)
        ImageView qrCodeImage;

        @InjectView(R.id.walletAddressButton)
        AutoResizeTextView addressButton;

        @OnClick(R.id.codeImage)
        public void codeButtonClicked()
        {
            setAddressOnClipboard(addressButton.getText().toString());
        }
        
        @OnClick(R.id.walletAddressButton)
        public void addressButtonClicked()
        {
            setAddressOnClipboard(addressButton.getText().toString());
        }

        public WalletViewHolder(View itemView)
        {
            super(itemView);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    protected void setAddressOnClipboard(String address)
    {
        if (address != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(context.getString(R.string.wallet_address_clipboard_title), address);
                clipboard.setPrimaryClip(clip);
            } else {
                android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setText(address);
            }

            Toast.makeText(context, context.getString(R.string.wallet_address_copied_toast), Toast.LENGTH_SHORT).show();
        }
    }
}


