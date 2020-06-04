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