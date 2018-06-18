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
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.persistence.Notification
import com.thanksmister.bitcoin.localtrader.utils.DateUtils
import kotlinx.android.synthetic.main.adapter_dashboard_notification_list.view.*
import timber.log.Timber

import java.util.Date


class NotificationAdapter(private val items: List<Notification>?, private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onSearchButtonClicked()
        fun onAdvertiseButtonClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        if (viewType == TYPE_ITEM) {
            return ItemViewHolder(v)
        } else if (viewType == TYPE_EMPTY) {
            return EmptyViewHolder(v)
        } else {
            return ProgressViewHolder(v)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (items == null) {
            return TYPE_PROGRESS
        } else if (items.isEmpty()) {
            return TYPE_EMPTY
        }
        return TYPE_ITEM
    }

    override fun getItemCount(): Int {
        if (items == null)
            return -1

        return if (items.isNotEmpty()) items.size else 1
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (viewHolder is ItemViewHolder) {
            viewHolder.bindItems(items!![position])
        }
    }

    fun getItemAt(position: Int): Notification? {
        return if (items != null && !items.isEmpty() && items.size > position) {
            items[position]
        } else null
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ItemViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bindItems(item: Notification) {
            itemView.messageBody!!.text = item.msg!!.trim { it <= ' ' }
            itemView.contactId!!.visibility = View.VISIBLE
            if (itemView.contactId != null) {
                itemView.contactId!!.text = itemView.context.getString(R.string.list_contact_id, item.contactId)
            } else if (item.advertisementId != null) {
                itemView.contactId!!.text = itemView.context.getString(R.string.list_advertisement_id, item.advertisementId)
            } else {
                itemView.contactId!!.visibility = View.GONE
            }
            val date = DateUtils.parseLocalDateISO(item.createdAt)
            itemView.createdAt!!.text = android.text.format.DateUtils.getRelativeTimeSpanString(date.time)
            if (item.read) {
                itemView.itemIcon!!.setImageResource(R.drawable.ic_action_notification)
            } else {
                itemView.itemIcon!!.setImageResource(R.drawable.ic_action_notification_new)
            }
        }
    }

    inner class EmptyViewHolder(itemView: View) : ViewHolder(itemView) {
        fun advertiseButtonClicked() {
            onItemClickListener.onAdvertiseButtonClicked()
        }
        fun searchButtonClicked() {
            onItemClickListener.onSearchButtonClicked()
        }
    }

    inner class ProgressViewHolder(itemView: View) : ViewHolder(itemView)

    companion object {
        const val TYPE_EMPTY = R.layout.view_empty_dashboard
        const val TYPE_PROGRESS = R.layout.view_progress_dashboard
        const val TYPE_ITEM = R.layout.adapter_dashboard_notification_list
    }
}

