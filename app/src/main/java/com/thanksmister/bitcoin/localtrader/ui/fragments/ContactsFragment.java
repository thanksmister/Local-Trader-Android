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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.EditAdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactsAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class ContactsFragment extends BaseFragment {
    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.recycleView)
    RecyclerView recycleView;

    @Inject
    protected SharedPreferences sharedPreferences;

    private ContactsAdapter itemAdapter;
    private List<ContactItem> contacts = Collections.emptyList();

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // can't retain nested fragments
        setRetainInstance(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setupList(List<ContactItem> items) {
        if (!isAdded())
            return;

        itemAdapter.replaceWith(items);
        recycleView.setAdapter(itemAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_dashboard_items, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        itemAdapter = getAdapter();
        recycleView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycleView.setLayoutManager(linearLayoutManager);

        ItemClickSupport.addTo(recycleView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                showContact(getAdapter().getItemAt(position));
            }
        });

        itemAdapter = new ContactsAdapter(getActivity(), new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onSearchButtonClicked() {
                showSearchScreen();
            }

            @Override
            public void onAdvertiseButtonClicked() {
                createAdvertisementScreen();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
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
    public void handleUpdate(){
        if(!isAdded()) {
            return;
        }
        toast(getString(R.string.toast_refreshing_data));
    }

    private void subscribeData() {
        Timber.d("subscribeData");

        dbManager.contactsQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Contacts subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<ContactItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .flatMap(new Func1<List<ContactItem>, Observable<List<ContactItem>>>() {
                    @Override
                    public Observable<List<ContactItem>> call(List<ContactItem> contactItems) {
                        // filter for only active trades
                        List<ContactItem> activeContacts = new ArrayList<ContactItem>();
                        for (ContactItem contactItem : contactItems) {
                            if(TradeUtils.isActiveTrade(contactItem.closed_at(), contactItem.canceled_at())) {
                                activeContacts.add(contactItem);
                            }
                        }
                        return Observable.just(activeContacts);
                    }
                })
                .subscribe(new Action1<List<ContactItem>>() {
                    @Override
                    public void call(final List<ContactItem> contactItems) {
                        Timber.d("ContactItems: " + contactItems.size());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                contacts = contactItems;
                                setupList(contacts);
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        setupList(contacts);
                        reportError(throwable);
                    }
                });

    }

    protected ContactsAdapter getAdapter() {
        return itemAdapter;
    }

    protected void showContact(ContactItem contact) {
        if (contact != null && !TextUtils.isEmpty(contact.contact_id())) {
            Intent intent = ContactActivity.createStartIntent(getActivity(), contact.contact_id());
            startActivity(intent);
        } else {
            toast(getString(R.string.toast_contact_not_exist));
        }
    }

    protected void createAdvertisementScreen() {
        Intent intent = EditAdvertisementActivity.createStartIntent(getActivity(), null, true);
        startActivityForResult(intent, EditAdvertisementActivity.REQUEST_CODE);
    }

    protected void showSearchScreen() {
        if (isAdded()) {
            ((MainActivity) getActivity()).navigateSearchView();
        }
    }
}