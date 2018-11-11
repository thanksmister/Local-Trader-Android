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
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.utils.Dates

import java.util.Date


class NotificationAdapter(private val context: Context, private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var items = emptyList<Notification>()

    interface OnItemClickListener {
        fun onSearchButtonClicked()
        fun onAdvertiseButtonClicked()
    }

    fun replaceWith(data: List<Notification>) {
        this.items = data
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationAdapter.ViewHolder {
        val itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false)
        if (viewType == TYPE_ITEM) {
            return ItemViewHolder(itemLayoutView)
        }
        return EmptyViewHolder(itemLayoutView)
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

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (viewHolder is ItemViewHolder) {
            val item = items[position]
            viewHolder.messageBody!!.text = item.message!!.trim { it <= ' ' }
            viewHolder.contactId!!.visibility = View.VISIBLE
            if (item.contactId != null) {
                viewHolder.contactId!!.text = context.getString(R.string.text_item_contact_num, item.contactId.toString())
            } else if (item.advertisementId != null) {
                viewHolder.contactId!!.text = context.getString(R.string.text_item_advertisement_number, item.advertisementId.toString())
            } else {
                viewHolder.contactId!!.visibility = View.GONE
            }
            val date = Dates.parseLocalDateISO(item.createdAt)
            viewHolder.createdAt!!.text = DateUtils.getRelativeTimeSpanString(date.time)
            if (item.read) {
                viewHolder.icon!!.setImageResource(R.drawable.ic_action_notification)
            } else {
                viewHolder.icon!!.setImageResource(R.drawable.ic_action_notification_new)
            }
        }
    }

    fun getItemAt(position: Int): Notification? {
        return if (!items.isEmpty() && items.size > position) {
            items[position]
        } else null
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ItemViewHolder internal constructor(itemView: View) : ViewHolder(itemView) {
        var messageBody: TextView? = null
        var icon: ImageView? = null
        var contactId: TextView? = null
        var createdAt: TextView? = null
    }

    // TODO kotlin replacement
    inner class EmptyViewHolder(itemView: View) : ViewHolder(itemView) {
        fun advertiseButtonClicked() {
            onItemClickListener.onAdvertiseButtonClicked()
        }
        fun searchButtonClicked() {
            onItemClickListener.onSearchButtonClicked()
        }
    }

    companion object {
        private val TYPE_EMPTY = R.layout.view_empty_dashboard
        private val TYPE_ITEM = R.layout.adapter_dashboard_notification_list
    }
}