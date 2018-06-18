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

package com.thanksmister.bitcoin.localtrader.di


import android.arch.lifecycle.ViewModel
import com.thanksmister.bitcoin.localtrader.ui.*
import com.thanksmister.bitcoin.localtrader.ui.activities.*
import com.thanksmister.bitcoin.localtrader.ui.fragments.*
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.*
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
internal abstract class AndroidBindingModule {

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindsLoginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindsMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModel::class)
    abstract fun bindsSearchViewModel(viewModel: SearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationsViewModel::class)
    abstract fun bindsNotificationsViewModel(viewModel: NotificationsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactsViewModel::class)
    abstract fun bindsContactsViewModel(viewModel: ContactsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdvertisementsViewModel::class)
    abstract fun bindsAdvertisementsViewModel(viewModel: AdvertisementsViewModel): ViewModel

    @ContributesAndroidInjector
    internal abstract fun baseActivity(): BaseActivity

    @ContributesAndroidInjector
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector
    internal abstract fun loginActivity(): LoginActivity

    @ContributesAndroidInjector
    internal abstract fun splashActivity(): SplashActivity

    @ContributesAndroidInjector
    internal abstract fun contactActivity(): ContactActivity

    @ContributesAndroidInjector
    internal abstract fun searchActivity(): SearchActivity

    @ContributesAndroidInjector
    internal abstract fun tradeRequestActivity(): TradeRequestActivity

    @ContributesAndroidInjector
    internal abstract fun promoActivity(): PromoActivity

    @ContributesAndroidInjector
    internal abstract fun bitcoinHandler(): BitcoinHandler

    @ContributesAndroidInjector
    internal abstract fun baseFragment(): BaseFragment

    @ContributesAndroidInjector
    internal abstract fun advertisementActivity(): AdvertisementActivity

    @ContributesAndroidInjector
    internal abstract fun settingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    internal abstract fun advertisementsFragment(): AdvertisementsFragment

    @ContributesAndroidInjector
    internal abstract fun notificationsFragment(): NotificationsFragment

    @ContributesAndroidInjector
    internal abstract fun contactsFragment(): ContactsFragment

    @ContributesAndroidInjector
    internal abstract fun searchFragment(): SearchFragment

    @ContributesAndroidInjector
    internal abstract fun dashBoardFragment(): MainFragment

}