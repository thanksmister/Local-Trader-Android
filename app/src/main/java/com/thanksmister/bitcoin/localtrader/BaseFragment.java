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

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.trello.rxlifecycle.components.support.RxFragment;

import butterknife.ButterKnife;
import rx.functions.Action0;

/**
 * Base fragment which performs injection using the activity object graph of its parent.
 */
public abstract class BaseFragment extends RxFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Injector.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
    }
    
    @Override 
    public void onAttach(Context context)
    {
        super.onAttach(context);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
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
    
    protected void reportError(Throwable throwable)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).reportError(throwable);
    }

    protected void handleError(Throwable throwable, boolean retry)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).handleError(throwable, retry);
    }

    protected void handleError(Throwable throwable)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).handleError(throwable, false);
    }

    protected void toast(int messageId)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).toast(messageId);
    }

    protected void toast(String message)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).toast(message);
    }

    protected void snack(String message)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).snack(message, false);
    }

    protected void snackError(String message)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).snackError(message);
    }

    protected void snack(String message, boolean retry)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).snack(message, retry);
    }

    public void showAlertDialog(AlertDialogEvent event)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).showAlertDialog(event);
    }

    public void showAlertDialog(AlertDialogEvent event, Action0 action)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).showAlertDialog(event, action);
    }

    public void showAlertDialog(AlertDialogEvent event, Action0 actionPos, Action0 actionNeg)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).showAlertDialog(event, actionPos, actionNeg);
    }

    public void showConfirmationDialog(ConfirmationDialogEvent event)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).showConfirmationDialog(event);
    }
    
    public void showProgressDialog(ProgressDialogEvent event)
    {
        if(isAdded())
            ((BaseActivity) getActivity()).showProgressDialog(event);
    }

    public void hideProgressDialog()
    {
        if(isAdded())
            ((BaseActivity) getActivity()).hideProgressDialog();
    }
}
