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

package com.thanksmister.bitcoin.localtrader.network

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Error
import com.thanksmister.bitcoin.localtrader.network.api.model.ErrorResponse
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber

/**
 * Created by Michael Ritchie on 5/17/18.
 */
class ApiError constructor(error: Throwable) {
    var message: String? = null
    var code: Int = 0
    var status = 0

    init {
        if (error is HttpException) {
            status = error.code();
            val errorJsonString = error.response().errorBody()?.string()
            Timber.d("json error: $errorJsonString")
            val errorResponse = Gson().fromJson<ErrorResponse>(errorJsonString, ErrorResponse::class.java)
            val errorLists = errorResponse.error.errorLists
            if(errorLists != null) {
                val errorList = errorLists.all
                if (!errorList.isEmpty()) {
                    message = errorList.get(0)
                }
            } else if (!TextUtils.isEmpty(errorResponse.error.message)) {
                message = errorResponse.error.message
            }
            code = errorResponse.error.errorCode.toInt()
            Timber.d("Error code: $code")
            Timber.d("Error message: $message")
        } else {
            this.message = error.message ?: this.message
        }
    }
}