/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.adapters

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.utils.Dates
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import kotlinx.android.synthetic.main.adapter_dashboard_advertisement_list.view.*
import kotlinx.android.synthetic.main.view_empty_advertisement.view.*

class AdvertisementsAdapter(private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<AdvertisementsAdapter.ViewHolder>() {

    private var items = emptyList<Advertisement>()
    private var methods = emptyList<Method>()

    interface OnItemClickListener {
        fun onSearchButtonClicked()
        fun onAdvertiseButtonClicked()
    }

    fun replaceWith(data: List<Advertisement>, methods: List<Method>) {
        this.items = data
        this.methods = methods
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if(items.isEmpty()) {
            viewHolder.bindEmpty(onItemClickListener)
        } else {
            viewHolder.bindItems(items[position], methods)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(advertisement: Advertisement, methods: List<Method>) {
            val tradeType = TradeType.valueOf(advertisement.tradeType)
            val type = when (tradeType) {
                TradeType.ONLINE_BUY -> itemView.context.getString(R.string.text_advertisement_item_online_buy)
                TradeType.ONLINE_SELL -> itemView.context.getString(R.string.text_advertisement_item_online_sale)
                TradeType.NONE -> "Local Advertisement"
            }
            val price = advertisement.tempPrice + " " + advertisement.currency
            //val location = advertisement.location
            val adLocation = if (TradeUtils.isOnlineTrade(advertisement)) advertisement.location else advertisement.city
            val paymentMethod = TradeUtils.getPaymentMethod(itemView.context, advertisement, methods)
            if (TextUtils.isEmpty(paymentMethod)) {
                itemView.advertisementsDetails.text = itemView.context.getString(R.string.text_in_caps, adLocation)
            } else {
                itemView.advertisementsDetails.text = itemView.context.getString(R.string.text_with_int, paymentMethod, adLocation)
            }
            itemView.advertisementsType.text = "$type $price"
            if (advertisement.visible) {
                itemView.advertisementsIcon.setImageResource(R.drawable.ic_action_visibility_dark)
            } else {
                itemView.advertisementsIcon.setImageResource(R.drawable.ic_action_visibility_off_dark)
            }

            if(advertisement.createdAt != null) {
                val date = Dates.parseLocaleDate(advertisement.createdAt)
                itemView.advertisementsDate.text = date
            }
            itemView.advertisementsId.text = advertisement.adId.toString()
        }

        fun bindEmpty(onItemClickListener: OnItemClickListener) {
            itemView.advertisementsAdvertiseButton.setOnClickListener {
                onItemClickListener.onAdvertiseButtonClicked()
            }
            itemView.advertisementsSearchButton.setOnClickListener {
                onItemClickListener.onSearchButtonClicked()
            }
        }
    }

    fun getItemAt(position: Int): Advertisement? {
        return if (!items.isEmpty() && items.size > position) {
            items[position]
        } else null
    }

    companion object {
        const val TYPE_EMPTY = R.layout.view_empty_advertisement
        const val TYPE_ITEM = R.layout.adapter_dashboard_advertisement_list
    }
}