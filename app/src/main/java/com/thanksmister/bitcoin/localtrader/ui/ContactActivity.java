package com.thanksmister.bitcoin.localtrader.ui;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.database.ContactItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.SessionItem;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.misc.MessageAdapter;
import com.thanksmister.bitcoin.localtrader.ui.release.PinCodeActivity;
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
import rx.functions.Action0;
import rx.functions.Action1;

import static rx.android.app.AppObservable.bindActivity;

public class ContactActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_ID = "com.thanksmister.extras.EXTRA_ID";
    
    @Inject
    DbManager dbManager;

    @InjectView(R.id.contactProgress)
    View progress;

    @InjectView(R.id.contactList)
    ListView list;

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
    private Contact contact;
    
    private DownloadManager downloadManager;
    private MenuItem cancelItem;
    private MenuItem disputeItem;
    
    private Observable<ContactItem> contactItemObservable;
    private Observable<Contact> contactObservable;
    private Observable<SessionItem> tokensObservable;
    private Observable<JSONObject> contactActionObservable;
    
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

        if (savedInstanceState == null) {
            contactId = getIntent().getStringExtra(EXTRA_ID);
        } else {
            contactId = savedInstanceState.getString(EXTRA_ID);
        }
        
        if(toolbar != null) {
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
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateMessage(message);
            }
        });

        contactButton = (Button) headerView.findViewById(R.id.contactButton);
        contactButton.setOnClickListener(view -> {
            if(view.getTag().equals(R.string.button_cancel)) {
                cancelContact();
            } else if (view.getTag().equals(R.string.button_release)) {
                releaseTrade();
            } else if (view.getTag().equals(R.string.button_fund)) {
                fundContact();
            } else if (view.getTag().equals(R.string.button_mark_paid)) {
                markContactPaid();
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

        list.addHeaderView(headerView, null, false);
        list.setOnItemClickListener((adapterView, view, i, l) -> {
            Message message = (Message) adapterView.getAdapter().getItem(i);
            setMessageOnClipboard(message);
        });

        adapter = new MessageAdapter(this);
        setAdapter(adapter);

        contactItemObservable = bindActivity(this, dbManager.contactQuery(contactId));
        contactObservable = bindActivity(this, dbManager.getContact(contactId));
        tokensObservable =  bindActivity(this, dbManager.getTokens());
                
        subscribeData();
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
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.contact);

        cancelItem = menu.findItem(R.id.action_cancel);
        disputeItem = menu.findItem(R.id.action_dispute);

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        updateData();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        
        unregisterReceiver(receiver);
    }

    @Override
    public void onRefresh()
    {
        updateData();
    }

    public void onRefreshStart()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(true);
    }
    
    public void onRefreshStop()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
            String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
            releaseTradeWithPin(pinCode);
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
                        showAdvertisement(contact.advertisement.id);
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
    
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }
    
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    public void subscribeData()
    {   
        /*contactItemObservable.subscribe(new Action1<ContactItem>()
        {
            @Override
            public void call(ContactItem contactItem)
            {
                if (contactItem != null) {
                    
                    *//*contact = new Contact();
                    contact = contact.convertContentItemToContact(contactItem);

                    setTitle(contact);
                    
                    setContact(contact);*//*

                    hideProgress();
                }
            }
        });*/
    }
    
    private void updateData()
    {
        onRefreshStart();
        
       /*contactObservable.subscribe(new Action1<Contact>()
       {
           @Override
           public void call(Contact contact)
           {
                dbManager.updateContact(contact);
                dbManager.updateMessages(contact.messages);
                onRefreshStop();
                
           }
       }, new Action1<Throwable>()
       {
           @Override
           public void call(Throwable throwable)
           {
               handleError(throwable);
               onRefreshStop();
           }
       }); */
    }
    
    public void setContact(Contact contact)
    {
        String date = Dates.parseLocalDateStringAbbreviatedTime(contact.created_at);
        String amount =  contact.amount + " " + contact.currency;
        String type = "";
        switch (contact.advertisement.trade_type) {
            case LOCAL_BUY:
            case LOCAL_SELL:
                type = (contact.is_buying)? getString(R.string.contact_list_buying_locally, amount, date):getString(R.string.contact_list_selling_locally, amount, date);
                //smsReleaseCodeLayout.setVisibility((contact.is_buying)? GONE:VISIBLE);
                break;
            case ONLINE_BUY:
            case ONLINE_SELL:
                type = (contact.is_buying)? getString(R.string.contact_list_buying_online, amount, contact.advertisement.payment_method, date):getString(R.string.contact_list_selling_online, amount, contact.advertisement.payment_method, date);
                //smsReleaseCodeLayout.setVisibility(GONE);
                break;
        }

        tradeType.setText(Html.fromHtml(type));
        tradePrice.setText(getString(R.string.trade_price, contact.amount, contact.currency));
        tradeAmount.setText(contact.amount_btc + " " + getString(R.string.btc));
        dealPrice.setText(Conversions.formatDealAmount(contact.amount_btc, contact.amount) + " " + contact.currency);

        tradeAmount.setText(contact.amount_btc + " " + getString(R.string.btc));
        traderName.setText((contact.is_buying)? contact.seller.username:contact.buyer.username);
        tradeFeedback.setText((contact.is_buying)? contact.seller.feedback_score:contact.buyer.feedback_score);
        tradeCount.setText((contact.is_buying)? contact.seller.trade_count:contact.buyer.trade_count);

        lastSeenIcon.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.advertisement.advertiser.last_online));

        int buttonTag = TradeUtils.getTradeActionButtonLabel(contact);
        if(buttonTag > 0)
            contactButton.setText(getString(buttonTag));

        contactButton.setTag(buttonTag);

        buttonLayout.setVisibility((buttonTag == 0)? View.GONE:View.VISIBLE);

        if(buttonTag == R.string.button_cancel) {    
            contactButton.setBackgroundResource(R.drawable.button_red_small_selector);   
        } else {
            contactButton.setBackgroundResource(R.drawable.button_green_small_selector);
        }

        String description = TradeUtils.getContactDescription(contact, this);
        noteText.setText(Html.fromHtml(description));
        noteText.setMovementMethod(LinkMovementMethod.getInstance());
        contactHeaderLayout.setVisibility((description == null)? View.GONE:View.VISIBLE);

        if(TradeUtils.canDisputeTrade(contact) && !TradeUtils.isLocalTrade(contact)) {
            //disputeItem.setVisible(buttonTag != R.string.button_dispute);
        }
          
        if(TradeUtils.canCancelTrade(contact)) {
            //cancelItem.setVisible(buttonTag != R.string.button_cancel);
        }
            
        getAdapter().replaceWith(contact.messages);
    }

    private MessageAdapter getAdapter()
    {
        return adapter;
    }

    private void setAdapter(MessageAdapter adapter)
    {
        list.setAdapter(adapter);
    }

    private void validateMessage(String message)
    {
        if (Strings.isBlank(message)) {
            return;
        }

        postMessage(contactId, message);
    }

    public void clearMessage()
    {
        message = null;
        newMessageText.setText("");
    }
    
    public void downloadAttachment(Message message)
    {
        tokensObservable.subscribe(new Action1<SessionItem>()
        {
            @Override
            public void call(SessionItem sessionItem)
            {
                String token = sessionItem.access_token();
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(message.attachment_url + "?access_token=" + token));
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                request.setVisibleInDownloadsUi(true);
                request.setMimeType(message.attachment_type);
                request.setTitle(message.attachment_name);
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
    
    public void postMessage(String contact_id, String message)
    {
        /*((BaseActivity) getContext()).showProgressDialog(new ProgressDialogEvent("Sending message..."));

        Observable<Response> postMessage = service.postMessage(contact_id, message);
        postMessage.subscribe(new Action1<Response>() {
            @Override
            public void call(Response response) {
                getView().clearMessage();
                ((BaseActivity) getContext()).hideProgressDialog();
                Toast.makeText(getContext(), getString(R.string.toast_message_sent), Toast.LENGTH_SHORT).show();
                //cancelCheck(); // stop auto checking
                getContact(contact_id); // refresh contact
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                ((BaseActivity) getContext()).hideProgressDialog();
                Toast.makeText(getContext(), getString(R.string.toast_error_message), Toast.LENGTH_SHORT).show();
            }
        });*/
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
        ConfirmationDialogEvent event = new ConfirmationDialogEvent(title, message, getString(R.string.button_ok), getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                contactAction(contactId, pinCode, action);
            }
        });

        showConfirmationDialog(event);
    }

    private void contactAction(final String contactId, final String pinCode, final ContactAction action)
    {
        contactActionObservable = bindActivity(this, dbManager.contactAction(contactId, pinCode, action));
        contactActionObservable.subscribe(new Action1<JSONObject>()
        {
            @Override
            public void call(JSONObject jsonObject)
            {
                hideProgressDialog();

                if (action == ContactAction.RELEASE) {
                    toast(R.string.trade_released_toast_text);
                }

                updateData();
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setMessageOnClipboard(Message message)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.message_clipboard_title), message.msg);
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setText(message.msg);
        }

        if(!Strings.isBlank(message.attachment_name)) {
            downloadAttachment(message);
            toast(R.string.message_copied_attachment_toast);
        } else {
            toast(R.string.message_copied_toast);
        }
    }
    
    public void showProfile()
    {
        String url = "https://localbitcoins.com/accounts/profile/" + ((contact.is_buying)? contact.seller.username:contact.buyer.username) + "/";
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
                title = (contact.is_buying)? "Buying Locally":"Selling Locally";
                break;
            case ONLINE_BUY:
            case ONLINE_SELL:
                title = (contact.is_buying)? "Buying Online":"Selling Online";
                break;
        }

        getSupportActionBar().setTitle(title);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                showDownload();
            }
        }
    };
}
