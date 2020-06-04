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
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Message
import com.thanksmister.bitcoin.localtrader.utils.Dates

class MessageAdapter(context: Context) : BaseAdapter() {

    private var data = emptyList<Message>()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Message {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun clear() {
        this.data = emptyList<Message>()
        notifyDataSetChanged()
    }

    fun replaceWith(data: List<Message>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?
        val holder: ViewHolder
        if (convertView != null) {
            view = convertView
            holder = view.tag as ViewHolder
        } else {
            view = inflater.inflate(R.layout.adapter_message_list, parent, false)
            holder = ViewHolder(view)
            view!!.tag = holder
        }
        val message = getItem(position)
        holder.senderName!!.text = message.sender.username
        val date = Dates.parseLocalDateISO(message.createdAt)
        holder.createdAt!!.text = DateUtils.getRelativeTimeSpanString(date.time)
        holder.messageBody!!.text = message.message
        if (!TextUtils.isEmpty(message.attachmentName)) {
            holder.attachmentLayout!!.visibility = View.VISIBLE
            holder.attachmentName!!.text = message.attachmentName
        } else {
            holder.attachmentLayout!!.visibility = View.GONE
            holder.attachmentName!!.text = ""
        }
        return view
    }

    private class ViewHolder(row: View) {
        var senderName: TextView? = null
        var createdAt: TextView? = null
        var messageBody: TextView? = null
        var attachmentLayout: View? = null
        var attachmentName: TextView? = null
        init {
            senderName = row.findViewById(R.id.messageSenderName)
            messageBody = row.findViewById(R.id.messageBody)
            attachmentName = row.findViewById(R.id.messageAttachmentName)
            attachmentLayout = row.findViewById(R.id.messageAttachmentLayout)
            createdAt = row.findViewById(R.id.messageCreatedAt)
        }
    }
}