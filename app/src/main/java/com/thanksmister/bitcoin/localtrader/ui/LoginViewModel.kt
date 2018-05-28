/*
 * Copyright (c) 2018 ThanksMister
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

package com.thanksmister.bitcoin.localtrader.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.text.TextUtils
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.events.AlertMessage
import com.thanksmister.bitcoin.localtrader.events.SnackbarMessage
import com.thanksmister.bitcoin.localtrader.events.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.ApiError
import com.thanksmister.bitcoin.localtrader.network.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.persistence.User
import com.thanksmister.bitcoin.localtrader.persistence.UserDao
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import java.net.HttpURLConnection
import javax.inject.Inject


class LoginViewModel @Inject
constructor(application: Application, private val dataSource: UserDao, private val preferences: Preferences) : AndroidViewModel(application) {

    private val disposable = CompositeDisposable()
    private val toastText = ToastMessage()
    private val alertText = AlertMessage()
    private val snackbarText = SnackbarMessage()
    private val navigate = MutableLiveData<Boolean>()

    fun getNavigateNextView(): MutableLiveData<Boolean> {
        return navigate
    }

    private fun showNavigateNextView(value: Boolean) {
        navigate.value = value
    }

    fun getToastMessage(): ToastMessage {
        return toastText
    }

    fun getAlertMessage(): AlertMessage {
        return alertText
    }

    fun getSnackBarMessage(): SnackbarMessage {
        return snackbarText
    }

    private fun showSnackbarMessage(message: Int) {
        snackbarText.value = message
    }

    private fun showAlertMessage(message: String) {
        Timber.d("alert message: " + message)
        alertText.value = message
    }

    private fun showToastMessage(message: String) {
        toastText.value = message
    }

    init {

    }

    public override fun onCleared() {
        //prevents memory leaks by disposing pending observable objects
        if ( !disposable.isDisposed) {
            disposable.clear()
        }
    }

    /**
     * Get the last item.
     * @return a [Flowable]
     */
    fun getUsers(): Flowable<User> {
        return dataSource.getItems()
                .filter {items -> items.isNotEmpty() }
                .map { items -> items[items.size - 1] }
    }

    fun insertUser(user: User) {
        disposable.add(Completable.fromAction {
            user.uid = 1
            dataSource.insertItem(user) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Database error" + error.message)}))
    }

    fun getOauthTokens(code: String, key: String, secret: String) {
        val api = LocalBitcoinsApi(preferences.endPoint()!!)
        val fetcher = LocalBitcoinsFetcher(this.getApplication(), api, preferences)
        disposable.add(fetcher.getOauthToken(code, key, secret)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({response ->
                    Timber.d("token: " + response.access_token)
                    Timber.d("refresh: " + response.refresh_token)
                    preferences.accessToken(response.access_token)
                    preferences.refreshToken(response.refresh_token)
                    getMyself()
                }, { error ->
                    var errorMessage = getApplication<BaseApplication>().getString(R.string.error_unknown_error)
                    if (error is HttpException) {
                        val errorCode = error.code()
                        Timber.e("Error code " + errorCode)
                        // {"error": "unsupported_grant_type"}
                        val errorJsonString = ApiError(error).message
                        Timber.e("Error body " + errorJsonString)
                        when (errorCode) {
                            // TODO make a error handler to return the error message from either code or error message
                            HttpURLConnection.HTTP_FORBIDDEN -> errorMessage = getApplication<BaseApplication>().getString(R.string.error_invalid_credentials)
                            HttpURLConnection.HTTP_UNAUTHORIZED -> errorMessage = getApplication<BaseApplication>().getString(R.string.error_invalid_credentials)
                            HttpURLConnection.HTTP_UNAVAILABLE -> errorMessage = getApplication<BaseApplication>().getString(R.string.error_no_internet)
                        }
                    }
                    showAlertMessage(errorMessage)
                }))
    }

    fun getMyself() {
        val endpoint = preferences.endPoint()!!
        val api = LocalBitcoinsApi(endpoint)
        val fetcher = LocalBitcoinsFetcher(this.getApplication(), api, preferences)
        disposable.add(fetcher.getMyself()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({user ->
                    Timber.d("username: " + user.username)
                    if(!TextUtils.isEmpty(user.username)) {
                        preferences.userName(user.username!!)
                    }
                    preferences.userFeedback(user.feedback_score.toString())
                    preferences.userTrades(user.trading_partners_count.toString())
                    insertUser(user)
                    showNavigateNextView(true)
                }, { error ->
                    Timber.e("Error message: " + error.message)
                    var errorMessage = getApplication<BaseApplication>().getString(R.string.error_unknown_error)
                    if (error is HttpException) {
                        val errorCode = error.code()
                        Timber.e("Error code: " + errorCode)
                        Timber.e("Error response body: " + error.response().body())
                        Timber.e("Error response message: " + error.response().message())
                        when (errorCode) {
                            HttpURLConnection.HTTP_FORBIDDEN -> errorMessage = getApplication<BaseApplication>().getString(R.string.error_invalid_credentials)
                            HttpURLConnection.HTTP_UNAUTHORIZED -> errorMessage = getApplication<BaseApplication>().getString(R.string.error_invalid_credentials)
                            HttpURLConnection.HTTP_UNAVAILABLE -> errorMessage = getApplication<BaseApplication>().getString(R.string.error_no_internet)
                        }
                    }
                    showAlertMessage(errorMessage)
                }))
    }

    /**
     * Network connectivity receiver to notify client of the network disconnect issues and
     * to clear any network notifications when reconnected. It is easy for network connectivity
     * to run amok that is why we only notify the user once for network disconnect with
     * a boolean flag.
     */
    companion object {

    }
}