package com.thanksmister.bitcoin.localtrader.ui.dashboard;

import android.app.Activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.ui.main.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.misc.AdvertisementAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.ContactAdapter;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public class DashboardFragment extends BaseFragment implements DashboardView
{
    public static final String EXTRA_METHODS = "com.thanksmister.extras.EXTRA_METHODS";
    private static final String ARG_SECTION_NUMBER = "section_number";

    @Inject
    DashboardPresenter presenter;

    @InjectView(R.id.dashContent)
    ScrollView content; 
    
    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView errorTextView;

    @InjectView(R.id.advertisementList)
    LinearListView advertisementList;

    @InjectView(R.id.contactList)
    LinearListView contactsList;

    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;

    @InjectView(R.id.tradesLayout)
    View tradesLayout;

    @InjectView(R.id.emptyAdvertisementsLayout)
    View emptyAdvertisementsLayout;

    @InjectView(R.id.emptyTradesLayout)
    View emptyTradesLayout;

    @InjectView(R.id.advertisementsLayout)
    View advertisementsLayout;

    @OnClick(R.id.emptyTradesLayout)
    public void emptyTradesButtonClicked()
    {
        presenter.showTradesScreen();
    }

    @OnClick(R.id.tradesButton)
    public void tradesButtonClicked()
    {
        presenter.showTradesScreen();
    }

    @OnClick(R.id.advertisementsButton)
    public void advertisementsButtonClicked()
    {
        presenter.createAdvertisementScreen();
    }

    @OnClick(R.id.emptyAdvertisementsLayout)
    public void emptyAdvertisementsButtonClicked()
    {
        presenter.createAdvertisementScreen();
    }

    @OnClick(R.id.dashboardFloatingButton)
    public void scanButtonClicked()
    {
        presenter.scanQrCode();
    }

    @Optional
    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        presenter.showSearchScreen();
    }

    @Optional
    @OnClick(R.id.advertiseButton)
    public void advertiseButtonClicked()
    {
        presenter.createAdvertisementScreen();
    }

    private DashboardContactAdapter contactAdapter;
    private DashboardAdvertisementAdapter advertisementAdapter;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DashboardFragment newInstance(int sectionNumber)
    {
        DashboardFragment fragment = new DashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public DashboardFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.view_dashboard, container, false);
        ButterKnife.inject(this, rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        contactAdapter = new DashboardContactAdapter(getActivity());
        advertisementAdapter = new DashboardAdvertisementAdapter(getActivity());

        setAdvertisementAdapter(advertisementAdapter);
        setContactAdapter(contactAdapter);

        contactsList.setOnItemClickListener((adapterView, view, position, l) -> {
            Contact contact = (Contact) contactsList.getItemAtPosition(position);
            Timber.d("Contact: " + contact.seller.username);
            presenter.showContact(contact);
        });

        advertisementList.setOnItemClickListener((adapterView, view, position, l) -> {
            Advertisement advertisement = (Advertisement) advertisementList.getItemAtPosition(position);
            Timber.d("Advertisement: " + advertisement.profile.username);
            
            presenter.showAdvertisement(advertisement);
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
    {
        inflater.inflate(R.menu.dashboard, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_search:
                presenter.showSearchScreen();
                return false;
            case R.id.action_send:
                presenter.showSendScreen();
                return true;
            case R.id.action_logout:
                presenter.logOut();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        Timber.d("onResume");

        presenter.onResume();
    }

    @Override
    public void onPause()
    {
        super.onResume();

        Timber.d("onDestroy");

        presenter.onDestroy();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);

       // Timber.d("onDestroy");

        //presenter.onDestroy();
    }

    @Override
    public Context getContext()
    {
        return getActivity();
    }

    @Override
    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    @Override
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    @Override
    public void setDashboard(Dashboard dashboard, List<Method> methods)
    {
        setContacts(dashboard.contacts);
        setAdvertisementList(dashboard.advertisements, methods);
        setMarketValue(dashboard);
    }

    protected void setMarketValue(Dashboard dashboard)
    {
        if(bitcoinPrice != null) {
            bitcoinPrice.setText("$" + dashboard.getBitstampValue() + " / BTC");
            bitcoinValue.setText("Source " + dashboard.exchange.name);
        }
    }

    protected void setContacts(List<Contact> data)
    {
        if(emptyTradesLayout != null) {
            emptyTradesLayout.setVisibility((data.size() == 0)? View.VISIBLE:View.GONE);
            getContactAdapter().replaceWith(data);
        }  
    }

    protected void setAdvertisementList(List<Advertisement> advertisements, List<Method> methods)
    {
        if(emptyAdvertisementsLayout != null) {
            emptyAdvertisementsLayout.setVisibility((advertisements.size() == 0) ? View.VISIBLE : View.GONE);
            getAdvertisementAdapter().replaceWith(advertisements, methods);
        }
    }

    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new DashboardModule(this));
    }

    protected void setContactAdapter(DashboardContactAdapter adapter)
    {
        contactsList.setAdapter(adapter);
    }

    protected void setAdvertisementAdapter(DashboardAdvertisementAdapter adapter)
    {
        advertisementList.setAdapter(adapter);
    }

    protected ContactAdapter getContactAdapter()
    {
        return contactAdapter;
    }

    public AdvertisementAdapter getAdvertisementAdapter()
    {
        return advertisementAdapter;
    }
}
