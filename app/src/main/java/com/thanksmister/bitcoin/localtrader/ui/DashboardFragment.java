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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.NotificationService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class DashboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    private static final String ARG_SECTION_NUMBER = "section_number";
    
    @Inject
    DataService dataService;

    @Inject
    NotificationService notificationService;
    
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
    //private Observable<List<Contact>> contactUpdateObservable;
    private Observable<List<Method>> methodUpdateObservable;
    private Observable<Exchange> exchangeUpdateObservable;

    Subscription messageSubscriptions = Subscriptions.empty();
    CompositeSubscription subscriptions;
    CompositeSubscription updateSubscriptions;
    
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
    
    private Handler handler;

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

        // refresh handler
        handler = new Handler();
        
        // database data
        methodObservable = bindFragment(this, dbManager.methodQuery().cache());
        contactObservable = bindFragment(this, dbManager.contactsQuery());
        advertisementObservable = bindFragment(this, dbManager.advertisementQuery());
        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
     
        // update data
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
                
        subscribeData();

        updateData(false);

        onRefreshStart();
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
    public void onRefresh()
    {
        updateData(true);
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
                Timber.e("Contacts Error");
                reportError(throwable);
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
                Timber.e("Exchange Error");
                reportError(throwable);
            }
        }));
    }
    
    protected void updateData(Boolean force)
    {
        Timber.d("UpdateData");
        
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
                Timber.e("Methods Error");

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
                Timber.e("Exchange Error");
                
                onRefreshStop();
                
                handleError(throwable);
            }
        }));

        Observable<List<Contact>> contactUpdateObservable = bindFragment(this, dataService.getContacts(DashboardType.ACTIVE, force));
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
        }).subscribe(new Action1<dashboardData>()
        {
            @Override
            public void call(dashboardData data)
            {
                onRefreshStop();
                
                // update in database background
                updateAdvertisements(data.advertisements);

                updateMessages(data.contacts);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                Timber.e("Advertisement Error");
                
                onRefreshStop();
                
                handleError(throwable);
            }
        }, new Action0() // on complete
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

    // update messages from contacts
    private void updateMessages(final List<Contact> contacts)
    {
        ArrayList<Contact> messageContacts = new ArrayList<>();
        for (Contact contact : contacts) {
            if(!contact.messages.isEmpty()) {
                messageContacts.add(contact);
            }
        }
        if(!messageContacts.isEmpty()) {
            messageSubscriptions = dbManager.updateMessagesFromContacts(messageContacts).subscribe(new Action1<List<Message>>()
            {
                @Override
                public void call(List<Message> messages)
                {
                    notificationService.messageNotifications(messages);

                    updateContacts(contacts);
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    Timber.e("Messages Error");

                    reportError(throwable);

                    updateContacts(contacts);
                }
            });
        }
    }
    
    private void updateContacts(List<Contact> contacts)
    {
        if (!contacts.isEmpty()) {
            
            TreeMap<String, ArrayList<Contact>> updatedContactList = dbManager.updateContacts(contacts);
            
            ArrayList<Contact> updatedContacts = updatedContactList.get(DbManager.UPDATES);
            if(!updatedContacts.isEmpty()) {
                Timber.d("updated contacts: " + updatedContacts.size());
                notificationService.contactUpdateNotification(updatedContacts);
            }
            
            ArrayList<Contact> addedContacts = updatedContactList.get(DbManager.ADDITIONS);
            if(!addedContacts.isEmpty()) {
                Timber.d("added contacts: " + addedContacts.size());
                notificationService.contactNewNotification(addedContacts);
            }
            ArrayList<Contact> deletedContacts = updatedContactList.get(DbManager.DELETIONS);
            if (!deletedContacts.isEmpty()) {
                Timber.d("deleted contacts: " + deletedContacts.size());
                notificationService.contactDeleteNotification(deletedContacts);
            }
        }
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

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        if(event == NetworkEvent.DISCONNECTED) {
            toast(R.string.error_no_internet);
        }
    }
}
