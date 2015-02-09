package com.thanksmister.bitcoin.localtrader.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.model.User;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.main.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.main.MainPresenter;
import com.thanksmister.bitcoin.localtrader.ui.main.MainView;

import rx.Observer;
import rx.Subscription;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class LoginPresenterImpl implements LoginPresenter
{
    private LoginView view;
    private DataService service;
    private Subscription subscription;
    private BaseApplication baseApplication;

    public LoginPresenterImpl(LoginView view, DataService service, BaseApplication baseApplication) 
    {
        this.view = view;
        this.service = service;
        this.baseApplication = baseApplication;
    }
    
    @Override
    public void onResume()
    {
        
    }

    @Override
    public void onDestroy()
    {
        
    }

    @Override
    public void setAuthorizationCode(final String code)
    {
        subscription = service.getAuthorization(new Observer<User>() {
            @Override
            public void onCompleted() {
                view.hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e.getMessage());
                view.showError(e.getMessage());
            }

            @Override
            public void onNext(User user) {
                view.hideProgress();
                Toast.makeText(getView().getContext(), "Login successful for " + user.username, Toast.LENGTH_SHORT).show();
                view.showMain();
            }
        }, code);
    }
    
    private LoginView getView()
    {
        return view;
    }
}
