/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import androidx.room.*
import androidx.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency


import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface CurrenciesDao {
    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM Currencies")
    fun getItems(): Flowable<List<Currency>>

    /**
     * Insert feeds in the database. If the feed already exists, replace it.
     * @param feed the feed to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<Currency>)

    @Transaction
    fun replaceItem(items: List<Currency>) {
        deleteAllItems()
        insertAll(items)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Currencies")
    fun deleteAllItems()
}