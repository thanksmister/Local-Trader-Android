/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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