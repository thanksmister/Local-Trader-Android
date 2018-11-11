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
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactsActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.DashboardViewModel
import kotlinx.android.synthetic.main.activity_main.*

import javax.inject.Inject

import timber.log.Timber

class DashboardFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener  {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: DashboardViewModel
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var pagerPosition = 0
    private var fragment: Fragment? = null
    private var onFragmentListener: OnFragmentListener? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                if (activity != null && !activity!!.isFinishing) {
                    fragment = AdvertisementsFragment.newInstance()
                    childFragmentManager.beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit()
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                fragment = ContactsFragment.newInstance()
                if (activity != null && !activity!!.isFinishing) {
                    childFragmentManager.beginTransaction().replace(R.id.content, fragment, CONTACTS_FRAGMENT).commit()
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                if (activity != null && !activity!!.isFinishing) {
                    fragment = NotificationsFragment.newInstance()
                    childFragmentManager.beginTransaction().replace(R.id.content, fragment, NOTIFICATIONS_FRAGMENT).commit()
                }
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface OnFragmentListener {
        fun updateSyncMap(key: String, value: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            pagerPosition = arguments!!.getInt("pagePosition", 0)
        }
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val navigation = activity!!.findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        fragment = AdvertisementsFragment.newInstance()
        childFragmentManager.beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit()

        mainSwipeLayout.setOnRefreshListener(this)
        mainSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))
        mainSwipeLayout.setProgressViewOffset(false, 48, 186)
        mainSwipeLayout.setDistanceToTriggerSync(250)

        setupToolbar()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentListener) {
            onFragmentListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnDashboardFragmentListener")
        }
    }

    private fun setupToolbar() {
        if (!isAdded || activity == null) {
            return
        }
        try {
            val ab = (activity as MainActivity).supportActionBar
            if (ab != null) {
                ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu)
                ab.title = ""
                ab.setDisplayHomeAsUpEnabled(true)
            }
        } catch (e: NoClassDefFoundError) {
            // TODO dialog
            /*showAlertDialog(new AlertDialogEvent(getString(R.string.error_device_title),
                    getString(R.string.error_device_softare_description)), new Action0() {
                @Override
                public void call() {
                    getActivity().finish();
                }
            });*/
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_dashboard, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt("pagePosition", 0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
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

        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pagePosition", pagerPosition)
        //super.onSaveInstanceState(outState);
    }

    override fun onResume() {
        super.onResume()
        updateData()
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

    override fun onRefresh() {
        Timber.d("onRefresh")
        updateData()
    }

    private fun updateData() {
        toast(getString(R.string.toast_refreshing_data))
        /* onFragmentListener.updateSyncMap(SYNC_ADVERTISEMENTS, true);
        dataService.getAdvertisements(true)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Advertisements update subscription safely unsubscribed");
                        onFragmentListener.updateSyncMap(SYNC_ADVERTISEMENTS, false);
                    }
                })
                .subscribe(new Action1<List<Advertisement>>() {
                    @Override
                    public void call(List<Advertisement> advertisements) {
                        onFragmentListener.updateSyncMap(SYNC_ADVERTISEMENTS, false);
                        if (advertisements != null && !advertisements.isEmpty()) {
                            dbManager.insertAdvertisements(advertisements);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onFragmentListener.updateSyncMap(SYNC_ADVERTISEMENTS, false);
                        if (throwable instanceof InterruptedIOException) {
                            Timber.d("Advertisements Error: " + throwable.getMessage());
                        } else {
                            handleError(throwable);
                        }
                    }
                });

        onFragmentListener.updateSyncMap(SYNC_CONTACTS, true);
        dataService.getContacts(DashboardType.ACTIVE)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Contacts update subscription safely unsubscribed");
                        onFragmentListener.updateSyncMap(SYNC_CONTACTS, false);
                    }
                })
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) {
                        onFragmentListener.updateSyncMap(SYNC_CONTACTS, false);
                        if (contacts != null) {
                            dbManager.insertContacts(contacts);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onFragmentListener.updateSyncMap(SYNC_CONTACTS, false);
                        if (throwable instanceof InterruptedIOException) {
                            Timber.d("Advertisements Error: " + throwable.getMessage());
                        } else {
                            handleError(throwable);
                        }
                    }
                });

        onFragmentListener.updateSyncMap(SYNC_NOTIFICATIONS, true);
        dataService.getNotifications()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Notifications update subscription safely unsubscribed");
                        onFragmentListener.updateSyncMap(SYNC_NOTIFICATIONS, false);
                    }
                })
                .subscribe(new Action1<List<Notification>>() {
                    @Override
                    public void call(List<Notification> notifications) {
                        onFragmentListener.updateSyncMap(SYNC_NOTIFICATIONS, false);
                        if (notifications != null) {
                            final HashMap<String, Notification> entryMap = new HashMap<>();
                            for (Notification notification : notifications) {
                                entryMap.put(notification.getNotification_id(), notification);
                            }

                            if(isAdded() && getActivity() != null && getActivity().getContentResolver() != null) {
                                synchronized (getActivity()) {
                                    ContentResolver contentResolver = getActivity().getContentResolver();
                                    Cursor cursor = contentResolver.query(SyncProvider.NOTIFICATION_TABLE_URI, null, null, null, null);
                                    if (cursor != null && cursor.getCount() > 0) {
                                        while (cursor.moveToNext()) {
                                            final long id = Db.getLong(cursor, NotificationItem.ID);
                                            String notificationId = Db.getString(cursor, NotificationItem.NOTIFICATION_ID);
                                            boolean notificationRead = Db.getBoolean(cursor, NotificationItem.READ);
                                            String url = Db.getString(cursor, NotificationItem.URL);
                                            Notification match = entryMap.get(notificationId);
                                            if (match != null) {
                                                entryMap.remove(notificationId);
                                                if (match.getRead() != notificationRead || !match.getUrl().equals(url)) {
                                                    NotificationItem.Builder builder = NotificationItem.createBuilder(match);
                                                    contentResolver.update(SyncProvider.NOTIFICATION_TABLE_URI, builder.build(), NotificationItem.ID + " = ?", new String[]{String.valueOf(id)});
                                                }
                                            } else {
                                                contentResolver.delete(SyncProvider.NOTIFICATION_TABLE_URI, NotificationItem.ID + " = ?", new String[]{String.valueOf(id)});
                                            }
                                        }
                                        cursor.close();
                                    }

                                    List<Notification> newNotifications = new ArrayList<>();
                                    if (!entryMap.isEmpty()) {
                                        for (Notification notification : entryMap.values()) {
                                            newNotifications.add(notification);
                                            contentResolver.insert(SyncProvider.NOTIFICATION_TABLE_URI, NotificationItem.createBuilder(notification).build());
                                        }
                                    }

                                    Timber.d("updateNotifications newNotifications: " + newNotifications.size());
                                    if (!newNotifications.isEmpty() && isAdded() && getActivity() != null) {
                                        NotificationService notificationService = new NotificationService(getActivity());
                                        notificationService.createNotifications(newNotifications);
                                    }
                                }
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onFragmentListener.updateSyncMap(SYNC_NOTIFICATIONS, false);
                        if (throwable instanceof InterruptedIOException) {
                            Timber.d("Notifications Error: " + throwable.getMessage());
                        } else {
                            handleError(throwable);
                        }
                    }
                });*/
    }

    private fun launchScanner() {
        if (isAdded && activity != null) {
            (activity as MainActivity).launchScanner()
        }
    }

    private fun showSearchScreen() {
        if (isAdded && activity != null) {
            // TODO move up to activity
        }
    }

    private fun showTradesScreen() {
        if (isAdded && activity != null) {
            val intent = ContactsActivity.createStartIntent(activity!!, DashboardType.RELEASED)
            startActivity(intent)
        }
    }

    private fun createAdvertisementScreen() {
        // TODO dialog
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
        showProgressDialog("Marking notifications read...")
        /* dbManager.notificationsQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Notification subscription safely unsubscribed");
                    }
                })
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
            val notificationId = notificationItem.notificationId
            /*dataService.markNotificationRead(notificationId)
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

        hideProgressDialog()
    }

    companion object {

        private val SYNC_ADVERTISEMENTS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_ADVERTISEMENTS"
        private val SYNC_CONTACTS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_CONTACTS"
        private val SYNC_NOTIFICATIONS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_NOTIFICATIONS"

        private val ADVERTISEMENTS_FRAGMENT = "com.thanksmister.fragment.ADVERTISEMENTS_FRAGMENT"
        private val CONTACTS_FRAGMENT = "com.thanksmister.fragment.CONTACTS_FRAGMENT"
        private val NOTIFICATIONS_FRAGMENT = "com.thanksmister.fragment.NOTIFICATIONS_FRAGMENT"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): DashboardFragment {
            val dashboardFragment = DashboardFragment()
            val args = Bundle()
            args.putInt("pagePosition", 0)
            dashboardFragment.arguments = args
            return DashboardFragment()
        }
    }
}