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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.NetworkConnectionException;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.ContentResolverAsyncHandler;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MessageItem;
import com.thanksmister.bitcoin.localtrader.data.database.NotificationItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisements.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.MessageAdapter;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

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
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class ContactActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_ID = "com.thanksmister.extras.EXTRA_ID";
    public static final String EXTRA_TYPE = "com.thanksmister.extras.EXTRA_NOTIFICATION_TYPE";

    @Inject
    DataService dataService;

    @Inject
    DbManager dbManager;
    
    @InjectView(R.id.contactList)
    ListView content;

    @InjectView(R.id.contactToolBar)
    Toolbar toolbar;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    private TextView tradePrice;
    private TextView tradeAmount;
    private TextView tradeReference;
    private TextView tradeId;
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
    private MessageAdapter adapter;
    private String message;

    private String contactId;
    private ContactItem contact;
    private DownloadManager downloadManager;
    private MenuItem cancelItem;
    private MenuItem disputeItem;
    
    private boolean messageScroll = false;
    private Handler handler;

    private Subscription subscription = Subscriptions.empty();
    private CompositeSubscription updateSubscription = new CompositeSubscription();
    private Subscription advertisementSubscription = Subscriptions.empty();

    public static Intent createStartIntent(Context context, String contactId)
    {
        Intent intent = new Intent(context, ContactActivity.class);
        intent.putExtra(EXTRA_ID, contactId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_contact);

        ButterKnife.inject(this);

        handler = new Handler();

        if (savedInstanceState == null) {
            contactId = getIntent().getStringExtra(EXTRA_ID);
        } else {
            contactId = savedInstanceState.getString(EXTRA_ID);
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
        ImageButton messageButton = (ImageButton) headerView.findViewById(R.id.messageButton);
        messageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                sendNewMessage();
            }
        });
        
        tradePrice = (TextView) headerView.findViewById(R.id.tradePrice);
        tradeAmount = (TextView) headerView.findViewById(R.id.tradeAmount);
        tradeReference = (TextView) headerView.findViewById(R.id.tradeReference);
        tradeId = (TextView) headerView.findViewById(R.id.tradeId);
        traderName = (TextView) headerView.findViewById(R.id.traderName);
        tradeFeedback = (TextView) headerView.findViewById(R.id.tradeFeedback);
        tradeType = (TextView) headerView.findViewById(R.id.tradeType);
        noteText = (TextView) headerView.findViewById(R.id.noteTextContact);
        tradeCount = (TextView) headerView.findViewById(R.id.tradeCount);
        dealPrice = (TextView) headerView.findViewById(R.id.dealPrice);
        lastSeenIcon = headerView.findViewById(R.id.lastSeenIcon);
        contactHeaderLayout = headerView.findViewById(R.id.contactHeaderLayout);
        
        buttonLayout = findViewById(R.id.buttonLayout);
        contactButton = (Button) findViewById(R.id.contactButton);
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
        toast("Refreshing data...");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        subscribeData();
        updateData();
        onRefreshStart();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        advertisementSubscription.unsubscribe();
        updateSubscription.unsubscribe();
        subscription.unsubscribe();
        
        if(content != null)
            content.clearAnimation();
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
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ID, contactId);
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
        if (toolbar != null)
            toolbar.inflateMenu(R.menu.contact);
        
        cancelItem = menu.findItem(R.id.action_cancel);
        disputeItem = menu.findItem(R.id.action_dispute);

        setMenuOptions();
        
        return true;
    }
    
    private void setMenuOptions()
    {
        if(contact != null) {
            int buttonTag = TradeUtils.getTradeActionButtonLabel(contact);
            if(TradeUtils.canDisputeTrade(contact) && !TradeUtils.isLocalTrade(contact) && disputeItem != null) {
                disputeItem.setVisible(buttonTag == R.string.button_dispute);
            }

            if(TradeUtils.canCancelTrade(contact) && cancelItem != null) {
                cancelItem.setVisible(buttonTag == R.string.button_cancel);
            }
        }
    }

    @Override
    public void onRefresh()
    {
        onRefreshStart();
        updateData();
    }

    public void onRefreshStop()
    {
        handler.removeCallbacks(refreshRunnable);
        
        if (swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }
    
    public void onRefreshStart()
    {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 50);
    }

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(swipeLayout != null)
                swipeLayout.setRefreshing(true);
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
            String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
            releaseTradeWithPin(pinCode);
        } else if (resultCode == PinCodeActivity.RESULT_CANCELED) {
            toast(R.string.toast_pin_code_canceled);
        } else if (resultCode == MessageActivity.RESULT_MESSAGE_SENT) {
            toast(R.string.toast_message_sent);
            onRefreshStart();
            updateData();
        } else if (resultCode == MessageActivity.RESULT_MESSAGE_CANCELED) {
            toast("Message canceled...");
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
                    case R.id.action_send:
                        sendNewMessage();
                        return true;
                    case R.id.action_profile:
                        showProfile();
                        return true;
                    case R.id.action_advertisement:
                        showAdvertisement();
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
        if ( content == null)
            return;
        
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        content.setVisibility(show ? View.VISIBLE : View.GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if (content != null)
                    content.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void subscribeData()
    {
        Timber.d("subscribeData");
        
        dbManager.markRecentMessagesSeen(contactId);

        dbManager.contactQuery(contactId)
                .subscribe(new Action1<ContactItem>()
                {
                    @Override
                    public void call(ContactItem contactItem)
                    {
                        if (contactItem != null) {
                            showContent(true);
                            setContact(contactItem);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        //reportError(throwable);
                    }
                });

        dbManager.messagesQuery(contactId)
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
                        //reportError(throwable);
                    }
                });
    }

    private void updateData()
    {
        Timber.d("updateData");

        if (!NetworkUtils.isNetworkConnected(ContactActivity.this)) {
            onRefreshStop();
            handleError(new NetworkConnectionException());
            return;
        }

        updateSubscription = new CompositeSubscription();

        updateSubscription.add(dataService.getContact(contactId)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Update contact subscription safely unsubscribed");
                    }
                })
                .compose(this.<Contact>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Contact>()
                {
                    @Override
                    public void call(Contact contact)
                    {
                        showContent(true);
                        setContact(ContactItem.convertContact(contact));
                        getAdapter().replaceWith(MessageItem.convertMessages(contact.messages, contact.contact_id));
                        updateContact(contact);
                        onRefreshStop();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                        onRefreshStop();
                        toast("Unable to retrieve contact.");
                    }
                }));

        // Mark any unread notifications matching the contact Id as read
        updateSubscription.add(dbManager.notificationsQuery()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Notifications subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<NotificationItem>>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<NotificationItem>>()
                {
                    @Override
                    public void call(final List<NotificationItem> notificationItems)
                    {
                        for (NotificationItem notificationItem : notificationItems) {
                            final String notificationId = notificationItem.notification_id();
                            final String notificationContactId = notificationItem.contact_id();
                            final boolean read = notificationItem.read();
                            if(contactId.equals(notificationContactId) && !read) {
                                dataService.markNotificationRead(notificationId)
                                        .doOnUnsubscribe(new Action0() {
                                            @Override
                                            public void call() {
                                                Timber.i("Mark notification read safely unsubscribed");
                                            }
                                        })
                                        .compose(ContactActivity.this.<JSONObject>bindUntilEvent(ActivityEvent.PAUSE))
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Action1<JSONObject>() {
                                            @Override
                                            public void call(JSONObject result) {
                                                if(!Parser.containsError(result)) {
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
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        Timber.e(throwable.getMessage());
                    }
                }));
    }

    private void updateContact(final Contact contact)
    {
        if (contact == null || TextUtils.isEmpty(contact.contact_id))
            return;

        final int messageCount = contact.messages.size();
        
        dbManager.contactQuery(contact.contact_id)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Update contact item subscription safely unsubscribed");
                    }
                })
                .compose(this.<ContactItem>bindUntilEvent(ActivityEvent.PAUSE))
                .map(new Func1<ContactItem, Object>()
                {
                    @Override
                    public Object call(final ContactItem contactItem)
                    {
                        if (contactItem != null) {
                            dbManager.updateContact(contact, messageCount, false, new ContentResolverAsyncHandler.AsyncQueryListener()
                            {
                                @Override
                                public void onQueryComplete()
                                {
                                    updateMessages(contact);
                                }
                            });
                        }

                        return null;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void updateMessages(Contact contact)
    {
        onRefreshStart();
        
        dbManager.updateMessages(contact.contact_id, contact.messages, new ContentResolverAsyncHandler.AsyncQueryListener()
        {
            @Override
            public void onQueryComplete()
            {
                if (messageScroll && content.getCount() > 1) {
                    content.smoothScrollToPosition(1);
                    messageScroll = false;
                }
               
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
        tradeReference.setText(contact.reference_code());
        tradeId.setText(contact.contact_id());
        dealPrice.setText(Conversions.formatDealAmount(contact.amount_btc(), contact.amount()) + " " + contact.currency());

        tradeAmount.setText(contact.amount_btc() + " " + getString(R.string.btc));
        traderName.setText((contact.is_buying()) ? contact.seller_username() : contact.buyer_username());
        tradeFeedback.setText((contact.is_buying()) ? contact.seller_feedback_score() : contact.buyer_feedback_score());
        tradeCount.setText((contact.is_buying()) ? contact.seller_trade_count() : contact.buyer_trade_count());

        if(contact.is_buying()) {
            lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.seller_last_online()));
        } else if (contact.is_selling()) {
            lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.buyer_last_online()));
        }
        
        int buttonTag = TradeUtils.getTradeActionButtonLabel(contact);
        if (buttonTag > 0)
            contactButton.setText(getString(buttonTag));

        contactButton.setTag(buttonTag);
        
        if (buttonTag == R.string.button_cancel || buttonTag == 0) {
            buttonLayout.setVisibility(View.GONE);
            contactButton.setBackgroundResource(R.drawable.button_red_small_selector);
        } else {
            buttonLayout.setVisibility(View.VISIBLE);
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

    public void downloadAttachment(final MessageItem message)
    {
        String token = AuthUtils.getAccessToken(sharedPreferences);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(message.attachment_url() + "?access_token=" + token));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setVisibleInDownloadsUi(true);
        request.setMimeType(message.attachment_type());
        request.setTitle(message.attachment_name());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }

    private void showDownload()
    {
        try {
            Intent i = new Intent();
            i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
            startActivity(i); 
        } catch (ActivityNotFoundException exception ) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
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
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE);
    }

    public void releaseTradeWithPin(final String pinCode)
    {
        showConfirmationDialog(new ConfirmationDialogEvent("Release Trade", "Are you sure you want to release this trade?", getString(R.string.button_ok), getString(R.string.button_cancel), new Action0()
        {
            @Override
            public void call()
            {
                showProgressDialog(new ProgressDialogEvent("Releasing trade..."));
                contactAction(contactId, pinCode, ContactAction.RELEASE);
            }
        }));
    }

    public void cancelContact()
    {
        showConfirmationDialog(new ConfirmationDialogEvent("Cancel Trade", getString(R.string.contact_cancel_confirm), getString(R.string.button_ok), getString(R.string.button_cancel), new Action0()
        {
            @Override
            public void call()
            {
                showProgressDialog(new ProgressDialogEvent("Canceling trade..."));
                contactAction(contactId, null, ContactAction.CANCEL);
            }
        }));
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
        dataService.contactAction(contactId, pinCode, action)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Contact action subscription safely unsubscribed");
                    }
                })
                .compose(this.<JSONObject>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>()
                {
                    @Override
                    public void call(JSONObject jsonObject)
                    {
                        hideProgressDialog();
                        
                        if (action == ContactAction.RELEASE || action == ContactAction.CANCEL) {
                            deleteContact(contactId, action);
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
                        reportError(throwable);
                        toast("Unable to perform contact action.");
                    }
                });
    }

    private void deleteContact(String contactId, final ContactAction action)
    {
        dbManager.deleteContact(contactId, new ContentResolverAsyncHandler.AsyncQueryListener()
        {
            @Override
            public void onQueryComplete()
            {
                hideProgressDialog();
                
                if (action == ContactAction.RELEASE) {
                    toast(getString(R.string.trade_released_toast_text));
                } else {
                    toast(getString(R.string.trade_canceled_toast_text));
                }
             
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
    
    private void sendNewMessage()
    {
        if(contact != null) {
            String contactName = (contact.is_buying()) ? contact.seller_username() : contact.buyer_username();
            startActivityForResult(MessageActivity.createStartIntent(ContactActivity.this, contactId, contactName), MessageActivity.REQUEST_MESSAGE_CODE);
        }
    }

    public void showProfile()
    {
        if (contact == null) {
            return;
        }

        try {
            String url = "https://localbitcoins.com/accounts/profile/" + ((contact.is_buying()) ? contact.seller_username() : contact.buyer_username()) + "/";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
    }

    public void showAdvertisement()
    {
        if (contact == null) 
            return;

        Observable<AdvertisementItem> updateAdvertisementObservable = dbManager.advertisementItemQuery(contact.advertisement_id());
        advertisementSubscription = updateAdvertisementObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AdvertisementItem>()
                {
                    @Override
                    public void call(AdvertisementItem advertisement)
                    {
                        if(advertisement != null) {
                            loadAdvertisementView(contact);
                        } else {
                            launchAdvertisementLink(contact);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        launchAdvertisementLink(contact);
                    }
                });
    }
    
    private void loadAdvertisementView(ContactItem contact)
    {
        Intent intent = AdvertisementActivity.createStartIntent(ContactActivity.this, contact.advertisement_id());
        startActivity(intent);
    }
    
    private void launchAdvertisementLink(ContactItem contact)
    {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(contact.advertisement_public_view()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(getString(R.string.toast_error_no_installed_ativity));
        }
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

        if(getSupportActionBar() != null)
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