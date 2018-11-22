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

class ContactAdapter(private val context: Context) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    protected var items = emptyList<Contact>()

    fun replaceWith(data: List<Contact>) {
        this.items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false)
        return ViewHolder(itemLayoutView)
    }

    override fun getItemViewType(position: Int): Int {
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
        viewHolder.bindItems(items[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(contact: Contact) {
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            var type = ""
            when (tradeType) {
                TradeType.LOCAL_BUY, TradeType.LOCAL_SELL -> type = if (contact.isBuying) itemView.context.getString(R.string.text_buying_locally) else itemView.context.getString(R.string.text_selling_locally)
                TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> type = if (contact.isBuying) itemView.context.getString(R.string.text_buying_online) else itemView.context.getString(R.string.text_selling_online)
                TradeType.NONE -> TODO()
            }
            val amount = contact.amount + " " + contact.currency
            val person = if (contact.isBuying) contact.seller.username else contact.buyer.username
            val date = Dates.parseLocaleDateTime(contact.createdAt)
            itemView.contactsTradeType.text = "$type - $amount"
            itemView.contactsTradeDetails.text = itemView.context.getString(R.string.text_with, person)
            itemView.contactsId.setText(contact.contactId)
            itemView.contactsDate.text = date
        }
    }

    companion object {
        const val TYPE_ITEM = R.layout.adapter_dashboard_contact_list
    }
}

