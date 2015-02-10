package com.thanksmister.bitcoin.localtrader.ui.promo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.ui.about.AboutPresenter;
import com.thanksmister.bitcoin.localtrader.ui.about.AboutView;
import com.thanksmister.bitcoin.localtrader.ui.login.LoginActivity;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2014, ThanksMister LLC
 */
public class PromoPresenterImpl implements PromoPresenter
{
    private PromoView view;

    public PromoPresenterImpl(PromoView view) 
    {
        this.view = view;
    }

    @Override
    public void onDestroy()
    {
    }

    @Override
    public void onResume()
    {
    }

    @Override
    public void showLoginView()
    {
        Intent intent = LoginActivity.createStartIntent(getContext());
        intent.setClass(getView().getContext(), LoginActivity.class);
        getContext().startActivity(intent);
    }

    @Override
    public void showRegistration()
    {
        String url = Constants.REGISTRATION_URL;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        getContext().startActivity(browserIntent);
    }

    private PromoView getView()      
    {
        return view;
    }

    private Context getContext()
    {
        return getView().getContext();
    }
}
