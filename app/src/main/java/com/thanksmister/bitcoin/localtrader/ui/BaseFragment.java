/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui;

import android.os.Bundle;

import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;

import dagger.android.support.DaggerFragment;

/**
 * Base fragment which performs injection using the activity object graph of its parent.
 */
public abstract class BaseFragment extends DaggerFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void reportError(Throwable throwable) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).reportError(throwable);
    }

    protected void handleError(Throwable throwable, boolean retry) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).handleError(throwable, retry);
    }

    protected void handleError(Throwable throwable) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).handleError(throwable, false);
    }

    protected void toast(int messageId) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).toast(messageId);
    }

    protected void toast(String message) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).toast(message);
    }

    public void showAlertDialog(AlertDialogEvent event) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).showAlertDialog(event);
    }

    public void showConfirmationDialog(ConfirmationDialogEvent event) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).showConfirmationDialog(event);
    }

    public void showProgressDialog(ProgressDialogEvent event, boolean cancelable) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).showProgressDialog(event, cancelable);
    }

    public void showProgressDialog(ProgressDialogEvent event) {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).showProgressDialog(event, false);
    }

    public void hideProgressDialog() {
        if (isAdded() && getActivity() != null)
            ((BaseActivity) getActivity()).hideProgressDialog();
    }
}