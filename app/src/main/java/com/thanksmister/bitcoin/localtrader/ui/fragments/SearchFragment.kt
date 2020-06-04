/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.content.SharedPreferences
import androidx.lifecycle.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.lifecycle.Observer
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.databinding.ViewSearchBinding
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.LocalMarketsActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchResultsActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.CurrencyAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.MethodAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.SpinnerAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SearchViewModel
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import kotlinx.android.synthetic.main.view_search.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class SearchFragment : BaseFragment() {

    private val binding: ViewSearchBinding by lazy {
        ViewSearchBinding.inflate(LayoutInflater.from(context), null, false)
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: SearchViewModel
    @Inject lateinit var sharedPreferences: SharedPreferences

    private var tradeType = TradeType.ONLINE_BUY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_TRADE_TYPE)) {
                tradeType = savedInstanceState.getSerializable(EXTRA_TRADE_TYPE) as TradeType
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = binding.root


    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)

        val countryNames = resources.getStringArray(R.array.country_names)
        val countryNamesList = ArrayList(Arrays.asList(*countryNames))
        countryNamesList.add(0, getString(R.string.text_list_country_any))
        val countryAdapter = SpinnerAdapter(requireActivity(), R.layout.spinner_layout, countryNamesList)
        binding.countrySpinner.adapter = countryAdapter

        if (!binding.countrySpinner.adapter.isEmpty) {
            var i = 0
            val countryName = viewModel.getSearchCountryName()
            for (name in countryNamesList) {
                if (name == countryName) {
                    binding.countrySpinner.setSelection(i)
                    break
                }
                i++
            }
        }

        binding.searchButton.setOnClickListener {
            showSearchResultsScreen()
        }

        val typeTitles = resources.getStringArray(R.array.list_types_spinner)
        val typeList = ArrayList(Arrays.asList(*typeTitles))

        val typeAdapter = SpinnerAdapter(requireActivity(), R.layout.spinner_layout, typeList)
        binding.typeSpinner.adapter = typeAdapter
        binding.currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                val exchange = binding.currencySpinner.adapter.getItem(i) as Currency
                exchange.code?.let {
                    viewModel.setSearchCurrency(it)
                }
            }
            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        tradeType = TradeType.valueOf(viewModel.getSearchTradeType())

        when (tradeType) {
            TradeType.ONLINE_BUY -> {
                binding.typeSpinner.setSelection(0)
            }
            TradeType.ONLINE_SELL -> {
                binding.typeSpinner.setSelection(1)
            }
            else -> {
                // na-da
            }
        }
        binding.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View?, position: Int, arg3: Long) {
                when (position) {
                    0 -> tradeType = TradeType.ONLINE_BUY
                    1 -> tradeType = TradeType.ONLINE_SELL
                }
                viewModel.setSearchTradeType(tradeType.name)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        binding.countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View?, position: Int, arg3: Long) {
                val countryCodes = resources.getStringArray(R.array.country_codes)
                val selectedCountryName = binding.countrySpinner.adapter.getItem(position) as String
                val selectedCountryCode = if (position == 0) "" else countryCodes[position - 1]
                viewModel.setSearchCountryName(selectedCountryName)
                viewModel.setSearchCountryCode(selectedCountryCode)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        binding.localMarketButton.visibility = View.VISIBLE
        binding.localMarketButton.setOnClickListener {
            showLocalMarkets()
        }
        observeViewModel(viewModel)
        lifecycle.addObserver(viewModel)

        binding.viewMoreButton.setOnClickListener {
            showLocalMarkets()
        }
    }

    private fun observeViewModel(viewModel: SearchViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { messageData ->
            messageData?.let {
                if(it.message.isNotEmpty()) {
                    dialogUtils.hideProgressDialog()
                    dialogUtils.showAlertDialog(requireActivity(), it.message)
                }
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(requireActivity(), message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(it)
            }
        })
        disposable += viewModel.getCurrencies()
                .applySchedulers()
                .subscribe( { data ->
                    if(data != null) {
                        setCurrencies(data)
                    } else {
                        val currencyList = ArrayList<Currency>()
                        setCurrencies(currencyList);
                    }
                }, { error ->
                    Timber.e(error.message)
                })
        disposable += viewModel.getMethods()
                .applySchedulers()
                .subscribe( { data ->
                    if(data != null) {
                        val items = ArrayList<Method>(data)
                        val method = Method()
                        method.key = "all"
                        method.code = "all"
                        method.name = getString(R.string.text_method_name_all)
                        items.add(0, method)
                        setMethods(items);
                    } else {
                        val items = ArrayList<Method>()
                        val method = Method()
                        method.key = "all"
                        method.code = "all"
                        method.name = getString(R.string.text_method_name_all)
                        items.add(0, method)
                        setMethods(items)
                    }
                }, { error ->
                    Timber.e(error.message)
                })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(EXTRA_TRADE_TYPE, tradeType)
        super.onSaveInstanceState(outState)
    }

    private fun setMethods(methods: ArrayList<Method>) {

        val typeAdapter = MethodAdapter(requireActivity(), R.layout.spinner_layout, methods)
        binding.paymentMethodSpinner.adapter = typeAdapter

        val methodKey = viewModel.getSearchPaymentMethod()
        var position = 0
        for (methodItem in methods) {
            if (methodItem.key == methodKey) {
                break
            }
            position++
        }

        if (position <= methods.size) {
            binding.paymentMethodSpinner.setSelection(position)
        }

        binding.paymentMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View?, position: Int, arg3: Long) {
                try {
                    val methodItem = binding.paymentMethodSpinner.adapter.getItem(position) as Method
                    methodItem.key?.let {
                        viewModel.setSearchPaymentMethod(it)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Timber.e("Error setting methods: " + e.message)
                }
            }
            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
    }

    private fun setCurrencies(currencies: List<Currency>) {
        val sortedCurrencies = CurrencyUtils.sortCurrencies(currencies)
        val searchCurrency = viewModel.getSearchCurrency()
        if (sortedCurrencies.isEmpty()) {
            val exchangeCurrency = Currency()
            exchangeCurrency.code = searchCurrency
            sortedCurrencies.add(exchangeCurrency) // just revert back to default
        }

        var containsAny = false
        for (currency in sortedCurrencies) {
            if (getString(R.string.text_currency_any).toLowerCase() == currency.code) {
                containsAny = true
                break
            }
        }

        if (!containsAny) {
            // add "any" for search option
            val exchangeCurrency = Currency()
            exchangeCurrency.code = getString(R.string.text_currency_any)
            sortedCurrencies.add(0, exchangeCurrency)
        }

        val typeAdapter = CurrencyAdapter(activity, R.layout.spinner_layout, sortedCurrencies)
        binding.currencySpinner.adapter = typeAdapter

        var i = 0
        for (currency in sortedCurrencies) {
            if (searchCurrency == currency.code) {
                binding.currencySpinner.setSelection(i)
                break
            }
            i++
        }
    }

    private fun showSearchResultsScreen() {
        val intent = SearchResultsActivity.createStartIntent(requireActivity())
        startActivity(intent)
    }

    private fun showLocalMarkets() {
        val intent = LocalMarketsActivity.createStartIntent(requireActivity())
        startActivity(intent)
        //if (isAdded && activity != null) {
            /*dialogUtils.run {
                showAlertHtmlDialog(requireActivity(), getString(R.string.local_markets), View.OnClickListener {
                    dialogUtils.clearDialogs()
                })
            }*/
        //}
    }

    companion object {
        const val EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE"
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }
}