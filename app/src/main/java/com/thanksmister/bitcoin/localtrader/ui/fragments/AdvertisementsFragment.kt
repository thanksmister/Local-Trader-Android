/*
 * Copyright (c) 2019 ThanksMister LLC
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
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.applySchedulersIo
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import kotlinx.android.synthetic.main.fragment_advertisements.*
import timber.log.Timber
import javax.inject.Inject

class AdvertisementsFragment : BaseFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: AdvertisementsViewModel

    private val advertisementsAdapter: AdvertisementsAdapter by lazy {
        AdvertisementsAdapter(object : AdvertisementsAdapter.OnItemClickListener {
            override fun onSearchButtonClicked() {
                showSearchScreen()
            }
            override fun onAdvertiseButtonClicked() {
                createAdvertisementScreen()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        advertisementsList.apply {
            setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(activity)
            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
            layoutManager = linearLayoutManager
            adapter = advertisementsAdapter
        }

        ItemClickSupport.addTo(advertisementsList).setOnItemClickListener { _, position, v ->
            val advertisement = advertisementsAdapter.getItemAt(position)
            advertisement?.let {
                showAdvertisement(advertisement)
            }
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AdvertisementsViewModel::class.java)
        observeViewModel(viewModel)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_advertisements, container, false)
    }

    private fun observeViewModel(viewModel: AdvertisementsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.showAlertDialog(requireActivity(), message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.toast(it)
            }
        })
        disposable += viewModel.getAdvertisementsData()
                .applySchedulersIo()
                .subscribe( { data ->
                    if(data != null) {
                      setupList(data.advertisements, data.methods)
                    }
                }, { error ->
                    Timber.e(error.message)
                })
    }

    private fun setupList(advertisementItems: List<Advertisement>, methods: List<Method>) {
        advertisementsAdapter.replaceWith(advertisementItems, methods)
    }

    private fun showAdvertisement(advertisement: Advertisement) {
        val intent = AdvertisementActivity.createStartIntent(requireActivity(), advertisement.adId)
        startActivityForResult(intent, AdvertisementActivity.REQUEST_CODE)
    }

    private fun createAdvertisementScreen() {
        dialogUtils.showAlertDialog(requireActivity(), getString(R.string.dialog_edit_advertisements),
                DialogInterface.OnClickListener { _, _ ->
                    try {
                        startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)))
                    } catch (ex: Exception) {
                        Toast.makeText(requireActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
                    }
                }, DialogInterface.OnClickListener { _, _ ->
            // na-da
        })
    }

    private fun showSearchScreen() {
        val intent = SearchActivity.createStartIntent(requireActivity())
        startActivity(intent)
    }

    companion object {
        fun newInstance(): AdvertisementsFragment {
            return AdvertisementsFragment()
        }
    }
}