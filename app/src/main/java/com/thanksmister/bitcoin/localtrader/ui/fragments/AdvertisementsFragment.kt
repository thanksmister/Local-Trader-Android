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

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.services.SyncProvider
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertisementsAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactsAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport
import kotlinx.android.synthetic.main.fragment_advertisements.*
import kotlinx.android.synthetic.main.view_dashboard_items.*

import java.lang.reflect.Field
import java.util.Collections

import javax.inject.Inject

class AdvertisementsFragment : BaseFragment() {

    private var adapter: AdvertisementsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        advertisementsList.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        advertisementsList.layoutManager = linearLayoutManager
    }

    private fun setupList(advertisementItems: List<Advertisement>, methodItems: List<Method>) {
        if (isAdded && adapter != null) {
            adapter!!.replaceWith(advertisementItems, methodItems)
            advertisementsList.adapter = adapter
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_advertisements, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity != null) {
            adapter = AdvertisementsAdapter(activity!!, object : AdvertisementsAdapter.OnItemClickListener {
                override fun onSearchButtonClicked() {
                    showSearchScreen()
                }
                override fun onAdvertiseButtonClicked() {
                    createAdvertisementScreen()
                }
            })
            advertisementsList.adapter = adapter
        }
        ItemClickSupport.addTo(advertisementsList).setOnItemClickListener { _, position, v ->
            val advertisement = adapter!!.getItemAt(position)
            if (advertisement != null) {
                showAdvertisement(advertisement)
            }
        }
    }
    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDetach() {
        super.onDetach()
    }

    // TODO convert id to int
    private fun showAdvertisement(advertisement: Advertisement) {
        if(activity != null) {
            val intent = AdvertisementActivity.createStartIntent(activity!!, advertisement.adId)
            startActivityForResult(intent, AdvertisementActivity.REQUEST_CODE)
        }
    }

    private fun createAdvertisementScreen() {
        if(activity != null && isAdded) {
            dialogUtils.showAlertDialog(activity!!, getString(R.string.dialog_edit_advertisements),
                    DialogInterface.OnClickListener { _, _ ->
                        try {
                            startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                        } catch (ex: ActivityNotFoundException) {
                            Toast.makeText(activity!!, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                        }
                    }, DialogInterface.OnClickListener { _, _ ->
                        // na-da
                    })
        }
    }

    private fun showSearchScreen() {
        if (isAdded && activity != null) {
            // TODO launch search
            //(activity as MainActivity).navigateSearchView()
        }
    }

    companion object {
        fun newInstance(): AdvertisementsFragment {
            return AdvertisementsFragment()
        }
    }

    /*@NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == ADVERTISEMENT_LOADER_ID && getActivity() != null) {
            return new CursorLoader(getActivity(), SyncProvider.ADVERTISEMENT_TABLE_URI, null, null, null, null);
        } else if (id == METHOD_LOADER_ID && getActivity() != null) {
            return new CursorLoader(getActivity(), SyncProvider.METHOD_TABLE_URI, null, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ADVERTISEMENT_LOADER_ID:
                advertisements = AdvertisementItem.getModelList(cursor);
                if (methods != null && advertisements != null) {
                    setupList(advertisements, methods);
                }
                break;
            case METHOD_LOADER_ID:
                methods = MethodItem.getModelList(cursor);
                if (methods != null && advertisements != null) {
                    setupList(advertisements, methods);
                }
                break;
            default:
                throw new Error("Incorrect loader Id");
        }
    }*/


}