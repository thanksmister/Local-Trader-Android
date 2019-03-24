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
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.AdvertisementsDao
import com.thanksmister.bitcoin.localtrader.persistence.ContactsDao
import com.thanksmister.bitcoin.localtrader.persistence.NotificationsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ContactsViewModel @Inject
constructor(application: Application,
            private val contactsDao: ContactsDao,
            private val notificationsDao: NotificationsDao,
            private val advertisementsDao: AdvertisementsDao,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val contactData = MutableLiveData<ContactData>()
    private val contacts = MutableLiveData<List<Contact>>()
    private val advertisement = MutableLiveData<Advertisement>()
    private val contactUpdated = MutableLiveData<Boolean>()
    private val contactDeleted = MutableLiveData<Boolean>()
    private val contactId = MutableLiveData<Int>()
    private val contact = MutableLiveData<Contact>()

    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getContactData(): LiveData<ContactData> {
        return contactData
    }

    fun setContactData(value: ContactData) {
        this.contactData.value = value
    }

    fun getContactsList(): LiveData<List<Contact>> {
        return contacts
    }

    private fun setContactsList(value: List<Contact>) {
        this.contacts.value = value
    }

    fun getAdvertisement(): LiveData<Advertisement?> {
        return advertisement
    }

    fun setAdvertisement(value: Advertisement?) {
        this.advertisement.value = value
    }

    fun getContactDeleted(): LiveData<Boolean> {
        return contactDeleted
    }

    private fun setContactDeleted(value: Boolean) {
        this.contactDeleted.value = value
    }

    fun getContactUpdated(): LiveData<Boolean> {
        return contactUpdated
    }

    private fun setContactUpdated(value: Boolean) {
        this.contactUpdated.value = value
    }

    fun getContactId(): LiveData<Int> {
        return contactId
    }

    private fun setContactId(value: Int) {
        this.contactId.value = value
    }

    fun getContact(): LiveData<Contact> {
        return contact
    }

    fun setContact(value: Contact) {
        this.contact.value = value
    }

    inner class ContactData {
        var contact = Contact()
        var messages = emptyList<Message>()
    }

    fun getContact(contactId: Int):Flowable<Contact> {
        return contactsDao.getItemById(contactId)
    }

    @Deprecated ("Don't save these to the database")
    fun getContactsByType(dashboardType: DashboardType):Flowable<List<Contact>> {
        return contactsDao.getItems()
                .map {contactList ->
                    val contacts = ArrayList<Contact>()
                    contactList.forEach() {contact ->
                        if(dashboardType == DashboardType.RELEASED) {
                            if(TradeUtils.isReleased(contact)) {
                                contacts.add(contact)
                            }
                        } else if( dashboardType == DashboardType.CANCELED) {
                            if(TradeUtils.isCanceledTrade(contact) && !TradeUtils.isReleased(contact)) {
                                contacts.add(contact)
                            }
                        } else if( dashboardType == DashboardType.CLOSED) {
                            if(TradeUtils.isClosedTrade(contact) && !TradeUtils.isReleased(contact) && !TradeUtils.canCancelTrade(contact)) {
                                contacts.add(contact)
                            }
                        }
                    }
                    contacts
                }
    }

    fun getActiveContacts():Flowable<List<Contact>> {
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

    fun fetchContactsByType(type: DashboardType) {
        disposable += fetcher.getContactsByType(type)
                .applySchedulers()
                .subscribe ({
                    setContactsList(it)
                }, {
                    error -> Timber.e("Contacts Type Error" + error.message)
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_error_retrieving_trades))
                    }
                })
    }

    @Deprecated ("Let's get with message data instead")
    fun fetchContact(contactId: Int) {
        disposable += fetcher.getContact(contactId)
                .applySchedulers()
                .subscribe ({
                    insertContact(it)
                }, {
                    error -> Timber.e("Contact Error " + error.message)
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(error.message)
                    }
                })
    }

    fun fetchContactData(contactId: Int) : Observable<ContactData> {
        return Observable.combineLatest(fetcher.getContact(contactId), fetcher.getContactMessages(contactId),
                BiFunction { contact, messages  ->
                    insertContact(contact)
                    val data = ContactData()
                    data.contact = contact
                    data.messages = messages
                    data
                })

    }

    fun contactAction(contactId: Int, pinCode: String?, action: ContactAction) {
        disposable += fetcher.contactAction(contactId, pinCode, action)
                .applySchedulers()
                .subscribe ({
                    Timber.d(it.asString)
                    if (action == ContactAction.RELEASE || action == ContactAction.CANCEL) {
                        contactsDao.deleteItem(contactId)
                        if (action == ContactAction.RELEASE) {
                            showToastMessage(getApplication<BaseApplication>().getString(R.string.trade_released_toast_text));
                        } else if (action == ContactAction.CANCEL) {
                            showToastMessage(getApplication<BaseApplication>().getString(R.string.trade_canceled_toast_text));
                        }
                        setContactDeleted(true)
                    } else {
                        fetchContact(contactId) // refresh contact
                    }
                }, {
                    error -> Timber.e("Contact Action Error" + error.message)
                    if(error is NetworkException) {
                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                        } else {
                            showNetworkMessage(error.message, error.code)
                        }
                    } else {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_contact_action))
                    }
                })
    }

    fun markNotificationRead(contactId: Int) {
        disposable += notificationsDao.getItemUnreadItemByContactId(contactId, false)
                .applySchedulers()
                .subscribe ({notification ->
                    if(notification != null) {
                        fetcher.markNotificationRead(notification.notificationId)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .debounce(200, TimeUnit.MILLISECONDS)
                                .dematerialize<List<Notification>>()
                                .subscribe ({
                                    notification.read = true
                                    updateNotification(notification)
                                }, {
                                    error -> Timber.e("Notification Error $error.message")
                                    if(error is NetworkException) {
                                        if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                                            showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                                        } else {
                                            showNetworkMessage(error.message, error.code)
                                        }
                                    } else {
                                        showAlertMessage(error.message)
                                    }
                                })
                    }
                }, {
                    error -> Timber.e("Notification Error $error.message")
                })
    }

    fun fetchMessages(contactId: Int): Observable<List<Message>> {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        val fetcher = LocalBitcoinsFetcher(getApplication(), api, preferences)
        return fetcher.getContactMessages(contactId)
    }

    fun getAdvertisement(adId: Int):Flowable<Advertisement> {
        return advertisementsDao.getItemById(adId)
    }

    private fun updateNotification(notification: Notification) {
        disposable += Completable.fromAction {
            notificationsDao.updateItem(notification)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Notification update error" + error.message)})
    }

    private fun insertContact(item: Contact) {
        disposable += Completable.fromAction {
            contactsDao.insertItem(item)
            }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Contact insert error" + error.message)})
    }

    private fun insertContacts(items: List<Contact>) {
        disposable += Completable.fromAction {
            contactsDao.insertItems(items)
        }
                .applySchedulers()
                .subscribe({
                }, { error -> Timber.e("Contact insert error" + error.message)})
    }
}