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

package com.thanksmister.bitcoin.localtrader.ui.adapters;

import android.content.Context;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;

import java.util.Collections;
import java.util.List;

public class PredictAdapter extends ArrayAdapter<Address> implements Filterable {
    private List<Address> data = Collections.emptyList();
    private final LayoutInflater inflater;

    public PredictAdapter(Context context, List<Address> data) {
        super(context, 0, data);
        this.inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Address getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void replaceWith(List<Address> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_address, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        Address address = getItem(position);
        String addressLine = address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : null;
        String output = "";
        if (addressLine != null) output += addressLine;
        if (address.getLocality() != null) output += ", " + address.getLocality();
        if (address.getCountryName() != null) output += ", " + address.getCountryName();

        holder.addressText.setText(output);

        return view;
    }

    static class ViewHolder {
        TextView addressText;
        public ViewHolder(View view) {

        }
    }
}