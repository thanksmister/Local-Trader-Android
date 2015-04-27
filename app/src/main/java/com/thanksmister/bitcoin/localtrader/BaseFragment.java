package com.thanksmister.bitcoin.localtrader;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;

import butterknife.ButterKnife;

/**
 * Base fragment which performs injection using the activity object graph of its parent.
 */
public abstract class BaseFragment extends DialogFragment
{
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
    }

    public Toolbar getToolbar()
    {
        if (toolbar == null) {
            throw new RuntimeException("Toolbar has not been set.  Make sure not to call getToolbar() until onViewCreated() at the earliest.");
        }
        return toolbar;
    }

    protected void handleError(Throwable throwable)
    {
        if(DataServiceUtils.isHttp403Error(throwable)) {
            toast(getString(R.string.error_authentication) + " Code 403");
            ((BaseActivity)getActivity()).logOut();
        } else if(DataServiceUtils.isHttp401Error(throwable)) {
            toast(getString(R.string.error_no_internet) + " Code 401");
        } else if(DataServiceUtils.isHttp500Error(throwable)) {
            toast(getString(R.string.error_service_error) + " Code 500");
        } else if(DataServiceUtils.isHttp404Error(throwable)) {
            toast(getString(R.string.error_service_error) + " Code 404");
        } else if(DataServiceUtils.isHttp400GrantError(throwable)) {
            toast(getString(R.string.error_authentication) + " Code 400 Grant Invalid");
            ((BaseActivity)getActivity()).logOut();
        } else if(DataServiceUtils.isHttp400Error(throwable)) {
            toast(getString(R.string.error_service_error) + " Code 400");
        } else {
            toast(R.string.error_generic_error);
        }
    }

    protected void toast(int messageId)
    {
        Toast.makeText(getActivity(), messageId, Toast.LENGTH_SHORT).show();
    }

    protected void toast(String message)
    {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    protected int getColor(int colorRes)
    {
        return getResources().getColor(colorRes);
    }
}
