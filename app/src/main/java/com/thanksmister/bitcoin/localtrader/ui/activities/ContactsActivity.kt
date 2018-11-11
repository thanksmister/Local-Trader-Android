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

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport


import java.util.ArrayList

import javax.inject.Inject

import timber.log.Timber

class ContactsActivity : BaseActivity() {

    internal var recycleView: RecyclerView? = null
    internal var emptyLayout: View? = null
    internal var progress: View? = null
    internal var emptyText: TextView? = null

    private var adapter: ContactAdapter? = null

    private var dashboardType: DashboardType? = DashboardType.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_contacts)

        if (savedInstanceState == null) {
            dashboardType = intent.getSerializableExtra(EXTRA_TYPE) as DashboardType
        } else {
            dashboardType = savedInstanceState.getSerializable(EXTRA_TYPE) as DashboardType
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        adapter = ContactAdapter(this@ContactsActivity)
        recycleView!!.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(this@ContactsActivity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        recycleView!!.layoutManager = linearLayoutManager

        ItemClickSupport.addTo(recycleView!!).setOnItemClickListener { recyclerView, position, v -> showContact(adapter!!.getItemAt(position)) }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_TYPE, dashboardType)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contacts, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_canceled -> {
                setContacts(ArrayList())
                updateData(DashboardType.CANCELED)
                return true
            }
            R.id.action_closed -> {
                setContacts(ArrayList())
                updateData(DashboardType.CLOSED)
                return true
            }
            R.id.action_released -> {
                setContacts(ArrayList())
                updateData(DashboardType.RELEASED)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        super.onResume()
        if (dashboardType != null) {
            updateData(dashboardType)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    fun showContent() {
        if (recycleView != null && emptyLayout != null && progress != null) {
            recycleView!!.visibility = View.VISIBLE
            emptyLayout!!.visibility = View.GONE
            progress!!.visibility = View.GONE
        }
    }

    fun showEmpty() {
        if (recycleView != null && emptyLayout != null && progress != null && emptyText != null) {
            recycleView!!.visibility = View.GONE
            emptyLayout!!.visibility = View.VISIBLE
            progress!!.visibility = View.GONE
            emptyText!!.text = getString(R.string.text_not_trades)
        }
    }

    fun showProgress() {
        if (recycleView != null && emptyLayout != null && progress != null) {
            recycleView!!.visibility = View.GONE
            emptyLayout!!.visibility = View.GONE
            progress!!.visibility = View.VISIBLE
        }
    }


    /*public void setToolBarMenu(Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_canceled:
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.CANCELED);
                        return true;
                    case R.id.action_closed:
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.CLOSED);
                        return true;
                    case R.id.action_released:
                        setContacts(new ArrayList<ContactItem>());
                        updateData(DashboardType.RELEASED);
                        return true;
                }
                return false;
            }
        });
    }*/

    private fun updateData(type: DashboardType?) {

        toast(getString(R.string.toast_loading_trades))
        showProgress()
        /*
        subscription.unsubscribe(); // stop subscribed database data
        dashboardType = type;
        setTitle(dashboardType);
        updateSubscription = dataService.getContacts(dashboardType)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Contact>>() {
                    @Override
                    public void call(List<Contact> contacts) {
                        Timber.d("Update Data Contacts: " + contacts.size());
                        ArrayList<ContactItem> contactItems = new ArrayList<ContactItem>();
                        for (Contact contact : contacts) {
                            contactItems.add(ContactItem.convertContact(contact));
                        }
                        setContacts(contactItems);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        showEmpty();
                        toast(getString(R.string.toast_error_retrieving_trades));
                    }
                });*/
    }

    protected fun showContact(contact: Contact?) {
        if (contact != null && contact.contactId != 0) {
            val intent = ContactActivity.createStartIntent(this@ContactsActivity, contact.contactId)
            startActivity(intent)
        } else {
            toast(getString(R.string.toast_contact_not_exist))
        }
    }

    private fun setContacts(contacts: List<Contact>) {
        if (contacts.isEmpty()) {
            showEmpty()
            return
        }

        showContent()
        adapter!!.replaceWith(contacts)
        recycleView!!.adapter = adapter
    }

    fun setTitle(dashboardType: DashboardType) {
        var title = ""
        when (dashboardType) {
            DashboardType.RELEASED -> title = getString(R.string.list_trade_filter2)
            DashboardType.CANCELED -> title = getString(R.string.list_trade_filter3)
            DashboardType.CLOSED -> title = getString(R.string.list_trade_filter4)
            else -> title = ""
        }

        if (supportActionBar != null) {
            supportActionBar!!.title = title
        }
    }

    companion object {
        val EXTRA_TYPE = "com.thanksmister.extras.EXTRA_NOTIFICATION_TYPE"

        fun createStartIntent(context: Context, dashboardType: DashboardType): Intent {
            val intent = Intent(context, ContactsActivity::class.java)
            intent.putExtra(EXTRA_TYPE, dashboardType)
            return intent
        }
    }
}
