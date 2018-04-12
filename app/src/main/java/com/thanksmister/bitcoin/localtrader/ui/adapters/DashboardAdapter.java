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
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class DashboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    class ContactViewHolder extends RecyclerView.ViewHolder {
        public ContactViewHolder(View itemView) {
            super(itemView);
        }
    }

    class AdvertisementViewHolder extends RecyclerView.ViewHolder {
        public AdvertisementViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        return position % 2 * 2;
    }

    @Override
    public int getItemCount() {
        return 0;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder holder;
        View v;
        Context context = viewGroup.getContext();
/*

        if (viewType == FIRST_TYPE) {
            v = LayoutInflater.from(context).inflate(R.layout.adapter_contact_list, viewGroup, false);
            holder = new ContactViewHolder(v); //Of type GeneralViewHolder
        } else {
            v = LayoutInflater.from(context).inflate(R.layout.adapter_advertisement_list, viewGroup, false);
            holder = new AdvertisementViewHolder(v);
        }
*/

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        switch (viewHolder.getItemViewType()) {

            /*case TYPE_IMAGE:
                ImageViewHolder imageViewHolder = (ImageViewHolder) viewHolder;
                imageViewHolder.mImage.setImageResource(...);
                break;

            case TYPE_GROUP:
                GroupViewHolder groupViewHolder = (GroupViewHolder) viewHolder;
                groupViewHolder.mContent.setText(...)
                groupViewHolder.mTitle.setText (...);
                break;*/
        }
    }
}
