/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.*

@Entity(tableName = "Currencies", indices = [(Index(value = arrayOf("code"), unique = true))])
class Currency {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "code")
    var code: String? = null

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "altcoin")
    var altcoin: Boolean = false

}
