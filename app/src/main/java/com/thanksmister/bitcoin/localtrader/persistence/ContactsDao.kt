/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import androidx.room.*
import androidx.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface ContactsDao {

    /**
     * Get a item by id.
     * @return the item from the table with a specific id.
     */
    @Query("SELECT * FROM Contacts WHERE contactId = :id")
    fun getItemById(id: Int): Flowable<Contact>

    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM Contacts ORDER BY createdAt DESC")
    fun getItems(): Flowable<List<Contact>>

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: Contact)

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(items: List<Contact>)

    /**
     * Delete item.
     */
    @Delete
    fun deleteItem(item: Contact)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Contacts WHERE contactId = :id")
    fun deleteItem(id: Int)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Contacts")
    fun deleteAllItems()
}