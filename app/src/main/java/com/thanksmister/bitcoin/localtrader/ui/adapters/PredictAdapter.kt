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
import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R

class PredictAdapter(context: Context, data: List<Address>) : ArrayAdapter<Address>(context, 0, data), Filterable {
    private var data = emptyList<Address>()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    init {
        this.data = data
    }

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Address? {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun replaceWith(data: List<Address>) {
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
            view = inflater.inflate(R.layout.adapter_address, parent, false)
            holder = ViewHolder(view!!)
            view.tag = holder
        }
        val address = getItem(position)
        val addressLine = if (address!!.maxAddressLineIndex > 0) address.getAddressLine(0) else null
        var output = ""
        if (addressLine != null) output += addressLine
        if (address.locality != null) output += ", " + address.locality
        if (address.countryName != null) output += ", " + address.countryName
        holder.addressText!!.text = output
        return view
    }

    internal class ViewHolder(view: View) {
        var addressText: TextView? = null

        init {
            addressText = view.findViewById<TextView>(R.id.addressText)
        }
    }
}