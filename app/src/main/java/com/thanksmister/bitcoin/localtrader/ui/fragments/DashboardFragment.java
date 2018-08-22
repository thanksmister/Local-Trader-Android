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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.database.Db;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.NotificationItem;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.network.api.model.Notification;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.network.services.NotificationService;
import com.thanksmister.bitcoin.localtrader.network.services.SyncProvider;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.trello.rxlifecycle.FragmentEvent;

import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dpreference.DPreference;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class DashboardFragment extends BaseFragment {

    private static final String SYNC_ADVERTISEMENTS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_ADVERTISEMENTS";
    private static final String SYNC_CONTACTS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_CONTACTS";
    private static final String SYNC_NOTIFICATIONS = "com.thanksmister.bitcoin.localtrader.sync.SYNC_NOTIFICATIONS";

    private static final String ADVERTISEMENTS_FRAGMENT = "com.thanksmister.fragment.ADVERTISEMENTS_FRAGMENT";
    private static final String CONTACTS_FRAGMENT = "com.thanksmister.fragment.CONTACTS_FRAGMENT";
    private static final String NOTIFICATIONS_FRAGMENT = "com.thanksmister.fragment.NOTIFICATIONS_FRAGMENT";

    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected DPreference preference;

    private int pagerPosition = 0;
    private Fragment fragment;
    private OnFragmentListener onFragmentListener;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentListener {
       void updateSyncMap(String key, boolean value);
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DashboardFragment newInstance() {
        DashboardFragment dashboardFragment = new DashboardFragment();
        Bundle args = new Bundle();
        args.putInt("pagePosition", 0);
        dashboardFragment.setArguments(args);
        return new DashboardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pagerPosition = getArguments().getInt("pagePosition", 0);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Timber.d("onViewCreated");

        BottomNavigationView navigation = (BottomNavigationView) getActivity().findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        fragment = AdvertisementsFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit();

        setupToolbar();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentListener) {
            onFragmentListener = (OnFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDashboardFragmentListener");
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        fragment = AdvertisementsFragment.newInstance();
                        getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit();
                    }
                    return true;
                case R.id.navigation_dashboard:
                    fragment = ContactsFragment.newInstance();
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, CONTACTS_FRAGMENT).commit();
                    }
                    return true;
                case R.id.navigation_notifications:
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        fragment = NotificationsFragment.newInstance();
                        getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, NOTIFICATIONS_FRAGMENT).commit();
                    }
                    return true;
            }
            return false;
        }

    };

    private void setupToolbar() {

        if (!isAdded() || getActivity() == null) {
            return;
        }

        try {
            ((MainActivity) getActivity()).setSupportActionBar(toolbar);
        } catch (NoClassDefFoundError e) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_device_title),
                    getString(R.string.error_device_softare_description)), new Action0() {
                @Override
                public void call() {
                    getActivity().finish();
                }
            });
            return;
        }

        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle("");
        ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_dashboard, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt("pagePosition", 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                showSearchScreen();
                return false;
            case R.id.action_trades:
                showTradesScreen();
                return true;
            case R.id.action_advertise:
                createAdvertisementScreen();
                return true;
            case R.id.action_clear_notifications:
                getUnreadNotifications();
                return true;
            case R.id.action_scan:
                launchScanner();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("pagePosition", pagerPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onRefresh() {
        Timber.d("onRefresh");
        updateData();
    }

    private void updateData() {
        toast(getString(R.string.toast_refreshing_data));
        onFragmentListener.updateSyncMap(SYNC_ADVERTISEMENTS, true);
        dataService.getAdvertisements(true)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Advertisements update subscription safely unsubscribed");
                        onFragmentListener.updateSyncMap(SYNC_ADVERTISEMENTS, false);
                    }
                })
                .compose(this.<List<Advertisement>>bindUntilEvent(FragmentEvent.DESTROY))
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
                .compose(this.<List<Contact>>bindUntilEvent(FragmentEvent.DESTROY))
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
                .compose(this.<List<Notification>>bindUntilEvent(FragmentEvent.DESTROY))
                .subscribe(new Action1<List<Notification>>() {
                    @Override
                    public void call(List<Notification> notifications) {
                        onFragmentListener.updateSyncMap(SYNC_NOTIFICATIONS, false);
                        if (notifications != null) {
                            final HashMap<String, Notification> entryMap = new HashMap<>();
                            for (Notification notification : notifications) {
                                entryMap.put(notification.notification_id, notification);
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
                                                if (match.read != notificationRead || !match.url.equals(url)) {
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
                });
    }

    private void launchScanner() {
        if (isAdded() && getActivity() != null) {
            ((MainActivity) getActivity()).launchScanner();
        }
    }

    private void showSearchScreen() {
        if (isAdded() && getActivity() != null) {
            ((MainActivity) getActivity()).navigateSearchView();
        }
    }

    private void showTradesScreen() {
        Intent intent = ContactsActivity.createStartIntent(getActivity(), DashboardType.RELEASED);
        startActivity(intent);
    }

    private void createAdvertisementScreen() {
        showAlertDialog(new AlertDialogEvent(getString(R.string.view_title_advertisements), getString(R.string.dialog_edit_advertisements)), new Action0() {
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
        });
    }

    private void getUnreadNotifications() {
        showProgressDialog(new ProgressDialogEvent("Marking notifications read..."));
        dbManager.notificationsQuery()
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
                });
    }

    private void markNotificationsRead(List<NotificationItem> notificationItems) {
        for (NotificationItem notificationItem : notificationItems) {
            final String notificationId = notificationItem.notification_id();
            dataService.markNotificationRead(notificationId)
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
                    });
        }

        hideProgressDialog();
    }
}