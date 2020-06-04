/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence


import androidx.room.*
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface ExchangeRateDao {
    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM ExchangeRate")
    fun getItems(): Flowable<List<ExchangeRate>>

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: ExchangeRate)

    /**
     * Delete all items and then replace them
     */
    @Transaction
    fun updateItem(item: ExchangeRate) {
        deleteAllItems()
        insertItem(item)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM ExchangeRate")
    fun deleteAllItems()
}