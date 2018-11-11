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
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.thanksmister.bitcoin.localtrader.architecture.AlertMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.persistence.AdvertisementsDao
import com.thanksmister.bitcoin.localtrader.persistence.MethodsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class AdvertisementsViewModel @Inject
constructor(application: Application, private val advertisementsDao: AdvertisementsDao, private val methodsDao: MethodsDao,
            private val preferences: Preferences) : AndroidViewModel(application) {

    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val adId = MutableLiveData<Int>()
    private val advertisement = MutableLiveData<Advertisement>()
    private val disposable = CompositeDisposable()


    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getAdId(): LiveData<Int> {
        return adId
    }

    private fun setAdId(value: Int) {
        this.adId.value = value
    }

    fun getAdvertisement(): LiveData<Advertisement> {
        return advertisement
    }

    private fun setAdvertisement(value: Advertisement) {
        this.advertisement.value = value
    }

    init {
    }

    public override fun onCleared() {
        Timber.d("onCleared")
        //prevents memory leaks by disposing pending observable objects
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
    }

    private fun showAlertMessage(message: String?) {
        Timber.d("showAlertMessage")
        alertText.value = message
    }

    private fun showToastMessage(message: String?) {
        Timber.d("showToastMessage")
        toastText.value = message
    }

    /**
     * Get the item.
     * @return a [Flowable] that will emit every time the item have been updated.
     */
    fun getAdvertisement(adId: Int):Flowable<Advertisement> {
        return advertisementsDao.getItemById(adId)
    }

    fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    /**
     * Get the item.
     * @return a [Flowable] that will emit every time the item have been updated.
     */
    fun getAdvertisments(): Flowable<List<Advertisement>> {
        return advertisementsDao.getItems()
                .filter {items -> items.isNotEmpty()}
    }

    fun fetchAdvertisment(adId: Int) {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.getAdvertisement(adId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertAdvertisement(it)
                }, {
                    error -> Timber.e("Error" + error.message)
                    showAlertMessage(error.message)
                }))
    }

    fun fetchAdvertisments() {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.advertisements
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertAdvertisements(it)
                }, {
                    error -> Timber.e("Error getting user" + error.message)
                    showAlertMessage(error.message)
                }))
    }

    private fun insertAdvertisements(items: List<Advertisement>) {
        disposable.add(Completable.fromAction {
            advertisementsDao.insertItems(items)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("User insert error" + error.message)}))
    }

    private fun insertAdvertisement(item: Advertisement) {
        disposable.add(Completable.fromAction {
                advertisementsDao.insertItem(item)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("User insert error" + error.message)}))
    }
}