/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;

import java.util.List;

public class CurrencyAdapter extends ArrayAdapter {
    private Context context;
    private List<Currency> items;

    public CurrencyAdapter(Context _context, int _resource, List<Currency> _items) {
        super(_context, _resource, _items);
        context = _context;
        items = _items;
    }

    @Override
    public Currency getItem(int position) {
        return items.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.spinner_transactions_header_layout, null);
        }

        TextView spinnerTarget = (TextView) convertView.findViewById(R.id.spinnerTarget);
        spinnerTarget.setText(items.get(position).getCode());

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.spinner_layout, null);
        }

        TextView spinnerTarget = (TextView) convertView.findViewById(R.id.spinnerTarget);
        spinnerTarget.setText(items.get(position).getCode());

        return convertView;
    }
}
