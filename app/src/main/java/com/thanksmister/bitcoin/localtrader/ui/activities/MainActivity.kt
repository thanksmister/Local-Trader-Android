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

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.kobakei.ratethisapp.RateThisApp
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.databinding.ActivityMainBinding
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType
import com.thanksmister.bitcoin.localtrader.network.api.model.User
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.network.sync.SyncUtils
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.fragments.AdvertisementsFragment
import com.thanksmister.bitcoin.localtrader.ui.fragments.ContactsFragment
import com.thanksmister.bitcoin.localtrader.ui.fragments.NotificationsFragment
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.DashboardViewModel
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SplashViewModel
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import timber.log.Timber
import javax.inject.Inject

class MainActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener, NavigationView.OnNavigationItemSelectedListener {

    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: DashboardViewModel

    private var connectionLiveData: ConnectionLiveData? = null

    private val advertisementFragment: AdvertisementsFragment by lazy {
        AdvertisementsFragment.newInstance()
    }

    private val contactsFragment: ContactsFragment by lazy {
        ContactsFragment.newInstance()
    }

    private val notificationsFragment: NotificationsFragment by lazy {
        NotificationsFragment.newInstance()
    }

    private var activeFragment: Fragment = advertisementFragment

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                supportFragmentManager.beginTransaction().hide(activeFragment).show(advertisementFragment).commitAllowingStateLoss()
                activeFragment = advertisementFragment
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                supportFragmentManager.beginTransaction().hide(activeFragment).show(contactsFragment).commitAllowingStateLoss()
                activeFragment = contactsFragment
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                supportFragmentManager.beginTransaction().hide(activeFragment).show(notificationsFragment).commitAllowingStateLoss()
                activeFragment = notificationsFragment
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")

        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        val bitcoinUri = intent.getStringExtra(BITCOIN_URI)
        val authenticated = preferences.hasCredentials()
        if (authenticated) {
            if (bitcoinUri != null) {
                val bitcoinAddress = WalletUtils.parseBitcoinAddress(bitcoinUri)
                val bitcoinAmount = WalletUtils.parseBitcoinAmount(bitcoinUri)
                startSendActivity(bitcoinAddress, bitcoinAmount)
            }
        }

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.menu_action_blockchain, R.string.menu_action_advertise)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)
        binding.navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        supportFragmentManager.beginTransaction().add(R.id.mainContainer, notificationsFragment, NOTIFICATIONS_FRAGMENT).hide(notificationsFragment).commit();
        supportFragmentManager.beginTransaction().add(R.id.mainContainer, contactsFragment, CONTACTS_FRAGMENT).hide(contactsFragment).commit();
        supportFragmentManager.beginTransaction().add(R.id.mainContainer, advertisementFragment, ADVERTISEMENTS_FRAGMENT).commit();

        binding.mainSwipeLayout.setOnRefreshListener(this)
        binding.mainSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))

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
            connected?.let {
                if (!it) {
                    onRefreshStop()
                    dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.error_network_retry),
                            DialogInterface.OnClickListener { dialog, which ->
                                dialogUtils.toast(getString(R.string.toast_refreshing_data))
                                viewModel.getDashboardData()
                            }, DialogInterface.OnClickListener { dialog, which ->
                    })
                }
            }
        })

        SyncUtils.requestSyncNow(applicationContext)
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        } else {
            finish()
        }
        System.exit(0);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard, menu)
        val itemLen = menu.size()
        for (i in 0 until itemLen) {
            val drawable = menu.getItem(i).icon
            if (drawable != null) {
                drawable.mutate()
                drawable.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
            R.id.action_search -> {
                startActivity(SearchActivity.createStartIntent(this@MainActivity))
                return false
            }
            R.id.action_trades -> {
                startActivity(ContactsActivity.createStartIntent(this@MainActivity, DashboardType.RELEASED))
                return true
            }
            R.id.action_advertise -> {
                createAdvertisementScreen()
                return true
            }
            R.id.action_clear_notifications -> {
                viewModel.markNotificationsRead()
                return true
            }
            R.id.action_scan -> {
                startActivity(ScanQrCodeActivity.createStartIntent(this@MainActivity))
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

    private fun onRefreshStart() {
        binding.mainSwipeLayout.isRefreshing = true
    }

    private fun onRefreshStop() {
        binding.mainSwipeLayout.isRefreshing = false
    }

    private fun observeViewModel(viewModel: DashboardViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { messageData ->
            onRefreshStop()
            messageData?.let {
                Timber.d("Error Message ${it.message}")
                when {
                    RetrofitErrorHandler.isHttp403Error(it.code)  -> {
                        dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.error_bad_token), DialogInterface.OnClickListener { dialog, which ->
                            logOut()
                        })
                    }
                    RetrofitErrorHandler.isNetworkError(it.code) ||
                    RetrofitErrorHandler.isHttp503Error(it.code) -> {
                        dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.error_network_retry), DialogInterface.OnClickListener { dialog, which ->
                            dialogUtils.toast(getString(R.string.toast_refreshing_data))
                            viewModel.getDashboardData()
                        }, DialogInterface.OnClickListener { _, _ ->
                            onRefreshStop()
                        })
                    }
                    RetrofitErrorHandler.isHttp400Error(messageData.code) -> {
                        dialogUtils.showAlertDialog(this@MainActivity, it.message, DialogInterface.OnClickListener { dialog, which ->
                            Timber.e("Bad request: ${it.message}")
                            onRefreshStop()
                        }, DialogInterface.OnClickListener { _, _ ->
                            onRefreshStop()
                        })
                    }
                    else -> dialogUtils.showAlertDialog(this@MainActivity, it.message, DialogInterface.OnClickListener { dialog, which ->
                        onRefreshStop()
                    }, DialogInterface.OnClickListener { _, _ ->
                        onRefreshStop()
                    })
                }
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                onRefreshStop()
                dialogUtils.showAlertDialog(this@MainActivity, it)
            }

        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.toast(it)
            }
        })
        viewModel.getSyncing().observe(this, Observer {
            if (it == DashboardViewModel.SYNC_COMPLETE) {
                onRefreshStop()
            } else if (it == DashboardViewModel.SYNC_STARTED) {
                onRefreshStart()
            } else if (it == DashboardViewModel.SYNC_ERROR) {
                onRefreshStop()
                viewModel.onCleared()
            }
        })

        disposable += viewModel.getExchange()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ exchange ->
                    exchange?.let {
                        setHeaderItem(it.rate, it.currency, it.name)
                    }
                }, { error ->
                    Timber.e("Exchange error: $error")
                })
        disposable += viewModel.getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ user ->
                    user?.let {
                        setupNavigationView(it)
                    }
                    onRefreshStop()
                }, { error ->
                    Timber.e("User error: $error")
                })

        dialogUtils.toast(getString(R.string.toast_refreshing_data))
        viewModel.getDashboardData()
    }

    private fun createAdvertisementScreen() {
        dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.dialog_edit_advertisements),
                DialogInterface.OnClickListener { _, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                    } catch (ex: Exception) {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                    }
                }, DialogInterface.OnClickListener { _, _ ->
            // na-da
        })
    }

    private fun setupNavigationView(user: User) {
        binding.navigationView.drawerHeaderView?.let {
            it.drawerUserName.text = user.username
            it.drawerTradeCount.text = user.confirmedTradeCountText
            it.drawerTradeFeedback.text = user.feedbackScore
        }
    }

    private fun startSendActivity(bitcoinAddress: String?, bitcoinAmount: String?) {
        startActivity(SendActivity.createStartIntent(this, bitcoinAddress, bitcoinAmount))
    }

    @SuppressLint("SetTextI18n")
    private fun setHeaderItem(rate: String?, currency: String?, exchange: String?) {
        headerBitcoinTitle.setText(R.string.text_title_market_price)
        if (!rate.isNullOrEmpty() && !currency.isNullOrEmpty() && !exchange.isNullOrEmpty()) {
            binding.headerBitcoinPrice.text = rate + " " + currency + "/" + getString(R.string.btc)
            binding.headerBitcoinValue.text = "$exchange ($currency)"
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