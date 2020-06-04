/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
                TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> type = if (contact.isBuying) itemView.context.getString(R.string.text_buying_online) else itemView.context.getString(R.string.text_selling_online)
                TradeType.NONE -> TODO()
            }
            val amount = contact.amount + " " + contact.currency
            val person = if (contact.isBuying) contact.seller.username else contact.buyer.username
            val date = Dates.parseLocaleDateTime(contact.createdAt)
            itemView.contactsTradeType.text = "$type - $amount"
            itemView.contactsTradeDetails.text = itemView.context.getString(R.string.text_with, person)
            itemView.adapterContactsId.text = contact.contactId.toString()
            itemView.contactsDate.text = date
        }
    }

    companion object {
        const val TYPE_ITEM = R.layout.adapter_dashboard_contact_list
    }
}

