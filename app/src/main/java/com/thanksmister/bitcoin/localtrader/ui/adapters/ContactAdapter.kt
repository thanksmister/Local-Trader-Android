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
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.utils.Dates

class ContactAdapter(private val context: Context) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    protected var items: List<Contact>? = null

    fun replaceWith(data: List<Contact>) {
        this.items = data
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactAdapter.ViewHolder {
        // create a new view
        val itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false)
        return ItemViewHolder(itemLayoutView)
    }

    override fun getItemViewType(position: Int): Int {
        return TYPE_ITEM
    }

    override fun getItemCount(): Int {
        if (items == null)
            return 0

        return if (items!!.size > 0) items!!.size else 0
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (viewHolder is ItemViewHolder) {
            val contact = items!![position]
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            var type = ""
            when (tradeType) {
                TradeType.LOCAL_BUY, TradeType.LOCAL_SELL -> type = if (contact.isBuying) context.getString(R.string.text_buying_locally) else context.getString(R.string.text_selling_locally)
                TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> type = if (contact.isBuying) context.getString(R.string.text_buying_online) else context.getString(R.string.text_selling_online)
            }

            val amount = contact.amount + " " + contact.currency
            val person = if (contact.isBuying) contact.seller.username else contact.buyer.username
            val date = Dates.parseLocaleDateTime(contact.createdAt)

            viewHolder.tradeType!!.text = "$type - $amount"
            viewHolder.tradeDetails!!.text = context.getString(R.string.text_with, person)
            viewHolder.contactId!!.setText(contact.contactId)
            viewHolder.contactDate!!.text = date
        }
    }

    fun getItemAt(position: Int): Contact? {
        return if (items != null && !items!!.isEmpty() && items!!.size > position) {
            items!![position]
        } else null
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ItemViewHolder internal constructor(itemView: View) : ViewHolder(itemView) {
        var tradeType: TextView? = null
        var icon: ImageView? = null
        var tradeDetails: TextView? = null
        var contactId: TextView? = null
        var contactDate: TextView? = null
    }

    companion object {
        private val TYPE_ITEM = R.layout.adapter_dashboard_contact_list
    }
}

