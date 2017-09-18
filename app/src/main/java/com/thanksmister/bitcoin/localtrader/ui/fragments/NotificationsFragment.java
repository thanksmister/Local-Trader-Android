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

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.NotificationItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.EditAdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.NotificationAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.NotificationUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.trello.rxlifecycle.FragmentEvent;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collections;
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

public class NotificationsFragment extends BaseFragment {
    
    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.recycleView)
    RecyclerView recycleView;

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected DPreference preference;

    private NotificationAdapter itemAdapter;
    private List<NotificationItem> notifications = Collections.emptyList();

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // can't retain nested fragments
        setRetainInstance(false);
        
        // clear all notification types
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(ns);
        notificationManager.cancel(NotificationUtils.NOTIFICATION_TYPE_NOTIFICATION);
    }

    @Override
    public void handleUpdate(){
        if(!isAdded()) {
            return;
        }
        toast(getString(R.string.toast_refreshing_data));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemAdapter = getAdapter();
        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);

        ItemClickSupport.addTo(recycleView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                NotificationItem notificationItem = getAdapter().getItemAt(position);
                if (notificationItem.contact_id() != null) {
                    showContact(notificationItem);
                } else if (notificationItem.advertisement_id() != null) {
                    showAdvertisement(notificationItem);
                } else {
                    try {
                        onNotificationLinkClicked(notificationItem);
                    } catch (ActivityNotFoundException e) {
                        toast(getString(R.string.text_cant_open_link));
                    }
                }
            }
        });
        recycleView.setAdapter(itemAdapter);
    }

    private void setupList(List<NotificationItem> items) {
        if (isAdded()) {
            itemAdapter.replaceWith(items);
            recycleView.setAdapter(itemAdapter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_dashboard_items, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
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

    protected void subscribeData() {
        
        dbManager.notificationsQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Notifications subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<NotificationItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<NotificationItem>>() {
                    @Override
                    public void call(final List<NotificationItem> items) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifications = items;
                                setupList(notifications);
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        setupList(notifications);
                        reportError(throwable);
                    }
                });
    }

    protected NotificationAdapter getAdapter() {
        if (itemAdapter == null) {
            itemAdapter = new NotificationAdapter(getActivity(), new NotificationAdapter.OnItemClickListener() {
                @Override
                public void onSearchButtonClicked() {
                    showSearchScreen();
                }
                @Override
                public void onAdvertiseButtonClicked() {
                    createAdvertisementScreen();
                }
            });
        }
        return itemAdapter;
    }

    private void createAdvertisementScreen() {
        Intent intent = EditAdvertisementActivity.createStartIntent(getActivity(), null, true);
        startActivityForResult(intent, EditAdvertisementActivity.REQUEST_CODE);
    }

    protected void showSearchScreen() {
        if (isAdded()) {
            ((MainActivity) getActivity()).navigateSearchView();
        }
    }
    
    private void onNotificationLinkClicked(final NotificationItem notification) {
        dataService.markNotificationRead(notification.notification_id())
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
                            dbManager.markNotificationRead(notification.notification_id());
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(!TextUtils.isEmpty(notification.url())) {
                                        launchNotificationLink(notification.url());
                                    }
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());
                    }
                });
    }
    
    private void launchNotificationLink(String url) {
        String currentEndpoint = AuthUtils.getServiceEndpoint(preference, sharedPreferences);
        Intent intent;
        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentEndpoint + url));
        startActivity(intent);
    }
    
    private void showAdvertisement(NotificationItem item) {
        if (item != null) {
            Intent intent = AdvertisementActivity.createStartIntent(getActivity(), item.advertisement_id());
            intent.setClass(getActivity(), AdvertisementActivity.class);
            startActivity(intent);
        } else {
            toast(getString(R.string.toast_error_opening_advertisement));
        }
    }

    private void showContact(NotificationItem item) {
        if (item != null) {
            Intent intent = ContactActivity.createStartIntent(getActivity(), item.contact_id());
            intent.setClass(getActivity(), ContactActivity.class);
            startActivity(intent);
        } else {
            toast(getString(R.string.toast_error_opening_contact));
        }
    }
}