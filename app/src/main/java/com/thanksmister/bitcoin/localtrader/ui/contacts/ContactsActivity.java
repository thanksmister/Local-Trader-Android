package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.functions.Action1;

import static rx.android.app.AppObservable.bindActivity;

public class ContactsActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_TYPE";

    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;
    
    @InjectView(R.id.contactsProgress)
    View progress;

    @InjectView(R.id.contactsEmpty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

    @InjectView(R.id.contactsList)
    ListView list;

    @InjectView(R.id.contactsToolBar)
    Toolbar toolbar;

    @OnClick(R.id.emptyRetryButton)
    public void emptyButtonClicked()
    {
        updateData(dashboardType);
    }

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private DashboardType dashboardType;
    private AdvertisementAdapter.ContactAdapter adapter;
    private Observable<List<ContactItem>> contactItemObservable;
    private Observable<List<Contact>> contactsObservable;
    
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

        list.setOnItemClickListener((adapterView, view, i, l) -> {
            Contact contact = (Contact) adapterView.getAdapter().getItem(i);
            getContact(contact);
        });

        adapter = new AdvertisementAdapter.ContactAdapter(this);
        setAdapter(getAdapter());
        
        contactItemObservable = bindActivity(this, dbManager.contactsQuery());
 
        subscribeData();
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

        updateData(dashboardType);
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
            swipeLayout.setRefreshing(true);
    }
    
    public void onRefreshStop()
    {
        hideProgress();
        
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }
    
    public void onError(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);

        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }
    
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId()) {
                    case R.id.action_active:
                        showProgress();
                        setContacts(new ArrayList<>());
                        updateData(DashboardType.ACTIVE);
                        return true;
                    case R.id.action_canceled:
                        showProgress();
                        setContacts(new ArrayList<>());
                        updateData(DashboardType.CANCELED);
                        return true;
                    case R.id.action_closed:
                        showProgress();
                        setContacts(new ArrayList<>());
                        updateData(DashboardType.CLOSED);
                        return true;
                    case R.id.action_released:
                        showProgress();
                        setContacts(new ArrayList<>());
                        updateData(DashboardType.RELEASED);
                        return true;
                }
                return false;
            }
        });
    }
    
    public void showProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }
    
    public void hideProgress()
    {
        empty.setVisibility(View.GONE); 
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }
    
    public void subscribeData()
    {
        contactItemObservable.subscribe(new Action1<List<ContactItem>>()
        {
            @Override
            public void call(List<ContactItem> contactItems)
            {
                if(dashboardType == DashboardType.ACTIVE) {
                    ArrayList<Contact> contacts = new ArrayList<Contact>();
                    for (ContactItem item : contactItems)
                    {
                        Contact contact = new Contact();
                        contacts.add(contact.convertContentItemToContact(item));
                    }
                    
                    if(contacts.isEmpty()) {
                        onError(getString(R.string.error_no_trade_data));
                    } else {
                        setContacts(contacts); 
                    }
                    
                }
            }
        });
    }

    public void updateData(DashboardType dashboardType)
    {
        onRefreshStart();
                
        this.dashboardType = dashboardType;

        contactsObservable = bindActivity(this, dataService.getContacts(dashboardType));
        
        setTitle(dashboardType);

        contactsObservable.subscribe(new Action1<List<Contact>>()
        {
            @Override
            public void call(List<Contact> contacts)
            {
                if(dashboardType == DashboardType.ACTIVE) {
                    dbManager.updateContacts(contacts); //update contacts and messages if active 
                }
                
                if(contacts.isEmpty()) {
                   onError(getString(R.string.error_no_trade_data));
                } else {
                    setContacts(contacts); 
                }
                
                onRefreshStop();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                handleError(throwable);

                if(dashboardType != DashboardType.ACTIVE) {
                    onError(getString(R.string.error_no_trade_data));
                }
                
                onRefreshStop();
            }
        });
    }

    public void getContact(final Contact contact)
    {
        Intent intent = ContactActivity.createStartIntent(ContactsActivity.this, contact.contact_id, dashboardType);
        startActivity(intent);
    }

    public void setContacts(List<Contact> contacts)
    {
        getAdapter().replaceWith(contacts);
    }

    private AdvertisementAdapter.ContactAdapter getAdapter()
    {
        return adapter;
    }

    private void setAdapter(AdvertisementAdapter.ContactAdapter adapter)
    {
        list.setAdapter(adapter);
    }
    
    public void setTitle(DashboardType dashboardType)
    {
        String title = "";
        switch (dashboardType) {
            case ACTIVE:
                title = getString(R.string.list_trade_filter1);
                break;
            case RELEASED:
                title = getString(R.string.list_trade_filter2);
                break;
            case CANCELED:
                title = getString(R.string.list_trade_filter3);
                break;
            case CLOSED:
                title = getString(R.string.list_trade_filter4);
                break;
        }

        getSupportActionBar().setTitle(title);
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        if(event == NetworkEvent.DISCONNECTED) {
           toast(R.string.error_no_internet);
        } 
    }
}
