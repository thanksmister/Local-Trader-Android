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

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.network.sync.SyncUtils
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SplashViewModel
import timber.log.Timber
import javax.inject.Inject


class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var viewModel: SplashViewModel

    private var connectionLiveData: ConnectionLiveData? = null
    private var syncComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_splash)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SplashViewModel::class.java)

        if (!preferences.hasCredentials()) {
            viewModel.resetPreferences()
            val intent = Intent(this, PromoActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            observeViewModel(viewModel)
            lifecycle.addObserver(viewModel)
        }
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        } else {
            finish()
        }
        System.exit(0)
    }

    override fun onStart() {
        super.onStart()
        connectionLiveData = ConnectionLiveData(this@SplashActivity)
        connectionLiveData?.observe(this, Observer { connected ->
            if(!connected!!) {
                showProgress(false)
                dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_network_retry) , DialogInterface.OnClickListener { dialog, which ->
                    showProgress(true)
                    viewModel.startSync()
                }, DialogInterface.OnClickListener { dialog, which ->
                    finish()
                })
            } else {
                dialogUtils.clearDialogs()
            }
        })
    }

    private fun observeViewModel(viewModel: SplashViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                showProgress(false)
                dialogUtils.clearDialogs()
                dialogUtils.showAlertDialog(this@SplashActivity, it,
                        DialogInterface.OnClickListener { _, _ ->
                            onBackPressed()
                        }, DialogInterface.OnClickListener { _, _ ->
                            onBackPressed()
                        })
            }
        })
        viewModel.getNetworkMessage().observe(this, Observer { messageData ->
            if(messageData != null) {
                when {
                    RetrofitErrorHandler.isHttp403Error(messageData.code) -> {
                        showProgress(false)
                        dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_bad_token), DialogInterface.OnClickListener { dialog, which ->
                            logOut()
                        })
                    }
                    RetrofitErrorHandler.isNetworkError(messageData.code) -> {
                        dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_network_retry), DialogInterface.OnClickListener { dialog, which ->
                            Timber.d("retry network!!")
                            showProgress(true)
                            viewModel.startSync()
                        }, DialogInterface.OnClickListener { dialog, which ->
                            onBackPressed()
                        })
                    }
                    else -> dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_network_retry), DialogInterface.OnClickListener { dialog, which ->
                        showProgress(false)
                        dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_network_retry), DialogInterface.OnClickListener { dialog, which ->
                            Timber.d("retry network!!")
                            showProgress(true)
                            viewModel.startSync()
                        }, DialogInterface.OnClickListener { dialog, which ->
                            onBackPressed()
                        })
                    })
                }
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if(message != null) {
                dialogUtils.toast(message)
            }
        })
        viewModel.getSyncing().observe(this, Observer {
            if(it == SplashViewModel.SYNC_COMPLETE && !syncComplete) {
                syncComplete = true
                startMainActivity()
            } else if (it == SplashViewModel.SYNC_ERROR) {
                showProgress(false)
                viewModel.onCleared()
                dialogUtils.showAlertDialog(this@SplashActivity, getString(R.string.error_network_retry), DialogInterface.OnClickListener { dialog, which ->
                    Timber.d("retry network!!")
                    showProgress(true)
                    viewModel.onCleared()
                    viewModel.startSync()
                }, DialogInterface.OnClickListener { dialog, which ->
                    onBackPressed()
                })
            }
        })

        showProgress(true)
        viewModel.startSync()
    }

    private fun showProgress(show: Boolean) {
        if(!show) {
            //splashProgressBar.visibility = View.INVISIBLE
        } else {
           // splashProgressBar.visibility = View.VISIBLE
        }
    }

    private fun startMainActivity() {
        val handler = Handler()
        handler.postDelayed(Runnable {
            SyncUtils.createSyncAccount(applicationContext)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }, 5000)
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }
}