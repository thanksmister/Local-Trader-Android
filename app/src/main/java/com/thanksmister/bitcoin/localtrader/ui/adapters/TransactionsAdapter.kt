/*
 * Copyright (c) 2019 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.adapters

import android.content.Context
import android.graphics.PorterDuff
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.TransactionType
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Dates
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import kotlinx.android.synthetic.main.adapter_contact_list.view.*
import kotlinx.android.synthetic.main.adapter_transaction_list.view.*

import java.util.Collections


class TransactionsAdapter() : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    private var items: List<Transaction> = emptyList()

    fun replaceWith(data: List<Transaction>) {
        this.items = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.size else 0
    }

    fun getItemAt(position: Int): Any? {
        return try {
            items[position]
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsAdapter.ViewHolder {
        val itemLayoutView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return ViewHolder(itemLayoutView)
    }

    override fun getItemViewType(position: Int): Int {
        return TYPE_TRANSACTION
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bindItems(items[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(transaction: Transaction) {
            val amount = Conversions.formatBitcoinAmount(transaction.amount)
            val transactionType = transaction.transactionType
            if (transactionType == TransactionType.RECEIVED) {
                val blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description)
                val blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress!!
                itemView.descriptionText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_received_description, blockUrl, blockAddress))
                itemView.descriptionText.movementMethod = LinkMovementMethod.getInstance()
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_received_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_down_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.text_green), PorterDuff.Mode.SRC_ATOP)
            } else if (transactionType == TransactionType.SENT) {
                val blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description)
                val blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress!!
                itemView.descriptionText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_sent_description, blockUrl, blockAddress))
                itemView.descriptionText.movementMethod = LinkMovementMethod.getInstance()
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_sent_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_up_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.red), PorterDuff.Mode.SRC_ATOP)
            } else if (transactionType == TransactionType.FEE) {
                itemView.descriptionText.text = itemView.context.getString(R.string.transaction_fee_description)
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_sent_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_up_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.red), PorterDuff.Mode.SRC_ATOP)
            } else if (transactionType == TransactionType.CONTACT_SENT) {
                //String contactId = Constants.BLOCKCHAIN_INFO_ADDRESS + WalletUtils.parseContactIdFromTransaction(transaction);
                itemView.descriptionText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_contact_sent, transaction.description))
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_sent_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_up_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.red), PorterDuff.Mode.SRC_ATOP)
            } else if (transactionType == TransactionType.CONTACT_RECEIVE) {
                itemView.descriptionText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_contact_received, transaction.description))
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_received_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_down_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.text_green), PorterDuff.Mode.SRC_ATOP)
            } else if (transactionType == TransactionType.AFFILIATE) {
                itemView.descriptionText!!.text = Html.fromHtml(itemView.context.getString(R.string.transaction_affiliate, transaction.description))
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_received_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_down_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.text_green), PorterDuff.Mode.SRC_ATOP)
            } else if (transactionType == TransactionType.INTERNAL) {
                val blockAddress = WalletUtils.parseBitcoinAddressFromTransaction(transaction.description)
                val blockUrl = Constants.BLOCKCHAIN_INFO_ADDRESS + blockAddress!!
                itemView.descriptionText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_internal, blockUrl, blockAddress))
                itemView.btcText.text = Html.fromHtml(itemView.context.getString(R.string.transaction_received_btc_amount, amount))
                itemView.transactionIcon.setImageResource(R.drawable.ic_arrow_down_circle_outline)
                itemView.transactionIcon.setColorFilter(itemView.context.resources.getColor(R.color.text_green), PorterDuff.Mode.SRC_ATOP)
            }

            if (!TextUtils.isEmpty(transaction.createdAt)) {
                itemView.dateText!!.text = Dates.parseLocaleDate(transaction.createdAt)
            }
        }
    }

    companion object {
        const val TYPE_TRANSACTION = R.layout.adapter_transaction_list
    }
}