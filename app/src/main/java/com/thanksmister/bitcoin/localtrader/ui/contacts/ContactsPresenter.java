package com.thanksmister.bitcoin.localtrader.ui.contacts;

import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface ContactsPresenter
{
    public void onDestroy();
    public void onResume();
    void getContacts(DashboardType dashboardType);
    void getContact(Contact contact);
}
