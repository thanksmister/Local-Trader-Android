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