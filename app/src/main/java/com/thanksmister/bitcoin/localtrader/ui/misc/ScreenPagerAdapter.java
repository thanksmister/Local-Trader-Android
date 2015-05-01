/*
 * Copyright (c) 2015 ThanksMister LLC
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
 */

package com.thanksmister.bitcoin.localtrader.ui.misc;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ScreenPagerAdapter extends PagerAdapter
{
    private int numberOfPages = 6;

    private Context context;
    private final LayoutInflater inflater;

    int[] res = {
            R.drawable.logo_screen,
            R.drawable.search_screen,
            R.drawable.wallet_screen,
            R.drawable.trade_screen,
            R.drawable.advertise_screen,
            R.drawable.lbc_screen};

    int[] backgroundColor = {
            R.color.gray_color,
            R.color.gray_color,
            R.color.gray_color,
            R.color.gray_color,
            R.color.gray_color,
            R.color.gray_color};

  
    public ScreenPagerAdapter(Context context)
    {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        return numberOfPages;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        ViewHolder holder;
        View view = inflater.inflate(R.layout.adapter_pager_layout, container, false);
        holder = new ViewHolder(view);

        String[] titleStrings = context.getResources().getStringArray(R.array.login_registration_titles);
        String[] textStrings = context.getResources().getStringArray(R.array.login_registration_screens);

        //holder.screenText.setTextColor(context.getResources().getColor(R.color.white));
        holder.screenText.setText(textStrings[position]);
        
        holder.headerText.setText(titleStrings[position]);
        
        holder.screenImage.setImageResource(res[position]);
        holder.background.setBackgroundColor(context.getResources().getColor(backgroundColor[position]));

        container.addView(view);

        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        container.removeView((LinearLayout) object);
    }

    static class ViewHolder
    {
        @InjectView(R.id.screenBackground) 
        View background;   
        
        @InjectView(R.id.screenImage)
        ImageView screenImage;

        @InjectView(R.id.headerText)
        TextView headerText;
        
        @InjectView(R.id.screenText) 
        TextView screenText;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}


