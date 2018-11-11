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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.NotificationAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils
import kotlinx.android.synthetic.main.fragment_notifications.*

import javax.inject.Inject

class NotificationsFragment : BaseFragment() {

    @Inject lateinit var sharedPreferences: SharedPreferences

    private var adapter: NotificationAdapter? = null
    private val notifications = emptyList<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // clear all notification types
        val ns = Context.NOTIFICATION_SERVICE
        val notificationManager = activity!!.getSystemService(ns) as NotificationManager
        notificationManager.cancel(NotificationUtils.NOTIFICATION_TYPE_NOTIFICATION)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationsList.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        notificationsList.layoutManager = linearLayoutManager
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
                    //showSearchScreen()
                }
                override fun onAdvertiseButtonClicked() {
                    createAdvertisementScreen()
                }
            })
            ItemClickSupport.addTo(notificationsList).setOnItemClickListener { recyclerView, position, v ->
                val notificationItem = adapter!!.getItemAt(position)
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
            notificationsList.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeData()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDetach() {
        super.onDetach()
        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            val childFragmentManager = Fragment::class.java.getDeclaredField("mChildFragmentManager")
            childFragmentManager.isAccessible = true
            childFragmentManager.set(this, null)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    protected fun subscribeData() {

        /*dbManager.notificationsQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Notifications subscription safely unsubscribed");
                    }
                })
                .subscribe(new Action1<List<NotificationItem>>() {
                    @Override
                    public void call(final List<NotificationItem> items) {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    notifications = items;
                                    setupList(notifications);
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        setupList(notifications);
                        reportError(throwable);
                    }
                });*/
    }

    /**
     * Creating or editing advertisements takes users to the LBC website
     */
    private fun createAdvertisementScreen() {
        /*showAlertDialog(new AlertDialogEvent(getString(R.string.view_title_advertisements), getString(R.string.dialog_edit_advertisements)), new Action0() {
            @Override
            public void call() {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Action0() {
            @Override
            public void call() {
                // na-da
            }
        });*/
    }


    private fun onNotificationLinkClicked(notification: Notification?) {
        /*dataService.markNotificationRead(notification.notification_id())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Mark notification read safely unsubscribed");
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject result) {
                        if (!Parser.containsError(result)) {
                            dbManager.markNotificationRead(notification.notification_id());
                            if(getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!TextUtils.isEmpty(notification.url())) {
                                            launchNotificationLink(notification.url());
                                        }
                                    }
                                });
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());
                    }
                });*/
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

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}