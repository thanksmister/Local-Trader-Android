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
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Doubles
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils

class AdvertiseAdapter(private val context: Context) : BaseAdapter() {

    private var data = emptyList<Advertisement>()
    private var methods = emptyList<Method>()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Advertisement {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun clear() {
        this.data = emptyList<Advertisement>()
        notifyDataSetChanged()
    }

    fun replaceWith(data: List<Advertisement>, methods: List<Method>) {
        this.methods = methods
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
            view = inflater.inflate(R.layout.adapter_advertise_layout, parent, false)
            holder = ViewHolder(view!!)
            view.tag = holder
        }

        val advertisement = getItem(position)

        if (TradeUtils.isOnlineTrade(advertisement)) { // online trade
            val paymentMethod = TradeUtils.getPaymentMethod(context, advertisement, methods)
            holder.tradLocation!!.text = paymentMethod
        } else {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val units_prefs = preferences.getString(context.getString(R.string.pref_key_distance), "0")
            val unit = if (units_prefs == "0") context.getString(R.string.list_unit_km) else context.getString(R.string.list_unit_mi)
            val distance = if (units_prefs == "0") Conversions.formatDecimalToString(Doubles.convertToDouble(advertisement.distance), Conversions.TWO_DECIMALS) else Conversions.kilometersToMiles(Doubles.convertToDouble(advertisement.distance))
            holder.tradLocation!!.text = distance + " " + unit + " → " + advertisement.location
        }

        if (advertisement.isATM) {
            holder.tradePrice!!.text = context.getText(R.string.text_atm)
        } else {
            holder.tradePrice!!.text = context.getString(R.string.trade_price, advertisement.tempPrice, advertisement.currency)
        }

        holder.traderName!!.text = advertisement.profile.username
        holder.tradeFeedback!!.text = advertisement.profile.feedbackScore.toString()
        holder.tradeCount!!.text = advertisement.profile.tradeCount

        /*if(editAdvertisement.isATM()) {
            holder.tradeLimit.setText("");
        } else if(editAdvertisement.min_amount == null) {
            holder.tradeLimit.setText("");
        } else if(editAdvertisement.max_amount == null) {
            holder.tradeLimit.setText(context.getString(R.string.trade_limit_min, editAdvertisement.min_amount, editAdvertisement.currency));
        } else { // no maximum set
            holder.tradeLimit.setText(context.getString(R.string.trade_limit_short, editAdvertisement.min_amount, editAdvertisement.max_amount));
        }*/

        if (advertisement.isATM) {
            holder.tradeLimit!!.text = ""
        } else {
            if (advertisement.maxAmount != null && advertisement.minAmount != null) {
                holder.tradeLimit!!.text = context.getString(R.string.trade_limit, advertisement.minAmount, advertisement.maxAmount, advertisement.currency)
            }
            if (advertisement.maxAmount == null && advertisement.minAmount != null) {
                holder.tradeLimit!!.text = context.getString(R.string.trade_limit_min, advertisement.minAmount, advertisement.currency)
            }
            if (advertisement.maxAmountAvailable != null && advertisement.minAmount != null) { // no maximum set
                holder.tradeLimit!!.text = context.getString(R.string.trade_limit, advertisement.minAmount, advertisement.maxAmountAvailable, advertisement.currency)
            } else if (advertisement.maxAmountAvailable != null) {
                holder.tradeLimit!!.text = context.getString(R.string.trade_limit_max, advertisement.maxAmountAvailable, advertisement.currency)
            }
        }

        holder.lastSeenIcon!!.setBackgroundResource(TradeUtils.determineLastSeenIcon(advertisement.profile.lastOnline!!))

        return view
    }

    internal class ViewHolder(view: View) {
        var tradePrice: TextView? = null
        var traderName: TextView? = null
        var tradeLimit: TextView? = null
        var tradeFeedback: TextView? = null
        var tradeCount: TextView? = null
        var tradLocation: TextView? = null
        var lastSeenIcon: View? = null
        init {
            tradePrice = view.findViewById<TextView>(R.id.advertiserTradePrice)
            traderName =view.findViewById<TextView>(R.id.advertiserTraderName)
            tradeLimit =view.findViewById<TextView>(R.id.advertiserTradeLimit)
            tradeFeedback =view.findViewById<TextView>(R.id.advertiserTradeFeedback)
            tradeCount =view.findViewById<TextView>(R.id.advertiserTradeCount)
            tradLocation =view.findViewById<TextView>(R.id.advertiserTradLocation)
            lastSeenIcon = view.findViewById<View>(R.id.advertiserLastSeenIcon)
        }
    }
}