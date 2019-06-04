/*
 * Copyright (c) 2019 ThanksMister LLC
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
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeRate
import com.thanksmister.bitcoin.localtrader.network.api.model.User

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