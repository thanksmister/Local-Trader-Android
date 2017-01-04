/*
 * Copyright (c) 2016 ThanksMister LLC
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.RecentMessageItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;
import com.thanksmister.bitcoin.localtrader.ui.components.MessagesAdapter;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

public class MessagesFragment extends BaseFragment
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

    private MessagesAdapter itemAdapter;
    private List<RecentMessageItem> messages = Collections.emptyList();
   
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MessagesFragment newInstance()
    {
        return new MessagesFragment();
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

    private void setupList(List<RecentMessageItem> items)
    {
        if(!isAdded()) 
            return;
        
        itemAdapter.replaceWith(items);
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
                showContact(getAdapter().getItemAt(position));
            }
        });

        itemAdapter = new MessagesAdapter(getActivity(), new MessagesAdapter.OnItemClickListener()
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
    
    @Override
    public void onResume()
    {
        super.onResume();
        subscribeData();
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
                                setupList(messages);
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        setupList(messages);
                        reportError(throwable);
                    }
                });
    }

    protected MessagesAdapter getAdapter()
    {
        return itemAdapter;
    }
    
    protected void showContact(RecentMessageItem recentMessage)
    {
        if(recentMessage != null) {
            Intent intent = ContactActivity.createStartIntent(getActivity(), recentMessage.contact_id());
            intent.setClass(getActivity(), ContactActivity.class);
            startActivity(intent);
        } else {
            toast("Error opening contact...");
        }
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