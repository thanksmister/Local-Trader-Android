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

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.network.services.GeoLocationService
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.AdvertiseAdapter
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils

import javax.inject.Inject

class SearchResultsActivity : BaseActivity() {

    //@Inject
    lateinit var geoLocationService: GeoLocationService

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    internal var content: ListView? = null
    internal var progress: View? = null
    internal var emptyLayout: View? = null
    internal var emptyText: TextView? = null

    private var adapter: AdvertiseAdapter? = null
    private var tradeType = TradeType.NONE

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_search_results)

        tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences))

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setTitle(getHeader(tradeType))
        }

        content!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val advertisement = adapterView.adapter.getItem(i) as Advertisement
            showAdvertiser(advertisement)
        }

        adapter = AdvertiseAdapter(this)
        setAdapter(adapter!!)
        updateData()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    public override fun handleRefresh() {
        updateData()
    }


    fun showContent() {
        if (content != null && progress != null && emptyLayout != null) {
            content!!.visibility = View.VISIBLE
            emptyLayout!!.visibility = View.GONE
            progress!!.visibility = View.GONE
        }
    }

    fun showEmpty() {
        if (content != null && progress != null && emptyLayout != null) {
            content!!.visibility = View.GONE
            emptyLayout!!.visibility = View.VISIBLE
            progress!!.visibility = View.GONE
            emptyText!!.setText(R.string.text_no_advertisers)
        }
    }

    fun showProgress() {
        if (content != null && progress != null && emptyLayout != null) {
            content!!.visibility = View.GONE
            emptyLayout!!.visibility = View.GONE
            progress!!.visibility = View.VISIBLE
        }
    }

    protected fun updateData() {

        val currency = SearchUtils.getSearchCurrency(sharedPreferences!!)
        val paymentMethod = SearchUtils.getSearchPaymentMethod(sharedPreferences!!)
        val country = SearchUtils.getSearchCountryName(sharedPreferences!!)
        val code = SearchUtils.getSearchCountryCode(sharedPreferences!!)
        val latitude = SearchUtils.getSearchLatitude(sharedPreferences!!)
        val longitude = SearchUtils.getSearchLongitude(sharedPreferences!!)

        toast(getString(R.string.toast_searching))
        showProgress()

        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
            /*geoLocationService.getLocalAdvertisements(latitude, longitude, tradeType)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Advertisement>>() {
                        @Override
                        public void call(final List<Advertisement> advertisements) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setData(advertisements, null);
                                }
                            });
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(final Throwable throwable) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showEmpty();
                                }
                            });
                        }
                    });*/
        } else {
            /*dbManager.methodQuery().cache()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<MethodItem>>() {
                        @Override
                        public void call(final List<MethodItem> methodItems) {
                            String method = TradeUtils.INSTANCE.getPaymentMethod(paymentMethod, methodItems);
                            geoLocationService.getOnlineAdvertisements(tradeType, country, code, currency, method)
                                    .subscribe(new Action1<List<Advertisement>>() {
                                        @Override
                                        public void call(final List<Advertisement> advertisements) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setData(advertisements, methodItems);
                                                }
                                            });
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(final Throwable throwable) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    toast(throwable.getMessage());
                                                    showEmpty();
                                                }
                                            });
                                        }
                                    });
                        }
                    });*/
        }
    }

    private fun setData(advertisements: List<Advertisement>, methodItems: List<Method>?) {
        if (advertisements.isEmpty()) {
            showEmpty()
        } else if ((tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL) && methodItems == null) {
            showEmpty()
            toast(getString(R.string.toast_error_advertisers))
        } else {
            showContent()
            getAdapter()!!.replaceWith(advertisements, methodItems)
        }
    }

    private fun setAdapter(adapter: AdvertiseAdapter) {
        content!!.adapter = adapter
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
        }

        header = getString(R.string.text_results_for, header)
        return header
    }

    fun showAdvertiser(advertisement: Advertisement) {
        val intent = AdvertiserActivity.createStartIntent(this, advertisement.adId)
        startActivity(intent)
    }

    companion object {


        fun createStartIntent(context: Context): Intent {
            return Intent(context, SearchResultsActivity::class.java)
        }
    }
}
