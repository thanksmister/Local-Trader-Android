/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.di;

import android.app.Service;

import com.thanksmister.bitcoin.localtrader.network.sync.SyncService;

import dagger.Binds;
import dagger.Module;
import dagger.android.AndroidInjector;
import dagger.android.ServiceKey;
import dagger.multibindings.IntoMap;

@Module(subcomponents = {
        SyncServiceSubcomponent.class
})
public abstract class ServicesModule {
    @Binds
    @IntoMap
    @ServiceKey(SyncService.class)
    abstract AndroidInjector.Factory<? extends Service> syncServiceInjectorFactory(SyncServiceSubcomponent.Builder builder);
}