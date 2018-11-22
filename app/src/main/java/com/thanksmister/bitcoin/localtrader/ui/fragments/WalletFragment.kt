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
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.adapters.TransactionsAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.WalletViewModel
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_wallet.*
import timber.log.Timber
import javax.inject.Inject

class WalletFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: WalletViewModel
    private var connectionLiveData: ConnectionLiveData? = null

    private val disposable = CompositeDisposable()
    private var adapter: TransactionsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ns = Context.NOTIFICATION_SERVICE
        val notificationManager = activity!!.getSystemService(ns) as NotificationManager
        notificationManager.cancel(NotificationUtils.NOTIFICATION_TYPE_BALANCE)

        connectionLiveData = ConnectionLiveData(activity!!)
        connectionLiveData?.observe(this, Observer { connected ->
            if(!connected!! && activity != null) {
                Toast.makeText(activity, getString(R.string.error_network_disconnected), Toast.LENGTH_SHORT).show()
                onRefreshStop()
            } else {
                toast(getString(R.string.toast_refreshing_data))
                viewModel.fetchNetworkData()
            }
        })
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
        transactionRecycleView.adapter = adapter

        setAppBarText("0.000000", "0.00", preferences.exchangeCurrency, preferences.selectedExchange)

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
            onRefreshStop()
        })
        disposable.add(viewModel.getWalletData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    Timber.d("wallet updated!!")
                    if (data != null) {
                        Timber.d("data $data")
                        setAppBarText(data.bitcoinAmount, data.bitcoinValue, data.currency, data.exchange)
                        setupList(data.transactions)
                        transactionRecycleView.visibility = View.VISIBLE
                    } else {
                        setAppBarText("0", "0", "-", "-")
                    }
                    onRefreshStop()
                }, { error ->
                    Timber.e(error.message)
                    onRefreshStop()
                }))

        toast(getString(R.string.toast_refreshing_data))
        viewModel.fetchNetworkData()
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
        viewModel.fetchNetworkData()
    }

    private fun onRefreshStop() {
        walletRefreshLayout.isRefreshing = false
    }

    private fun setupList(transactionItems: List<Transaction>?) {
        val itemAdapter = adapter
        if (!transactionItems!!.isEmpty()) {
            itemAdapter!!.replaceWith(transactionItems)
        } else {
            itemAdapter!!.replaceWith(ArrayList<Transaction>())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAppBarText(btcAmount: String?, btcValue: String?, currency: String?, exchange: String?) {
        bitcoinTitle.setText(R.string.wallet_account_balance)
        if(btcAmount != null) {
            bitcoinPrice.text = btcAmount
        }
        if(btcValue != null && currency != null && exchange != null) {
            bitcoinValue.text = "≈ $btcValue $currency ($exchange)"
        }
    }

    companion object {
        fun newInstance(): WalletFragment {
            return WalletFragment()
        }
    }
}