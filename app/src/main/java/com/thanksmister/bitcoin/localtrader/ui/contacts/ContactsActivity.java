package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactModule;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactPresenter;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactView;
import com.thanksmister.bitcoin.localtrader.ui.contact.MessageAdapter;
import com.thanksmister.bitcoin.localtrader.ui.release.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class ContactsActivity extends BaseActivity implements ContactsView
{
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_TYPE";

    @Inject
    ContactsPresenter presenter;

    @InjectView(R.id.contactsProgress)
    View progress;

    @InjectView(R.id.contactsEmpty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView errorTextView;

    @InjectView(R.id.contactsList)
    ListView list;

    @InjectView(R.id.contactsToolBar)
    Toolbar toolbar;

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

        list.setOnItemClickListener((adapterView, view, i, l) -> {
            Contact contact = (Contact) adapterView.getAdapter().getItem(i);
            presenter.getContact(contact);
        });

        adapter = new ContactAdapter(this);
        setAdapter(getAdapter());
        
        presenter.getContacts(dashboardType);
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
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_active:
                        presenter.getContacts(DashboardType.ACTIVE);
                        return true;
                    case R.id.action_canceled:
                        presenter.getContacts(DashboardType.CANCELED);
                        return true;
                    case R.id.action_closed:
                        presenter.getContacts(DashboardType.CLOSED);
                        return true;
                    case R.id.action_released:
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
    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    @Override
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
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
