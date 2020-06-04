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
import com.thanksmister.bitcoin.localtrader.network.api.model.*

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface NotificationsDao {

    /**
     * Get a item by id.
     * @return the item from the table with a specific id.
     */
    @Query("SELECT * FROM Notifications WHERE notificationId = :id")
    fun getItemById(id: String): Flowable<Notification>

    /**
     * Get a unread item by contact id.
     * @return the item from the table with a specific id and read is false.
     */
    @Query("SELECT * FROM Notifications WHERE read = :read")
    fun getItemUnreadItems(read: Boolean): Flowable<List<Notification>>

    /**
     * Get a unread item by contact id.
     * @return the item from the table with a specific id and read is false.
     */
    @Query("SELECT * FROM Notifications WHERE contactId = :id AND read = :read")
    fun getItemUnreadItemByContactId(id: Int, read: Boolean): Flowable<Notification>

    /**
     * Get all items ordered by date
     * @return list of all items with specific date
     */
    @Query("SELECT * FROM Notifications ORDER BY createdAt DESC")
    fun getItems(): Flowable<List<Notification>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateItem(item: Notification)

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: Notification)

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(items: List<Notification>)

    @Transaction
    fun replaceItems(items: List<Notification>) {
        deleteAllItems()
        insertItems(items)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Notifications WHERE notificationId = :id")
    fun deleteItem(id: String)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Notifications")
    fun deleteAllItems()
}