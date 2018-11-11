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

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.WalletActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.SectionRecycleViewAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.TransactionsAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.WalletViewModel
import com.thanksmister.bitcoin.localtrader.utils.Calculations
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_wallet.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class WalletFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: WalletViewModel

    private val disposable = CompositeDisposable()
    private var adapter: TransactionsAdapter? = null
    private var sectionRecycleViewAdapter: SectionRecycleViewAdapter? = null

    private inner class WalletData {
        var wallet: Wallet? = null
        var exchange: ExchangeRate? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ns = Context.NOTIFICATION_SERVICE
        val notificationManager = activity!!.getSystemService(ns) as NotificationManager
        notificationManager.cancel(NotificationUtils.NOTIFICATION_TYPE_BALANCE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_wallet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)

        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        transactionRecycleView!!.layoutManager = linearLayoutManager
        transactionRecycleView.setHasFixedSize(true)

        walletRefreshLayout.setOnRefreshListener(this)
        walletRefreshLayout.setColorSchemeColors(resources.getColor(R.color.red))

        adapter = TransactionsAdapter(activity!!)
        sectionRecycleViewAdapter = createAdapter()
        transactionRecycleView.adapter = sectionRecycleViewAdapter

        setAppBarText("0", "0", "-")

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: WalletViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.showAlertDialog(activity!!, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            Toast.makeText(activity!!, message, Toast.LENGTH_LONG).show()
        })
        toast(getString(R.string.toast_refreshing_data))
        viewModel.fetchWallet()
        disposable.add(
                viewModel.getWallet()
                        .zipWith(viewModel.getExchange(), BiFunction { wallet: Wallet, exchange: ExchangeRate ->
                            val data = WalletData()
                            data.wallet = wallet
                            data.exchange = exchange
                            data
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( { data ->
                            if (data.wallet != null) {
                                if(data.exchange != null) {
                                    setAppBarText(data.wallet!!.total.balance, data.exchange!!.rate, data.exchange!!.rate)
                                } else {
                                    setAppBarText(data.wallet!!.total.balance, "", "-")
                                }
                                setupList(data.wallet!!.transactions)
                                transactionRecycleView.visibility = View.VISIBLE
                            }
                            onRefreshStop()
                        }, { error ->
                            Timber.e(error.message)
                        }))
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

    override fun onRefresh() {
        viewModel.fetchWallet()
    }

    private fun onRefreshStop() {
        walletRefreshLayout.isRefreshing = false
    }

    private fun setupList(transactionItems: List<Transaction>?) {
        val itemAdapter = adapter
        if (!transactionItems!!.isEmpty()) {
            itemAdapter!!.replaceWith(transactionItems)
            val sections = ArrayList<SectionRecycleViewAdapter.Section>()
            sections.add(SectionRecycleViewAdapter.Section(1, getString(R.string.wallet_recent_activity_header)))
            if (!sectionRecycleViewAdapter!!.hasSections()) {
                addAdapterSection(sections)
            }
        } else {
            itemAdapter!!.replaceWith(ArrayList<Transaction>())
        }
        sectionRecycleViewAdapter!!.updateBaseAdapter(itemAdapter)
    }

    private fun addAdapterSection(sections: List<SectionRecycleViewAdapter.Section>) {
        try {
            val section = arrayOfNulls<SectionRecycleViewAdapter.Section>(sections.size)
            sectionRecycleViewAdapter!!.setSections(sections.toTypedArray<SectionRecycleViewAdapter.Section>())
            sectionRecycleViewAdapter!!.notifyDataSetChanged()
        } catch (e: IllegalStateException) {
            Timber.e(e.message)
        }
    }

    private fun createAdapter(): SectionRecycleViewAdapter {
        val itemAdapter = adapter
        return SectionRecycleViewAdapter(activity, R.layout.section, R.id.section_text, itemAdapter)
    }

    @SuppressLint("SetTextI18n")
    private fun setAppBarText(balance: String?, rate: String?, exchange: String?) {
        if(rate != null && balance != null && exchange != null) {
            val currency = preferences.exchangeCurrency
            val btcValue = Calculations.computedValueOfBitcoin(rate, balance)
            val btcAmount = Conversions.formatBitcoinAmount(balance) + " " + getString(R.string.btc)
            bitcoinPrice.text = btcAmount
            bitcoinTitle.setText(R.string.wallet_account_balance)
            bitcoinValue.text = "≈ $btcValue $currency ($exchange)"
        }
    }

    companion object {
        fun newInstance(): WalletFragment {
            return WalletFragment()
        }
    }
}