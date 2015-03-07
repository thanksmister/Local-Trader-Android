package com.thanksmister.bitcoin.localtrader.ui.contact;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactAction;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.release.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;

import retrofit.client.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class ContactPresenterImpl implements ContactPresenter
{
    private ContactView view;
    private DataService service;
    private Bus bus;
    private Subscription subscription;
    private Contact contact;
    private String pinCode;

    public ContactPresenterImpl(ContactView view, DataService service, Bus bus) 
    {
        this.view = view;
        this.service = service;
        this.bus = bus;
    }

    @Override
    public void onResume()
    {
        bus.register(this);
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();

        bus.unregister(this);
    }

    @Override
    public void getContact(String contactId)
    {
        subscription = service.getContact(new Observer<Contact>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable throwable) {
                RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                if(retroError.isAuthenticationError()) {
                    Toast.makeText(getContext(), retroError.getMessage(), Toast.LENGTH_SHORT).show();
                    ((BaseActivity) getContext()).logOut();
                } else {
                    getView().onError("Error loading trade.");
                }
                
                getView().onRefreshStop();
            }

            @Override
            public void onNext(Contact results) {
  
                getView().hideProgress();
                contact = results;
                getView().setContact(contact);

                getView().onRefreshStop();
                //setTitle(contact);
            }
        }, contactId);
    }

    @Override
    public void postMessage(String contact_id, String message)
    {
        ((BaseActivity) getContext()).showProgressDialog(new ProgressDialogEvent("Sending message..."));
        
        Observable<Response> postMessage = service.postMessage(contact_id, message);
        postMessage.subscribe(new Action1<Response>() {
            @Override
            public void call(Response response) {
                getView().clearMessage();
                ((BaseActivity) getContext()).hideProgressDialog();
                Toast.makeText(getContext(), getContext().getString(R.string.toast_message_sent), Toast.LENGTH_SHORT).show();
                //cancelCheck(); // stop auto checking
                getContact(contact_id); // refresh contact
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                ((BaseActivity) getContext()).hideProgressDialog();
                Toast.makeText(getContext(), getContext().getString(R.string.toast_error_message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disputeContact()
    {
        createAlert("Dispute Trade", getContext().getString(R.string.contact_dispute_confirm), contact.contact_id, null, ContactAction.DISPUTE);
    }

    @Override
    public void fundContact()
    {
        createAlert("Fund Trade", getContext().getString(R.string.contact_fund_confirm), contact.contact_id, null, ContactAction.FUND);
    }

    @Override
    public void markContactPaid()
    {
        createAlert("Mark Paid", getContext().getString(R.string.contact_paid_confirm), contact.contact_id, null, ContactAction.PAID);
    }

    @Override
    public void releaseTrade()
    {
        Intent intent = PinCodeActivity.createStartIntent(getContext());
        intent.setClass(getContext(), PinCodeActivity.class);
        ((BaseActivity)getContext()).startActivityForResult(intent, PinCodeActivity.RESULT_VERIFIED);
    }

    @Override
    public void releaseTradeWithPin(String pinCode)
    {
        ((BaseActivity) getContext()).showProgressDialog(new ProgressDialogEvent("Releasing trade..."));
        contactAction(contact.contact_id, pinCode, ContactAction.RELEASE);
    }

    @Override
    public void cancelContact()
    {
        createAlert("Cancel Trade", getContext().getString(R.string.contact_cancel_confirm), contact.contact_id, null, ContactAction.CANCEL);
    }

    public void createAlert(String title, String message, final String contactId, final String pinCode, final ContactAction action)
    {
        Context context = getView().getContext();
        ConfirmationDialogEvent event = new ConfirmationDialogEvent(title, message, getContext().getString(R.string.button_ok), getContext().getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                contactAction(contactId, pinCode, action);
            }
        });

        ((BaseActivity) context).showConfirmationDialog(event);
    }

    private void contactAction(final String contactId, final String pinCode, final ContactAction action)
    {
        subscription = service.contactAction(new Observer<Object>() {
            @Override
            public void onCompleted() {
                ((BaseActivity) getContext()).hideProgressDialog();
                if(action == ContactAction.RELEASE) {
                    Toast.makeText(getContext(), getContext().getString(R.string.trade_released_toast_text), Toast.LENGTH_SHORT).show();
                    //((BaseActivity) getView()).finish();
                }
            }

            @Override
            public void onError(Throwable e) {
                ((BaseActivity) getContext()).hideProgressDialog();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                //((BaseActivity) getContext()).showAlertDialog(new AlertDialogEvent("Error", e.getMessage()));
                Timber.e("Error Contact Action: " + e.getMessage());
            }

            @Override
            public void onNext(Object o) {
                
                getContact(contactId); // refresh contact
            }
            
        }, contactId, pinCode, action);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    @Override
    public void setMessageOnClipboard(Message message)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getContext().getString(R.string.message_clipboard_title), message.msg);
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(message.msg);
        }

        if(!Strings.isBlank(message.attachment_name)) {
            getView().downloadAttachment(message, service.getAccessToken());
            Toast.makeText(getContext(), getContext().getString(R.string.message_copied_attachment_toast), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.message_copied_toast), Toast.LENGTH_SHORT).show();
        }
    }
 
    @Override
    public void showProfile()
    {
        Context context = getView().getContext();
        String url = "https://localbitcoins.com/accounts/profile/" + ((contact.is_buying)? contact.seller.username:contact.buyer.username) + "/";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }

    @Override
    public void showAdvertisement(String advertisement_id)
    {
        /*Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(contact.actions.advertisement_public_view));
        getView().getContext().startActivity(browserIntent);*/
        Context context = getView().getContext();
        Intent intent = AdvertisementActivity.createStartIntent(context, advertisement_id);
        intent.setClass(context, AdvertisementActivity.class);
        context.startActivity(intent);
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
    }

    private ContactView getView()
    {
        return view;
    }
    
    private Context getContext()
    {
        return getView().getContext();
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        //Timber.d("onNetworkEvent: " + event.name());

        if(event == NetworkEvent.DISCONNECTED) {
            //cancelCheck(); // stop checking we have no network
        } else  {
            //startCheck();
        }
    }
}
