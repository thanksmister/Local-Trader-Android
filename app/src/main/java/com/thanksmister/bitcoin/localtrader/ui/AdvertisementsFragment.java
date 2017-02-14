/*
 * Copyright (c) 2015 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.AdvertisementsAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class AdvertisementsFragment extends BaseFragment
{
    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;
    
    @Inject
    Bus bus;

    @InjectView(R.id.recycleView)
    RecyclerView recycleView;

    @Inject
    protected SharedPreferences sharedPreferences;

    private AdvertisementsAdapter itemAdapter;
    //private List<ContactItem> contacts = Collections.emptyList();
    private List<AdvertisementItem> advertisements = Collections.emptyList();
    //private List<RecentMessageItem> messages = Collections.emptyList();
    private List<MethodItem> methods = Collections.emptyList();

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AdvertisementsFragment newInstance()
    {
        return new AdvertisementsFragment();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // can't retain nested fragments
        setRetainInstance(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setupList(List<AdvertisementItem> advertisementItems, List<MethodItem> methodItems)
    {
        if(!isAdded()) 
            return;
        
        itemAdapter.replaceWith(advertisementItems, methodItems);
        recycleView.setAdapter(itemAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_dashboard_items, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itemAdapter = getAdapter();
        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);

        ItemClickSupport.addTo(recycleView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener()
        {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v)
            {
                AdvertisementItem advertisement = getAdapter().getItemAt(position);
                if(advertisement != null && !TextUtils.isEmpty(advertisement.ad_id())) {
                    showAdvertisement(getAdapter().getItemAt(position));
                } else {
                    toast("There was a problem loading the selected avertisement.");
                }
            }
        });

        itemAdapter = new AdvertisementsAdapter(getActivity(), new AdvertisementsAdapter.OnItemClickListener()
        {
            @Override
            public void onSearchButtonClicked()
            {
                showSearchScreen();
            }

            @Override
            public void onAdvertiseButtonClicked()
            {
                createAdvertisementScreen();
            }
        });
    }

    /*public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        Timber.d("Request Code: " + requestCode);
        Timber.d("Result Code: " + requestCode);
        
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (requestCode == EditActivity.REQUEST_CODE) {
            if (resultCode == EditActivity.RESULT_CREATED || resultCode == EditActivity.RESULT_UPDATED) {
                updateData();
            }
        } else if (requestCode == AdvertisementActivity.REQUEST_CODE) {
            if ( resultCode == AdvertisementActivity.RESULT_DELETED) {
                updateData();
            }
        }
    }*/
    
    @Override
    public void onResume()
    {
        super.onResume();
        subscribeData();
        updateData();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onDetach()
    {
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
    
    protected void subscribeData()
    {
        Timber.d("subscribeData");

        //dbManager.clearDashboard();

        /*dbManager.exchangeQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<ExchangeItem>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<ExchangeItem>()
                {
                    @Override
                    public void call(ExchangeItem exchangeItem)
                    {
                        if (exchangeItem != null) {
                            setHeaderItem(exchangeItem);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });


        dbManager.contactsQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Contacts subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<ContactItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<ContactItem>>()
                {
                    @Override
                    public void call(final List<ContactItem> contactItems)
                    {
                        Timber.d("ContactItems: " + contactItems.size());
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                contacts = contactItems;
                                setupList(contacts, messages, advertisements, methods);
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        setupList(contacts, messages, advertisements, methods);
                        reportError(throwable);
                    }
                });

        dbManager.recentMessagesQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Recent messages subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<RecentMessageItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<RecentMessageItem>>()
                {
                    @Override
                    public void call(final List<RecentMessageItem> items)
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                messages = items;
                                setupList(contacts, messages, advertisements, methods);
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        setupList(contacts, messages, advertisements, methods);
                        reportError(throwable);
                    }
                });*/

        dbManager.advertisementsQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Advertisement subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<AdvertisementItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<AdvertisementItem>>()
                {
                    @Override
                    public void call(final List<AdvertisementItem> items)
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                advertisements = items;
                                setupList(advertisements, methods);
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        setupList(advertisements, methods);
                        reportError(throwable);
                    }
                });

        dbManager.methodQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Methods subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<MethodItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<MethodItem>>()
                {
                    @Override
                    public void call(final List<MethodItem> items)
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                methods = items;
                                setupList(advertisements, methods);
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        setupList(advertisements, methods);
                        reportError(throwable);
                    }
                });
    }

    protected void updateData()
    {
        Timber.d("UpdateData");

        if (!NetworkUtils.isNetworkConnected(getActivity())) {
            handleError(new NetworkConnectionException());
            return;
        }

        CompositeSubscription updateSubscriptions = new CompositeSubscription();

        updateSubscriptions.add(dataService.getMethods()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Update Methods subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Method>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<Method>>()
                {
                    @Override
                    public void call(List<Method> results)
                    {
                        if(results == null)
                            results = new ArrayList<Method>();
                        
                        Method method = new Method();
                        method.code = "all";
                        method.name = "All";
                        results.add(0, method);

                        dbManager.updateMethods(results);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable, true);
                    }
                }));

        updateSubscriptions.add(dataService.getAdvertisements()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Update Advertisements subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Advertisement>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Advertisement>>()
                {
                    @Override
                    public void call(List<Advertisement> advertisements)
                    {
                        if (advertisements != null && !advertisements.isEmpty())
                            dbManager.updateAdvertisements(advertisements);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable, true);
                    }
                }));
    }

    protected AdvertisementsAdapter getAdapter()
    {
        return itemAdapter;
    }
    
    protected void showAdvertisement(AdvertisementItem advertisement)
    {
        Intent intent = AdvertisementActivity.createStartIntent(getActivity(), advertisement.ad_id());
        intent.setClass(getActivity(), AdvertisementActivity.class);
        startActivityForResult(intent, AdvertisementActivity.REQUEST_CODE);
    }

    protected void createAdvertisementScreen()
    {
        Intent intent = EditActivity.createStartIntent(getActivity(), true, null);
        intent.setClass(getActivity(), EditActivity.class);
        startActivityForResult(intent, EditActivity.REQUEST_CODE);
    }

    protected void showSearchScreen()
    {
        bus.post(NavigateEvent.SEARCH);
    }
}