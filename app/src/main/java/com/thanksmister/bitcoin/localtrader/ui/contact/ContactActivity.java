package com.thanksmister.bitcoin.localtrader.ui.contact;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.ui.release.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Dates;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ContactActivity extends BaseActivity implements ContactView, SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_ID = "com.thanksmister.extras.EXTRA_ID";

    @Inject
    ContactPresenter presenter;

    @InjectView(R.id.contactProgress)
    View progress;

    @InjectView(R.id.contactEmpty)
    View empty;

    @InjectView(R.id.retryTextView)
    TextView errorTextView;

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
    private List<String> numbersList;
    private MessageAdapter adapter;
    private String message;
    private Contact contact;
    private String contactId;
    private DownloadManager downloadManager;

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
                presenter.cancelContact();
            } else if (view.getTag().equals(R.string.button_release)) {
                presenter.releaseTrade();
            } else if (view.getTag().equals(R.string.button_fund)) {
                presenter.fundContact();
            } else if (view.getTag().equals(R.string.button_mark_paid)) {
                presenter.markContactPaid();
            }
        });

        newMessageText = (EditText) headerView.findViewById(R.id.newMessageText);
        newMessageText.addTextChangedListener(new TextWatcher() {
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
            presenter.setMessageOnClipboard(message);
        });

        adapter = new MessageAdapter(this);
        setAdapter(adapter);
        
        presenter.getContact(contactId);
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
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new ContactModule(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.contact);

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        presenter.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        unregisterReceiver(receiver);

        presenter.onDestroy();
    }

    @Override
    public void onRefresh()
    {
        presenter.getContact(contactId);
    }

    @Override
    public void onRefreshStop()
    {
        if(swipeLayout != null)
            swipeLayout.setRefreshing(false);
    }

    @Override
    public void onError(String message)
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        errorTextView.setText(message);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
            String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
            presenter.releaseTradeWithPin(pinCode);
        } 
    }

    @Override
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
  
                switch (menuItem.getItemId()) {
                    case R.id.action_profile:
                        presenter.showProfile();
                        return true;
                    case R.id.action_advertisement:
                        presenter.showAdvertisement(contact.advertisement.id);
                        return true;
                    case R.id.action_dispute:
                        presenter.disputeContact();
                        return true;
                }
                return false;
            }
        });
    }

    @Override 
    public Context getContext() 
    {
        return this;
    }

    @Override
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    @Override
    public void setContact(Contact contact)
    {
        this.contact = contact;
        //this.progress.setVisibility(GONE);
        //this.list.setVisibility(VISIBLE);

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

        presenter.postMessage(contact.contact_id, message);
    }

    public void clearMessage()
    {
        message = null;
        newMessageText.setText("");
    }
    
    @Override
    public void downloadAttachment(Message message, String token)
    {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(message.attachment_url + "?access_token=" + token));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE|DownloadManager.Request.NETWORK_WIFI);
        request.setVisibleInDownloadsUi(true);
        request.setMimeType(message.attachment_type);
        request.setTitle(message.attachment_name);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }

    private void showDownload() 
    {
        Intent i = new Intent();
        i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(i);
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
