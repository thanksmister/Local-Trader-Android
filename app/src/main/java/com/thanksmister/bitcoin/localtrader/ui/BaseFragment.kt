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

package com.thanksmister.bitcoin.localtrader.ui

import android.os.Bundle
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils

import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * Base fragment which performs injection using the activity object graph of its parent.
 */
abstract class BaseFragment : DaggerFragment() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var dialogUtils: DialogUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(dialogUtils)
    }

    protected fun toast(messageId: Int) {
        if (isAdded && activity != null)
            (activity as BaseActivity).toast(messageId)
    }

    protected fun toast(message: String) {
        if (isAdded && activity != null)
            (activity as BaseActivity).toast(message)
    }

    @Deprecated ("Moved to dialog utils")
    fun showProgressDialog(message: String, cancelable: Boolean) {
        if (isAdded && activity != null)
            (activity as BaseActivity).showProgressDialog(message, cancelable)
    }

    @Deprecated ("Moved to dialog utils")
    fun showProgressDialog(message: String) {
        if (isAdded && activity != null)
            (activity as BaseActivity).showProgressDialog(message, false)
    }

    @Deprecated ("Moved to dialog utils")
    fun hideProgressDialog() {
        if (isAdded && activity != null)
            (activity as BaseActivity).hideProgressDialog()
    }
}