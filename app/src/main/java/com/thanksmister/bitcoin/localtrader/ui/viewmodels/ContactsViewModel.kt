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
import com.thanksmister.bitcoin.localtrader.architecture.MessageData
import com.thanksmister.bitcoin.localtrader.architecture.NetworkMessage
import com.thanksmister.bitcoin.localtrader.architecture.ToastMessage
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.Message
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.ContactsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class ContactsViewModel @Inject
constructor(application: Application, private val contactsDao: ContactsDao, private val preferences: Preferences) : BaseViewModel(application) {

    private val contactId = MutableLiveData<Int>()
    private val contact = MutableLiveData<Contact>()

    fun getContactId(): LiveData<Int> {
        return contactId
    }

    private fun setContactId(value: Int) {
        this.contactId.value = value
    }

    fun getContact(): LiveData<Contact> {
        return contact
    }

    private fun setContact(value: Contact) {
        this.contact.value = value
    }

    init {
    }

    fun getContact(contactId: Int):Flowable<Contact> {
        return contactsDao.getItemById(contactId)
    }

    fun getContacts():Flowable<List<Contact>> {
        return contactsDao.getItems()
                .map { contactList  ->
                    val contacts = ArrayList<Contact>()
                    contactList.forEach() {
                        if (it.closedAt != null && it.canceledAt != null && TradeUtils.tradeIsActive(it.closedAt!!, it.canceledAt!!)) {
                            contacts.add(it);
                        }
                    }
                    contacts
                }
    }

    fun fetchContact(contactId: Int) {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        disposable.add(fetcher.getContact(contactId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    insertContact(it)
                }, {
                    error -> Timber.e("Contact Error" + error.message)
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

    fun fetchMessages(contactId: Int): Observable<List<Message>> {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        return fetcher.getContactMessages(contactId)

    }

    private fun insertContact(item: Contact) {
        disposable.add(Completable.fromAction {
            contactsDao.insertItem(item)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("Contact insert error" + error.message)}))
    }
}