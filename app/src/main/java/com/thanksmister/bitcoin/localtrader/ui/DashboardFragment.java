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

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.NotificationService;
import com.thanksmister.bitcoin.localtrader.data.services.SqlBriteContentProvider;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementAdapter;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.DashboardAdvertisementAdapter;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.DashboardContactAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.LinearListView;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import static rx.android.app.AppObservable.bindFragment;

public class DashboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener
{
    @Inject
    DataService dataService;

    @Inject
    NotificationService notificationService;
    
    @Inject
    DbManager dbManager;

    @Inject
    BriteDatabase db;

    @Inject
    Bus bus;

    @InjectView(R.id.fab)
    FloatingActionButton fab;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.dashContent)
    View content; 
    
    @InjectView(android.R.id.progress)
    View progress;
    
    @InjectView(R.id.bitcoinTitle)
    TextView bitcoinTitle;
    
    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;
    
    @InjectView(R.id.bitcoinLayout)
    View bitcoinLayout;

    @InjectView(R.id.advertisementList)
    LinearListView advertisementList;

    @InjectView(R.id.contactList)
    LinearListView contactsList;
    
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

    Observable<List<AdvertisementItem>> advertisementObservable;
    Observable<List<MethodItem>> methodObservable;
    Observable<List<ContactItem>> contactsObservable;

    Observable<ExchangeItem> exchangeObservable;
    //Observable<WalletItem> walletObservable;
    
    Observable<List<Method>> methodUpdateObservable;
    Observable<Exchange> exchangeUpdateObservable;
    Observable<List<Advertisement>> advertisementUpdateObservable;

    Subscription messageSubscriptions = Subscriptions.empty();
    CompositeSubscription subscriptions;
    CompositeSubscription updateSubscriptions;
    
    private DashboardContactAdapter contactAdapter;
    private DashboardAdvertisementAdapter advertisementAdapter;

    ContentResolver contentResolver;
    SqlBriteContentProvider sqlBriteContentProvider;
    
    private class AdvertisementData {
        public List<AdvertisementItem> advertisements;
        public List<MethodItem> methods;
    }

    private Handler handler;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DashboardFragment newInstance()
    {
        DashboardFragment fragment = new DashboardFragment();
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
   
        // refresh handler
        handler = new Handler();

        // sqlbrite content provider
        contentResolver = getActivity().getContentResolver();
        sqlBriteContentProvider = SqlBriteContentProvider.create(contentResolver);
        sqlBriteContentProvider.setLoggingEnabled(true);
        
        // database data
        methodObservable = bindFragment(this, dbManager.methodQuery().cache());
        advertisementObservable = bindFragment(this, dbManager.advertisementsQuery());
        contactsObservable = bindFragment(this, dbManager.contactsQuery());
        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
        
        // update data
        methodUpdateObservable = bindFragment(this, dataService.getMethods().cache());
        exchangeUpdateObservable = bindFragment(this, dataService.getExchange());
    }

    private void setupFab()
    {
        fab.setOnClickListener(this);
    }

    private void setupToolbar()
    {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);
        
        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle("");
        ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_dashboard, container, false);
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

        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                ContactItem contact = (ContactItem) contactsList.getItemAtPosition(i);
                showContact(contact);
            }
        });
        advertisementList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                AdvertisementItem advertisement = (AdvertisementItem) advertisementList.getItemAtPosition(i);
                showAdvertisement(advertisement);
            }
        });

        setupToolbar();
        setupFab();
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
    public void onResume()
    {
        super.onResume();

        onRefreshStart();
        subscribeData();
        updateData(false);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        
        subscriptions.unsubscribe();
        updateSubscriptions.unsubscribe();
        messageSubscriptions.unsubscribe();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDetach()
    {
        ButterKnife.reset(this);
        
        super.onDetach();
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.fab) {
            bus.post(NavigateEvent.QRCODE);
        }
    }

    @Override
    public void onRefresh()
    {
        onRefreshStart();
        updateData(false);
    }
    
    public void onRefreshStart()
    {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 1000);
    }
    
    private Runnable refreshRunnable = new Runnable() 
    {
        @Override
        public void run() {
            swipeLayout.setRefreshing(true);
        }
    };
    
    protected void onRefreshStop()
    {
        handler.removeCallbacks(refreshRunnable);
        swipeLayout.setRefreshing(false);
    }
    
    protected void subscribeData()
    {
        subscriptions = new CompositeSubscription();
        
        subscriptions.add(exchangeObservable.subscribe(new Action1<ExchangeItem>()
        {
            @Override
            public void call(ExchangeItem exchangeItem)
            {
                if(exchangeItem != null) {
                    setAppBarText(exchangeItem);
                }
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                reportError(throwable);
            }
        }));
        
        subscriptions.add(contactsObservable
                .subscribe(new Action1<List<ContactItem>>()
                {
                    @Override
                    public void call(List<ContactItem> contactItems)
                    {
                        setContacts(contactItems);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                }));
        
        Observable<List<MethodItem>> methodObservable = db.createQuery(MethodItem.TABLE, MethodItem.QUERY).map(MethodItem.MAP).cache();
        Observable<List<AdvertisementItem>> advertisementObservable = db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY).map(AdvertisementItem.MAP);
        
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
                setAdvertisementList(advertisementData.advertisements, advertisementData.methods);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                reportError(throwable);
            }
        }));
    }
    
    protected void updateData(Boolean force)
    {
        updateSubscriptions = new CompositeSubscription();
        updateSubscriptions.add(methodUpdateObservable.subscribe(new Action1<List<Method>>()
        {
            @Override
            public void call(List<Method> methods)
            {
                updateMethods(methods);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();
                reportError(throwable);
            }
        }));

        updateSubscriptions.add(exchangeUpdateObservable.subscribe(new Action1<Exchange>()
        {
            @Override
            public void call(Exchange exchange)
            {
                updateExchange(exchange);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();
                reportError(throwable);
            }
        }));

        advertisementUpdateObservable = bindFragment(this, dataService.getAdvertisements(force));
        
        updateSubscriptions.add(advertisementUpdateObservable.subscribe(new Action1<List<Advertisement>>()
        {
            @Override
            public void call(List<Advertisement> advertisements)
            {
                onRefreshStop();
                updateAdvertisements(advertisements);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();
                handleError(throwable, true);
            }
        }, new Action0()
        {
            @Override
            public void call()
            {
                onRefreshStop();
            }
        }));
    }
    
    private void updateMethods(List<Method> methods) 
    {
        dbManager.updateMethods(methods);
    }
    
    private void updateExchange(Exchange exchange)
    {
        dbManager.updateExchange(exchange);
    }
    
    private void updateAdvertisements(List<Advertisement> advertisements)
    {
        dbManager.updateAdvertisements(advertisements);
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

    protected DashboardContactAdapter getContactAdapter()
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

    protected void logOut()
    {
        bus.post(NavigateEvent.LOGOUT_CONFIRM);
    }

    protected void showTradesScreen()
    {
        Intent intent = ContactsActivity.createStartIntent(getActivity(), DashboardType.RELEASED);
        intent.setClass(getActivity(), ContactsActivity.class);
        getActivity().startActivity(intent);
    }

    protected void showContact(ContactItem contact)
    {
        Intent intent = ContactActivity.createStartIntent(getActivity(), contact.contact_id(), DashboardType.ACTIVE);
        intent.setClass(getActivity(), ContactActivity.class);
        getActivity().startActivity(intent);
    }
    
    protected void showAdvertisement(AdvertisementItem advertisement)
    {
        Intent intent = AdvertisementActivity.createStartIntent(getActivity(), advertisement.ad_id());
        intent.setClass(getActivity(), AdvertisementActivity.class);
        getActivity().startActivity(intent);
    }

    protected void createAdvertisementScreen()
    {
        Intent intent = EditActivity.createStartIntent(getActivity(), true, null);
        intent.setClass(getActivity(), EditActivity.class);
        getActivity().startActivity(intent);
    }
    
    protected void setAppBarText(ExchangeItem exchange)
    {
        String value = Calculations.calculateAverageBidAskFormatted(exchange.bid(), exchange.ask());
        bitcoinTitle.setText("MARKET PRICE");
        bitcoinPrice.setText("$" + value + " / BTC");
        bitcoinValue.setText("Source " + exchange.exchange());
    }
}
