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

import android.arch.persistence.room.*
import io.reactivex.Flowable

/**
 * Data Access Object for the messages table.
 */
@Dao
interface RateDao {
    /**
     * Get a item by id.
     * @return the item from the table with a specific id.
     */
    @Query("SELECT * FROM Rates WHERE uid = :id")
    fun getItemById(id: String): Flowable<Rate>

    /**
     * Get all items
     * @return list of all items
     */
    @Query("SELECT * FROM Rates ORDER BY created_at DESC")
    fun getItems(): Flowable<List<Rate>>

    /**
     * Insert a item in the database. If the item already exists, replace it.
     * @param item to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(item: Rate):Long

    @Transaction
    fun replaceItem(item: Rate) {
        deleteAllItems()
        insertItem(item)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Rates")
    fun deleteAllItems()
}