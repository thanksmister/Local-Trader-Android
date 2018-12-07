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

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchResultsActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.CurrencyAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.MethodAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.PredictAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.SpinnerAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SearchViewModel
import com.thanksmister.bitcoin.localtrader.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_search.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class SearchFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: SearchViewModel

    @Inject lateinit var locationManager: LocationManager

    private val disposable = CompositeDisposable()
    private var predictAdapter: PredictAdapter? = null
    private var tradeType = TradeType.LOCAL_BUY
    private var locationMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_TRADE_TYPE)) {
                tradeType = savedInstanceState.getSerializable(EXTRA_TRADE_TYPE) as TradeType
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_search, container, false)
    }

    override fun onViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragmentView, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)

        val countryNames = resources.getStringArray(R.array.country_names)
        val countryNamesList = ArrayList(Arrays.asList(*countryNames))
        countryNamesList.add(0, "Any")
        val countryAdapter = SpinnerAdapter(activity!!, R.layout.spinner_layout, countryNamesList)
        countrySpinner!!.adapter = countryAdapter

        var i = 0
        val countryName = viewModel.getSearchCountryName()
        for (name in countryNamesList) {
            if (name == countryName) {
                countrySpinner!!.setSelection(i)
                break
            }
            i++
        }

        searchButton.setOnClickListener {
            showSearchResultsScreen()
        }

        clearButton.setOnClickListener {
            viewModel.setSearchLatitude(0.0)
            viewModel.setSearchLongitude(0.0)
            viewModel.clearSearchLocationAddress()
            editLocationText.setText("")
            editLatitude.setText("")
            editLongitude!!.setText("")
            showEditTextLayout()
        }

        val locationTitles = resources.getStringArray(R.array.list_location_spinner)
        val locationList = ArrayList(Arrays.asList(*locationTitles))

        val locationAdapter = SpinnerAdapter(activity!!, R.layout.spinner_layout, locationList)
        locationSpinner!!.adapter = locationAdapter

        val typeTitles = resources.getStringArray(R.array.list_types_spinner)
        val typeList = ArrayList(Arrays.asList(*typeTitles))

        val typeAdapter = SpinnerAdapter(activity!!, R.layout.spinner_layout, typeList)
        typeSpinner!!.adapter = typeAdapter
        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val exchange = currencySpinner!!.adapter.getItem(i) as Currency
                if(exchange.code != null) {
                    viewModel.setSearchCurrency(exchange.code!!)
                }
            }
            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        tradeType = TradeType.valueOf(viewModel.getSearchTradeType())
        if (locationMenuItem != null) {
            locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
        }
        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
            onlineOptionsLayout.visibility = View.GONE
            localOptionsLayout.visibility = View.VISIBLE
        } else {
            onlineOptionsLayout.visibility = View.VISIBLE
            localOptionsLayout.visibility = View.GONE
        }
        when (tradeType) {
            TradeType.LOCAL_BUY -> {
                typeSpinner.setSelection(0)
                locationSpinner.setSelection(0)
            }
            TradeType.LOCAL_SELL -> {
                typeSpinner.setSelection(1)
                locationSpinner.setSelection(0)
            }
            TradeType.ONLINE_BUY -> {
                typeSpinner.setSelection(0)
                locationSpinner.setSelection(1)
            }
            TradeType.ONLINE_SELL -> {
                typeSpinner.setSelection(1)
                locationSpinner.setSelection(1)
            }
            else -> {
                // na-da
            }
        }
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, arg3: Long) {
                when (position) {
                    0 -> tradeType = (if (locationSpinner.selectedItemPosition == 0) TradeType.LOCAL_BUY else TradeType.ONLINE_BUY)
                    1 -> tradeType = (if (locationSpinner.selectedItemPosition == 0) TradeType.LOCAL_SELL else TradeType.ONLINE_SELL)
                }
                viewModel.setSearchTradeType(tradeType.name)
                if (locationMenuItem != null) {
                    locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, arg3: Long) {
                val countryCodes = resources.getStringArray(R.array.country_codes)
                val selectedCountryName = countrySpinner.adapter.getItem(position) as String
                val selectedCountryCode = if (position == 0) "" else countryCodes[position - 1]
                Timber.d("Selected Country Name: $selectedCountryName")
                Timber.d("Selected Country Code: $selectedCountryCode")
                viewModel.setSearchCountryName(selectedCountryName)
                viewModel.setSearchCountryCode(selectedCountryCode)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, arg3: Long) {
                when (position) {
                    0 -> {
                        tradeType = if (typeSpinner!!.selectedItemPosition == 0) TradeType.LOCAL_BUY else TradeType.LOCAL_SELL
                        onlineOptionsLayout!!.visibility = View.GONE
                        localOptionsLayout!!.visibility = View.VISIBLE
                    }
                    1 -> {
                        tradeType = if (typeSpinner!!.selectedItemPosition == 0) TradeType.ONLINE_BUY else TradeType.ONLINE_SELL
                        onlineOptionsLayout!!.visibility = View.VISIBLE
                        localOptionsLayout!!.visibility = View.GONE
                    }
                }
                Timber.d("TradeType: $tradeType")
                viewModel.setSearchTradeType(tradeType.name)
                if (locationMenuItem != null) {
                    locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        editLocationText.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val address = predictAdapter!!.getItem(i)
            editLocationText.setText("")
            if(address != null) {
                saveAddress(address)
                displayAddress(address)
            }
        }
        editLocationText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                if (!TextUtils.isEmpty(charSequence)) {
                    viewModel.doAddressLookup(charSequence.toString())
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        predictAdapter = PredictAdapter(activity!!, ArrayList())
        setEditLocationAdapter(predictAdapter!!)

        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: SearchViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                activity!!.runOnUiThread {
                    dialogUtils.showAlertDialog(activity!!, message.message!!)
                }
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                activity!!.runOnUiThread {
                    dialogUtils.hideProgressDialog()
                    dialogUtils.showAlertDialog(activity!!, message)
                }
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                activity!!.runOnUiThread {
                    dialogUtils.toast(message)
                }
            }
        })
        viewModel.getAddress().observe(this, Observer { address ->
            if (address != null && activity != null) {
                activity!!.runOnUiThread {
                    displayAddress(address)
                }
            }
        })
        viewModel.getAddresses().observe(this, Observer { addresses ->
            if (addresses != null && predictAdapter != null) {
                activity!!.runOnUiThread {
                    predictAdapter!!.replaceWith(addresses)
                }
            }
        })
        disposable.add(viewModel.getCurrencies()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null) {
                        setCurrencies(data)
                    } else {
                        val currencyList = ArrayList<Currency>()
                        setCurrencies(currencyList);
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
        disposable.add(viewModel.getMethods()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
                        setMethods(items);
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(EXTRA_TRADE_TYPE, tradeType)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.search, menu)
        locationMenuItem = menu!!.findItem(R.id.action_location)
        if (locationMenuItem != null) {
            locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_location -> {
                checkLocationEnabled()
                return true
            }
        }
        return false
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> checkLocationEnabled()
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                dialogUtils.toast(R.string.toast_search_canceled)
                if (isAdded && activity != null) {
                    activity!!.finish()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.REQUEST_PERMISSIONS -> {
                if (grantResults.isNotEmpty()) {
                    var permissionsDenied = false
                    for (permission in grantResults) {
                        if (permission != PackageManager.PERMISSION_GRANTED) {
                            permissionsDenied = true
                            break
                        }
                    }
                    if (permissionsDenied) {
                        closeView()
                    } else {
                        startLocationMonitoring()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setMethods(methods: ArrayList<Method>) {
        val typeAdapter = MethodAdapter(activity!!, R.layout.spinner_layout, methods)
        paymentMethodSpinner!!.adapter = typeAdapter
        val methodKey = viewModel.getSearchPaymentMethod()
        var position = 0
        for (methodItem in methods) {
            if (methodItem.key == methodKey) {
                break
            }
            position++
        }

        if (position <= methods.size) {
            paymentMethodSpinner!!.setSelection(position)
        }

        paymentMethodSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, arg3: Long) {
                try {
                    val methodItem = paymentMethodSpinner!!.adapter.getItem(position) as Method
                    viewModel.setSearchPaymentMethod(methodItem.key!!)
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
        currencySpinner!!.adapter = typeAdapter

        var i = 0
        for (currency in sortedCurrencies) {
            if (searchCurrency == currency.code) {
                currencySpinner!!.setSelection(i)
                break
            }
            i++
        }
    }

    private fun saveAddress(address: Address) {
        Timber.d("SaveAddress: " + address.toString())
        viewModel.setAddress(address)
    }

    /**
     * Displays the shortened version of the current address
     *
     * @param address Address
     */
    private fun displayAddress(address: Address?) {
        val shortAddress = SearchUtils.getDisplayAddress(address!!)
        if (!TextUtils.isEmpty(shortAddress)) {
            if (address.hasLatitude()) {
                editLatitude!!.setText(address.latitude.toString())
            }
            if (address.hasLongitude()) {
                editLongitude!!.setText(address.longitude.toString())
            }
            locationText!!.text = shortAddress
            searchButton!!.isEnabled = true
            showLocationLayout()
        }
    }

    private fun showEditTextLayout() {
        if (locationText!!.isShown) {
            locationText!!.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_out_right))
            editLocationLayout!!.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left))
            locationText!!.visibility = View.GONE
            editLocationLayout!!.visibility = View.VISIBLE
            editLocationText.requestFocus()
        }
    }

    private fun showLocationLayout() {
        if (editLocationLayout!!.isShown) {
            editLocationLayout!!.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_out_right))
            locationText!!.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left))
            editLocationLayout!!.visibility = View.GONE
            locationText!!.visibility = View.VISIBLE
            try {
                if (isAdded) {
                    val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(activity!!.currentFocus!!.windowToken, 0)
                }
            } catch (e: NullPointerException) {
                Timber.w("Error closing keyboard")
            }
        }
    }

    private fun setEditLocationAdapter(adapter: PredictAdapter) {
        editLocationText.setAdapter(adapter)
    }

    private fun showSearchResultsScreen() {
        val tradeType = TradeType.valueOf(viewModel.getSearchTradeType())
        if (TradeUtils.isLocalTrade(tradeType)) {
            val latitude = editLatitude.text.toString()
            val longitude = editLongitude.text.toString()
            if (TextUtils.isEmpty(latitude) || TextUtils.isEmpty(longitude)) {
                viewModel.setSearchLatitude(0.0)
                viewModel.setSearchLongitude(0.0)
            } else if (!SearchUtils.coordinatesValid(latitude, longitude)) {
                dialogUtils.showAlertDialog(activity!!, getString(R.string.error_invalid_coordinates))
                return
            } else if (SearchUtils.coordinatesValid(latitude, longitude)) {
                viewModel.setSearchLatitude(Doubles.convertToDouble(latitude))
                viewModel.setSearchLongitude( Doubles.convertToDouble(longitude))
            }

            val lat = viewModel.getSearchLatitude()
            val lon = viewModel.getSearchLongitude()
            if (lon == 0.0 && lat == 0.0) {
                dialogUtils.showAlertDialog(activity!!, getString(R.string.error_need_valid_coordinates))
                return
            }
        }
        val intent = SearchResultsActivity.createStartIntent(activity!!)
        startActivity(intent)
    }

    private fun closeView() {
        if (isAdded && activity != null) {
            dialogUtils.toast(getString(R.string.toast_search_canceled))
            activity!!.finish()
        }
    }

    private fun checkLocationEnabled() {
        if (isAdded && activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), Constants.REQUEST_PERMISSIONS)
                    return
                }
            }
            if (!NetworkUtils.hasLocationServices(locationManager)) {
                showNoLocationServicesWarning()
                return
            }
            startLocationMonitoring()
        }
    }

    /**
     * Using the ReactiveLocationProvider to do location lookup because its more convenient than using
     * the LocationManager and seems to perform better.
     */
    private fun startLocationMonitoring() {
        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), Constants.REQUEST_PERMISSIONS)
            return
        }
        dialogUtils.showProgressDialog(activity!!, getString(R.string.dialog_progress_location), true)
        viewModel.startLocationMonitoring()
    }

    private fun showNoLocationServicesWarning() {
        dialogUtils.showAlertDialog(activity!!, getString(R.string.warning_no_location_active), DialogInterface.OnClickListener { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_CHECK_SETTINGS);
        }, DialogInterface.OnClickListener { dialog, which ->
            closeView()
        })
    }

    companion object {
        const val EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE"
        const val REQUEST_CHECK_SETTINGS = 0
        const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }
}