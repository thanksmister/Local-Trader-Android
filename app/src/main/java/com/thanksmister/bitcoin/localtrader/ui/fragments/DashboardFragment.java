/*
 * Copyright (c) 2017 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.NotificationItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.EditAdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.trello.rxlifecycle.FragmentEvent;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dpreference.DPreference;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class DashboardFragment extends BaseFragment {

    private static final String ADVERTISEMENTS_FRAGMENT = "com.thanksmister.fragment.ADVERTISEMENTS_FRAGMENT";
    private static final String CONTACTS_FRAGMENT = "com.thanksmister.fragment.CONTACTS_FRAGMENT";
    private static final String NOTIFICATIONS_FRAGMENT = "com.thanksmister.fragment.NOTIFICATIONS_FRAGMENT";
    
    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;
    
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected DPreference preference;
    
    private int pagerPosition = 0;
    private Fragment fragment;

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
       
        Timber.d("DashboardFragment");
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

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    fragment = AdvertisementsFragment.newInstance();
                    getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, ADVERTISEMENTS_FRAGMENT).commit();
                    return true;
                case R.id.navigation_dashboard:
                    fragment = ContactsFragment.newInstance();
                    getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, CONTACTS_FRAGMENT).commit();
                    return true;
                case R.id.navigation_notifications:
                    fragment = NotificationsFragment.newInstance();
                    getChildFragmentManager().beginTransaction().replace(R.id.content, fragment, NOTIFICATIONS_FRAGMENT).commit();
                    return true;
            }
            return false;
        }

    };
    
    private void setupToolbar() {
        
        if (!isAdded()) {
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
        Timber.d("onCreateView");
        return inflater.inflate(R.layout.view_dashboard, container, false);
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
            case R.id.action_send:
                showSendScreen();
                return true;
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
        super.onSaveInstanceState(outState);
        outState.putInt("pagePosition", pagerPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        ButterKnife.reset(this);

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
        if(!isAdded() || fragment == null) {
            return;
        }
        if (fragment.getTag().equals(ADVERTISEMENTS_FRAGMENT)) {
            ((AdvertisementsFragment) fragment).handleUpdate();
        } else if (fragment.getTag().equals(CONTACTS_FRAGMENT)) {
            ((ContactsFragment) fragment).handleUpdate();
        }
    }

    private void showSendScreen() {
        if (isAdded()) {
            ((MainActivity) getActivity()).navigateSendView();
        }
    }

    private void launchScanner() {
        if (isAdded()) {
            ((MainActivity) getActivity()).launchScanner();
        }
    }

    private void showSearchScreen() {
        if (isAdded()) {
            ((MainActivity) getActivity()).navigateSearchView();
        }
    }

    private void showTradesScreen() {
        Intent intent = ContactsActivity.createStartIntent(getActivity(), DashboardType.RELEASED);
        startActivity(intent);
    }

    private void createAdvertisementScreen() {
        Intent intent = EditAdvertisementActivity.createStartIntent(getActivity(), null, true);
        startActivityForResult(intent, EditAdvertisementActivity.REQUEST_CODE);
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