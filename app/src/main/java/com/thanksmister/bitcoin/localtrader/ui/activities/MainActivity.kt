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

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.google.zxing.android.IntentIntegrator
import com.kobakei.ratethisapp.RateThisApp
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent
import com.thanksmister.bitcoin.localtrader.network.ApiErrorHandler.CODE_THREE
import com.thanksmister.bitcoin.localtrader.network.ApiErrorHandler.STATUS_403
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.ACTION_SYNC
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.ACTION_TYPE_CANCELED
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.ACTION_TYPE_COMPLETE
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.ACTION_TYPE_ERROR
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.ACTION_TYPE_START
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.EXTRA_ACTION_TYPE
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.EXTRA_ERROR_CODE
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter.Companion.EXTRA_ERROR_MESSAGE
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.MainViewModel
import com.thanksmister.bitcoin.localtrader.ui.fragments.*
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import javax.inject.Inject

@BaseActivity.RequiresAuthentication
class MainActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener, NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemSelectedListener {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: MainViewModel

    private var fragment: Fragment? = null
    
    private val syncBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val syncActionType = intent.getStringExtra(EXTRA_ACTION_TYPE)
            var extraErrorMessage = ""
            if(intent.hasExtra(EXTRA_ERROR_MESSAGE)) {
                extraErrorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
            }
            val extraErrorCode = intent.getIntExtra(EXTRA_ERROR_CODE, 0)
            val extraErrorStatus = intent.getIntExtra(EXTRA_ERROR_CODE, 0)
            handleSyncEvent(syncActionType, extraErrorMessage, extraErrorCode, extraErrorStatus)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupNavigationView()

