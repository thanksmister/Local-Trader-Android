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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.app.NotificationManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.NotificationAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.NotificationsViewModel
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_notifications.*
import timber.log.Timber

import javax.inject.Inject

class NotificationsFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: NotificationsViewModel
    lateinit var notificationUtils: NotificationUtils

    private val disposable = CompositeDisposable()

    private var adapter: NotificationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // clear all notification types
        /*val ns = Context.NOTIFICATION_SERVICE
        val notificationManager = activity!!.getSystemService(ns) as NotificationManager
        notificationManager.cancel(NotificationUtils.NOTIFICATION_TYPE_NOTIFICATION)*/
        notificationUtils.clearNotification()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationsList.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        notificationsList.layoutManager = linearLayoutManager

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(NotificationsViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun setupList(items: List<Notification>) {
        if (isAdded && adapter != null) {
            adapter!!.replaceWith(items)
            notificationsList.adapter = adapter
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(activity != null && isAdded) {
            adapter = NotificationAdapter(activity!!, object : NotificationAdapter.OnItemClickListener {
                override fun onSearchButtonClicked() {
                    showSearchScreen()
                }
                override fun onAdvertiseButtonClicked() {
                    createAdvertisementScreen()
                }
            })
            ItemClickSupport.addTo(notificationsList).setOnItemClickListener { _, position, _ ->
                val notificationItem = adapter!!.getItemAt(position)
                when {
                    notificationItem!!.contactId != null -> showContact(notificationItem)
                    notificationItem.advertisementId != null -> showAdvertisement(notificationItem)
                    else -> try {
                        onNotificationLinkClicked(notificationItem)
                    } catch (e: ActivityNotFoundException) {
                        toast(getString(R.string.text_cant_open_link))
                    }
                }
            }
            notificationsList.adapter = adapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
    }

    private fun observeViewModel(viewModel: NotificationsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.showAlertDialog(activity!!, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                toast(message)
            }
        })
        disposable.add(viewModel.getNotifications()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null) {
                        setupList(data)
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    private fun createAdvertisementScreen() {
        if(activity != null && isAdded) {
            dialogUtils.showAlertDialog(activity!!, getString(R.string.dialog_edit_advertisements),
                    DialogInterface.OnClickListener { _, _ ->
                        try {
                            startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)))
                        } catch (ex: ActivityNotFoundException) {
                            Toast.makeText(activity!!, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
                        }
                    }, DialogInterface.OnClickListener { _, _ ->
                // na-da
            })
        }
    }

    private fun showSearchScreen() {
        if(activity != null && isAdded) {
            val intent = SearchActivity.createStartIntent(activity!!)
            startActivity(intent)
        }
    }

    private fun onNotificationLinkClicked(notification: Notification) {
        viewModel.markNotificationRead(notification)
        if (!TextUtils.isEmpty(notification.url)) {
            launchNotificationLink(notification.url!!)
        }
    }

    private fun launchNotificationLink(url: String) {
        val currentEndpoint = preferences.getServiceEndpoint()
        val intent: Intent
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentEndpoint + url))
        startActivity(intent)
    }

    private fun showAdvertisement(item: Notification?) {
        if (item != null && isAdded && activity != null && item.advertisementId != null) {
            val intent = AdvertisementActivity.createStartIntent(activity!!, item.advertisementId!!)
            intent.setClass(activity!!, AdvertisementActivity::class.java)
            startActivity(intent)
        } else {
            toast(getString(R.string.toast_error_opening_advertisement))
        }
    }

    private fun showContact(item: Notification?) {
        if (item != null && isAdded && activity != null && item.contactId != null) {
            val intent = ContactActivity.createStartIntent(activity!!, item.contactId!!)
            intent.setClass(activity!!, ContactActivity::class.java)
            startActivity(intent)
        } else {
            toast(getString(R.string.toast_error_opening_contact))
        }
    }

    companion object {
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}