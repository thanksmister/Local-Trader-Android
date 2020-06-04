/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.di;

import com.thanksmister.bitcoin.localtrader.network.sync.SyncService;

import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Subcomponent(modules = {})
public interface SyncServiceSubcomponent extends AndroidInjector<SyncService> {
    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<SyncService> {
    }
}