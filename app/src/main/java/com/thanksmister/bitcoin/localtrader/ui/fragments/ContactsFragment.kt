/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactsAdapter
import com.thanksmister.bitcoin.localtrader.ui.controls.ItemClickSupport
import kotlinx.android.synthetic.main.fragment_contacts.*

import java.lang.reflect.Field
import java.util.Collections

import javax.inject.Inject

import timber.log.Timber

class ContactsFragment : BaseFragment() {

    protected var adapter: ContactsAdapter? = null
    private val contacts = emptyList<ContactItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // can't retain nested fragments
        retainInstance = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        contactsRecyclerView.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        contactsRecyclerView.layoutManager = linearLayoutManager

        ItemClickSupport.addTo(contactsRecyclerView).setOnItemClickListener { contactsRecyclerView, position, v -> showContact(adapter!!.getItemAt(position)) }

        adapter = ContactsAdapter(activity, object : ContactsAdapter.OnItemClickListener {
            override fun onSearchButtonClicked() {
                showSearchScreen()
            }
            override fun onAdvertiseButtonClicked() {
                createAdvertisementScreen()
            }
        })

        contactsRecyclerView.setAdapter(adapter)
        setupList(ArrayList<ContactItem>())
    }

    private fun setupList(items: List<ContactItem>) {
        if (isAdded && activity != null) {
            adapter!!.replaceWith(items)
            contactsRecyclerView.adapter = adapter
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        subscribeData()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDetach() {
        super.onDetach()
        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            val childFragmentManager = Fragment::class.java.getDeclaredField("mChildFragmentManager")
            childFragmentManager.isAccessible = true
            childFragmentManager.set(this, null)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    private fun subscribeData() {
        Timber.d("subscribeData")

        /*dbManager.contactsQuery()
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
                            if (TradeUtils.tradeIsActive(contactItem.closed_at(), contactItem.canceled_at())) {
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
                                contacts_history = contactItems;
                                setupList(contacts_history);
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        setupList(contacts_history);
                        reportError(throwable);
                    }
                });*/

    }

    protected fun showContact(contact: ContactItem?) {
        if (contact != null && !TextUtils.isEmpty(contact.contact_id())) {
            val intent = ContactActivity.createStartIntent(activity, contact.contact_id())
            startActivity(intent)
        } else {
            toast(getString(R.string.toast_contact_not_exist))
        }
    }

    protected fun createAdvertisementScreen() {
        /*showAlertDialog(new AlertDialogEvent(getString(R.string.view_title_advertisements), getString(R.string.dialog_edit_advertisements)), new Action0() {
            @Override
            public void call() {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Action0() {
            @Override
            public void call() {
                // na-da
            }
        });*/
    }

    protected fun showSearchScreen() {
        if (isAdded) {
            //(activity as MainActivity).navigateSearchView()
        }
    }

    companion object {

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): ContactsFragment {
            return ContactsFragment()
        }
    }
}