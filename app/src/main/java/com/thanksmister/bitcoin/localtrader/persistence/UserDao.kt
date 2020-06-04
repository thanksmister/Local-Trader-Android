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
import com.thanksmister.bitcoin.localtrader.network.api.model.User

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * Data Access Object.
 */
@Dao
interface UserDao {
    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM User")
    fun getItems(): Single<List<User>>

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: User)

    /**
     * Delete all items and then replace them
     */
    @Transaction
    fun updateItem(item: User) {
        deleteAllItems()
        insertItem(item)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM User")
    fun deleteAllItems()
}