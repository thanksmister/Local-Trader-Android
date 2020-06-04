/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import androidx.room.*
import androidx.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface WalletDao {
    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM Wallet")
    fun getItems(): Flowable<List<Wallet>>

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: Wallet)

    /**
     * Delete all items and then replace them
     */
    @Transaction
    fun updateItem(item: Wallet) {
        deleteAllItems()
        insertItem(item)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Wallet")
    fun deleteAllItems()

}