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

package com.thanksmister.bitcoin.localtrader.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import com.thanksmister.bitcoin.localtrader.network.api.model.*

/**
 * The Room database
 */
@Database(entities = arrayOf(Advertisement::class, Contact::class, Wallet::class, Notification::class,
        Method::class, User::class, Currency::class, Message::class, ExchangeRate::class), version = 3, exportSchema = false)

@TypeConverters(AdvertisementConverter::class, BuyerConverter::class, TotalConverter::class, TransactionConverter::class,
        SellerConverter::class, AccountDetailsConverter::class, ProfileConverter::class, ActionsConverter::class,
        AddressConverter::class, StringConverter::class, SenderConverter::class)
abstract class LocalTraderDatabase : RoomDatabase() {

    abstract fun advertisementsDao(): AdvertisementsDao
    abstract fun contactsDao(): ContactsDao
    abstract fun walletDao(): WalletDao
    abstract fun notificationsDao(): NotificationsDao
    abstract fun methodsDao(): MethodsDao
    abstract fun currenciesDao(): CurrenciesDao
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun exchangeDao(): ExchangeRateDao

    companion object {

        @Volatile private var INSTANCE: LocalTraderDatabase? = null

        @JvmStatic fun getInstance(context: Context): LocalTraderDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        LocalTraderDatabase::class.java, "local_trader_v3.db")
                        .fallbackToDestructiveMigration()
                        .build()
    }
}