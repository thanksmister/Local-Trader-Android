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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;
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
import com.thanksmister.bitcoin.localtrader.data.services.SyncUtils;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;
import com.thanksmister.bitcoin.localtrader.ui.components.SectionRecycleViewAdapter;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func3;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class DashboardFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener, AppBarLayout.OnOffsetChangedListener
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

    @InjectView(R.id.appBarLayout)
    AppBarLayout appBarLayout;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.recycleView)
    RecyclerView recycleView;
   
    @InjectView(R.id.bitcoinTitle)
    TextView bitcoinTitle;
    
    @InjectView(R.id.bitcoinPrice)
    TextView bitcoinPrice;

    @InjectView(R.id.bitcoinValue)
    TextView bitcoinValue;
   
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;
    
    /*@OnClick(R.id.advertiseButton)
    public void advertiseButtonClicked()
    {
        createAdvertisementScreen();
    }
    
    @Optional
    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        showSearchScreen();
    }*/

    private Observable<List<AdvertisementItem>> advertisementObservable;
    private Observable<List<MethodItem>> methodObservable;
    private Observable<List<ContactItem>> contactsObservable;

    private Observable<ExchangeItem> exchangeObservable;
    private Observable<List<Method>> methodUpdateObservable;
    private Observable<Exchange> exchangeUpdateObservable;
    private Observable<List<Advertisement>> advertisementUpdateObservable;

    private Subscription messageSubscriptions = Subscriptions.empty();
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private CompositeSubscription updateSubscriptions = new CompositeSubscription();
    
    private ItemAdapter itemAdapter;
    private ContentResolver contentResolver;
    private SqlBriteContentProvider sqlBriteContentProvider;
    
    private class DataItem {
        public List<AdvertisementItem> advertisements;
        public List<MethodItem> methods;
        public List<ContactItem> contacts;
        public ExchangeItem exchange;
    }

    private Handler handler;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DashboardFragment newInstance()
    {
        return new DashboardFragment();
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i)
    {
        if (i == 0) {
            swipeLayout.setEnabled(true);
        } else {
            swipeLayout.setEnabled(false);
        }
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
    
    private void setupList(DataItem dataItem)
    {
        // provide combined data
        ArrayList<Object> items = new ArrayList<>();
        ItemAdapter itemAdapter = getAdapter();
        
        if(dataItem.advertisements.isEmpty() && dataItem.contacts.isEmpty()) {
            //This is the code to provide a sectioned list
            itemAdapter.replaceWith(items, dataItem.methods);
            recycleView.setAdapter(itemAdapter);
            return;
        }
        
        items.addAll(dataItem.contacts);
        items.addAll(dataItem.advertisements);
        
        itemAdapter.replaceWith(items, dataItem.methods);
        
        //This is the code to provide a sectioned list
        List<SectionRecycleViewAdapter.Section> sections = new ArrayList<SectionRecycleViewAdapter.Section>();
        
        if(dataItem.contacts.size() > 0) {
            //int start = (dataItem.exchange == null)? 0:1;
            sections.add(new SectionRecycleViewAdapter.Section(0, getString(R.string.dashboard_active_trades_header)));
        }
        
        if(dataItem.advertisements.size() > 0) {
            //int first = (dataItem.exchange == null)? 0:1;
            int start = (dataItem.contacts.isEmpty())? 0:dataItem.contacts.size();
            sections.add(new SectionRecycleViewAdapter.Section(start, getString(R.string.dashboard_advertisements_header)));
        }

        //Add your adapter to the sectionAdapter
        SectionRecycleViewAdapter.Section[] section = new SectionRecycleViewAdapter.Section[sections.size()];
        SectionRecycleViewAdapter mSectionedAdapter = new SectionRecycleViewAdapter(getActivity(), R.layout.section, R.id.section_text, itemAdapter);
        mSectionedAdapter.setSections(sections.toArray(section));

        //Apply this adapter to the RecyclerView
        recycleView.setAdapter(mSectionedAdapter);
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
        
        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);
        recycleView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                super.onScrolled(recyclerView, dx, dy);
                int topRowVerticalPosition = (recycleView == null || recycleView.getChildCount() == 0) ? 0 : recycleView.getChildAt(0).getTop();
                swipeLayout.setEnabled(topRowVerticalPosition >= 0);
            }
        });

        ItemClickSupport.addTo(recycleView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                
                int idx = recyclerView.getChildPosition(v);
                idx  = ((SectionRecycleViewAdapter) recyclerView.getAdapter()).sectionedPositionToPosition(idx);
                Object item = getAdapter().getItemAt(idx);
                if(item instanceof ContactItem) {
                    showContact((ContactItem) item);
                } else if (item instanceof AdvertisementItem) {
                    showAdvertisement((AdvertisementItem) item);
                }
            }
        });
        
        itemAdapter = new ItemAdapter(getActivity());
        setupToolbar();
        setupFab();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode == EditActivity.REQUEST_CODE) {
            if (resultCode == EditActivity.RESULT_CREATED || resultCode == EditActivity.RESULT_UPDATED ) {
                updateData(true);
            }
        }
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
            case R.id.action_trades:
                showTradesScreen();
                return true;
            case R.id.action_advertise:
                createAdvertisementScreen();
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

        appBarLayout.addOnOffsetChangedListener(this);

        onRefreshStart();
        subscribeData();
        updateData(false);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        appBarLayout.removeOnOffsetChangedListener(this);
        
        subscriptions.unsubscribe();
        updateSubscriptions.unsubscribe();
        messageSubscriptions.unsubscribe();
        handler.removeCallbacks(refreshRunnable);
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

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.fab) {
            bus.post(NavigateEvent.QRCODE);
        } else if (view.getId() == R.id.container_list_item) {
            int idx = recycleView.getChildPosition(view);

            idx  = ((SectionRecycleViewAdapter) recycleView.getAdapter()).sectionedPositionToPosition(idx);
            Timber.d("Position: " + idx);

            Object item = getAdapter().getItemAt(idx);

            if(item instanceof ContactItem) {
                showContact((ContactItem) item);
            } else if (item instanceof AdvertisementItem) {
                showAdvertisement((AdvertisementItem) item);
            }
        }
    }

    @Override
    public void onRefresh()
    {
        updateData(true);
        SyncUtils.TriggerRefresh(getActivity().getApplicationContext());
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
                    setHeaderITem(exchangeItem);
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
        
        //Observable<List<MethodItem>> methodObservable = db.createQuery(MethodItem.TABLE, MethodItem.QUERY).map(MethodItem.MAP).cache();
        //Observable<List<AdvertisementItem>> advertisementObservable = db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY).map(AdvertisementItem.MAP);
        subscriptions.add(Observable.zip(methodObservable, contactsObservable, advertisementObservable, new Func3<List<MethodItem>, List<ContactItem>, List<AdvertisementItem>, DataItem>()
        {
            @Override
            public DataItem call(List<MethodItem> methodItems, List<ContactItem> contactItems, List<AdvertisementItem> advertisementItems)
            {
                DataItem dataItem = new DataItem();
                dataItem.contacts = contactItems;
                dataItem.advertisements = advertisementItems;
                dataItem.methods = methodItems;
                //dataItem.exchange = exchangeItem;
                return dataItem;
            }
        }).subscribe(new Action1<DataItem>()
        {
            @Override
            public void call(DataItem dataItem)
            {
                setupList(dataItem);
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
                handleError(throwable, true);
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
                handleError(throwable, true);
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
        Method method = new Method();
        method.code = "all";
        method.name = "All";
        methods.add(0, method);
      
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
    
    protected ItemAdapter getAdapter()
    {
        return itemAdapter;
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
        getActivity().startActivityForResult(intent, EditActivity.REQUEST_CODE);
    }
    
    protected void setHeaderITem(ExchangeItem exchange)
    {
        String value = Calculations.calculateAverageBidAskFormatted(exchange.bid(), exchange.ask());
        bitcoinTitle.setText("MARKET PRICE");
        bitcoinPrice.setText("$" + value + " / BTC");
        bitcoinValue.setText("Source " + exchange.exchange());
    }
}
