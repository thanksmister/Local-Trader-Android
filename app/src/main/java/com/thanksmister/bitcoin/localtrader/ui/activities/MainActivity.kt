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
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.text.TextUtils
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.kobakei.ratethisapp.RateThisApp
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.api.model.User
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.fragments.*
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.DashboardViewModel
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.*
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

class MainActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener, NavigationView.OnNavigationItemSelectedListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: DashboardViewModel

    private var connectionLiveData: ConnectionLiveData? = null
    private val disposable = CompositeDisposable()

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainContainer, AdvertisementsFragment.newInstance(), ADVERTISEMENTS_FRAGMENT).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainContainer, ContactsFragment.newInstance(), CONTACTS_FRAGMENT).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainContainer, NotificationsFragment.newInstance(), NOTIFICATIONS_FRAGMENT).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.error_device_softare_description),
                    DialogInterface.OnClickListener { dialog, which ->
                        finish()
                    })
            return
        }

        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            //supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            //supportActionBar!!.setHomeButtonEnabled(true)
            supportActionBar!!.title = ""
        }

        val bitcoinUri = intent.getStringExtra(BITCOIN_URI)
        val authenticated = preferences.hasCredentials()
        if (authenticated) {
            if (bitcoinUri != null) {
                val bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri)
                val bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri)
                startSendActivity(bitcoinAddress, bitcoinAmount)
            }
        }

        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.menu_action_blockchain, R.string.menu_action_advertise)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        supportFragmentManager.beginTransaction().replace(R.id.mainContainer, AdvertisementsFragment.newInstance(), ADVERTISEMENTS_FRAGMENT).commit()

        mainSwipeLayout.setOnRefreshListener(this)
        mainSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))
        mainSwipeLayout.setProgressViewOffset(false, 48, 186)
        mainSwipeLayout.setDistanceToTriggerSync(250)

        // Application rating dialog
        val config = RateThisApp.Config(7, 10)
        RateThisApp.init(config)
        RateThisApp.onCreate(this)
        RateThisApp.showRateDialogIfNeeded(this, R.style.CustomAlertDialog)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DashboardViewModel::class.java)
        observeViewModel(viewModel)
    }

    override fun onStart() {
        super.onStart()
        connectionLiveData = ConnectionLiveData(this@MainActivity)
        connectionLiveData?.observe(this, Observer { connected ->
            if(!connected!!) {
                Toast.makeText(this@MainActivity, getString(R.string.error_network_disconnected), Toast.LENGTH_SHORT).show()
                onRefreshStop()
            } else {
                // na-da
            }
        })
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout != null)
                    drawerLayout!!.openDrawer(GravityCompat.START)
                return true
            }
            R.id.action_search -> {
                startActivity(SearchActivity.createStartIntent(this@MainActivity))
                return false
            }
            R.id.action_trades -> {
                //showTradesScreen()
                return true
            }
            R.id.action_advertise -> {
                createAdvertisementScreen()
                return true
            }
            R.id.action_clear_notifications -> {
               // TODO mark notifications as read
                return true
            }
            R.id.action_scan -> {
                launchScanner()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigationItemSearch -> {
                onRefreshStop()
                startActivity(SearchActivity.createStartIntent(this@MainActivity))
            }
            R.id.navigationItemSend -> {
                onRefreshStop()
                startActivity(SendActivity.createStartIntent(this@MainActivity, null, null))
            }
            R.id.navigationItemReceive -> {
                onRefreshStop()
                startActivity(RequestActivity.createStartIntent(this@MainActivity))
            }
            R.id.navigationItemWallet -> {
                onRefreshStop()
                startActivity(WalletActivity.createStartIntent(this@MainActivity))
            }
            R.id.navigationItemAbout -> {
                onRefreshStop()
                startActivity(AboutActivity.createStartIntent(this@MainActivity))
            }
            R.id.navigationItemSettings -> {
                onRefreshStop()
                startActivity(SettingsActivity.createStartIntent(this@MainActivity))
            }
            else -> {
                onRefreshStop()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onRefresh() {
        viewModel.getDashboardData()
    }

    private fun onRefreshStop() {
        mainSwipeLayout.isRefreshing = false
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

    private fun observeViewModel(viewModel: DashboardViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if(message?.message != null) {
                dialogUtils.showAlertDialog(this@MainActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if(message != null)
                dialogUtils.showAlertDialog(this@MainActivity, message)
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        })
        toast(getString(R.string.toast_refreshing_data))
        viewModel.getDashboardData()
        disposable.add(
                viewModel.getExchange()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( { exchange ->
                            if(exchange != null) {
                                runOnUiThread {
                                    setHeaderItem(exchange.rate, exchange.currency, exchange.name);
                                }
                            }
                            onRefreshStop()
                        }, { error ->
                            Timber.e("Exchange error: $error")
                        }))
        disposable.add (
                viewModel.getUser()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( { user ->
                            if(user != null) {
                                runOnUiThread {
                                    setupNavigationView(user)
                                }
                            }
                            onRefreshStop()
                        }, { error ->
                            Timber.e("User error: $error")
                        }))
    }

    override fun launchScanner() {
        onRefreshStop()
        super.launchScanner()
    }

    private fun createAdvertisementScreen() {
        dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.dialog_edit_advertisements),
                DialogInterface.OnClickListener { _, _ ->
                    try {
                        startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                    } catch (ex: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                    }
                }, DialogInterface.OnClickListener { _, _ ->
            // na-da
        })
    }

    private fun launchPromoScreen() {
        val intent = Intent(this@MainActivity, PromoActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupNavigationView(user: User) {
        try {
            val headerView = navigationView.getHeaderView(0)
            if (headerView != null) {
                //val userName = headerView.findViewById<TextView>(R.id.userName)
                //val tradeCount = headerView.findViewById<TextView>(R.id.userTradeCount)
                //val feedbackScore = headerView.findViewById<TextView>(R.id.userTradeFeedback)
                drawerUserName.text = user.username
                drawerTradeCount.text = user.confirmedTradeCountText
                drawerTradeFeedback.text = user.feedbackScore
            }
        } catch (e: Exception) {
            System.out.print(e.message)
            Timber.d(e.message)
        }
    }

    private fun startSendActivity(bitcoinAddress: String?, bitcoinAmount: String?) {
        startActivity(SendActivity.createStartIntent(this, bitcoinAddress, bitcoinAmount))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null || intent.extras == null) {
            return
        }
        // TODO we need to add to notifications all the deep linking Id's
        val extras = intent.extras
        val type = extras!!.getInt(EXTRA_NOTIFICATION_TYPE, 0)
        if (type == NotificationUtils.NOTIFICATION_TYPE_CONTACT) {
            onRefreshStop()
            val id = extras.getString(EXTRA_NOTIFICATION_ID)
            if (id != null) {
                val launchIntent = ContactActivity.createStartIntent(this, id.toInt())
                startActivity(launchIntent)
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_ADVERTISEMENT) {
            onRefreshStop()
            val id = extras.getString(EXTRA_NOTIFICATION_ID)
            if (TextUtils.isEmpty(id)) {
                showAlertDialog(getString(R.string.error_advertisement), getString(R.string.error_no_advertisement))
            } else {
                val launchIntent = AdvertisementActivity.createStartIntent(this, Integer.parseInt(id))
                startActivity(launchIntent)
            }
        } else if (type == NotificationUtils.NOTIFICATION_TYPE_BALANCE) {
            onRefreshStop()
            startActivity(WalletActivity.createStartIntent(this@MainActivity))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setHeaderItem(rate: String?, currency: String?, exchange: String?) {
        headerBitcoinTitle.setText(R.string.text_title_market_price)
        if(rate != null && currency != null && exchange != null) {
            headerBitcoinPrice.text = rate + " " + currency + "/" + getString(R.string.btc)
            headerBitcoinValue.text = "$exchange ($currency)"
        }
    }

    companion object {
        const val ADVERTISEMENTS_FRAGMENT = "com.thanksmister.fragment.ADVERTISEMENTS_FRAGMENT"
        const val CONTACTS_FRAGMENT = "com.thanksmister.fragment.CONTACTS_FRAGMENT"
        const val NOTIFICATIONS_FRAGMENT = "com.thanksmister.fragment.NOTIFICATIONS_FRAGMENT"
        const val BITCOIN_URI = "com.thanksmister.extra.BITCOIN_URI"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        fun createStartIntent(context: Context, bitcoinUri: String): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(BITCOIN_URI, bitcoinUri)
            return intent
        }
    }
}