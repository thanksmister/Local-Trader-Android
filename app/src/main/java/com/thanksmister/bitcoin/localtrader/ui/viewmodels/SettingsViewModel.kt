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