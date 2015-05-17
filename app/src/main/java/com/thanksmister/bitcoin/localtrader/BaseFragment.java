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

package com.thanksmister.bitcoin.localtrader;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;

import butterknife.ButterKnife;
import timber.log.Timber;

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
    
    protected void reportError(Throwable throwable)
    {
        if(throwable != null)
            Timber.e("Data Error: " + throwable.getLocalizedMessage());
    }

    protected void handleError(Throwable throwable)
    {
        if(DataServiceUtils.isNetworkError(throwable)) {
            toast(getString(R.string.error_no_internet) + ", Code 503");
        } else if(DataServiceUtils.isHttp403Error(throwable)) {
            toast(getString(R.string.error_authentication) + ", Code 403");
            //((BaseActivity)getActivity()).logOut();
        } else if(DataServiceUtils.isHttp401Error(throwable)) {
            toast(getString(R.string.error_no_internet) + ", Code 401");
        } else if(DataServiceUtils.isHttp500Error(throwable)) {
            toast(getString(R.string.error_service_error) + ", Code 500");
        } else if(DataServiceUtils.isHttp404Error(throwable)) {
            toast(getString(R.string.error_service_error) + ", Code 404");
        } else if(DataServiceUtils.isHttp400GrantError(throwable)) {
            toast(getString(R.string.error_authentication) + ", Code 400 Grant Invalid");
            //((BaseActivity)getActivity()).logOut();
        } else if(DataServiceUtils.isHttp400Error(throwable)) {
            toast(getString(R.string.error_service_error) + ", Code 400");
        } else {
            toast(R.string.error_generic_error);
        }

        if(throwable != null)
            Timber.e("Data Error: " + throwable.getLocalizedMessage());
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
