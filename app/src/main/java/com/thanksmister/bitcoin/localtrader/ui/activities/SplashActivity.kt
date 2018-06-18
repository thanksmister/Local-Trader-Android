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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.os.Bundle

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity

import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SplashViewModel
import timber.log.Timber
import javax.inject.Inject

@BaseActivity.RequiresAuthentication
class SplashActivity : BaseActivity() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: SplashViewModel

    private val syncIntentFilter = IntentFilter(SyncAdapter.ACTION_SYNC)

    private val syncBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val syncActionType = intent.getStringExtra(SyncAdapter.EXTRA_ACTION_TYPE)
            val extraErrorMessage = intent.getStringExtra(SyncAdapter.EXTRA_ERROR_MESSAGE)
            val extraErrorCode = intent.getIntExtra(SyncAdapter.EXTRA_ERROR_CODE, 0)
            val extraErrorStatus = intent.getIntExtra(SyncAdapter.EXTRA_ERROR_CODE, 0)
            handleStartSync(syncActionType, extraErrorMessage, extraErrorCode, extraErrorStatus)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_splash)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SplashViewModel::class.java)
        observeViewModel(viewModel)
        lifecycle.addObserver(dialogUtils)

        if (!preferences.hasCredentials()) {
            val intent = Intent(this, PromoActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } else if (!preferences.hasUserInfo()) {
            // load the user and rest of the data
            Timber.d("Does not have user info!!")
            viewModel.getMyself()
        } else if (preferences.firstTime()) {
            preferences.forceUpdates(false)
            SyncUtils.requestSyncNow(this)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(syncBroadcastReceiver, syncIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(syncBroadcastReceiver)
    }

    private fun observeViewModel(viewModel: SplashViewModel) {
        viewModel.getNavigateNextView().observe(this, Observer {response ->
            if(response == true) {

            }
        })
        viewModel.getAlertMessage().observe(this, Observer {message ->
            dialogUtils.showAlertDialog(this@SplashActivity, message!!)
        })
    }

    override fun handleNetworkDisconnect() {
        dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_title), getString(R.string.error_no_internet))
    }

    protected fun handleStartSync(syncActionType: String, extraErrorMessage: String, extraErrorCode: Int, extraErrorStatus: Int) {
        Timber.d("handleSyncEvent: " + syncActionType)
        when (syncActionType) {
            SyncAdapter.ACTION_TYPE_START -> {
            }
            SyncAdapter.ACTION_TYPE_COMPLETE -> {
                preferences.firstTime(false)
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
            SyncAdapter.ACTION_TYPE_CANCELED -> {
            }
            SyncAdapter.ACTION_TYPE_ERROR -> {
                Timber.e("Sync error: " + extraErrorMessage + "code: " + extraErrorCode)
                dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_sync), object:DialogInterface.OnClickListener{
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        finish()
                    }
                })
            }
        }
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }
}