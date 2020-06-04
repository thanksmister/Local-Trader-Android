/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.persistence.CurrenciesDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import io.reactivex.Flowable
import javax.inject.Inject

class SettingsViewModel @Inject
constructor(application: Application, private val currenciesDao: CurrenciesDao,
            private val preferences: Preferences) : BaseViewModel(application) {

    init {
    }

    fun getCurrencies(): Flowable<List<Currency>> {
        return currenciesDao.getItems()
                .filter {items -> items.isNotEmpty()}
    }

    // TODO remove all tables from database
    /*
    Completable.fromAction(new Action() {
        @Override
        public void run() throws Exception {
            getRoomDatabase().clearAllTables();
        }
    }).subscribeOn(getSchedulerProvider().io())
            .observeOn(getSchedulerProvider().ui())
            .subscribe(new Action() {
                @Override
                public void run() throws Exception {
                    Log.d(TAG, "--- clearAllTables(): run() ---");
                    getInteractor().setUserAsLoggedOut();
                    getMvpView().openLoginActivity();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.d(TAG, "--- clearAllTables(): accept(Throwable throwable) ----");
                    Log.d(TAG, "throwable.getMessage(): "+throwable.getMessage());


                }
            });
     */
}