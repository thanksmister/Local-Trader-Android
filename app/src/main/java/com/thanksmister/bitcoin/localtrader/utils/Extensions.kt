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

package com.thanksmister.bitcoin.localtrader.utils

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers


/**
 * Created by Michael Ritchie on 2/17/19.
 */
operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

fun Disposable.disposeProper() = this.let {
    if(it.isDisposed.not()) {
        it.dispose()
    }
}

fun Completable.applySchedulersComputation(): Completable {
    return subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
}

fun Completable.applySchedulers(): Completable {
    return subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.applySchedulers(): Observable<T> {
    return subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.applySchedulersIo(): Observable<T> {
    return subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.applySchedulersComputation(): Observable<T> {
    return subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
}


fun <T> Flowable<T>.applySchedulers(): Flowable<T> {
    return subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
}

fun <T> Flowable<T>.applySchedulersIo(): Flowable<T> {
    return subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}

fun <T> Single<T>.applySchedulersComputation(): Single<T> {
    return subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
}

fun <T> Single<T>.applySchedulers(): Single<T> {
    return subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}
