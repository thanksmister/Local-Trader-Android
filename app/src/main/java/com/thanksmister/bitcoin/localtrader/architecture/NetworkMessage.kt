/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.architecture

import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import android.text.TextUtils
import com.thanksmister.bitcoin.localtrader.network.api.model.Transaction

/**
 * A SingleLiveEvent used for Alert dialog messages. Like a [SingleLiveEvent] but also prevents
 * null messages and uses a custom observer.
 *
 *
 * Note that only one observer is going to be notified of changes.
 * https://github.com/googlesamples/android-architecture/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SnackbarMessage.java
 */
class NetworkMessage : SingleLiveEvent<MessageData>() {

    fun observe(owner: LifecycleOwner, observer: AlertObserver) {
        super.observe(owner, Observer { t ->
            if (t == null) {
                return@Observer
            }
            observer.onNewMessage(t)
        })
    }

    interface AlertObserver {
        /**
         * Called when there is a new message to be shown.
         * @param alertMessage The new message, non-null.
         */
        fun onNewMessage(messageData: MessageData)
    }
}