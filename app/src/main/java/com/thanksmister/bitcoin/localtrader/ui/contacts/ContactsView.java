package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.content.Context;
import android.support.v7.widget.Toolbar;

import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface ContactsView
{
    void setToolBarMenu(Toolbar toolbar);

    Context getContext();

    public void onError(String message);

    public void onRefreshStop();
    
    public void showProgress();

    public void hideProgress();

    void setContacts(List<Contact> contacts);

    void setTitle(DashboardType dashboardType);
}