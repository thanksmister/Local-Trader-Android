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
import android.support.v4.app.Fragment;
import android.view.View;

import com.squareup.leakcanary.RefWatcher;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;

import butterknife.ButterKnife;

/**
 * Base fragment which performs injection using the activity object graph of its parent.
 */
public abstract class BaseFragment extends Fragment
{
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

    @Override 
    public void onDestroy() 
    {
        super.onDestroy();
        
        if (BuildConfig.DEBUG) {
            //RefWatcher refWatcher = BaseApplication.getRefWatcher(getActivity());
            //refWatcher.watch(this);
        }
    }

    public void launchScanner()
    {
        ((MainActivity) getActivity()).launchScanner();
    }
    
    protected void reportError(Throwable throwable)
    {
        ((MainActivity) getActivity()).reportError(throwable);
    }

    protected void handleError(Throwable throwable, boolean retry)
    {
        ((MainActivity) getActivity()).handleError(throwable, retry);
    }

    protected void handleError(Throwable throwable)
    {
        ((MainActivity) getActivity()).handleError(throwable, false);
    }

    protected void toast(int messageId)
    {
        ((MainActivity) getActivity()).toast(messageId);
    }

    protected void toast(String message)
    {
        ((MainActivity) getActivity()).toast(message);
    }

    protected int getColor(int colorRes)
    {
        return getResources().getColor(colorRes);
    }
}
