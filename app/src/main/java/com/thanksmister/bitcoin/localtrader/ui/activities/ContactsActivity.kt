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


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.DashboardType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.ContactsViewModel
import kotlinx.android.synthetic.main.view_contacts.*
import kotlinx.android.synthetic.main.view_empty.*
import java.util.*
import javax.inject.Inject

class ContactsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: ContactsViewModel

    private var connectionLiveData: ConnectionLiveData? = null

    private val adapter: ContactAdapter by lazy {
        ContactAdapter(this@ContactsActivity)
    }

    private var dashboardType: DashboardType? = null

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

        val linearLayoutManager = LinearLayoutManager(this@ContactsActivity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        contactsRecycleView.layoutManager = linearLayoutManager

        contactsRecycleView.setHasFixedSize(true)

        ItemClickSupport.addTo(contactsRecycleView).setOnItemClickListener { recyclerView, position, v ->
            showContact(adapter.getItemAt(position))
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ContactsViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun observeViewModel(viewModel: ContactsViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message?.message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@ContactsActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@ContactsActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(message)
            }
        })
        viewModel.getContactsList().observe(this, Observer { list ->
            list?.let {
                dialogUtils.hideProgressDialog()
                if(it.isEmpty()) {
                    showEmpty()
                } else {
                    setContacts(it)
                }
            }
        })
        dashboardType?.let {
            updateData(it)
        }
    }

    override fun onStart() {
        super.onStart()
        connectionLiveData = ConnectionLiveData(this@ContactsActivity)
        connectionLiveData?.observe(this, Observer { connected ->
            if(!connected!!) {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@ContactsActivity, getString(R.string.error_network_retry) , DialogInterface.OnClickListener { dialog, which ->
                    dashboardType?.let {
                        updateData(it)
                    }
                }, DialogInterface.OnClickListener { dialog, which ->
                    finish()
                })
            }
        })
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

    private fun showContent() {
        contactsRecycleView.visibility = View.VISIBLE
        contactEmptyLayout.visibility = View.GONE
    }

    private fun showEmpty() {
        contactsRecycleView.visibility = View.GONE
        contactEmptyLayout.visibility = View.VISIBLE
        emptyText.text = getString(R.string.text_not_trades)
    }

    private fun updateData(type: DashboardType) {
        dialogUtils.showProgressDialog(this@ContactsActivity, getString(R.string.toast_loading_trades))
        dashboardType = type
        setTitle(type)
        viewModel.fetchContactsByType(type)
    }

    private fun showContact(contact: Contact?) {
        contact?.let {
            val intent = ContactActivity.createStartIntent(this@ContactsActivity, contact.contactId)
            startActivity(intent)
        }
    }

    private fun setContacts(contacts: List<Contact>) {
        if(contacts.isNotEmpty()) {
            showContent()
            adapter.replaceWith(contacts)
            contactsRecycleView.adapter = adapter
        } else {
            contactsRecycleView.visibility = View.GONE
            contactEmptyLayout.visibility = View.GONE
            adapter.replaceWith(ArrayList())
        }
    }

    private fun setTitle(dashboardType: DashboardType) {
        val title = when (dashboardType) {
            DashboardType.RELEASED -> getString(R.string.list_trade_filter2)
            DashboardType.CANCELED -> getString(R.string.list_trade_filter3)
            DashboardType.CLOSED -> getString(R.string.list_trade_filter4)
            else -> ""
        }
        if (supportActionBar != null) {
            supportActionBar!!.title = title
        }
    }

    companion object {
        const val EXTRA_TYPE = "com.thanksmister.extras.EXTRA_NOTIFICATION_TYPE"
        fun createStartIntent(context: Context, dashboardType: DashboardType): Intent {
            val intent = Intent(context, ContactsActivity::class.java)
            intent.putExtra(EXTRA_TYPE, dashboardType)
            return intent
        }
    }
}
