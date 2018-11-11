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
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.android.gms.location.LocationRequest
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchResultsActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.CurrencyAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.MethodAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.PredictAdapter
import com.thanksmister.bitcoin.localtrader.ui.adapters.SpinnerAdapter
import com.thanksmister.bitcoin.localtrader.utils.*
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class SearchFragment : BaseFragment() {

    @Inject lateinit var sharedPreferences: SharedPreferences

    @Inject lateinit var locationManager: LocationManager

    //@BindView(R.id.editLocationText)
    internal var editLocation: AutoCompleteTextView? = null

    //@BindView(R.id.locationText)
    internal var locationText: TextView? = null

    //@BindView(R.id.editLatitude)
    internal var editLatitude: EditText? = null

    //@BindView(R.id.editLongitude)
    internal var editLongitude: EditText? = null

    //@BindView(R.id.editLocationLayout)
    internal var editLocationLayout: View? = null

    //@BindView(R.id.locationSpinner)
    internal var locationSpinner: Spinner? = null

    // @BindView(R.id.typeSpinner)
    internal var typeSpinner: Spinner? = null

    //@BindView(R.id.countrySpinner)
    internal var countrySpinner: Spinner? = null

    //@BindView(R.id.paymentMethodSpinner)
    internal var paymentMethodSpinner: Spinner? = null

    //@BindView(R.id.onlineOptionsLayout)
    internal var onlineOptionsLayout: View? = null

    //@BindView(R.id.localOptionsLayout)
    internal var localOptionsLayout: View? = null

    //@BindView(R.id.searchButton)
    internal var searchButton: Button? = null

    // @BindView(R.id.clearButton)
    internal var clearButton: ImageButton? = null

    //@BindView(R.id.currencySpinner)
    internal var currencySpinner: Spinner? = null

    private var predictAdapter: PredictAdapter? = null
    private var tradeType = TradeType.LOCAL_BUY

    private var locationMenuItem: MenuItem? = null

    //@OnClick(R.id.clearButton)
    fun clearButtonClicked() {
        SearchUtils.setSearchLatitude(sharedPreferences, 0.0)
        SearchUtils.setSearchLongitude(sharedPreferences, 0.0)
        SearchUtils.clearSearchLocationAddress(sharedPreferences)
        editLocation!!.setText("")
        editLatitude!!.setText("")
        editLongitude!!.setText("")
        showEditTextLayout()
    }

    // @OnClick(R.id.searchButton)
    fun searchButtonClicked() {
        showSearchResultsScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ADDRESS)) {
                tradeType = savedInstanceState.getSerializable(EXTRA_TRADE_TYPE) as TradeType
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_TRADE_TYPE, tradeType)
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

    override fun onResume() {
        super.onResume()
        subscribeData()
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_search, container, false)
    }

    override fun onViewCreated(fragmentView: View, savedInstanceState: Bundle?) {

        super.onViewCreated(fragmentView, savedInstanceState)

        val countryNames = resources.getStringArray(R.array.country_names)
        val countryNamesList = ArrayList(Arrays.asList(*countryNames))
        countryNamesList.add(0, "Any")
        val countryAdapter = SpinnerAdapter(activity, R.layout.spinner_layout, countryNamesList)
        countrySpinner!!.adapter = countryAdapter

        var i = 0
        val countryName = SearchUtils.getSearchCountryName(sharedPreferences)
        for (name in countryNamesList) {
            if (name == countryName) {
                countrySpinner!!.setSelection(i)
                break
            }
            i++
        }

        val locationTitles = resources.getStringArray(R.array.list_location_spinner)
        val locationList = ArrayList(Arrays.asList(*locationTitles))

        val locationAdapter = SpinnerAdapter(activity, R.layout.spinner_layout, locationList)
        locationSpinner!!.adapter = locationAdapter

        val typeTitles = resources.getStringArray(R.array.list_types_spinner)
        val typeList = ArrayList(Arrays.asList(*typeTitles))

        val typeAdapter = SpinnerAdapter(activity, R.layout.spinner_layout, typeList)
        typeSpinner!!.adapter = typeAdapter

        currencySpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val exchange = currencySpinner!!.adapter.getItem(i) as Currency
                if(exchange.code != null) {
                    SearchUtils.setSearchCurrency(sharedPreferences, exchange.code!!)
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences))

        if (locationMenuItem != null) {
            locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
        }

        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
            onlineOptionsLayout!!.visibility = View.GONE
            localOptionsLayout!!.visibility = View.VISIBLE
        } else {
            onlineOptionsLayout!!.visibility = View.VISIBLE
            localOptionsLayout!!.visibility = View.GONE
        }

        when (tradeType) {
            TradeType.LOCAL_BUY -> {
                typeSpinner!!.setSelection(0)
                locationSpinner!!.setSelection(0)
            }
            TradeType.LOCAL_SELL -> {
                typeSpinner!!.setSelection(1)
                locationSpinner!!.setSelection(0)
            }
            TradeType.ONLINE_BUY -> {
                typeSpinner!!.setSelection(0)
                locationSpinner!!.setSelection(1)
            }
            TradeType.ONLINE_SELL -> {
                typeSpinner!!.setSelection(1)
                locationSpinner!!.setSelection(1)
            }
        }

        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, arg3: Long) {
                when (position) {
                    0 -> tradeType = (if (locationSpinner!!.selectedItemPosition == 0) TradeType.LOCAL_BUY else TradeType.ONLINE_BUY)
                    1 -> tradeType = (if (locationSpinner!!.selectedItemPosition == 0) TradeType.LOCAL_SELL else TradeType.ONLINE_SELL)
                }
                SearchUtils.setSearchTradeType(sharedPreferences, tradeType.name)
                if (locationMenuItem != null) {
                    locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        countrySpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, arg3: Long) {
                val countryCodes = resources.getStringArray(R.array.country_codes)

                val selectedCountryName = countrySpinner!!.adapter.getItem(position) as String
                val selectedCountryCode = if (position == 0) "" else countryCodes[position - 1]

                Timber.d("Selected Country Name: $selectedCountryName")
                Timber.d("Selected Country Code: $selectedCountryCode")

                SearchUtils.setSearchCountryName(sharedPreferences, selectedCountryName)
                SearchUtils.setSearchCountryCode(sharedPreferences, selectedCountryCode)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        locationSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                SearchUtils.setSearchTradeType(sharedPreferences, tradeType.name)
                if (locationMenuItem != null) {
                    locationMenuItem!!.isVisible = tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        editLocation!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val address = predictAdapter!!.getItem(i)
            editLocation!!.setText("")
            saveAddress(address)
            displayAddress(address)
        }

        editLocation!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                if (!TextUtils.isEmpty(charSequence)) {
                    doAddressLookup(charSequence.toString())
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        predictAdapter = PredictAdapter(activity, ArrayList())
        setEditLocationAdapter(predictAdapter!!)

        val address = SearchUtils.getSearchLocationAddress(sharedPreferences)
        displayAddress(address)
        setupToolbar()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> checkLocationEnabled()
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                toast(R.string.toast_search_canceled)
                if (isAdded) {
                    // TODO close this when its an activity
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.REQUEST_PERMISSIONS -> {
                if (grantResults.size > 0) {
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

    private fun setupToolbar() {
        if (activity != null) {
            val ab = (activity as MainActivity).supportActionBar
            if (ab != null) {
                ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu)
                ab.title = getString(R.string.view_title_buy_sell)
                ab.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun subscribeData() {
        /*dbManager.currencyQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<CurrencyItem>>() {
                    @Override
                    public void call(List<CurrencyItem> currencyItems) {
                        if (currencyItems == null || currencyItems.isEmpty()) {
                            fetchCurrencies();
                        } else {
                            List<ExchangeCurrency> exchangeCurrencies = new ArrayList<ExchangeCurrency>();
                            exchangeCurrencies = ExchangeCurrencyItem.getCurrencies(currencyItems);
                            setCurrencies(exchangeCurrencies);
                        }
                    }
                });
        dbManager.methodQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Method subscription safely unsubscribed");
                    }
                })
                .subscribe(new Action1<List<MethodItem>>() {
                    @Override
                    public void call(List<MethodItem> methodItems) {
                        if (methodItems == null || methodItems.isEmpty()) {
                            fetchMethods();
                        } else {
                            Method method = new Method();
                            method.setCode("all");
                            method.setName(getString(R.string.text_method_name_all));
                            MethodItem methodItem = MethodItem.getModelItem(method);
                            methodItems.add(0, methodItem);
                            setMethods(methodItems);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(new Throwable(getString(R.string.error_unable_load_payment_methods)));
                    }
                });*/
    }

    private fun fetchCurrencies() {

        /*dataService.getCurrencies()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Currencies network subscription safely unsubscribed");
                    }
                })
                .subscribe(new Action1<List<ExchangeCurrency>>() {
                    @Override
                    public void call(List<ExchangeCurrency> currencies) {
                        if (currencies != null) {
                            dbManager.insertCurrencies(currencies);
                            dataService.setCurrencyExpireTime();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);

                    }
                });*/
    }

    private fun fetchMethods() {

        Timber.d("getMethods")

        /*dataService.getMethods()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Method network subscription safely unsubscribed");
                    }
                })
                .subscribe(new Action1<List<Method>>() {
                    @Override
                    public void call(List<Method> methods) {
                        if (methods != null) {
                            dbManager.updateMethods(methods);
                            dataService.setMethodsExpireTime();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        toast(getString(R.string.toast_loading_methods));
                    }
                });*/
    }

    private fun setMethods(methods: List<Method>) {

        val typeAdapter = MethodAdapter(activity, R.layout.spinner_layout, methods)
        paymentMethodSpinner!!.adapter = typeAdapter

        val methodCode = SearchUtils.getSearchPaymentMethod(sharedPreferences!!)
        var position = 0
        for (methodItem in methods) {
            if (methodItem.code == methodCode) {
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
                    SearchUtils.setSearchPaymentMethod(sharedPreferences, methodItem.code!!)
                } catch (e: IndexOutOfBoundsException) {
                    Timber.e("Error setting methods: " + e.message)
                }

            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
    }

    /**
     * Set the currencies to be used for a new editAdvertisement
     *
     * @param currencies
     */
    private fun setCurrencies(currencies: MutableList<Currency>) {
        var currencies = currencies

        currencies = CurrencyUtils.sortCurrencies(currencies)
        val searchCurrency = SearchUtils.getSearchCurrency(sharedPreferences)
        if (currencies.isEmpty()) {
            val exchangeCurrency = Currency()
            exchangeCurrency.code = searchCurrency
            currencies.add(exchangeCurrency) // just revert back to default
        }

        // TODO this is temporary fix for issues with Any as default currency
        var containsAny = false
        for (currency in currencies) {
            if (getString(R.string.text_currency_any).toLowerCase() == currency.code) {
                containsAny = true
                break
            }
        }

        if (!containsAny) {
            // add "any" for search option
            val exchangeCurrency = Currency()
            exchangeCurrency.code = getString(R.string.text_currency_any)
            currencies.add(0, exchangeCurrency)
        }

        val typeAdapter = CurrencyAdapter(activity, R.layout.spinner_layout, currencies)
        currencySpinner!!.adapter = typeAdapter

        var i = 0
        for (currency in currencies) {
            if (searchCurrency == currency.code) {
                currencySpinner!!.setSelection(i)
                break
            }
            i++
        }
    }

    /**
     * Saves the current address to user preferences
     *
     * @param address Address
     */
    fun saveAddress(address: Address?) {

        Timber.d("SaveAddress: " + address!!.toString())

        SearchUtils.setSearchLocationAddress(sharedPreferences, address)
        if (address.hasLatitude()) {
            SearchUtils.setSearchLatitude(sharedPreferences, address.latitude)
        }
        if (address.hasLongitude()) {
            SearchUtils.setSearchLongitude(sharedPreferences, address.longitude)
        }
        if (!TextUtils.isEmpty(address.countryCode)) {
            SearchUtils.setSearchCountryCode(sharedPreferences, address.countryCode)
        }
        if (!TextUtils.isEmpty(address.getAddressLine(0))) {
            SearchUtils.setSearchCountryName(sharedPreferences, address.getAddressLine(0))
        }
    }

    /**
     * Displays the shortened version of the current address
     *
     * @param address Address
     */
    fun displayAddress(address: Address?) {
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

    private fun showAddressError() {
        // TODO dialog utils
        //showAlertDialog(new AlertDialogEvent(getString(R.string.error_address_title), getString(R.string.error_dialog_no_address_edit)));
    }

    private fun showEditTextLayout() {
        if (locationText!!.isShown) {
            locationText!!.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_out_right))
            editLocationLayout!!.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left))
            locationText!!.visibility = View.GONE
            editLocationLayout!!.visibility = View.VISIBLE
            editLocation!!.requestFocus()
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
        if (editLocation != null) {
            editLocation!!.setAdapter(adapter)
        }
    }

    private fun doAddressLookup(locationName: String) {
        /*final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getContext().getApplicationContext());
        locationProvider.getGeocodeObservable(locationName, MAX_ADDRESSES)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Address lookup subscription safely unsubscribed");
                    }
                })
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Address>>() {
                    @Override
                    public void call(final List<Address> addresses) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!addresses.isEmpty()) {
                                        predictAdapter.replaceWith(addresses);
                                    }
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                                    } catch (NullPointerException e) {
                                        Timber.w("Error closing keyboard");
                                    }
                                    Timber.e("Address lookup error: " + throwable.getMessage());
                                    showAddressError();
                                }
                            });
                        }
                    }
                });*/
    }

    private fun showSearchResultsScreen() {
        val tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences))
        if (TradeUtils.isLocalTrade(tradeType)) {

            val latitude = editLatitude!!.text.toString()
            val longitude = editLongitude!!.text.toString()

            if (TextUtils.isEmpty(latitude) || TextUtils.isEmpty(longitude)) {
                SearchUtils.setSearchLatitude(sharedPreferences, 0.0)
                SearchUtils.setSearchLongitude(sharedPreferences, 0.0)
            } else if (!SearchUtils.coordinatesValid(latitude, longitude)) {
                // TODO dialog
                //showAlertDialog(new AlertDialogEvent("Error", "Invalid longitude or latitude entered. Latitude should be between -90 and 90.  Longitude should be between -180 and 180."));
                return
            } else if (SearchUtils.coordinatesValid(latitude, longitude)) {
                SearchUtils.setSearchLatitude(sharedPreferences, Doubles.convertToDouble(latitude))
                SearchUtils.setSearchLongitude(sharedPreferences, Doubles.convertToDouble(longitude))
            }

            val lat = SearchUtils.getSearchLatitude(sharedPreferences)
            val lon = SearchUtils.getSearchLongitude(sharedPreferences)
            if (lon == 0.0 && lat == 0.0) {
                // TODO dialog
                //showAlertDialog(new AlertDialogEvent("Error", "To search local advertisers, enter valid latitude and longitude values."));
                return
            }
        }
        val intent = SearchResultsActivity.createStartIntent(activity!!)
        startActivity(intent)
    }

    private fun closeView() {
        if (isAdded) {
            toast(getString(R.string.toast_search_canceled))
            // TODO finish activity
        }
    }

    // ------  LOCATION SERVICES ---------

    fun checkLocationEnabled() {
        Timber.d("checkLocationEnabled")
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

        showProgressDialog(getString(R.string.dialog_progress_location), true)

        val request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100)

        val locationProvider = ReactiveLocationProvider(activity)
        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), Constants.REQUEST_PERMISSIONS)
            return
        }
        /*geoLocationSubscription = locationProvider.getUpdatedLocation(request)
                .timeout(20000, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(new Func1<Throwable, Observable<Location>>() {
                    @Override
                    public Observable<Location> call(Throwable throwable) {
                        return Observable.just(null);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("geoLocationSubscription lookup subscription safely unsubscribed");
                    }
                })
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(final Location location) {
                        if (location != null) {
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        reverseLocationLookup(location);
                                    }
                                });
                            }
                        } else {
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getLocationFromLocationManager();
                                    }
                                });
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getLocationFromLocationManager();
                                }
                            });
                        }
                    }
                });*/
    }

    // TODO move these to base fragment

    /**
     * Used as a backup location service in case the first one fails, which
     * switch to using the LocationManager instead.
     */
    private fun getLocationFromLocationManager() {
        Timber.d("getLocationFromLocationManager")
        locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_COARSE
        try {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    if (location != null) {
                        locationManager.removeUpdates(this)
                        reverseLocationLookup(location)
                    } else {
                        hideProgressDialog()
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                    Timber.d("onStatusChanged: $status")
                }

                override fun onProviderEnabled(provider: String) {
                    Timber.d("onProviderEnabled")
                }

                override fun onProviderDisabled(provider: String) {
                    Timber.d("onProviderDisabled")
                }
            }
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 100, 5f, locationListener)
        } catch (e: IllegalArgumentException) {
            hideProgressDialog()
            Timber.e("Location manager could not use network provider", e)
            // TODO dialog
            //showAlertDialog(new AlertDialogEvent(getString(R.string.error_location_title), getString(R.string.error_location_message)));
        } catch (e: SecurityException) {
            if (isAdded) {
                hideProgressDialog()
                Timber.e("Location manager could not use network provider", e)
                // TODO dialog
                //showAlertDialog(new AlertDialogEvent(getString(R.string.error_location_title), getString(R.string.error_location_message)));
            }
        }

    }

    /**
     * Used from the location manager to do a reverse lookup of the address from the coordinates.
     *
     * @param location
     */
    private fun reverseLocationLookup(location: Location?) {
        val locationProvider = ReactiveLocationProvider(activity)
        /*locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Address lookup subscription safely unsubscribed");
                    }
                })
                .map(new Func1<List<Address>, Address>() {
                    @Override
                    public Address call(List<Address> addresses) {
                        return (addresses != null && !addresses.isEmpty()) ? addresses.get(0) : null;
                    }
                })
                .subscribe(new Action1<Address>() {
                    @Override
                    public void call(final Address address) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    if (address == null) {
                                        Timber.d("Address reverseLocationLookup error");
                                        showAddressError();
                                    } else {
                                        saveAddress(address);
                                        displayAddress(address);
                                    }
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Timber.e("reverseLocationLookup: " + throwable.getMessage());
                                    hideProgressDialog();
                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_address_lookup_title), getString(R.string.error_address_lookup_description)));
                                }
                            });
                        }
                    }
                });*/
    }

    private fun showNoLocationServicesWarning() {
        // TODO dialog
        /* showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_location_services_title), getString(R.string.warning_no_location_active)), new Action0() {
            @Override
            public void call() {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(myIntent, REQUEST_CHECK_SETTINGS);
            }
        }, new Action0() {
            @Override
            public void call() {
                closeView();
            }
        });*/
    }

    companion object {

        private val EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS"
        private val EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE"
        val REQUEST_CHECK_SETTINGS = 0
        val REQUEST_GOOGLE_PLAY_SERVICES = 1972

        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }
}