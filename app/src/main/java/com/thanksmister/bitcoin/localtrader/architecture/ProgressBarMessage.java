/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.architecture;

import androidx.annotation.StringRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.annotation.Nullable;
import android.text.TextUtils;

/**
 * A SingleLiveEvent used for Alert dialog messages. Like a {@link SingleLiveEvent} but also prevents
 * null messages and uses a custom observer.
 * <p>
 * Note that only one observer is going to be notified of changes.
 * https://github.com/googlesamples/android-architecture/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SnackbarMessage.java
 */
public class ProgressBarMessage extends SingleLiveEvent<String> {
    public void observe(LifecycleOwner owner, final ProgressBarObserver observer) {
        super.observe(owner, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String t) {
                if (TextUtils.isEmpty(t)) {
                    return;
                }
                observer.onNewMessage(t);
            }
        });
    }

    public interface ProgressBarObserver {
        /**
         * Called when there is a new message to be shown.
         * @param message The new message, non-null.
         */
        void onNewMessage(String message);
    }
}