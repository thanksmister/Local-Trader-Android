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
 */

package com.thanksmister.bitcoin.localtrader.network;


import android.support.annotation.NonNull;

import com.thanksmister.bitcoin.localtrader.persistence.Notification;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static com.thanksmister.bitcoin.localtrader.network.Status.ERROR;
import static com.thanksmister.bitcoin.localtrader.network.Status.LOADING;
import static com.thanksmister.bitcoin.localtrader.network.Status.SUCCESS;


/**
 * Response holder provided to the UI
 * https://proandroiddev.com/mvvm-architecture-using-livedata-rxjava-and-new-dagger-android-injection-639837b1eb6c
 */
public class NotificationsResponse {

    public  Status status;

    @Nullable
    public final List<Notification> data;

    @Nullable
    public final Throwable error;

    private NotificationsResponse(Status status, @Nullable List<Notification> data, @Nullable Throwable error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static NotificationsResponse loading() {
        return new NotificationsResponse(LOADING, null, null);
    }

    public static NotificationsResponse success(@NonNull List<Notification> data) {
        return new NotificationsResponse(SUCCESS, data, null);
    }

    public static NotificationsResponse complete() {
        return new NotificationsResponse(SUCCESS, new ArrayList<>(), null);
    }

    public static NotificationsResponse error(@NonNull Throwable error) {
        return new NotificationsResponse(ERROR, null, error);
    }
}