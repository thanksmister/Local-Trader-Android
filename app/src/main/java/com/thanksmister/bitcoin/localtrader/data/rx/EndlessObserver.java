package com.thanksmister.bitcoin.localtrader.data.rx;

/**
 * Author: Michael Ritchie
 * Date: 9/18/14
 * Copyright 2013, ThanksMister LLC
 */
import rx.Observer;

public abstract class EndlessObserver<T> implements Observer<T> {
    @Override public void onCompleted() {
    }

    @Override public void onError(Throwable throwable) {
    }
}
       