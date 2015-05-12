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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.DashboardAdvertisementAdapter;
import com.thanksmister.bitcoin.localtrader.ui.contacts.DashboardContactAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.LinearListView;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementAdapter;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import retrofit.client.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class DashboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    private static final String ARG_SECTION_NUMBER = "section_number";
    
    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;

    @InjectView(R.id.dashContent)
    View content; 
    
    @InjectView(android.R.id.progress)
    View progress;

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

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @OnClick(R.id.emptyTradesLayout)
    public void emptyTradesButtonClicked()
    {
        showTradesScreen();
    }

    @OnClick(R.id.tradesButton)
    public void tradesButtonClicked()
    {
        showTradesScreen();
    }

    @OnClick(R.id.advertisementsButton)
    public void advertisementsButtonClicked()
    {
        createAdvertisementScreen();
    }

    @OnClick(R.id.emptyAdvertisementsLayout)
    public void emptyAdvertisementsButtonClicked()
    {
        createAdvertisementScreen();
    }

    @OnClick(R.id.dashboardFloatingButton)
    public void scanButtonClicked()
    {
        scanQrCode();
    }

    @Optional
    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        showSearchScreen();
    }

    @Optional
    @OnClick(R.id.advertiseButton)
    public void advertiseButtonClicked()
    {
        createAdvertisementScreen();
    }

    private Observable<List<AdvertisementItem>> advertisementObservable;
    private Observable<List<MethodItem>> methodObservable;
    private Observable<List<ContactItem>> contactObservable;
   
    private Observable<ExchangeItem> exchangeObservable;

    private Observable<List<Contact>> contactUpdateObservable;
    private Observable<List<Method>> methodUpdateObservable;
    private Observable<Exchange> exchangeUpdateObservable;
  
    CompositeSubscription subscriptions = new CompositeSubscription();
    CompositeSubscription updateSubscriptions = new CompositeSubscription();

    private DashboardContactAdapter contactAdapter;
    private DashboardAdvertisementAdapter advertisementAdapter;

    
    private class AdvertisementData {
        public List<AdvertisementItem> advertisements;
        public List<MethodItem> methods;
    }

    private class dashboardData
    {
        public List<Advertisement> advertisements;
        public List<Contact> contacts;
    }

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
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // database data
        methodObservable = bindFragment(this, dbManager.methodQuery().cache());
        contactObservable = bindFragment(this, dbManager.contactsQuery());
        advertisementObservable = bindFragment(this, dbManager.advertisementQuery());
        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
      
        
        // update data
        contactUpdateObservable = bindFragment(this, dataService.getContacts(DashboardType.ACTIVE));
        methodUpdateObservable = bindFragment(this, dataService.getMethods().cache());
        exchangeUpdateObservable = bindFragment(this, dataService.getExchange());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_dashboard, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) 
    {
        super.onViewCreated(view, savedInstanceState);
        
        showProgress();

        subscribeData();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        contactAdapter = new DashboardContactAdapter(getActivity());
        advertisementAdapter = new DashboardAdvertisementAdapter(getActivity());

        setAdvertisementAdapter(advertisementAdapter);
        setContactAdapter(contactAdapter);

        contactsList.setOnItemClickListener((adapterView, view, position, l) -> {
            ContactItem contact = (ContactItem) contactsList.getItemAtPosition(position);
            showContact(contact);
        });

        advertisementList.setOnItemClickListener((adapterView, view, position, l) -> {
            AdvertisementItem advertisement = (AdvertisementItem) advertisementList.getItemAtPosition(position);
            showAdvertisement(advertisement);
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
                showSearchScreen();
                return false;
            case R.id.action_send:
                showSendScreen();
                return true;
            case R.id.action_logout:
                logOut();
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
        
        updateData(false);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onDetach()
    {
        ButterKnife.reset(this);

        if(subscriptions != null) 
            subscriptions.clear();
        
        if(updateSubscriptions != null)
            updateSubscriptions.clear();
        
        super.onDetach();
    }

    public Context getContext()
    {
        return getActivity();
    }

    @Override
    public void onRefresh()
    {
        updateData(true);
    }

    protected void onRefreshStart()
    {
        swipeLayout.setRefreshing(true);
    }

    protected void onRefreshStop()
    {
        hideProgress();
        swipeLayout.setRefreshing(false);
    }
    
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    public void hideProgress()
    {
        content.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
    }
    
    protected void subscribeData()
    {
        /*if(subscriptions.hasSubscriptions()) {
            subscriptions.clear();
            subscriptions = new CompositeSubscription(); 
        }
*/
        subscriptions.add(Observable.combineLatest(methodObservable, advertisementObservable, new Func2<List<MethodItem>, List<AdvertisementItem>, AdvertisementData>()
        {
            @Override
            public AdvertisementData call(List<MethodItem> methods, List<AdvertisementItem> advertisements)
            {
                AdvertisementData advertisementData = new AdvertisementData();
                advertisementData.methods = methods;
                advertisementData.advertisements = advertisements;
                return advertisementData;
            }
        }).subscribe(new Action1<AdvertisementData>()
        {
            @Override
            public void call(AdvertisementData advertisementData)
            {
                onRefreshStop();

                setAdvertisementList(advertisementData.advertisements, advertisementData.methods);
            }
        }));
        
        subscriptions.add(contactObservable.subscribe(new Action1<List<ContactItem>>()
        {
            @Override
            public void call(List<ContactItem> items)
            {
                Timber.d("Contacts: " + items.size());
                setContacts(items);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e(throwable.getMessage());
            }
        }));

        subscriptions.add(exchangeObservable.subscribe(new Action1<ExchangeItem>()
        {
            @Override
            public void call(ExchangeItem exchange)
            {
                if (exchange != null) {
                    String value = Calculations.calculateAverageBidAskFormatted(exchange.bid(), exchange.ask());
                    setMarketValue(exchange.exchange(), value);
                }
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e(throwable.getMessage());
            }
        }));
    }
    
    protected void updateData(boolean force)
    {
        updateSubscriptions.add(exchangeUpdateObservable.subscribe(new Action1<Exchange>()
        {
            @Override
            public void call(Exchange exchange)
            {
                dbManager.updateExchange(exchange);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e(throwable.getMessage());
            }
        }));
        
        updateSubscriptions.add(methodUpdateObservable.subscribe(new Action1<List<Method>>()
        {
            @Override
            public void call(List<Method> methods)
            {
                dbManager.updateMethods(methods);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e(throwable.getMessage());
            }
        }));

        Observable<List<Advertisement>> advertisementUpdateObservable = bindFragment(this, dataService.getAdvertisements(force));
        updateSubscriptions.add(Observable.combineLatest(contactUpdateObservable, advertisementUpdateObservable, new Func2<List<Contact>, List<Advertisement>, dashboardData>()
        {
            @Override
            public dashboardData call(List<Contact> contacts, List<Advertisement> advertisements)
            {
                dashboardData data = new dashboardData();
                data.contacts = contacts;
                data.advertisements = advertisements;
                return data;
            }
        }).subscribe(new Action1<dashboardData>() {
            @Override
            public void call(dashboardData data)
            {
                onRefreshStop();

                // update in database background
                updateAdvertisements(data.advertisements);
                updateContacts(data.contacts);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();

                handleError(throwable);
            }
        }));
    }
    
    private void updateAdvertisements(List<Advertisement> advertisements)
    {
        Observable<Boolean> observable;
        observable = bindFragment(this, dbManager.updateAdvertisements(advertisements));
        observable.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                // great!
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable.getMessage());
            }
        });
    }
    
    private void updateContacts(List<Contact> contacts)
    {
        Observable<TreeMap<String, ArrayList<Contact>>> updateContactObservable;
        updateContactObservable = bindFragment(this, dbManager.updateContacts(contacts));
        updateContactObservable.subscribe(new Action1<TreeMap<String, ArrayList<Contact>>>() {
            @Override
            public void call(TreeMap<String, ArrayList<Contact>> stringArrayListTreeMap)
            {
                // TODO can handle notifications here if needed
            }
        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable throwable)
            {
                Timber.e(throwable.getMessage());
            }
        });
    }

    protected void setMarketValue(String exchange, String value)
    {
        if (bitcoinPrice != null) {
            bitcoinPrice.setText("$" + value + " / BTC");
            bitcoinValue.setText("Source " + exchange);
        }
    }

    protected void setContacts(List<ContactItem> data)
    {
        if(emptyTradesLayout != null) {
            emptyTradesLayout.setVisibility((data.size() == 0) ? View.VISIBLE : View.GONE);
            getContactAdapter().replaceWith(data);
        }  
    }

    protected void setAdvertisementList(List<AdvertisementItem> advertisements, List<MethodItem> methods)
    {
        
        if(emptyAdvertisementsLayout != null) {
            emptyAdvertisementsLayout.setVisibility((advertisements.size() == 0) ? View.VISIBLE : View.GONE);
            getAdvertisementAdapter().replaceWith(advertisements, methods);
        }
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

    protected AdvertisementAdapter getAdvertisementAdapter()
    {
        return advertisementAdapter;
    }

    protected void showSendScreen()
    {
        bus.post(NavigateEvent.SEND);
    }

    protected void showSearchScreen()
    {
        bus.post(NavigateEvent.SEARCH);
    }

    public void scanQrCode()
    {
        bus.post(NavigateEvent.QRCODE);
    }

    protected void logOut()
    {
        bus.post(NavigateEvent.LOGOUT_CONFIRM);
    }

    protected void showTradesScreen()
    {
        Intent intent = ContactsActivity.createStartIntent(getContext(), DashboardType.RELEASED);
        intent.setClass(getContext(), ContactsActivity.class);
        getContext().startActivity(intent);
    }

    protected void showContact(ContactItem contact)
    {
        Intent intent = ContactActivity.createStartIntent(getContext(), contact.contact_id(), DashboardType.ACTIVE);
        intent.setClass(getContext(), ContactActivity.class);
        getContext().startActivity(intent);
    }
    
    protected void showAdvertisement(AdvertisementItem advertisement)
    {
        Intent intent = AdvertisementActivity.createStartIntent(getContext(), advertisement.ad_id());
        intent.setClass(getContext(), AdvertisementActivity.class);
        getContext().startActivity(intent);
    }

    protected void createAdvertisementScreen()
    {
        Intent intent = EditActivity.createStartIntent(getContext(), true, null);
        intent.setClass(getContext(), EditActivity.class);
        getContext().startActivity(intent);
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        if(event == NetworkEvent.DISCONNECTED) {
            toast(R.string.error_no_internet);
        }
    }
}
