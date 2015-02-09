package com.thanksmister.bitcoin.localtrader.ui.contact;

import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementActivity;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface ContactPresenter
{
    public void onDestroy();
    public void onResume();
    void getContact(String contactId);
    void postMessage(String contact_id, String message);

    void releaseTradeWithPin(String pinCode);

    void cancelContact();
    void releaseTrade();
    void disputeContact();
    void fundContact();
    void markContactPaid();
    void setMessageOnClipboard(Message message);
    void showProfile();
    void showAdvertisement(String advertisement_id);
}
