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

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.network.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.trello.rxlifecycle.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class ContactsActivity extends BaseActivity {
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_NOTIFICATION_TYPE";

    @Inject
    DataService dataService;

    @BindView(R.id.recycleView)
    RecyclerView recycleView;

    @BindView(R.id.contactsToolBar)
    Toolbar toolbar;

    @BindView(R.id.emptyLayout)
    View emptyLayout;

    @BindView(R.id.resultsProgress)
    View progress;

    @BindView(R.id.emptyText)
    TextView emptyText;

    private ContactAdapter itemAdapter;
   
    private DashboardType dashboardType = DashboardType.NONE;

    private Subscription updateSubscription = Subscriptions.empty();
    private Subscription subscription = Subscriptions.empty();

    public static Intent createStartIntent(Context context, DashboardType dashboardType) {
        Intent intent = new Intent(context, ContactsActivity.class);
        intent.putExtra(EXTRA_TYPE, dashboardType);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_contacts);

        ButterKnife.bind(this);
        
        if (savedInstanceState == null) {
            dashboardType = (DashboardType) getIntent().getSerializableExtra(EXTRA_TYPE);
        } else {
            dashboardType = (DashboardType) savedInstanceState.getSerializable(EXTRA_TYPE);
        }
        
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if(getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            setToolBarMenu(toolbar);
        }

        itemAdapter = new ContactAdapter(ContactsActivity.this);
        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ContactsActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);

        ItemClickSupport.addTo(recycleView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                showContact(getAdapter().getItemAt(position));
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_TYPE, dashboardType);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbar != null)
            toolbar.inflateMenu(R.menu.contacts);

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(dashboardType != null) {
            updateData(dashboardType); 
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        subscription.unsubscribe();
        updateSubscription.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void showContent() {
        if(recycleView != null && emptyLayout != null && progress != null) {
            recycleView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            progress.setVisibility(View.GONE); 
        }
    }
    
    public void showEmpty() {
        if(recycleView != null && emptyLayout != null && progress != null && emptyText != null) {
            recycleView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            emptyText.setText(getString(R.string.text_not_trades));
        }
    }

    public void showProgress() {
        if(recycleView != null && emptyLayout != null && progress != null) {
            recycleView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
        }
    }

    public void setToolBarMenu(Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_canceled:
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.CANCELED);
                        return true;
                    case R.id.action_closed:
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.CLOSED);
                        return true;
                    case R.id.action_released:
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.RELEASED);
                        return true;
                }
                return false;
            }
        });
    }

    public void updateData(final DashboardType type) {
        
        if (!NetworkUtils.isNetworkConnected(ContactsActivity.this)) {
            handleError(new NetworkConnectionException());
            return;
        }

        toast(getString(R.string.toast_loading_trades));
        showProgress();

        subscription.unsubscribe(); // stop subscribed database data
        dashboardType = type;
        setTitle(dashboardType);
        updateSubscription = dataService.getContacts(dashboardType)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<List<Contact>>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) {
                        Timber.d("Update Data Contacts: " + contacts.size());
                        ArrayList<ContactItem> contactItems = new ArrayList<ContactItem>();
                        for (Contact contact : contacts) {
                            contactItems.add(ContactItem.convertContact(contact));
                        }
                        setContacts(contactItems);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        showEmpty();
                        toast(getString(R.string.toast_error_retrieving_trades));
                    }
                });
    }

    protected void showContact(ContactItem contact) {
        if (contact != null && !TextUtils.isEmpty(contact.contact_id())) {
            Intent intent = ContactActivity.createStartIntent(ContactsActivity.this, contact.contact_id());
            startActivity(intent);
        } else {
            toast(getString(R.string.toast_contact_not_exist));
        }
    }

    private void setContacts(List<ContactItem> contacts) {
        if(contacts.isEmpty()) {
            showEmpty();
            return;
        }

        showContent();
        getAdapter().replaceWith(contacts);
        recycleView.setAdapter(itemAdapter);
    }

    private ContactAdapter getAdapter() {
        return itemAdapter;
    }

    public void setTitle(DashboardType dashboardType) {
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

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title); 
        }
    }
}
