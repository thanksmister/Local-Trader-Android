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