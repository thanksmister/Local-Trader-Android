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

package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class ContactsActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_TYPE";
   
    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;
    
    @InjectView(R.id.contactsProgress)
    View progress;
    
    @InjectView(R.id.contactsList)
    ListView content;

    @InjectView(R.id.contactsToolBar)
    Toolbar toolbar;
    
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private DashboardType dashboardType = DashboardType.NONE;
    private ContactAdapter adapter;

    private Subscription updateSubscription = Subscriptions.empty();
    private Subscription subscription = Subscriptions.empty();
    
    public static Intent createStartIntent(Context context, DashboardType dashboardType)
    {
        Intent intent = new Intent(context, ContactsActivity.class);
        intent.putExtra(EXTRA_TYPE, dashboardType);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_contacts);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            dashboardType =  (DashboardType) getIntent().getSerializableExtra(EXTRA_TYPE);
        } else {
            dashboardType = (DashboardType) savedInstanceState.getSerializable(EXTRA_TYPE);
        }
        
        
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setToolBarMenu(toolbar);
        }

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        content.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                ContactItem contact = (ContactItem) adapterView.getAdapter().getItem(i);
                getContact(contact);
            }
        });

        // hack because swipe refresh and scroll up with child content don't play well together
        content.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                int topRowVerticalPosition = (content == null || content.getChildCount() == 0) ? 0 : content.getChildAt(0).getTop();
                swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });
     
        adapter = new ContactAdapter(this);
        setAdapter(getAdapter());
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_TYPE, dashboardType);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.contacts);

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        onRefreshStart();
        updateData(dashboardType);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();

        subscription.unsubscribe();
        
        updateSubscription.unsubscribe();
        
        if(progress != null)
            progress.clearAnimation();
        
        if(content != null)
            content.clearAnimation();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onRefresh()
    {
        updateData(dashboardType);
    }

    public void onRefreshStart()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    public void onRefreshStop()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }
    
    public void showContent(final boolean show)
    {
        if (progress == null || content == null)
            return;
        
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progress.setVisibility(show ? View.GONE : View.VISIBLE);
        progress.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (progress != null)
                    progress.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        content.setVisibility(show ? View.VISIBLE : View.GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (content != null)
                    content.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
    
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId()) {
                    case R.id.action_canceled:
                        showContent(false);
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.CANCELED);
                        return true;
                    case R.id.action_closed:
                        showContent(false);
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.CLOSED);
                        return true;
                    case R.id.action_released:
                        showContent(false);
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.RELEASED);
                        return true;
                }
                return false;
            }
        });
    }

    public void updateData(final DashboardType type)
    {
        subscription.unsubscribe(); // stop subscribed database data
        
        dashboardType = type;

        setTitle(dashboardType);
        
        updateSubscription = dataService.getContacts(dashboardType, true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Contact>>()
                {
                    @Override
                    public void call(List<Contact> contacts)
                    {
                        Timber.d("Update Data Contacts: " + contacts.size());
                        onRefreshStop();
                        if (contacts.isEmpty()) {
                            toast(getString(R.string.error_no_trade_data));
                        } else {
                            showContent(true);
                            ArrayList<ContactItem> contactItems = new ArrayList<ContactItem>();
                            for (Contact contact : contacts) {
                                contactItems.add(ContactItem.convertContact(contact));
                            }
                            setContacts(contactItems);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        onRefreshStop();
                        toast("Error retrieving trades.");
                    }
                });
    }
  
    private void getContact(final ContactItem contact)
    {
        Intent intent = ContactActivity.createStartIntent(ContactsActivity.this, contact.contact_id(), dashboardType);
        startActivity(intent);
    }

    private void setContacts(List<ContactItem> contacts)
    {
        getAdapter().replaceWith(contacts);
    }

    private ContactAdapter getAdapter()
    {
        return adapter;
    }

    private void setAdapter(ContactAdapter adapter)
    {
        content.setAdapter(adapter);
    }
    
    public void setTitle(DashboardType dashboardType)
    {
        String title = "";
        switch (dashboardType) {
            case RELEASED:
                title = getString(R.string.list_trade_filter2);
                break;
            case CANCELED:
                title = getString(R.string.list_trade_filter3);
                break;
            case CLOSED:
                title = getString(R.string.list_trade_filter4);
                break;
            default:
                title = "";
                break;
        }

        getSupportActionBar().setTitle(title);
    }
}
