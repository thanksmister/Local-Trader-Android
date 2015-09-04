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

package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContentResolverAsyncHandler;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MessageItem;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.MessageAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;


public class ContactActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_ID = "com.thanksmister.extras.EXTRA_ID";
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_TYPE";

    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;

    @InjectView(R.id.contactProgress)
    View progress;

    @InjectView(R.id.contactList)
    ListView content;

    @InjectView(R.id.contactToolBar)
    Toolbar toolbar;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private TextView tradePrice;
    private TextView tradeAmount;
    private TextView traderName;
    private TextView tradeFeedback;
    private TextView tradeCount;
    private TextView tradeType;
    private TextView noteText;
    private View lastSeenIcon;
    private View buttonLayout;
    private View contactHeaderLayout;
    private TextView dealPrice;
    private Button contactButton;
    private ImageButton sendMessage;
    private EditText newMessageText;
    private MessageAdapter adapter;
    private String message;

    private String contactId;
    private ContactItem contact;
    private DashboardType dashboardType;

    private DownloadManager downloadManager;
    private MenuItem cancelItem;
    private MenuItem disputeItem;
  
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription subscription = Subscriptions.empty();
    private Subscription tokensSubscription = Subscriptions.empty();
    private Subscription updateSubscription = Subscriptions.empty();
    private Subscription postSubscription = Subscriptions.empty();
    private Subscription actionSubscription = Subscriptions.empty();

    private boolean messageScroll = false;

    public static Intent createStartIntent(Context context, String contactId, DashboardType dashboardType)
    {
        Intent intent = new Intent(context, ContactActivity.class);
        intent.putExtra(EXTRA_ID, contactId);
        intent.putExtra(EXTRA_TYPE, dashboardType);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_contact);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            contactId = getIntent().getStringExtra(EXTRA_ID);
            dashboardType = (DashboardType) getIntent().getSerializableExtra(EXTRA_TYPE);
        } else {
            contactId = savedInstanceState.getString(EXTRA_ID);
            dashboardType = (DashboardType) savedInstanceState.getSerializable(EXTRA_TYPE);
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            setToolBarMenu(toolbar);
        }

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

        View headerView = View.inflate(this, R.layout.view_contact_header, null);

        tradePrice = (TextView) headerView.findViewById(R.id.tradePrice);
        tradeAmount = (TextView) headerView.findViewById(R.id.tradeAmount);
        traderName = (TextView) headerView.findViewById(R.id.traderName);
        tradeFeedback = (TextView) headerView.findViewById(R.id.tradeFeedback);
        tradeType = (TextView) headerView.findViewById(R.id.tradeType);
        noteText = (TextView) headerView.findViewById(R.id.noteTextContact);
        tradeCount = (TextView) headerView.findViewById(R.id.tradeCount);
        dealPrice = (TextView) headerView.findViewById(R.id.dealPrice);
        lastSeenIcon = headerView.findViewById(R.id.lastSeenIcon);
        buttonLayout = headerView.findViewById(R.id.buttonLayout);
        contactHeaderLayout = headerView.findViewById(R.id.contactHeaderLayout);

        sendMessage = (ImageButton) headerView.findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                validateMessage(message);
            }
        });

        contactButton = (Button) headerView.findViewById(R.id.contactButton);
        contactButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (view.getTag().equals(R.string.button_cancel)) {
                    cancelContact();
                } else if (view.getTag().equals(R.string.button_release)) {
                    releaseTrade();
                } else if (view.getTag().equals(R.string.button_fund)) {
                    fundContact();
                } else if (view.getTag().equals(R.string.button_mark_paid)) {
                    markContactPaid();
                }
            }
        });

        newMessageText = (EditText) headerView.findViewById(R.id.newMessageText);
        newMessageText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                message = editable.toString();
            }
        });

        content.addHeaderView(headerView, null, false);
        content.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                MessageItem message = (MessageItem) adapterView.getAdapter().getItem(i);
                setMessageOnClipboard(message);
            }
        });

        content.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                int topRowVerticalPosition = (content == null || content.getChildCount() == 0) ? 0 : content.getChildAt(0).getTop();
                swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });

        adapter = new MessageAdapter(this);
        setAdapter(adapter);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        subscribeData();
        onRefreshStart();
        updateData();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        subscriptions.unsubscribe();
        subscription.unsubscribe();
        updateSubscription.unsubscribe();
        tokensSubscription.unsubscribe();
        postSubscription.unsubscribe();
        actionSubscription.unsubscribe();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(EXTRA_ID, contactId);
        outState.putSerializable(EXTRA_TYPE, dashboardType);
        //super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Timber.d("onCreateOptionsMenu");
        
        if (toolbar != null)
            toolbar.inflateMenu(R.menu.contact);
        
        cancelItem = menu.findItem(R.id.action_cancel);
        disputeItem = menu.findItem(R.id.action_dispute);

        setMenuOptions();
        
        return true;
    }

    @Override
    public void onRefresh()
    {
        updateData();
    }
    
    private void setMenuOptions()
    {
        if(contact != null) {
            int buttonTag = TradeUtils.getTradeActionButtonLabel(contact);
            if(TradeUtils.canDisputeTrade(contact) && !TradeUtils.isLocalTrade(contact)) {
                disputeItem.setVisible(buttonTag != R.string.button_dispute);
            }

            if(TradeUtils.canCancelTrade(contact)) {
                cancelItem.setVisible(buttonTag != R.string.button_cancel);
            }
        }
    }

    public void onRefreshStop()
    {
        if (swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    public void onRefreshStart()
    {
        if (swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
            String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
            releaseTradeWithPin(pinCode);
        } else if (resultCode == PinCodeActivity.RESULT_CANCELED) {
            toast(R.string.toast_pin_code_canceled);
        }
    }

    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {

                switch (menuItem.getItemId()) {
                    case R.id.action_profile:
                        showProfile();
                        return true;
                    case R.id.action_advertisement:
                        showAdvertisement(contact.advertisement_id());
                        return true;
                    case R.id.action_dispute:
                        disputeContact();
                        return true;
                    case R.id.action_cancel:
                        cancelContact();
                        return true;
                }
                return false;
            }
        });
    }
    
    public void showContent(final boolean show)
    {
        if (progress == null || content == null)
            return;
        
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progress.setVisibility(show ? View.GONE : View.VISIBLE);
        progress.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progress.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        content.setVisibility(show ? View.VISIBLE : View.GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                content.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void subscribeData()
    {
        Timber.d("subscribeData");

        subscriptions = new CompositeSubscription();

        subscriptions.add(dbManager.contactQuery(contactId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ContactItem>()
                {
                    @Override
                    public void call(ContactItem contactItem)
                    {
                        if (contactItem != null) {
                            showContent(true);
                            onRefreshStop();
                            setContact(contactItem);
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

        subscriptions.add(dbManager.messagesQuery(contactId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<MessageItem>>()
                {
                    @Override
                    public void call(List<MessageItem> messageItems)
                    {
                        if (!messageItems.isEmpty()) {
                            getAdapter().replaceWith(messageItems);
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
    }

    private void updateData()
    {
        updateSubscription = dataService.getContact(contactId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Contact>()
                {
                    @Override
                    public void call(Contact contact)
                    {
                        if (TradeUtils.isActiveTrade(contact)) {

                            updateContact(contact);

                        } else {

                            showContent(true);
                            hideProgressDialog();
                            onRefreshStop();

                            setContact(ContactItem.convertContact(contact));
                            getAdapter().replaceWith(MessageItem.convertMessages(contact.messages, contact.contact_id));
                        }

                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable, true);
                        hideProgressDialog();
                        onRefreshStop();
                    }
                });
    }

    private void updateContact(final Contact contact)
    {
        if (contact.contact_id == null)
            throw new Error("Contact has no valid ID");

        int messageCount = contact.messages.size();

        dbManager.updateContact(contact, messageCount, false, new ContentResolverAsyncHandler.AsyncQueryListener()
        {
            @Override
            public void onQueryComplete()
            {
                Timber.d("updateContact onQueryComplete");
                updateMessages(contact);
            }
        });
    }

    private void updateMessages(Contact contact)
    {
        Timber.d("updateMessages");

        dbManager.updateMessages(contact.contact_id, contact.messages, new ContentResolverAsyncHandler.AsyncQueryListener()
        {
            @Override
            public void onQueryComplete()
            {
                if(messageScroll && content.getCount() > 1) {
                    content.smoothScrollToPosition(1); 
                    messageScroll = false;
                }
             
                hideProgressDialog();
                onRefreshStop();
            }
        });
    }

    public void setContact(ContactItem contact)
    {
        this.contact = contact;

        String date = Dates.parseLocalDateStringAbbreviatedTime(contact.created_at());
        String amount = contact.amount() + " " + contact.currency();
        String type = "";

        TradeType adTradeType = TradeType.valueOf(contact.advertisement_trade_type());
        switch (adTradeType) {
            case LOCAL_BUY:
            case LOCAL_SELL:
                type = (contact.is_buying()) ? getString(R.string.contact_list_buying_locally, amount, date) : getString(R.string.contact_list_selling_locally, amount, date);
                break;
            case ONLINE_BUY:
            case ONLINE_SELL:
                String paymentMethod = TradeUtils.getPaymentMethodName(contact.advertisement_payment_method());
                type = (contact.is_buying()) ? getString(R.string.contact_list_buying_online, amount, paymentMethod, date) : getString(R.string.contact_list_selling_online, amount, paymentMethod, date);
                break;
        }

        tradeType.setText(Html.fromHtml(type));
        tradePrice.setText(getString(R.string.trade_price, contact.amount(), contact.currency()));
        tradeAmount.setText(contact.amount_btc() + " " + getString(R.string.btc));
        dealPrice.setText(Conversions.formatDealAmount(contact.amount_btc(), contact.amount()) + " " + contact.currency());

        tradeAmount.setText(contact.amount_btc() + " " + getString(R.string.btc));
        traderName.setText((contact.is_buying()) ? contact.seller_username() : contact.buyer_username());
        tradeFeedback.setText((contact.is_buying()) ? contact.seller_feedback_score() : contact.buyer_feedback_score());
        tradeCount.setText((contact.is_buying()) ? contact.seller_trade_count() : contact.buyer_trade_count());

        lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.advertiser_last_online()));

        int buttonTag = TradeUtils.getTradeActionButtonLabel(contact);
        if (buttonTag > 0)
            contactButton.setText(getString(buttonTag));

        contactButton.setTag(buttonTag);

        buttonLayout.setVisibility((buttonTag == 0) ? View.GONE : View.VISIBLE);

        if (buttonTag == R.string.button_cancel) {
            contactButton.setBackgroundResource(R.drawable.button_red_small_selector);
        } else {
            contactButton.setBackgroundResource(R.drawable.button_green_small_selector);
        }

        String description = TradeUtils.getContactDescription(contact, this);
        noteText.setText(Html.fromHtml(description));
        noteText.setMovementMethod(LinkMovementMethod.getInstance());
        contactHeaderLayout.setVisibility((description == null) ? View.GONE : View.VISIBLE);

        setMenuOptions();
    }

    private MessageAdapter getAdapter()
    {
        return adapter;
    }

    private void setAdapter(MessageAdapter adapter)
    {
        content.setAdapter(adapter);
    }

    private void validateMessage(String message)
    {
        if (Strings.isBlank(message)) {
            return;
        }

        postMessage(message);
    }

    public void resetMessageAndRefresh()
    {
        message = null;
        newMessageText.setText("");
        messageScroll = true; // tells our system to scroll when loading new messages

        // hide keyboard and notify
        try{
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.e("Error closing keyboard");
        }
        
        toast(R.string.toast_message_sent);

        onRefreshStart();
        updateData();
    }

    public void downloadAttachment(final MessageItem message)
    {
        Observable<SessionItem> tokensObservable = dataService.getTokens();
        tokensSubscription = tokensObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SessionItem>()
                {
                    @Override
                    public void call(SessionItem sessionItem)
                    {
                        String token = sessionItem.access_token();

                        Timber.d("Download URL : " + message.attachment_url() + "?access_token=" + token);

                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(message.attachment_url() + "?access_token=" + token));
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                        request.setVisibleInDownloadsUi(true);
                        request.setMimeType(message.attachment_type());
                        request.setTitle(message.attachment_name());
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        downloadManager.enqueue(request);
                    }
                });
    }

    private void showDownload()
    {
        Intent i = new Intent();
        i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(i);
    }

    public void postMessage(String message)
    {
        showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_send_message)));

        Observable<JSONObject> messageObservable =  dataService.postMessage(contactId, message);
        postSubscription = messageObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>()
                {
                    @Override
                    public void call(JSONObject jsonObject)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                hideProgressDialog();
                                resetMessageAndRefresh();
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                hideProgressDialog();
                                toast(R.string.toast_error_message);
                            }
                        });
                    }
                });
    }

    public void disputeContact()
    {
        showProgressDialog(new ProgressDialogEvent("Disputing..."));
        createAlert("Dispute Trade", getString(R.string.contact_dispute_confirm), contactId, null, ContactAction.DISPUTE);
    }

    public void fundContact()
    {
        showProgressDialog(new ProgressDialogEvent("Funding..."));
        createAlert("Fund Trade", getString(R.string.contact_fund_confirm), contactId, null, ContactAction.FUND);
    }

    public void markContactPaid()
    {
        showProgressDialog(new ProgressDialogEvent("Marking paid..."));
        createAlert("Mark Paid", getString(R.string.contact_paid_confirm), contactId, null, ContactAction.PAID);
    }

    public void releaseTrade()
    {
        Intent intent = PinCodeActivity.createStartIntent(ContactActivity.this);
        startActivityForResult(intent, PinCodeActivity.RESULT_VERIFIED);
    }

    public void releaseTradeWithPin(String pinCode)
    {
        showProgressDialog(new ProgressDialogEvent("Releasing trade..."));
        contactAction(contactId, pinCode, ContactAction.RELEASE);
    }

    public void cancelContact()
    {
        createAlert("Cancel Trade", getString(R.string.contact_cancel_confirm), contactId, null, ContactAction.CANCEL);
    }

    public void createAlert(String title, String message, final String contactId, final String pinCode, final ContactAction action)
    {
        ConfirmationDialogEvent event = new ConfirmationDialogEvent(title, message, getString(R.string.button_ok), getString(R.string.button_cancel), new Action0()
        {
            @Override
            public void call()
            {
                contactAction(contactId, pinCode, action);
            }
        });

        showConfirmationDialog(event);
    }

    private void contactAction(final String contactId, final String pinCode, final ContactAction action)
    {
        Observable<JSONObject> contactActionObservable = dataService.contactAction(contactId, pinCode, action);
        actionSubscription = contactActionObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>()
                {
                    @Override
                    public void call(JSONObject jsonObject)
                    {
                        if (action == ContactAction.RELEASE) {
                            deleteContact(contactId);
                        } else {
                            onRefreshStart();
                            updateData();
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        hideProgressDialog();
                        handleError(throwable);
                    }
                });
    }

    private void deleteContact(String contactId)
    {
        dbManager.deleteContact(contactId, new ContentResolverAsyncHandler.AsyncQueryListener()
        {

            @Override
            public void onQueryComplete()
            {
                hideProgressDialog();
                toast(getString(R.string.trade_released_toast_text));
                finish();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setMessageOnClipboard(MessageItem message)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.message_clipboard_title), message.message());
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setText(message.message());
        }

        if (!Strings.isBlank(message.attachment_name())) {
            downloadAttachment(message);
            toast(R.string.message_copied_attachment_toast);
        } else {
            toast(R.string.message_copied_toast);
        }
    }

    public void showProfile()
    {
        if (contact == null) {
            return;
        }
        
        String url = "https://localbitcoins.com/accounts/profile/" + ((contact.is_buying()) ? contact.seller_username() : contact.buyer_username()) + "/";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    public void showAdvertisement(String advertisement_id)
    {
        Intent intent = AdvertisementActivity.createStartIntent(this, advertisement_id);
        startActivity(intent);
    }

    protected void setTitle(Contact contact)
    {
        String title = "";
        switch (contact.advertisement.trade_type) {
            case LOCAL_BUY:
            case LOCAL_SELL:
                title = (contact.is_buying) ? "Buying Locally" : "Selling Locally";
                break;
            case ONLINE_BUY:
            case ONLINE_SELL:
                title = (contact.is_buying) ? "Buying Online" : "Selling Online";
                break;
        }

        getSupportActionBar().setTitle(title);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                showDownload();
            }
        }
    };
}