        mainSwipeLayout.setOnRefreshListener(this)
        mainSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))
        mainSwipeLayout.setProgressViewOffset(false, 48, 186)
        mainSwipeLayout.setDistanceToTriggerSync(250)

        val bitcoinUri = intent.getStringExtra(BITCOIN_URI)
        val authenticated = preferences.hasCredentials()
        if (authenticated) {
            if (bitcoinUri != null) {
                val bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri)
                val bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri)
                startSendFragment(bitcoinAddress, bitcoinAmount)
            }
        }

        if(savedInstanceState == null) {
            fragment = AdvertisementsFragment.newInstance()
            supportFragmentManager.beginTransaction().replace(R.id.main_content_frame, fragment, ADVERTISEMENTS_FRAGMENT).commit()
        }

        // Application rating dialog
        if(preferences.hasCredentials()) {
            val config = RateThisApp.Config(7, 10)
            RateThisApp.init(config)
            RateThisApp.onCreate(this)
            RateThisApp.showRateDialogIfNeeded(this, R.style.DialogTheme)
        }

        navigation.setOnNavigationItemSelectedListener(this)
        mainNavigationView.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        observeViewModel(viewModel)
        lifecycle.addObserver(dialogUtils)
    }

    override fun onResume() {
        super.onResume()
        if ((this as Any).javaClass.isAnnotationPresent(BaseActivity.RequiresAuthentication::class.java)) {
            val authenticated = preferences.hasCredentials()
            if (!authenticated) {
                launchPromoScreen()
                return
            }
        }
        // TODO move to base activity?
        registerReceiver(syncBroadcastReceiver, syncIntentFilter)
        viewModel.getExchangePrice()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    private fun observeViewModel(viewModel: MainViewModel) {
        viewModel.getAlertMessage().observe(this, Observer {message ->
            if(hasNetworkConnectivity()) {
                dialogUtils.showAlertDialog(this@MainActivity, message!!)
            }
        })
        disposable.add(viewModel.getExchangeRate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({rate ->
                    this@MainActivity.runOnUiThread({
                        Timber.d("Exchange rate " + rate.exchangeRate)
                        setHeaderItem(rate.exchangeRate, rate.currency, rate.displayName);
                    })
                }, { error -> Timber.e("Unable to get exchange rate: " + error)}))
    }

    override fun handleNetworkDisconnect() {
        snack(getString(R.string.error_no_internet), false)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(syncBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Timber.e(e.message)
        }
    }

    override fun onRefresh() {
        preferences.forceUpdates(true)
        SyncUtils.requestSyncNow(this@MainActivity)
        handleRefresh()
    }

    private fun onRefreshStart() {
        if (mainSwipeLayout != null) {
            mainSwipeLayout.setRefreshing(true)
        }
    }

    private fun onRefreshStop() {
        if (mainSwipeLayout != null) {
            mainSwipeLayout.setRefreshing(false)
        }
    }

    private fun launchPromoScreen() {
        val intent = Intent(this@MainActivity, PromoActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupNavigationView() {

        val headerView = mainNavigationView.getHeaderView(0)
        val userName = headerView.findViewById<TextView>(R.id.userName)
        val tradeCount = headerView.findViewById<TextView>(R.id.userTradeCount)
        val feedbackScore = headerView.findViewById<TextView>(R.id.userTradeFeedback)

        // TODO move this to view model and get user from database
        userName.text = preferences.userName()
        tradeCount.text = preferences.userTrades()
        feedbackScore.text = preferences.userFeedback()

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                val inputMethodManager = this@MainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(this@MainActivity.currentFocus!!.windowToken, 0)
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> {
                fragment = AdvertisementsFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.main_content_frame, fragment, ADVERTISEMENTS_FRAGMENT).commit()
                return@onNavigationItemSelected true
            }
            R.id.navigation_dashboard -> {
                fragment = ContactsFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.main_content_frame, fragment, CONTACTS_FRAGMENT).commit()
                return@onNavigationItemSelected true
            }
            R.id.navigation_notifications -> {
                fragment = NotificationsFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.main_content_frame, fragment, NOTIFICATIONS_FRAGMENT).commit()
                return@onNavigationItemSelected true
            }
            R.id.navigationItemSearch -> {
                val intent = SearchActivity.createStartIntent(this@MainActivity)
                startActivity(intent)
                return@onNavigationItemSelected true
            }
            R.id.navigationItemSend -> {
                //setContentFragment(DRAWER_SEND)
                return@onNavigationItemSelected true
            }
            R.id.navigationItemReceive -> {
                //setContentFragment(DRAWER_RECEIVE)
                return@onNavigationItemSelected true
            }
            R.id.navigationItemWallet -> {
                //setContentFragment(DRAWER_WALLET)
                return@onNavigationItemSelected true
            }
            R.id.navigationItemAbout -> {
                //setContentFragment(DRAWER_ABOUT)
                onRefreshStop()
            }
            R.id.navigationItemSettings -> {
                val intent = SettingsActivity.createStartIntent(this)
                startActivity(intent)
                return@onNavigationItemSelected true
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return@onNavigationItemSelected true
    }

    private fun startSendFragment(bitcoinAddress: String?, bitcoinAmount: String?) {
        /*fragment = SendFragment.newInstance(bitcoinAddress, bitcoinAmount)

        supportFragmentManager.beginTransaction()
                .replace(R.id.main_content_frame, fragment, SEND_FRAGMENT)
                .commitAllowingStateLoss()*/
        // TODO start activity
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode == REQUEST_SCAN) {
            val scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
            if (scanningResult != null) {
                val bitcoinAddress = WalletUtils.parseBitcoinAddress(scanningResult.contents)
                val bitcoinAmount = WalletUtils.parseBitcoinAmount(scanningResult.contents)
                startSendFragment(bitcoinAddress, bitcoinAmount)
            } else {
                toast(getString(R.string.toast_scan_canceled))
            }
        } else if (requestCode == EditAdvertisementActivity.REQUEST_CODE) {
            if (resultCode == EditAdvertisementActivity.RESULT_UPDATED) {
                onRefresh()
            }
        } else if (requestCode == AdvertisementActivity.REQUEST_CODE) {
            if (resultCode == AdvertisementActivity.RESULT_DELETED) {
                onRefresh()
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent == null || intent.extras == null) return

        val extras = intent.extras
        val type = extras!!.getInt(EXTRA_NOTIFICATION_TYPE, 0)
        if (type == NotificationUtils.NOTIFICATION_TYPE_CONTACT) {
            val id = extras.getString(EXTRA_NOTIFICATION_ID)
            if (id != null) {
                val launchIntent = ContactActivity.createStartIntent(this, id)
                startActivity(launchIntent)
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT) {
            val id = extras.getString(EXTRA_NOTIFICATION_ID)
            if (TextUtils.isEmpty(id)) {
                showAlertDialog(AlertDialogEvent(getString(R.string.error_advertisement), getString(R.string.error_no_advertisement)))
            } else {
                val launchIntent = AdvertisementActivity.createStartIntent(this, id!!)
                startActivity(launchIntent)
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            // TODO start wallet activity
            //setContentFragment(DRAWER_WALLET)
            //mainNavigationView.getMenu().findItem(R.id.navigationItemWallet).setChecked(true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setHeaderItem(rate: String?, currency: String?, exchange: String?) {
        bitcoinTitle.setText(R.string.text_title_market_price)
        if(!TextUtils.isEmpty(rate)) {
            bitcoinPrice.text = "$rate $currency / "  + getString(R.string.btc)
        } else {
            bitcoinPrice.text = "0 $currency  / "  + getString(R.string.btc)
        }
        if(!TextUtils.isEmpty(exchange)) {
            bitcoinValue.text = "$exchange ($currency)"
        } else {
            bitcoinValue.text = "--------"
        }
    }

    // TODO we need a way to queue up all updates to show refresh stop properly
    private fun handleSyncEvent(syncActionType: String?, extraErrorMessage: String, extraErrorCode: Int, errorStatus: Int) {
        when (syncActionType) {
            ACTION_TYPE_START -> {}
            ACTION_TYPE_COMPLETE -> onRefreshStop()
            ACTION_TYPE_CANCELED -> onRefreshStop()
            ACTION_TYPE_ERROR -> {
                Timber.e("Sync Error: $extraErrorMessage code: $extraErrorCode status: $errorStatus")
                onRefreshStop()
                if (extraErrorCode == CODE_THREE || errorStatus == STATUS_403) {
                    dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.error_bad_token), object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            onLoggedOut()
                        }
                    })
                }
            }
        }
    }

    companion object {
        const val ADVERTISEMENTS_FRAGMENT = "com.thanksmister.fragment.ADVERTISEMENTS_FRAGMENT"
        const val CONTACTS_FRAGMENT = "com.thanksmister.fragment.CONTACTS_FRAGMENT"
        const val NOTIFICATIONS_FRAGMENT = "com.thanksmister.fragment.NOTIFICATIONS_FRAGMENT"
        val syncIntentFilter = IntentFilter(ACTION_SYNC)
        const val BITCOIN_URI = "com.thanksmister.extra.BITCOIN_URI"
        const val SEND_FRAGMENT = "com.thanksmister.fragment.SEND_FRAGMENT"
        var EXTRA_NOTIFICATION_ID = "extra_notification_id"
        var EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val DRAWER_DASHBOARD = 0
        const val REQUEST_SCAN = 49374
        fun createStartIntent(context: Context, bitcoinUri: String): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(BITCOIN_URI, bitcoinUri)
            return intent
        }
    }
}