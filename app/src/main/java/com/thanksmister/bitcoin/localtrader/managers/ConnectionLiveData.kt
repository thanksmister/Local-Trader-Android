/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.managers

import androidx.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionLiveData(private val context: Context) : MutableLiveData<Boolean>() {

    private var connCallbackListener: ConnCallbackListener? = null

    init {
        connCallbackListener = object : ConnCallbackListener {
            override fun networkConnect() {
                value = true
            }
            override fun networkDisconnect() {
                value = false
            }
        }
    }

    override fun onActive() {
        super.onActive()
        context.registerReceiver(connectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(connectionReceiver)
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetworkInfo = connectivityManager.activeNetworkInfo
            if (currentNetworkInfo != null && currentNetworkInfo.isConnected) {
                Timber.d("Network Connected")
                hasNetwork.set(true)
                value = true
            } else if (hasNetwork.get()) {
                Timber.d("Network Disconnected")
                hasNetwork.set(false)
                value = false
            }
        }
    }

    interface ConnCallbackListener {
        fun networkConnect()
        fun networkDisconnect()
    }

    companion object {
        var hasNetwork = AtomicBoolean(true)
    }
}
