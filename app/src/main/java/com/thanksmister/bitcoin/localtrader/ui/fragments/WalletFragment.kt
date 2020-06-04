/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.annotation.SuppressLint
import androidx.lifecycle.*
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.databinding.ViewWalletBinding
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.adapters.TransactionsAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.WalletViewModel
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import timber.log.Timber
import javax.inject.Inject

class WalletFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private val binding: ViewWalletBinding by lazy {
        ViewWalletBinding.inflate(LayoutInflater.from(context), null, false)
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var viewModel: WalletViewModel

    @Inject lateinit var notificationUtils: NotificationUtils

    private val transactionsAdapter: TransactionsAdapter by lazy {
       TransactionsAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transactionRecycleView.apply {
            val linearLayoutManager = LinearLayoutManager(activity)
            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            adapter = transactionsAdapter
        }

        binding.walletRefreshLayout.setOnRefreshListener(this)
        binding.walletRefreshLayout.setColorSchemeColors(resources.getColor(R.color.red))

        setAppBarText("0.000000", "0.00", preferences.exchangeCurrency, preferences.selectedExchange)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
        observeViewModel(viewModel)
        lifecycle.addObserver(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        notificationUtils.clearNotification()
    }

    private fun observeViewModel(viewModel: WalletViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            message?.let {
                onRefreshStop()
                when {
                    RetrofitErrorHandler.isHttp403Error(it.code)  -> {
                        dialogUtils.showAlertDialog(requireActivity(), getString(R.string.error_bad_token), DialogInterface.OnClickListener { dialog, which ->
                            (requireActivity() as BaseActivity).logOut()
                        })
                    }
                    RetrofitErrorHandler.isNetworkError(it.code) ||
                            RetrofitErrorHandler.isHttp503Error(it.code) -> {
                        dialogUtils.showAlertDialog(requireActivity(), getString(R.string.error_network_retry), DialogInterface.OnClickListener { dialog, which ->
                            dialogUtils.toast(getString(R.string.toast_refreshing_data))
                            viewModel.fetchNetworkData()
                        }, DialogInterface.OnClickListener { _, _ -> })
                    }
                    RetrofitErrorHandler.isHttp400Error(it.code) -> {
                        dialogUtils.showAlertDialog(requireActivity(), it.message, DialogInterface.OnClickListener { dialog, which ->
                            Timber.e("Bad request: ${it.message}")
                        }, DialogInterface.OnClickListener { _, _ -> })
                    }
                    else -> dialogUtils.showAlertDialog(requireActivity(), it.message, DialogInterface.OnClickListener { dialog, which ->
                    }, DialogInterface.OnClickListener { _, _ -> })
                }
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                onRefreshStop()
                dialogUtils.showAlertDialog(requireActivity(), it)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                onRefreshStop()
                dialogUtils.toast(it)
            }
        })
        disposable.add(viewModel.getWalletData()
                .applySchedulers()
                .subscribe( { data ->
                    if (data != null) {
                        setAppBarText(data.bitcoinAmount, data.bitcoinValue, data.currency, data.exchange)
                        setupList(data.transactions)
                        binding.transactionRecycleView.visibility = View.VISIBLE
                    } else {
                        setAppBarText("0", "0", "-", "-")
                    }
                    onRefreshStop()
                }, { error ->
                    Timber.e(error.message)
                    onRefreshStop()
                }))

        dialogUtils.toast(getString(R.string.toast_refreshing_data))
        viewModel.fetchNetworkData()
    }

    override fun onRefresh() {
        viewModel.fetchNetworkData()
    }

    private fun onRefreshStop() {
        binding.walletRefreshLayout.isRefreshing = false
    }

    private fun setupList(transactionItems: List<Transaction>) {
        if (!transactionItems.isEmpty()) {
            transactionsAdapter.replaceWith(transactionItems)
        } else {
            transactionsAdapter.replaceWith(ArrayList<Transaction>())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAppBarText(btcAmount: String?, btcValue: String?, currency: String?, exchange: String?) {
        binding.bitcoinTitle.setText(R.string.wallet_account_balance)
        if(btcAmount != null) {
            binding.bitcoinPrice.text = btcAmount
        }
        if(btcValue != null && currency != null && exchange != null) {
            binding.bitcoinValue.text = "≈ $btcValue $currency ($exchange)"
        }
    }

    companion object {
        fun newInstance(): WalletFragment {
            return WalletFragment()
        }
    }
}