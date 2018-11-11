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
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.Wallet

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface MethodsDao {
    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM Methods")
    fun getItems(): Flowable<List<Method>>

    /**
     * Insert feeds in the database. If the feed already exists, replace it.
     * @param feed the feed to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<Method>)

    @Transaction
    fun replaceItem(items: List<Method>) {
        deleteAllItems()
        insertAll(items)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Methods")
    fun deleteAllItems()
}