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
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet

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
    @Query("SELECT * FROM Advertisements ORDER BY createdAt DESC")
    fun getItems(): Flowable<List<Advertisement>>

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