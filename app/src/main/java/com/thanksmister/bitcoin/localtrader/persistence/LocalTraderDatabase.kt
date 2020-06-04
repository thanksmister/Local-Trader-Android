/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.persistence

import androidx.room.*
import androidx.room.TypeConverters
import android.content.Context
import com.thanksmister.bitcoin.localtrader.network.api.model.*

/**
 * The Room database
 */
@Database(entities = arrayOf(Advertisement::class, Contact::class, Wallet::class, Notification::class,
        Method::class, User::class, Currency::class, Message::class, ExchangeRate::class), version = 6, exportSchema = false)

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