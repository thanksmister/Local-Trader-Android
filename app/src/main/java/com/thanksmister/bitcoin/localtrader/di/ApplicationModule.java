/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */
package com.thanksmister.bitcoin.localtrader.di;

import android.app.Application;
import android.content.Context;

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.persistence.AdvertisementsDao;
import com.thanksmister.bitcoin.localtrader.persistence.ContactsDao;
import com.thanksmister.bitcoin.localtrader.persistence.CurrenciesDao;
import com.thanksmister.bitcoin.localtrader.persistence.ExchangeRateDao;
import com.thanksmister.bitcoin.localtrader.persistence.LocalTraderDatabase;
import com.thanksmister.bitcoin.localtrader.persistence.MessageDao;
import com.thanksmister.bitcoin.localtrader.persistence.MethodsDao;
import com.thanksmister.bitcoin.localtrader.persistence.NotificationsDao;
import com.thanksmister.bitcoin.localtrader.persistence.UserDao;
import com.thanksmister.bitcoin.localtrader.persistence.WalletDao;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
abstract class ApplicationModule {

    @Binds
    abstract Application application(BaseApplication baseApplication);

    @Provides
    static Context provideContext(Application application) {
        return application;
    }

    @Provides
    static LocalTraderDatabase provideDatabase(Application app) {
        return LocalTraderDatabase.getInstance(app);
    }

    @Provides
    static ContactsDao provideContactsDao(LocalTraderDatabase database) {
        return database.contactsDao();
    }

    @Provides
    static UserDao provideUserDao(LocalTraderDatabase database) {
        return database.userDao();
    }

    @Provides
    static AdvertisementsDao provideAdvertisementsDao(LocalTraderDatabase database) {
        return database.advertisementsDao();
    }

    @Provides
    static MessageDao provideMessageDao(LocalTraderDatabase database) {
        return database.messageDao();
    }

    @Provides
    static NotificationsDao provideNotificationsDao(LocalTraderDatabase database) {
        return database.notificationsDao();
    }

    @Provides
    static MethodsDao provideMethodsDao(LocalTraderDatabase database) {
        return database.methodsDao();
    }

    @Provides
    static CurrenciesDao provideCurrenciesDao(LocalTraderDatabase database) {
        return database.currenciesDao();
    }

    @Provides
    static WalletDao provideWalletDao(LocalTraderDatabase database) {
        return database.walletDao();
    }

    @Provides
    static ExchangeRateDao provideExchangeRateDao(LocalTraderDatabase database) {
        return database.exchangeDao();
    }
}