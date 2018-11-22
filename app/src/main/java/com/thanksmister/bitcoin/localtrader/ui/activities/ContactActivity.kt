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

import android.annotation.TargetApi
import android.app.DownloadManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Html
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.ContactAction
import com.thanksmister.bitcoin.localtrader.network.api.model.Message
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.MessageAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.ContactsViewModel
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Dates
import com.thanksmister.bitcoin.localtrader.utils.Strings
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_contact.*
import timber.log.Timber
import javax.inject.Inject

class ContactActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: ContactsViewModel

    private val disposable = CompositeDisposable()

    private var detailsEthereumAddress: TextView? = null
    private var detailsSortCode: TextView? = null
    private var detailsBSB: TextView? = null
    private var detailsAccountNumber: TextView? = null
    private var detailsBillerCode: TextView? = null
    private var detailsPhoneNumber: TextView? = null
    private var detailsReceiverEmail: TextView? = null
    private var detailsReceiverName: TextView? = null
    private var detailsIbanName: TextView? = null
    private var detailsSwiftBic: TextView? = null
    private var detailsReference: TextView? = null

    private var onlineOptionsLayout: View? = null
    private var detailsEthereumAddressLayout: View? = null
    private var detailsSortCodeLayout: View? = null
    private var detailsBSBLayout: View? = null
    private var detailsAccountNumberLayout: View? = null
    private var detailsBillerCodeLayout: View? = null
    private var detailsPhoneNumberLayout: View? = null
    private var detailsReceiverEmailLayout: View? = null
    private var detailsReceiverNameLayout: View? = null
    private var detailsIbanLayout: View? = null
    private var detailsSwiftBicLayout: View? = null
    private var detailsReferenceLayout: View? = null
    private var tradePrice: TextView? = null
    private var tradeAmountTitle: TextView? = null
    private var tradeAmount: TextView? = null
    private var tradeReference: TextView? = null
    private var tradeId: TextView? = null
    private var traderName: TextView? = null
    private var tradeFeedback: TextView? = null
    private var tradeCount: TextView? = null
    private var tradeType: TextView? = null
    private var noteText: TextView? = null
    private var lastSeenIcon: View? = null
    private var buttonLayout: View? = null
    private var contactHeaderLayout: View? = null
    private var dealPrice: TextView? = null
    private var contactButton: Button? = null
    private var adapter: MessageAdapter? = null
    private var contactId: Int = 0
    private var contact: Contact? = null
    private var downloadManager: DownloadManager? = null
    private var cancelItem: MenuItem? = null
    private var disputeItem: MenuItem? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                showDownload()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_contact)

        contactId = if (savedInstanceState == null) {
            intent.getIntExtra(EXTRA_ID, 0)
        } else {
            savedInstanceState.getInt(EXTRA_ID, 0)
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = ""
        }

        contactSwipeLayout.setOnRefreshListener(this)
        contactSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))

        val headerView = View.inflate(this, R.layout.view_contact_header, null)
        val messageButton = headerView.findViewById<View>(R.id.messageButton) as ImageButton
        messageButton.setOnClickListener { sendNewMessage() }

        detailsEthereumAddress = headerView.findViewById<View>(R.id.detailsEthereumAddress) as TextView
        detailsSortCode = headerView.findViewById<View>(R.id.detailsSortCode) as TextView
        detailsBSB = headerView.findViewById<View>(R.id.detailsBSB) as TextView
        detailsAccountNumber = headerView.findViewById<View>(R.id.detailsAccountNumber) as TextView
        detailsBillerCode = headerView.findViewById<View>(R.id.detailsBillerCode) as TextView
        detailsPhoneNumber = headerView.findViewById<View>(R.id.detailsPhoneNumber) as TextView
        detailsReceiverEmail = headerView.findViewById<View>(R.id.detailsReceiverEmail) as TextView
        detailsReceiverName = headerView.findViewById<View>(R.id.detailsReceiverName) as TextView
        detailsIbanName = headerView.findViewById<View>(R.id.detailsIbanName) as TextView
        detailsSwiftBic = headerView.findViewById<View>(R.id.detailsSwiftBic) as TextView
        detailsReference = headerView.findViewById<View>(R.id.detailsReference) as TextView

        onlineOptionsLayout = headerView.findViewById(R.id.onlineOptionsLayout)
        detailsEthereumAddressLayout = headerView.findViewById(R.id.detailsEthereumAddressLayout)
        detailsSortCodeLayout = headerView.findViewById(R.id.detailsSortCodeLayout)
        detailsBSBLayout = headerView.findViewById(R.id.detailsBSBLayout)
        detailsAccountNumberLayout = headerView.findViewById(R.id.detailsAccountNumberLayout)
        detailsBillerCodeLayout = headerView.findViewById(R.id.detailsBillerCodeLayout)
        detailsPhoneNumberLayout = headerView.findViewById(R.id.detailsPhoneNumberLayout)
        detailsReceiverEmailLayout = headerView.findViewById(R.id.detailsReceiverEmailLayout)
        detailsReceiverNameLayout = headerView.findViewById(R.id.detailsReceiverNameLayout)
        detailsIbanLayout = headerView.findViewById(R.id.detailsIbanLayout)
        detailsSwiftBicLayout = headerView.findViewById(R.id.detailsSwiftBicLayout)
        detailsReferenceLayout = headerView.findViewById(R.id.detailsReferenceLayout)

        tradeAmountTitle = headerView.findViewById<View>(R.id.tradeAmountTitle) as TextView
        tradePrice = headerView.findViewById<View>(R.id.tradePrice) as TextView
        tradeAmount = headerView.findViewById<View>(R.id.tradeAmount) as TextView
        tradeReference = headerView.findViewById<View>(R.id.tradeReference) as TextView
        tradeId = headerView.findViewById<View>(R.id.tradeId) as TextView
        traderName = headerView.findViewById<View>(R.id.traderName) as TextView
        tradeFeedback = headerView.findViewById<View>(R.id.tradeFeedback) as TextView
        tradeType = headerView.findViewById<View>(R.id.tradeType) as TextView
        noteText = headerView.findViewById<View>(R.id.noteTextContact) as TextView
        tradeCount = headerView.findViewById<View>(R.id.tradeCount) as TextView
        dealPrice = headerView.findViewById<View>(R.id.dealPrice) as TextView
        lastSeenIcon = headerView.findViewById(R.id.lastSeenIcon)
        contactHeaderLayout = headerView.findViewById(R.id.contactHeaderLayout)
        buttonLayout = findViewById(R.id.buttonLayout)
        contactButton = findViewById<View>(R.id.contactButton) as Button
        contactButton!!.setOnClickListener { view ->
            if (view.tag == R.string.button_cancel) {
                cancelContact()
            } else if (view.tag == R.string.button_release) {
                releaseTrade()
            } else if (view.tag == R.string.button_fund) {
                fundContact()
            } else if (view.tag == R.string.button_mark_paid) {
                markContactPaid()
            }
        }

        contactList.addHeaderView(headerView, null, false)
        contactList.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val message = adapterView.adapter.getItem(i) as Message
            setMessageOnClipboard(message)
        }

        contactList.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                val topRowVerticalPosition = if (contactList == null || contactList.childCount == 0) 0 else contactList.getChildAt(0).top
                contactSwipeLayout.isEnabled = firstVisibleItem == 0 && topRowVerticalPosition >= 0
            }
        })

        adapter = MessageAdapter(this)
        setAdapter(adapter!!)

        if (contactId == 0) {
            dialogUtils.showAlertDialog(this@ContactActivity, getString(R.string.toast_error_contact_data), DialogInterface.OnClickListener { dialog, which ->
                finish();
            })
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ContactsViewModel::class.java)
        observeViewModel(viewModel)
    }

    override fun onResume() {
        super.onResume()
        onRefreshStart()
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposable.isDisposed) {
            try {
                disposable.clear()
            } catch (e: UndeliverableException) {
                Timber.e(e.message)
            }
        }
        unregisterReceiver(receiver)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_ID, contactId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        when (item.itemId) {
            R.id.action_send -> {
                sendNewMessage()
                return true
            }
            R.id.action_profile -> {
                showProfile()
                return true
            }
            R.id.action_advertisement -> {
                showAdvertisement()
                return true
            }
            R.id.action_dispute -> {
                disputeContact()
                return true
            }
            R.id.action_cancel -> {
                cancelContact()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contact, menu)
        cancelItem = menu.findItem(R.id.action_cancel)
        disputeItem = menu.findItem(R.id.action_dispute)
        return true
    }

    private fun observeViewModel(viewModel: ContactsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                dialogUtils.showAlertDialog(this@ContactActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            Toast.makeText(this@ContactActivity, message, Toast.LENGTH_LONG).show()
        })
        disposable.add(viewModel.getContact(contactId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                   if(data != null) {
                       setMenuOptions(data)
                       setTitle(data)
                       setContact(data)
                       showOnlineOptions(data)
                       contactList.visibility = View.VISIBLE
                   }
                }, { error ->
                    Timber.e("Contact error: $error")
                    runOnUiThread {
                        dialogUtils.showAlertDialog(this@ContactActivity, getString(R.string.error_title),
                                getString(R.string.toast_error_opening_advertisement), DialogInterface.OnClickListener { _, _ ->
                            finish()
                        })
                    }
                    if (!BuildConfig.DEBUG) {
                        Crashlytics.setString("contact", contactId.toString())
                        Crashlytics.logException(error)
                    }
                }))
        updateData()
    }

    private fun setMenuOptions(contact: Contact?) {
        if (contact != null) {
            val buttonTag = TradeUtils.getTradeActionButtonLabel(contact)
            if (TradeUtils.canDisputeTrade(contact) && !TradeUtils.isLocalTrade(contact) && disputeItem != null) {
                disputeItem!!.isVisible = buttonTag == R.string.button_dispute
            }
            if (TradeUtils.canCancelTrade(contact) && cancelItem != null) {
                cancelItem!!.isVisible = buttonTag == R.string.button_cancel
            }
        }
    }

    override fun onRefresh() {
        updateData()
    }

    private fun handleNetworkDisconnect() {
        onRefreshStop()
        Toast.makeText(this@ContactActivity, getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show()
    }

    private fun onRefreshStop() {
        contactSwipeLayout.isRefreshing = false
    }

    private fun onRefreshStart() {
        contactSwipeLayout.isRefreshing = true
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
            val pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE)
            releaseTradeWithPin(pinCode)
        } else if (resultCode == PinCodeActivity.RESULT_CANCELED) {
            toast(R.string.toast_pin_code_canceled)
        } else if (resultCode == MessageActivity.RESULT_MESSAGE_SENT) {
            updateData()
        } else if (resultCode == MessageActivity.RESULT_MESSAGE_CANCELED) {
            toast(getString(R.string.toast_message_canceled))
        }
    }

    private fun updateData() {
        toast(getString(R.string.toast_refreshing_data))
        viewModel.fetchContact(contactId)
        disposable.add(viewModel.fetchMessages(contactId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null) {
                        if (!data.isEmpty() && adapter != null) {
                            adapter!!.replaceWith(data)
                        }
                    }
                }, { error ->
                    Timber.e("Messages error: $error")
                }))
    }

    private fun whatTheHeck() {
        /*dbManager.notificationsQuery()
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Notifications subscription safely unsubscribed");
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<NotificationItem>>() {
                    @Override
                    public void call(final List<NotificationItem> notificationItems) {
                        for (NotificationItem notificationItem : notificationItems) {
                            final String notificationId = notificationItem.notification_id();
                            final String notificationContactId = notificationItem.contact_id();
                            final boolean read = notificationItem.read();
                            if (contactId.equals(notificationContactId) && !read) {
                                dataService.markNotificationRead(notificationId)
                                        .doOnUnsubscribe(new Action0() {
                                            @Override
                                            public void call() {
                                                Timber.i("Mark notification read safely unsubscribed");
                                            }
                                        })
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Action1<JSONObject>() {
                                            @Override
                                            public void call(JSONObject result) {
                                                if (!Parser.containsError(result)) {
                                                    dbManager.markNotificationRead(notificationId);
                                                }
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                Timber.e(throwable.getMessage());
                                            }
                                        });
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());
                    }
                });*/
    }

    private fun setContact(contact: Contact) {

        this.contact = contact
        val date = Dates.parseLocalDateStringAbbreviatedTime(contact.createdAt)
        val amount = contact.amount + " " + contact.currency
        var type = ""

        val adTradeType = TradeType.valueOf(contact.advertisement.tradeType)
        when (adTradeType) {
            TradeType.LOCAL_BUY, TradeType.LOCAL_SELL -> type = if (contact.isBuying) getString(R.string.contact_list_buying_locally, amount, date) else getString(R.string.contact_list_selling_locally, amount, date)
            TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> {
                var paymentMethod = TradeUtils.getPaymentMethodName(contact.advertisement.paymentMethod)
                paymentMethod = paymentMethod.replace("_", " ")
                type = if (contact.isBuying) getString(R.string.contact_list_buying_online, amount, paymentMethod, date) else getString(R.string.contact_list_selling_online, amount, paymentMethod, date)
            }
            TradeType.NONE -> TODO()
        }

        tradeType!!.text = Html.fromHtml(type)
        tradePrice!!.text = getString(R.string.trade_price, contact.amount, contact.currency)
        tradeAmount!!.text = contact.amountBtc + " " + getString(R.string.btc)
        tradeReference!!.text = contact.referenceCode
        tradeId!!.setText(contact.contactId)
        dealPrice!!.text = Conversions.formatDealAmount(contact.amountBtc, contact.amount) + " " + contact.currency

        tradeAmount!!.text = contact.amountBtc + " " + getString(R.string.btc)
        traderName!!.text = if (contact.isBuying) contact.seller.username else contact.buyer.username
        tradeFeedback!!.setText(if (contact.isBuying) contact.seller.feedbackScore else contact.buyer.feedbackScore)
        tradeCount!!.text = if (contact.isBuying) contact.seller.tradeCount else contact.buyer.tradeCount

        if (contact.isBuying) {
            lastSeenIcon!!.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.seller.lastOnline!!))
        } else if (contact.isSelling) {
            lastSeenIcon!!.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.buyer.lastOnline!!))
        }

        val buttonTag = TradeUtils.getTradeActionButtonLabel(contact)
        contactButton!!.tag = buttonTag
        if (buttonTag > 0) {
            contactButton!!.text = getString(buttonTag)
        }

        if (buttonTag == R.string.button_cancel || buttonTag == 0) {
            buttonLayout!!.visibility = View.GONE
        } else {
            buttonLayout!!.visibility = View.VISIBLE
        }

        val description = TradeUtils.getContactDescription(contact, this)
        if (!TextUtils.isEmpty(description)) {
            noteText!!.text = Html.fromHtml(description)
        }
        contactHeaderLayout!!.visibility = if (description == null) View.GONE else View.VISIBLE

        if (TradeUtils.isOnlineTrade(contact)) {
            tradeAmountTitle!!.setText(R.string.text_escrow_amount)
            showOnlineOptions(contact)
        }
    }

    private fun showOnlineOptions(contact: Contact?) {
        if (contact != null) {
            onlineOptionsLayout!!.visibility = View.VISIBLE
            if (!TextUtils.isEmpty(contact.accountDetails.bsb)) {
                detailsBSB!!.text = contact.accountDetails.bsb
                detailsBSBLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.iban)) {
                detailsIbanName!!.text = contact.accountDetails.iban
                detailsIbanLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.sortCode)) {
                detailsSortCode!!.text = contact.accountDetails.sortCode
                detailsSortCodeLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.receiverName)) {
                detailsReceiverName!!.text = contact.accountDetails.receiverName
                detailsReceiverNameLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.receiverEmail)) {
                detailsReceiverEmail!!.text = contact.accountDetails.receiverEmail
                detailsReceiverEmailLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.swiftBic)) {
                detailsSwiftBic!!.text = contact.accountDetails.swiftBic
                detailsSwiftBicLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.ethereumAddress)) {
                detailsEthereumAddress!!.text = contact.accountDetails.ethereumAddress
                detailsEthereumAddressLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.reference)) {
                detailsReference!!.text = contact.accountDetails.reference
                detailsReferenceLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.phoneNumber)) {
                detailsPhoneNumber!!.text = contact.accountDetails.phoneNumber
                detailsPhoneNumberLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.billerCode)) {
                detailsBillerCode!!.text = contact.accountDetails.billerCode
                detailsBillerCodeLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.accountNumber)) {
                detailsAccountNumber!!.text = contact.accountDetails.accountNumber
                detailsAccountNumberLayout!!.visibility = View.VISIBLE
            }
        }
    }

    private fun setAdapter(adapter: MessageAdapter) {
        contactList.adapter = adapter
    }

    private fun downloadAttachment(message: Message) {
        if (TextUtils.isEmpty(message.attachmentUrl)) {
            showAlertDialog(getString(R.string.toast_attachment_empty))
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("message_download", message.attachmentUrl)
                Crashlytics.logException(Exception("Error downloading url: " + message.attachmentUrl!!))
            }
            return
        }
        val token = preferences.getAccessToken()
        val request = DownloadManager.Request(Uri.parse(message.attachmentUrl + "?accessToken=" + token))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        request.setVisibleInDownloadsUi(true)
        request.setMimeType(message.attachmentType)
        request.setTitle(message.attachmentName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        try {
            downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager!!.enqueue(request)
        } catch (e: NullPointerException) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("message_download", message.attachmentUrl)
                Crashlytics.logException(Exception("Error downloading url: " + message.attachmentUrl!!))
            }
        }
    }

    private fun showDownload() {
        try {
            val i = Intent()
            i.action = DownloadManager.ACTION_VIEW_DOWNLOADS
            startActivity(i)
        } catch (exception: ActivityNotFoundException) {
            showAlertDialog(getString(R.string.toast_error_no_installed_ativity))
        }
    }

    private fun disputeContact() {
        showProgressDialog(getString(R.string.progress_disputing))
        createAlert(getString(R.string.alert_dispute_trade_title), getString(R.string.contact_dispute_confirm), contactId, null, ContactAction.DISPUTE)
    }

    private fun fundContact() {
        showProgressDialog(getString(R.string.progress_funding))
        createAlert(getString(R.string.alert_fund_trade_title), getString(R.string.contact_fund_confirm), contactId, null, ContactAction.FUND)
    }

    private fun markContactPaid() {
        showProgressDialog(getString(R.string.progress_marking_paid))
        createAlert(getString(R.string.alert_mark_paid_title), getString(R.string.contact_paid_confirm), contactId, null, ContactAction.PAID)
    }

    private fun releaseTrade() {
        val intent = PinCodeActivity.createStartIntent(this@ContactActivity)
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE)
    }

    private fun releaseTradeWithPin(pinCode: String) {
        // TODO dialogutils
        /*showConfirmationDialog(new ConfirmationDialogEvent(getString(R.string.alert_release_trade_title), "Are you sure you want to release this trade?", getString(R.string.button_ok), getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                showProgressDialog(new ProgressDialogEvent(getString(R.string.progress_releasing)));
                contactAction(contactId, pinCode, ContactAction.RELEASE);
            }
        }));*/
    }

    private fun cancelContact() {
        // TODO dialogutils
        /*showConfirmationDialog(new ConfirmationDialogEvent(getString(R.string.alert_cancel_trade_title), getString(R.string.contact_cancel_confirm), getString(R.string.button_ok), getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                showProgressDialog(new ProgressDialogEvent(getString(R.string.progress_canceling_trade)));
                contactAction(contactId, null, ContactAction.CANCEL);
            }
        }));*/
    }

    private fun createAlert(title: String, message: String, contactId: Int, pinCode: String?, action: ContactAction) {
        // TODO dialogutils
        /*ConfirmationDialogEvent event = new ConfirmationDialogEvent(title, message, getString(R.string.button_ok), getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                contactAction(contactId, pinCode, action);
            }
        });
        showConfirmationDialog(event);*/
    }

    private fun contactAction(contactId: String, pinCode: String, action: ContactAction) {
        /* dataService.contactAction(contactId, pinCode, action)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Contact action subscription safely unsubscribed");
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject jsonObject) {
                        hideProgressDialog();
                        if (action == ContactAction.RELEASE || action == ContactAction.CANCEL) {
                            deleteContact(contactId, action);
                        } else {
                            updateContact();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        showAlertDialog(getString(R.string.error_contact_action));
                    }
                });*/
    }

    private fun deleteContact(contactId: String, action: ContactAction) {
        /*dbManager.deleteContact(contactId, new ContentResolverAsyncHandler.AsyncQueryListener() {
            @Override
            public void onQueryComplete() {
                hideProgressDialog();
                if (action == ContactAction.RELEASE) {
                    toast(getString(R.string.trade_released_toast_text));
                } else {
                    toast(getString(R.string.trade_canceled_toast_text));
                }
                finish();
            }
        });*/
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun setMessageOnClipboard(message: Message) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.message_clipboard_title), message.message)
        clipboard.primaryClip = clip
        if (!Strings.isBlank(message.attachmentName)) {
            downloadAttachment(message)
            toast(R.string.message_copied_attachment_toast)
        } else {
            toast(R.string.message_copied_toast)
        }
    }

    private fun sendNewMessage() {
        if (contact != null) {
            val contactName = if (contact!!.isBuying) contact!!.seller.username else contact!!.buyer.username
            startActivityForResult(MessageActivity.createStartIntent(this@ContactActivity, contactId, contactName), MessageActivity.REQUEST_MESSAGE_CODE)
        }
    }

    private fun showProfile() {
        if (contact != null) {
            try {
                val url = "https://localbitcoins.com/accounts/profile/" + (if (contact!!.isBuying) contact!!.seller.username else contact!!.buyer.username) + "/"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (e: SecurityException) {
                showAlertDialog(getString(R.string.error_hijack_link) + e.message)
            } catch (e: ActivityNotFoundException) {
                showAlertDialog(getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    private fun showAdvertisement() {
        if (contact != null) {
            /*dbManager.advertisementItemQuery(contact.advertisement_id())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<AdvertisementItem>() {
                        @Override
                        public void call(AdvertisementItem advertisement) {
                            if (advertisement != null) {
                                loadAdvertisementView(contact);
                            } else {
                                launchAdvertisementLink(contact);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            launchAdvertisementLink(contact);
                        }
                    });*/
        }
    }

    private fun loadAdvertisementView(contact: Contact?) {
        if (contact != null) {
            if (contact.advertisement.id != null) {
                // TODO show alert dialog
                /*showAlertDialog(new AlertDialogEvent(getString(R.string.error_advertisement), getString(R.string.error_no_advertisement)), new Action0() {
                    @Override
                    public void call() {
                        finish();
                    }
                });*/
            } else {
                val intent = AdvertisementActivity.createStartIntent(this@ContactActivity, contact.advertisement.id!!)
                startActivity(intent)
            }
        }
    }

    private fun launchAdvertisementLink(contact: Contact?) {
        if (contact != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.actions.advertisementPublicView))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                showAlertDialog(getString(R.string.toast_error_no_installed_ativity))
            }

        }
    }

    private fun setTitle(contact: Contact?) {
        if (contact != null) {
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            val title = when (tradeType) {
                TradeType.LOCAL_BUY, TradeType.LOCAL_SELL -> if (contact.isBuying) getString(R.string.text_buying_locally) else getString(R.string.text_selling_locally)
                TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> if (contact.isBuying) getString(R.string.text_buying_online) else getString(R.string.text_selling_online)
                else -> getString(R.string.text_trade)
            }
            if (supportActionBar != null) {
                supportActionBar!!.title = title
            }
        }
    }

    companion object {
        const val EXTRA_ID = "com.thanksmister.extras.EXTRA_ID"
        fun createStartIntent(context: Context, contactId: Int): Intent {
            val intent = Intent(context, ContactActivity::class.java)
            intent.putExtra(EXTRA_ID, contactId)
            return intent
        }
    }
}