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

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.*

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType
import com.thanksmister.bitcoin.localtrader.network.services.SyncUtils
import com.thanksmister.bitcoin.localtrader.persistence.Notification
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.MainViewModel
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactsActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity

import javax.inject.Inject

class MainFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener  {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: MainViewModel

    private var pagerPosition = 0
    private var fragment: Fragment? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                fragment = AdvertisementsFragment.newInstance()
                childFragmentManager.beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                fragment = ContactsFragment.newInstance()
                childFragmentManager.beginTransaction().replace(R.id.content, fragment, CONTACTS_FRAGMENT).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                fragment = NotificationsFragment.newInstance()
                childFragmentManager.beginTransaction().replace(R.id.content, fragment, NOTIFICATIONS_FRAGMENT).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            pagerPosition = arguments!!.getInt("pagePosition", 0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navigation = activity!!.findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        fragment = AdvertisementsFragment.newInstance()
        childFragmentManager.beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_dashboard, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt("pagePosition", 0)
        }

        // Obtain ViewModel from ViewModelProviders, using this fragment as LifecycleOwner.
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        // Observe data on the ViewModel, exposed as a LiveData
        /*viewModel.data.observe(this, Observer { data ->
            // Set the text exposed by the LiveData
            view?.findViewById<TextView>(R.id.text)?.text = data
        })*/

    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.advertisements, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
       /* when (item!!.itemId) {
            R.id.action_search -> {
                showSearchScreen()
                return false
            }
            R.id.action_trades -> {
                showTradesScreen()
                return true
            }
            R.id.action_advertise -> {
                createAdvertisementScreen()
                return true
            }
            R.id.action_clear_notifications -> {
                getUnreadNotifications()
                return true
            }
            R.id.action_scan -> {
                launchScanner()
                return true
            }
            else -> {
            }
        }
*/
        return false
    }

    // TODO save onto the model
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pagePosition", pagerPosition)
        // super.onSaveInstanceState(outState);
    }

    override fun onRefresh() {
        preferences.forceUpdates(true)
        SyncUtils.requestSyncNow(activity)
        viewModel.getExchangeRate()
    }

    private fun launchScanner() {
        if (isAdded && activity != null) {
            (activity as MainActivity).launchScanner()
        }
    }

    private fun showSearchScreen() {
        if (isAdded && activity != null) {
            //(activity as MainActivity).navigateSearchView()
        }
    }

    private fun showTradesScreen() {
        val intent = ContactsActivity.createStartIntent(activity, DashboardType.RELEASED)
        startActivity(intent)
    }

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

    private fun getUnreadNotifications() {
        //showProgressDialog(ProgressDialogEvent("Marking notifications read..."))
        /* dbManager.notificationsQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Notification subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<NotificationItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<NotificationItem>>() {
                    @Override
                    public void call(List<NotificationItem> notificationItems) {
                        final List<NotificationItem> unreadNotifications = new ArrayList<NotificationItem>();
                        for (NotificationItem notificationItem : notificationItems) {
                            if (!notificationItem.read()) {
                                unreadNotifications.add(notificationItem);
                            }
                        }
                        if (!unreadNotifications.isEmpty()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    markNotificationsRead(unreadNotifications);
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        reportError(throwable);
                    }
                });*/
    }

    private fun markNotificationsRead(notificationItems: List<Notification>) {
        for (notificationItem in notificationItems) {
            //final String notificationId = notificationItem.getId();
            /* dataService.markNotificationRead(notificationId)
                    .doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            Timber.i("Mark notification read safely unsubscribed");
                        }
                    })
                    .compose(this.<JSONObject>bindUntilEvent(FragmentEvent.PAUSE))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<JSONObject>() {
                        @Override
                        public void call(JSONObject result) {
                            if (!Parser.containsError(result)) {
                                dbManager.markNotificationRead(notificationId);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.e(throwable.getMessage());
                        }
                    });*/
        }

        //hideProgressDialog()
    }

    companion object {
        const val ADVERTISEMENTS_FRAGMENT = "com.thanksmister.fragment.ADVERTISEMENTS_FRAGMENT"
        const val CONTACTS_FRAGMENT = "com.thanksmister.fragment.CONTACTS_FRAGMENT"
        const val NOTIFICATIONS_FRAGMENT = "com.thanksmister.fragment.NOTIFICATIONS_FRAGMENT"
        /**
         * Returns a new instance of this fragment for the given section number.
         */
        fun newInstance(): MainFragment {
            val dashboardFragment = MainFragment()
            val args = Bundle()
            args.putInt("pagePosition", 0)
            dashboardFragment.arguments = args
            return MainFragment()
        }
    }
}