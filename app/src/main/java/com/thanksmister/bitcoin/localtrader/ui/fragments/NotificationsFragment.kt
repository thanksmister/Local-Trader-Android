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

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.thanksmister.bitcoin.localtrader.utils.applySchedulersIo
import kotlinx.android.synthetic.main.fragment_notifications.*
import timber.log.Timber
import javax.inject.Inject

class NotificationsFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: NotificationsViewModel
    @Inject
    lateinit var notificationUtils: NotificationUtils

    private val notificationAdapter: NotificationAdapter by lazy {
        NotificationAdapter(object : NotificationAdapter.OnItemClickListener {
            override fun onSearchButtonClicked() {
                showSearchScreen()
            }
            override fun onAdvertiseButtonClicked() {
                createAdvertisementScreen()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // clear all notification types
        notificationUtils.clearNotification()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ItemClickSupport.addTo(notificationsList).setOnItemClickListener { _, position, _ ->
            val notificationItem = notificationAdapter.getItemAt(position)
            notificationItem?.let {
                when {
                    it.contactId != null -> showContact(notificationItem)
                    it.advertisementId != null -> showAdvertisement(notificationItem)
                    else -> try {
                        onNotificationLinkClicked(it)
                    } catch (e: ActivityNotFoundException) {
                        dialogUtils.toast(getString(R.string.text_cant_open_link))
                    }
                }
            }
        }

        notificationsList.apply {
            adapter = notificationAdapter
            notificationsList.setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(activity)
            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(NotificationsViewModel::class.java)
        observeViewModel(viewModel)
        lifecycle.addObserver(viewModel)
    }

    private fun setupList(items: List<Notification>) {
        notificationAdapter.replaceWith(items)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    private fun observeViewModel(viewModel: NotificationsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.showAlertDialog(requireActivity(), message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.toast(message)
            }
        })
        disposable.add(viewModel.getNotifications()
                .applySchedulersIo()
                .subscribe( { data ->
                    if(data != null) {
                        setupList(data)
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    private fun createAdvertisementScreen() {
        if(isAdded) {
            dialogUtils.showAlertDialog(requireActivity(), getString(R.string.dialog_edit_advertisements),
                    DialogInterface.OnClickListener { _, _ ->
                        try {
                            startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)))
                        } catch (ex: ActivityNotFoundException) {
                            dialogUtils.toast(getString(R.string.toast_error_no_installed_ativity))
                        }
                    }, DialogInterface.OnClickListener { _, _ ->
                // na-da
            })
        }
    }

    private fun showSearchScreen() {
        if(isAdded) {
            val intent = SearchActivity.createStartIntent(requireActivity())
            startActivity(intent)
        }
    }

    private fun onNotificationLinkClicked(notification: Notification) {
        viewModel.markNotificationRead(notification)
        notification.url?.let {
            launchNotificationLink(it)
        }
    }

    private fun launchNotificationLink(url: String) {
        val currentEndpoint = preferences.getServiceEndpoint()
        val intent: Intent
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentEndpoint + url))
        startActivity(intent)
    }

    private fun showAdvertisement(item: Notification?) {
        if (item != null && isAdded && item.advertisementId != null) {
            val intent = AdvertisementActivity.createStartIntent(requireActivity(), item.advertisementId!!)
            intent.setClass(requireActivity(), AdvertisementActivity::class.java)
            startActivity(intent)
        } else {
            dialogUtils.toast(getString(R.string.toast_error_opening_advertisement))
        }
    }

    private fun showContact(item: Notification?) {
        if (item != null && isAdded && item.contactId != null) {
            val intent = ContactActivity.createStartIntent(requireActivity(), item.contactId!!)
            intent.setClass(requireActivity(), ContactActivity::class.java)
            startActivity(intent)
        } else {
            dialogUtils.toast(getString(R.string.toast_error_opening_contact))
        }
    }

    companion object {
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}