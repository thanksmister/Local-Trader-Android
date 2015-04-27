/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.drawer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DrawerAdapter extends ArrayAdapter<String>
{
    protected String[] items;
    protected Context context;
    protected final LayoutInflater inflater;
    protected final int textViewResourceId;
    protected final int viewLayout;

    /*
    <array name="drawer_items">
        <item>@string/view_title_dashboard</item>
        <item>@string/view_title_buy_sell</item>
        <item>@string/view_title_wallet</item>
        <item>@string/view_title_request</item>
        <item>@string/view_title_settings</item>
        <item>@string/view_title_about</item>
    </array>
     */
    
   
    int[] res = {
            R.drawable.dashboard_small,
            R.drawable.search_small,
            R.drawable.send_small,
            R.drawable.wallet_small,
            R.drawable.info_small};
    
    public DrawerAdapter(Context context, int viewLayout, int textViewResourceId, String[] items)
    {
        super(context, viewLayout, textViewResourceId, items);
        this.viewLayout = viewLayout;
        this.textViewResourceId = textViewResourceId;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @Override
    public boolean isEnabled(int position)
    {
        return true;
    }

    @Override
    public int getCount()
    {
        return items.length;
    }

    @Override
    public String getItem(int position)
    {
        return items[position];
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public void replaceWith(String[] data)
    {
        items = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent)
    {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_drawer_list, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        holder.drawerText.setText(getItem(position));
        holder.drawerIcon.setImageResource(res[position]);
        
        return view;
    }

    static class ViewHolder
    {   
        @InjectView(R.id.drawerText) TextView drawerText;
        @InjectView(R.id.drawerIcon) ImageView drawerIcon;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}


