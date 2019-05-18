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

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertiseAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SearchViewModel
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_empty.*
import kotlinx.android.synthetic.main.view_search_results.*
import timber.log.Timber
import javax.inject.Inject

class SearchResultsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: SearchViewModel

    private var adapter: AdvertiseAdapter? = null
    private var tradeType = TradeType.NONE
    private var methods: List<Method> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_search_results)

        tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences))

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = getHeader(tradeType)
        }

        searchResultsList.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val advertisement = adapterView.adapter.getItem(i) as Advertisement
            showAdvertiser(advertisement)
        }

        adapter = AdvertiseAdapter(this)
        setAdapter(adapter!!)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: SearchViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@SearchResultsActivity, message.message!!)
                showEmpty()
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@SearchResultsActivity, message)
                showEmpty()
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if(message != null) {
                dialogUtils.toast(message)
            }
        })
        viewModel.getAdvertisements().observe(this, Observer { advertisements ->
            dialogUtils.hideProgressDialog()
            if (advertisements != null && !advertisements.isEmpty() && !methods.isEmpty()) {
                setData(advertisements, methods)
                showContent()
            } else {
                showEmpty()
            }
        })
        dialogUtils.showProgressDialog(this@SearchResultsActivity, getString(R.string.toast_searching))
        disposable.add(viewModel.getMethods()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null) {
                        methods = data
                    }
                }, { error ->
                    Timber.e(error.message)
                }))

        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
            viewModel.getPlaces(tradeType)
        } else {
            viewModel.getOnlineAdvertisements(tradeType)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.searchresults, menu)
        return true
    }

    private fun showContent() {
        searchResultsList.visibility = View.VISIBLE
        searchResultsEmpty.visibility = View.GONE
    }

    private fun showEmpty() {
        searchResultsList.visibility = View.GONE
        searchResultsEmpty.visibility = View.VISIBLE
        emptyText.setText(R.string.text_no_advertisers)
    }

    private fun setData(advertisements: List<Advertisement>, methodItems: List<Method>) {
        getAdapter()!!.replaceWith(advertisements, methodItems)
    }

    private fun setAdapter(adapter: AdvertiseAdapter) {
        searchResultsList.adapter = adapter
    }

    private fun getAdapter(): AdvertiseAdapter? {
        return adapter
    }

    private fun getHeader(tradeType: TradeType?): String {
        var header = ""
        if (tradeType == null || tradeType == TradeType.NONE) {
            return header
        }
        when (tradeType) {
            TradeType.LOCAL_BUY -> header = getString(R.string.search_local_sellers_header)
            TradeType.LOCAL_SELL -> header = getString(R.string.search_local_buyers_header)
            TradeType.ONLINE_BUY -> header = getString(R.string.search_online_sellers_header)
            TradeType.ONLINE_SELL -> header = getString(R.string.search_online_buyers_header)
            else -> {
                // na-da
            }
        }

        header = getString(R.string.text_results_for, header)
        return header
    }

    private fun showAdvertiser(advertisement: Advertisement) {
        val intent = AdvertiserActivity.createStartIntent(this, advertisement.adId)
        startActivity(intent)
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, SearchResultsActivity::class.java)
        }
    }
}
