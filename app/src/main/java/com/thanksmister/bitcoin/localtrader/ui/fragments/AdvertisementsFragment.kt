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

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.*

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem
import com.thanksmister.bitcoin.localtrader.persistence.Method
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertisementsAdapter
import com.thanksmister.bitcoin.localtrader.ui.controls.ItemClickSupport
import kotlinx.android.synthetic.main.fragment_advertisements.*

import java.lang.reflect.Field
import java.util.Collections

import javax.inject.Inject

class AdvertisementsFragment : BaseFragment() {

    private var itemAdapter: AdvertisementsAdapter? = null
    private val advertisements = emptyList<AdvertisementItem>()
    private val methods = emptyList<Method>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        advertisementsRecyclerView.setHasFixedSize(true)

        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        advertisementsRecyclerView.layoutManager = linearLayoutManager

        ItemClickSupport.addTo(advertisementsRecyclerView).setOnItemClickListener { recyclerView, position, v ->
            val advertisement = itemAdapter!!.getItemAt(position)
            if (advertisement != null && !TextUtils.isEmpty(advertisement.ad_id())) {
                showAdvertisement(itemAdapter!!.getItemAt(position)!!)
            }
        }

        itemAdapter = AdvertisementsAdapter(activity, object : AdvertisementsAdapter.OnItemClickListener {
            override fun onSearchButtonClicked() {
                showSearchScreen()
            }
            override fun onAdvertiseButtonClicked() {
                createAdvertisementScreen()
            }
        })

        advertisementsRecyclerView.adapter = itemAdapter
        setupList(ArrayList<AdvertisementItem>(), ArrayList<Method>())
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.advertisements, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_clear_notifications -> {
                //viewModel.markNotificationsRead()
                return true
            }
        }
        return false
    }

    private fun setupList(advertisementItems: List<AdvertisementItem>, methodItems: List<Method>) {
        if (isAdded) {
            itemAdapter!!.replaceWith(advertisementItems, methodItems)
            advertisementsRecyclerView.adapter = itemAdapter
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_advertisements, container, false)
    }

    override fun onResume() {
        super.onResume()
        //advertisementObserver = new AdvertisementObserver(new Handler());
        if (isAdded && activity != null) {
            //getActivity().getContentResolver().registerContentObserver(SyncProvider.ADVERTISEMENT_TABLE_URI, true, advertisementObserver);
        }

    }

    override fun onPause() {
        super.onPause()
        if (activity != null) {
            //getActivity().getContentResolver().unregisterContentObserver(advertisementObserver);
        }
    }

    override fun onDetach() {

        super.onDetach()

        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            val childFragmentManager = Fragment::class.java.getDeclaredField("mChildFragmentManager")
            childFragmentManager.isAccessible = true
            childFragmentManager.set(this, null)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    private fun showAdvertisement(advertisement: AdvertisementItem) {
        val intent = AdvertisementActivity.createStartIntent(activity, advertisement.ad_id())
        startActivityForResult(intent, AdvertisementActivity.REQUEST_CODE)
    }

    private fun createAdvertisementScreen() {
        /* showAlertDialog(new AlertDialogEvent(getString(R.string.view_title_advertisements), getString(R.string.dialog_edit_advertisements)), new Action0() {
            @Override
            public void call() {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Action0() {
            @Override
            public void call() {
                // na-da
            }
        });*/
    }

    protected fun showSearchScreen() {
        if (isAdded && activity != null) {
            //(activity as MainActivity).navigateSearchView()
        }
    }

    companion object {

        private val ADVERTISEMENT_LOADER_ID = 1
        private val METHOD_LOADER_ID = 2

        fun newInstance(): AdvertisementsFragment {
            return AdvertisementsFragment()
        }
    }

    /* @NonNull
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }*/

    /* private class AdvertisementObserver extends ContentObserver {
        AdvertisementObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (selfChange && getActivity() != null) {
                getActivity().getSupportLoaderManager().restartLoader(ADVERTISEMENT_LOADER_ID, null, AdvertisementsFragment.this);
            }
        }
    }*/
}