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

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ContactsActivity extends BaseActivity implements ContactsView, SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_TYPE";

    @Inject
    ContactsPresenter presenter;

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
        presenter.getContacts(dashboardType);
    }

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private DashboardType dashboardType;
    private ContactAdapter adapter;

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
            presenter.getContact(contact);
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
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new ContactsModule(this));
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

        presenter.onResume();

        presenter.getContacts(dashboardType);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    public void onRefresh()
    {
        presenter.getContacts(dashboardType);
    }

    @Override
    public void onRefreshStop()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    @Override
    public void onError(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);

        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }

    @Override
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_active:
                        showProgress();
                        setContacts(new ArrayList<>());
                        presenter.getContacts(DashboardType.ACTIVE);
                        return true;
                    case R.id.action_canceled:
                        showProgress();
                        setContacts(new ArrayList<>());
                        presenter.getContacts(DashboardType.CANCELED);
                        return true;
                    case R.id.action_closed:
                        showProgress();
                        setContacts(new ArrayList<>());
                        presenter.getContacts(DashboardType.CLOSED);
                        return true;
                    case R.id.action_released:
                        showProgress();
                        setContacts(new ArrayList<>());
                        presenter.getContacts(DashboardType.RELEASED);
                        return true;
                }
                return false;
            }
        });
    }

    @Override 
    public Context getContext() 
    {
        return this;
    }

    @Override
    public void showProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        empty.setVisibility(View.GONE); 
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    @Override
    public void setContacts(List<Contact> contacts)
    {
        getAdapter().replaceWith(contacts);
    }

    private ContactAdapter getAdapter()
    {
        return adapter;
    }

    private void setAdapter(ContactAdapter adapter)
    {
        list.setAdapter(adapter);
    }

    @Override
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

    
}
