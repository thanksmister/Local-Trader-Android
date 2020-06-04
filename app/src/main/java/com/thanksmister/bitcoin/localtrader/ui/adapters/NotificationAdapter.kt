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

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.utils.Dates
import kotlinx.android.synthetic.main.adapter_dashboard_notification_list.view.*
import kotlinx.android.synthetic.main.view_empty_notifications.view.*

class NotificationAdapter(private val onItemClickListener: OnItemClickListener): RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var items = emptyList<Notification>()

    interface OnItemClickListener {
        fun onSearchButtonClicked()
        fun onAdvertiseButtonClicked()
    }

    fun replaceWith(data: List<Notification>) {
        this.items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationAdapter.ViewHolder {
        val itemLayoutView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
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

    fun getItemAt(position: Int): Notification? {
        return if (items.isNotEmpty() && items.size > position) {
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
        fun bindItems(item: Notification) {
            itemView.notificationsMessageBody.text = item.message!!.trim { it <= ' ' }
            itemView.notificationsContactId.visibility = View.VISIBLE
            if (item.contactId != null) {
                itemView.notificationsContactId.text = itemView.context.getString(R.string.text_item_contact_num, item.contactId.toString())
            } else if (item.advertisementId != null) {
                itemView.notificationsContactId.text = itemView.context.getString(R.string.text_item_advertisement_number, item.advertisementId.toString())
            } else {
                itemView.notificationsContactId.visibility = View.GONE
            }
            val date = Dates.parseLocalDateISO(item.createdAt)
            itemView.notificationsCreatedAt.text = DateUtils.getRelativeTimeSpanString(date.time)
            if (item.read) {
                itemView.notificationsItemIcon.setImageResource(R.drawable.ic_action_notification)
            } else {
                itemView.notificationsItemIcon.setImageResource(R.drawable.ic_action_notification_new)
            }
        }
        fun bindEmpty(onItemClickListener: OnItemClickListener) {
            itemView.notificationsAdvertiseButton.setOnClickListener {
                onItemClickListener.onAdvertiseButtonClicked()
            }
            itemView.notificationsSearchButton.setOnClickListener {
                onItemClickListener.onSearchButtonClicked()
            }
        }
    }

    companion object {
        const val TYPE_EMPTY = R.layout.view_empty_notifications
        const val TYPE_ITEM = R.layout.adapter_dashboard_notification_list
    }
}