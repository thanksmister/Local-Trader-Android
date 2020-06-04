/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.*

@Entity(tableName = "Methods", indices = [(Index(value = arrayOf("key"), unique = true))])
class Method {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "key")
    var key: String? = null

    @ColumnInfo(name = "code")
    var code: String? = null

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "currencies")
    @TypeConverters(StringConverter::class)
    var currencies: ArrayList<String> = ArrayList<String>()
}



