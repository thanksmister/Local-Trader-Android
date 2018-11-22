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

package com.thanksmister.bitcoin.localtrader.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.utils.Dates
import kotlinx.android.synthetic.main.adapter_dashboard_contact_list.view.*
import kotlinx.android.synthetic.main.view_empty_contacts.view.*

class ContactsAdapter(private val context: Context, private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private var items = emptyList<Contact>()

    interface OnItemClickListener {
        fun onSearchButtonClicked()
        fun onAdvertiseButtonClicked()
    }

    fun replaceWith(data: List<Contact>) {
        this.items = data
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false)
        return ViewHolder(itemLayoutView)
    }

    override fun getItemViewType(position: Int): Int {
        if (items.isEmpty()) {
            return TYPE_EMPTY
        }
        return TYPE_ITEM
    }

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.size else 1
    }

    fun getItemAt(position: Int): Contact? {
        return if (!items.isEmpty() && items.size > position) {
            items[position]
        } else null
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if(items.isEmpty()) {
            viewHolder.bindEmpty(onItemClickListener)
        } else {
            viewHolder.bindItems(items[position])
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(contact: Contact) {
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            var type = ""
            when (tradeType) {
                TradeType.LOCAL_BUY, TradeType.LOCAL_SELL -> type = if (contact.isBuying) itemView.context.getString(R.string.text_buying_locally) else itemView.context.getString(R.string.text_selling_locally)
                TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> type = if (contact.isBuying) itemView.context.getString(R.string.text_buying_online) else itemView.context.getString(R.string.text_selling_online)
                else -> {
                }
            }
            val amount = contact.amount + " " + contact.currency
            val person = if (contact.isBuying) contact.seller.username else contact.buyer.username
            val date = Dates.parseLocaleDateTime(contact.createdAt)
            itemView.contactsTradeType.text = "$type - $amount"
            itemView.contactsTradeDetails.text = itemView.context.getString(R.string.text_with, person)
            itemView.contactsId.setText(contact.contactId)
            itemView.contactsDate.text = date
        }
        fun bindEmpty(onItemClickListener: OnItemClickListener) {
            itemView.contactsAdvertiseButton.setOnClickListener {
                onItemClickListener.onAdvertiseButtonClicked()
            }
            itemView.contactsSearchButton.setOnClickListener {
                onItemClickListener.onSearchButtonClicked()
            }
        }
    }

    companion object {
        private val TYPE_EMPTY = R.layout.view_empty_contacts
        private val TYPE_ITEM = R.layout.adapter_dashboard_contact_list
    }
}

