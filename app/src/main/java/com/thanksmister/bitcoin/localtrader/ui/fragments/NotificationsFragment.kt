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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.*
import android.widget.Toast

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.persistence.Notification
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.NotificationAdapter
import com.thanksmister.bitcoin.localtrader.ui.controls.ItemClickSupport
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.NotificationsViewModel
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.SearchViewModel
import com.thanksmister.bitcoin.localtrader.utils.DialogUtils
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_notifications.*
import timber.log.Timber

import javax.inject.Inject

class NotificationsFragment : BaseFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: NotificationsViewModel
    private var itemAdapter: NotificationAdapter? = null
    private val notifications = emptyList<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        notificationsRecyclerView.setHasFixedSize(true)
        notificationsRecyclerView.setLayoutManager(linearLayoutManager)

        ItemClickSupport.addTo(notificationsRecyclerView).setOnItemClickListener { recyclerView, position, v ->
            val notificationItem = itemAdapter!!.getItemAt(position)
            if (notificationItem!!.contactId != null) {
                showContact(notificationItem)
            } else if (notificationItem.advertisementId != null) {
                showAdvertisement(notificationItem)
            } else {
                try {
                    onNotificationLinkClicked(notificationItem)
                } catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.text_cant_open_link))
                }
            }
        }

        itemAdapter = getAdapter(ArrayList<Notification>())
        notificationsRecyclerView.adapter = itemAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.notifications, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_clear_notifications -> {
                viewModel.markNotificationsRead()
                return true
            }
        }
        return false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(NotificationsViewModel::class.java)

        lifecycle.addObserver(dialogUtils)

        observeViewModel(viewModel)

        // clear all notifications
        val ns = Context.NOTIFICATION_SERVICE
        val notificationManager = activity!!.getSystemService(ns) as NotificationManager
        notificationManager.cancel(NotificationUtils.NOTIFICATION_TYPE_NOTIFICATION)
        setHasOptionsMenu(true)
    }

    private fun getAdapter(notifications: ArrayList<Notification>) : NotificationAdapter{
        return NotificationAdapter(notifications, object : NotificationAdapter.OnItemClickListener {
            override fun onSearchButtonClicked() {
                showSearchScreen()
            }
            override fun onAdvertiseButtonClicked() {
                createAdvertisementScreen()
            }
        })
    }

    private fun observeViewModel(viewModel: NotificationsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            Timber.d("getAlertMessage")
            if (isAdded && activity != null) {
                dialogUtils.showAlertDialog(activity!!.applicationContext, message!!)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            Timber.d("getToastMessage")
            if (isAdded && activity != null) {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        })
        viewModel.getNotificationUrl().observe(this, Observer { url ->
            if (isAdded && activity != null && !TextUtils.isEmpty(url)) {
                launchNotificationLink(url!!);
            }
        })
        disposable.add(viewModel.getNotifications()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ notifications ->
                    notificationsRecyclerView.adapter = getAdapter(notifications)
                    notificationsRecyclerView.invalidate()
                }, { error -> Timber.e("Error Notifications: ${error.message}")}))
    }

    /**
     * Creating or editing advertisements takes users to the LBC website
     */
    private fun createAdvertisementScreen() {
        if (isAdded && activity != null) {
            dialogUtils.showAlertDialog(activity!!, getString(R.string.dialog_edit_advertisements), object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    try {
                        val intent =  Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL));
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(activity, getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                    }
                }
            })
        }
    }

    protected fun showSearchScreen() {
        if (isAdded && activity != null) {
            val intent = SearchActivity.createStartIntent(activity!!)
            startActivity(intent)
        }
    }

    private fun onNotificationLinkClicked(notification: Notification) {
        viewModel.markNotificationRead(notification)
    }

    private fun launchNotificationLink(url: String) {
        val currentEndpoint = preferences.endPoint()
        val intent: Intent
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentEndpoint!! + url))
        startActivity(intent)
    }

    private fun showAdvertisement(item: Notification?) {
        if (item != null && isAdded && activity != null && item.advertisementId != null) {
            val intent = AdvertisementActivity.createStartIntent(activity, item.advertisementId!!)
            intent.setClass(activity!!, AdvertisementActivity::class.java)
            startActivity(intent)
        } else {
            toast(getString(R.string.toast_error_opening_advertisement))
        }
    }

    private fun showContact(item: Notification?) {
        if (item != null && isAdded && activity != null) {
            val intent = ContactActivity.createStartIntent(activity, item.contactId)
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