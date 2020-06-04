/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui

import android.os.Bundle
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import com.thanksmister.bitcoin.localtrader.utils.disposeProper

import dagger.android.support.DaggerFragment
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

/**
 * Base fragment which performs injection using the activity object graph of its parent.
 */
abstract class BaseFragment : DaggerFragment() {

    val disposable = CompositeDisposable()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var dialogUtils: DialogUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(dialogUtils)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.disposeProper()
    }
}