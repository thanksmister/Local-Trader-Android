/*
 * Copyright (c) 2019 ThanksMister LLC
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

import androidx.lifecycle.ViewModel
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
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
    @ViewModelKey(BaseViewModel::class)
    abstract fun bindsBaseViewModel(viewModel: BaseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WalletViewModel::class)
    abstract fun bindsWalletViewModel(viewModel: WalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindsLoginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdvertisementsViewModel::class)
    abstract fun bindsAdvertisementsViewModel(viewModel: AdvertisementsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DashboardViewModel::class)
    abstract fun bindsDashboardViewModel(viewModel: DashboardViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactsViewModel::class)
    abstract fun bindsContactViewModel(viewModel: ContactsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationsViewModel::class)
    abstract fun bindsNotificationsViewModel(viewModel: NotificationsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindsSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModel::class)
    abstract fun bindsSearchViewModel(viewModel: SearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PinCodeViewModel::class)
    abstract fun bindsPinCodeViewModel(viewModel: PinCodeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessageViewModel::class)
    abstract fun bindsMessageViewModel(viewModel: MessageViewModel): ViewModel

    @ContributesAndroidInjector
    internal abstract fun baseActivity(): BaseActivity

    @ContributesAndroidInjector
    internal abstract fun baseBitcoinHandler(): BitcoinHandler

    @ContributesAndroidInjector
    internal abstract fun splashActivity(): SplashActivity

    @ContributesAndroidInjector
    internal abstract fun promoActivity(): PromoActivity

    @ContributesAndroidInjector
    internal abstract fun settingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    internal abstract fun advertisementActivity(): AdvertisementActivity

    @ContributesAndroidInjector
    internal abstract fun activityAdvertiserActivity(): AdvertiserActivity

    @ContributesAndroidInjector
    internal abstract fun activityContactActivity(): ContactActivity

    @ContributesAndroidInjector
    internal abstract fun activityContactsActivity(): ContactsActivity

    @ContributesAndroidInjector
    internal abstract fun activityEditAdvertisementActivity(): EditAdvertisementActivity

    @ContributesAndroidInjector
    internal abstract fun activityEditLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    internal abstract fun activityMainActivity(): MainActivity

    @ContributesAndroidInjector
    internal abstract fun activityPinCodeActivity(): PinCodeActivity

    @ContributesAndroidInjector
    internal abstract fun activityScanQrCodeActivity(): ScanQrCodeActivity

    @ContributesAndroidInjector
    internal abstract fun activitySearchResultsActivity(): SearchResultsActivity

    @ContributesAndroidInjector
    internal abstract fun activitySendActivity(): SendActivity

    @ContributesAndroidInjector
    internal abstract fun activityAboutActivity(): AboutActivity

    @ContributesAndroidInjector
    internal abstract fun activityRequestActivity(): RequestActivity

    @ContributesAndroidInjector
    internal abstract fun activitySearchActivity(): SearchActivity

    @ContributesAndroidInjector
    internal abstract fun activityShareQrCodeActivity(): ShareQrCodeActivity

    @ContributesAndroidInjector
    internal abstract fun activityTradeRequestActivity(): TradeRequestActivity

    @ContributesAndroidInjector
    internal abstract fun activityWalletActivity(): WalletActivity

    @ContributesAndroidInjector
    internal abstract fun activityContactHistoryActivity(): ContactHistoryActivity

    @ContributesAndroidInjector
    internal abstract fun activityMessageActivity(): MessageActivity

    @ContributesAndroidInjector
    internal abstract fun baseFragment(): BaseFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentAbout(): AboutFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentSetting(): SettingsFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentAdvertisementsFragment(): AdvertisementsFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentContactsFragment(): ContactsFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentSearchFragment(): SearchFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentNotificationsFragment(): NotificationsFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentRequestFragment(): RequestFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentSendFragment(): SendFragment

    @ContributesAndroidInjector
    internal abstract fun fragmentWalletFragment(): WalletFragment
}