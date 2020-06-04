/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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
public class AlertMessage extends SingleLiveEvent<String> {
    public void observe(LifecycleOwner owner, final AlertObserver observer) {
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

    public interface AlertObserver {
        /**
         * Called when there is a new message to be shown.
         * @param alertMessage The new message, non-null.
         */
        void onNewMessage(String alertMessage);
    }
}