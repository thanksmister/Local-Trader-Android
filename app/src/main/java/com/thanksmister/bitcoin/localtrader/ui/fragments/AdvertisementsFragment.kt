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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertisementsAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.AdvertisementsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_advertisements.*
import timber.log.Timber
import javax.inject.Inject

class AdvertisementsFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: AdvertisementsViewModel

    private val disposable = CompositeDisposable()
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

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AdvertisementsViewModel::class.java)
        observeViewModel(viewModel)
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

    override fun onDestroy() {
        super.onDestroy()
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
    }

    private fun observeViewModel(viewModel: AdvertisementsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.showAlertDialog(activity!!, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.toast(message)
            }
        })
        disposable.add(viewModel.getAdvertisementsData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null) {
                      setupList(data.advertisements, data.methods)
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    private fun setupList(advertisementItems: List<Advertisement>, methods: List<Method>) {
        if (isAdded && adapter != null) {
            adapter!!.replaceWith(advertisementItems, methods)
            advertisementsList.adapter = adapter
        }
    }

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
                            startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)))
                        } catch (ex: ActivityNotFoundException) {
                            Toast.makeText(activity!!, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
                        }
                    }, DialogInterface.OnClickListener { _, _ ->
                        // na-da
                    })
        }
    }

    private fun showSearchScreen() {
        if (isAdded && activity != null) {
            val intent = SearchActivity.createStartIntent(activity!!)
            startActivity(intent)
        }
    }

    companion object {
        fun newInstance(): AdvertisementsFragment {
            return AdvertisementsFragment()
        }
    }
}