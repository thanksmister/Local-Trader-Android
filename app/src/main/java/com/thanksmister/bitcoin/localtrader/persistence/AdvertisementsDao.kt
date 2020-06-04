/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import androidx.room.*
import androidx.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface AdvertisementsDao {

    /**
     * Get a item by id.
     * @return the item from the table with a specific id.
     */
    @Query("SELECT * FROM Advertisements WHERE adId = :id")
    fun getItemById(id: Int): Flowable<Advertisement>

    /**
     * Get all items ordered by date
     * @return list of all items with specific date
     */
    @Query("SELECT * FROM Advertisements WHERE tradeType IN(:types) ORDER BY createdAt DESC")
    fun getItems(types: List<String>): Flowable<List<Advertisement>>

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: Advertisement)

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(items: List<Advertisement>)

    /**
     * Delete all items.
     */
    @Delete
    fun deleteItem(item: Advertisement)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Advertisements")
    fun deleteAllItems()
}