/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.di;

import com.thanksmister.bitcoin.localtrader.BaseApplication;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {
        AndroidSupportInjectionModule.class,
        ApplicationModule.class,
        ServicesModule.class,
        ApiModule.class,
        ActivityModule.class,
        AndroidBindingModule.class,
        DaggerViewModelInjectionModule.class
})

@ApplicationScope
public interface ApplicationComponent extends AndroidInjector<BaseApplication> {
    @Component.Builder
    abstract class Builder extends AndroidInjector.Builder<BaseApplication>{
    }
}