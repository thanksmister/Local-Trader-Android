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
import android.widget.ArrayAdapter
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Method

class MethodAdapter(context: Context, _resource: Int, _items: List<Method>) : ArrayAdapter<Method>(context, _resource, _items) {
    private var items = emptyList<Method>()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    init {
        items = _items
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = if (convertView != null) {
            convertView
        } else {
            inflater.inflate(R.layout.spinner_transactions_header_layout, parent, false)
        }
        if (position < count) {
            val spinnerTarget = view.findViewById<View>(R.id.spinnerTarget) as TextView
            spinnerTarget.text = items[position].name
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.spinner_layout, parent, false)
        if (position < count) {
            val spinnerTarget = view.findViewById<View>(R.id.spinnerTarget) as TextView
            spinnerTarget.text = items[position].name
        }
        return view
    }
}