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
import android.widget.Toast
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.services.SyncAdapter
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.LoginViewModel
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SplashViewModel
import timber.log.Timber
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_splash)

        if (!preferences.hasCredentials()) {
            val intent = Intent(this, PromoActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } else {
            viewModel = ViewModelProviders.of(this, viewModelFactory).get(SplashViewModel::class.java)
            observeViewModel(viewModel)
        }
    }

    override fun handleNetworkDisconnect() {
        showAlertDialog(getString(R.string.error_no_internet))
        viewModel.onCleared()
    }

    private fun observeViewModel(viewModel: SplashViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if(message != null)
                dialogUtils.showAlertDialog(this@SplashActivity, message)
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            Toast.makeText(this@SplashActivity, message, Toast.LENGTH_LONG).show()
        })
        viewModel.getSyncing().observe(this, Observer {
            if(it == false) {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        })
        viewModel.startSync()
    }

    companion object {
        fun createStartIntent(context: Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }
}