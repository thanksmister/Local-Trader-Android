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

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency
import com.thanksmister.bitcoin.localtrader.persistence.Currency

class CurrencyAdapter(context: Context, resource: Int, private val items: List<Currency>) : ArrayAdapter<Currency>(context, resource, items) {

    override fun getItem(position: Int): Currency? {
        return items[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var returnView = convertView
        if (returnView == null) {
            val mInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            returnView = mInflater.inflate(R.layout.spinner_transactions_header_layout, null)
        }
        val spinnerTarget = returnView?.findViewById<View>(R.id.spinnerTarget) as TextView
        spinnerTarget.text = items[position].name
        return returnView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var returnView = convertView
        if (returnView == null) {
            val mInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            returnView = mInflater.inflate(R.layout.spinner_layout, null)
        }
        val spinnerTarget = returnView?.findViewById<View>(R.id.spinnerTarget) as TextView
        spinnerTarget.text = items[position].name
        return returnView
    }
}
