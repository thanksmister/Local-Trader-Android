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
import com.thanksmister.bitcoin.localtrader.network.api.model.Message

import io.reactivex.Flowable

/**
 * Data Access Object for the messages table.
 */
@Dao
interface MessageDao {

    /**
     * Get a message by id.
     * @return the message from the table with a specific id.
     */
    @Query("SELECT * FROM Messages WHERE contactId = :contact_id")
    fun getMessageById(contact_id: String): Flowable<Message>

    /**
     * Get all messages
     * @return list of all messages
     */
    @Query("SELECT * FROM Messages")
    fun getMessages(): Flowable<List<Message>>

    /**
     * Insert a message in the database. If the message already exists, replace it.
     * @param user the message to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: Message)

    /**
     * Delete all messages.
     */
    @Query("DELETE FROM Messages")
    fun deleteAllMessages()
}