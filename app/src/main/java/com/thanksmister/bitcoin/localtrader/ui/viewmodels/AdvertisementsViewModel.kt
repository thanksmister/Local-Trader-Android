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
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.architecture.AlertMessage
import com.thanksmister.bitcoin.localtrader.architecture.MessageData
import com.thanksmister.bitcoin.localtrader.architecture.NetworkMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.AdvertisementsDao
import com.thanksmister.bitcoin.localtrader.persistence.MethodsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.Parser
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

class AdvertisementsViewModel @Inject
constructor(application: Application,
            private val advertisementsDao: AdvertisementsDao,
            private val methodsDao: MethodsDao,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val advertisementDeleted = MutableLiveData<Boolean>()
    private val advertisementUpdated = MutableLiveData<Boolean>()
    private val advertisement = MutableLiveData<Advertisement>()
    private var fetcher: LocalBitcoinsFetcher? = null

    fun getAdvertisementUpdated(): LiveData<Boolean> {
        return advertisementUpdated
    }

    private fun setAdvertisementUpdated(value: Boolean) {
        this.advertisementUpdated.value = value
    }

    fun getAdvertisementDeleted (): LiveData<Boolean> {
        return advertisementDeleted
    }

    private fun setAdvertisementDeleted (value: Boolean) {
        this.advertisementDeleted.value = value
    }

    fun getAdvertisement(): LiveData<Advertisement> {
        return advertisement
    }

    private fun setAdvertisement(value: Advertisement) {
        this.advertisement.value = value
    }

    inner class AdvertisementsData {
        var advertisements = emptyList<Advertisement>()
        var methods = emptyList<Method>()
    }

    inner class AdvertisementData {
        var advertisement = Advertisement()
        var method = Method()
    }

    inner class AdvertiserData {
        var advertisement = Advertisement()
        var method = Method()
    }

    init {
        setAdvertisementUpdated(false)
        setAdvertisementDeleted(false)
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getAdvertisementData(adId: Int): Flowable<AdvertisementData> {
        Timber.d("getAdvertisementData $adId")
        return Flowable.combineLatest(getAdvertisement(adId), getMethods(),
                BiFunction { advertisement, methods  ->
                    val data = AdvertisementData()
                    data.advertisement = advertisement
                    val method = TradeUtils.getMethodForAdvertisement(advertisement, methods)
                    if (method != null) {
                        data.method = method
                    }
                    data
                })
    }

    private fun getAdvertisement(adId: Int):Flowable<Advertisement> {
        return advertisementsDao.getItemById(adId)
    }

    private fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    fun getAdvertisementsData(): Flowable<AdvertisementsData> {
        return Flowable.combineLatest(getAdvertisements(), getMethods(),
                BiFunction { advertisements, methods  ->
                    val data = AdvertisementsData()
                    data.advertisements = advertisements
                    data.methods = methods
                    data
                })
    }

    private fun getAdvertisements(): Flowable<List<Advertisement>> {
        return advertisementsDao.getItems()
                .filter {items -> items.isNotEmpty()}
    }

    fun fetchAdvertisement(adId: Int) {
        Timber.d("fetchAdvertisement $adId")
        disposable.add(fetcher!!.getAdvertisement(adId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    if(!it.isEmpty()) {
                        insertAdvertisement(it[0])
                    }
                }, {
                    error ->
                    Timber.e("Error " + error.message)
                    Timber.e("error is SocketTimeoutException " + (error is SocketTimeoutException))
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else if (error is SocketTimeoutException) {
                        Timber.e("SocketTimeOut: ${error.message}")
                    } else {
                        showAlertMessage(error.message)
                    }
                }))
    }

    fun fetchAdvertiser(adId: Int): Observable<AdvertiserData> {
        return Observable.combineLatest(fetcher!!.getAdvertisement(adId), getMethods().toObservable(),
                BiFunction { advertisements, methods  ->
                    val data = AdvertiserData()
                    if(!advertisements.isEmpty()) {
                        val advertisement = advertisements[0]
                        data.advertisement = advertisement
                        val method = TradeUtils.getMethodForAdvertisement(advertisement, methods)
                        if (method != null) {
                            data.method = method
                        }
                    }
                    data
                })
    }

    fun deleteAdvertisement(advertisement: Advertisement) {
        disposable.add(fetcher!!.deleteAdvertisement(advertisement.adId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    deleteAdvertisementItem(advertisement)
                    setAdvertisementDeleted(true)
                }, {
                    error -> Timber.e("Error deleting advertisement  ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                }))
    }

    fun updateAdvertisement(advertisement: Advertisement) {
        disposable.add(fetcher!!.updateAdvertisement(advertisement)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    //val response = Parser.parseDataMessage(it.toString(), defaultResponse)
                    insertAdvertisement(advertisement)
                    setAdvertisementUpdated(true)
                }, {
                    error -> Timber.e("Error updating advertisement  ${error.message}")
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                }))
    }

    private fun insertAdvertisement(item: Advertisement) {
        disposable.add(Completable.fromAction {
                advertisementsDao.insertItem(item)
            }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("User insert error" + error.message)}))
    }

    private fun deleteAdvertisementItem(item: Advertisement) {
        disposable.add(Completable.fromAction {
            advertisementsDao.deleteItem(item)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("User insert error" + error.message)}))
    }
}