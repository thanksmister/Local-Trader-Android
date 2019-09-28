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

package com.thanksmister.bitcoin.localtrader.network.exceptions

import android.content.Context

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.utils.StringUtils

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException

class RetrofitErrorHandler///api/ads/, 42, Given nonce was too small.
(private val mContext: Context) {

    private fun parseErrorMessage(json: String): ErrorResponse {

        val errorResponse = ErrorResponse()
        errorResponse.message = ""
        errorResponse.code = ExceptionCodes.NO_ERROR_CODE

        val jsonObject: JSONObject

        // check for special invalid grant error
        if (json.contains("invalid_grant")) {
            errorResponse.message = "invalid_grant"
            errorResponse.code = ExceptionCodes.INVALID_GRANT
            return errorResponse
        }

        try {
            jsonObject = JSONObject(json)
            var errorObj = JSONObject()
            var error_code = ExceptionCodes.NO_ERROR_CODE
            var error_message = StringBuilder()

            if (jsonObject.has("error")) {
                errorObj = jsonObject.getJSONObject("error")
                error_message = StringBuilder(errorObj.getString("message"))
            } else if (jsonObject.has("message")) {
                error_message = StringBuilder(jsonObject.getString("message"))
            }

            if (errorObj.has("error_code")) {
                error_code = errorObj.getInt("error_code")
            } else if (jsonObject.has("error_code")) {
                error_code = jsonObject.getInt("error_code")
            }
            // TODO setup a test case to properly parse this
            //{"message":"One or more parameters did not validate. Please check the API documentation for usage.","errors":{"amount":"Not enough balance"},"error_code":19}

            if (errorObj.has("errors")) {
                error_message = StringBuilder()
                val errors = errorObj.getJSONObject("errors")
                val keys = errors.keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val message = errors.getString(key)
                    error_message.append(StringUtils.convertCamelCase(key)).append(" ").append(message).append(" ")
                }
            }

            errorResponse.message = error_message.toString()
            errorResponse.code = error_code

        } catch (e: JSONException) {
            Timber.e(e.message)
        }

        return errorResponse
    }

    private fun parseError(response: String): ErrorResponse {
        //Timber.e("Error Json Response: " + response);

        val errorResponse = ErrorResponse()
        val jsonObject: JSONObject

        try {
            jsonObject = JSONObject(response)
            val errorObj = jsonObject.getJSONObject("error")

            var error_code = ExceptionCodes.NO_ERROR_CODE
            if (errorObj.has("error_code")) {
                error_code = errorObj.getInt("error_code")
            }

            var error_message = StringBuilder(errorObj.getString("message")) // TODO this is too generic

            if (errorObj.has("errors")) {
                error_message = StringBuilder()
                val errors = errorObj.getJSONObject("errors")
                val keys = errors.keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val message = errors.getString(key)
                    error_message.append(StringUtils.convertCamelCase(key)).append(" ").append(message).append(" ")
                }
            }

            errorResponse.message = error_message.toString()
            errorResponse.code = error_code

        } catch (e: JSONException) {
            Timber.e(e.message)
            errorResponse.message = "Network error."
            errorResponse.code = ExceptionCodes.NO_ERROR_CODE
        }

        return errorResponse
    }

    fun create(throwable: Throwable): NetworkException {

        if (throwable is NetworkException) {
            return throwable
        } else if (throwable is ConnectException) {
            return NetworkException(mContext.getString(R.string.error_network_disconnected), ExceptionCodes.SERVICE_ERROR_CODE)
        } else if (throwable is SocketTimeoutException) {
            return NetworkException(mContext.getString(R.string.error_network_disconnected), ExceptionCodes.SOCKET_ERROR_CODE)
        } else if (throwable is DataRefreshException) {
            return NetworkException(mContext.getString(R.string.error_data_refresh), ExceptionCodes.NO_REFRESH_NEEDED)
        } else if (throwable is NetworkConnectionException) {
            return NetworkException(mContext.getString(R.string.error_network_disconnected), ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE)
        } else if (throwable is AuthenticationException) {
            return NetworkException(mContext.getString(R.string.error_authentication), ExceptionCodes.AUTHENTICATION_ERROR_CODE)
        } else if (throwable is UnknownHostException) {
            return NetworkException(mContext.getString(R.string.error_network_disconnected), ExceptionCodes.SERVICE_ERROR_CODE)
        }

        if (isHttp403Error(throwable) || isHttp400Error(throwable)) {
            try {
                val json = JSONObject((throwable as HttpException).response().errorBody().source().readUtf8()).getString("error")
                Timber.e("JSON: $json")
                val errorResponse = parseErrorMessage(json)
                Timber.e("Error: " + errorResponse.message!!)
                Timber.e("Code: " + errorResponse.code)
                return NetworkException(errorResponse.message, errorResponse.code)
            } catch (e: IOException) {
                Timber.e("JSON Parsing Error: " + e.message)
                val code = (throwable as HttpException).code()
                return NetworkException(throwable.message, code)
            } catch (e: JSONException) {
                Timber.e("JSON Parsing Error: " + e.message)
                val code = (throwable as HttpException).code()
                return NetworkException(throwable.message, code)
            }
        }

        if (isHttp401Error(throwable)) {
            return NetworkException(mContext.getString(R.string.error_generic_server_down), 401)
        }

        if (isHttp1007Error(throwable)) {
            return NetworkException(mContext.getString(R.string.error_network_http_error), 1007)
        }

        if (isHttp500Error(throwable)) {
            return NetworkException(mContext.getString(R.string.error_service_unreachable_error), 500)
        }

        if (isHttp404Error(throwable)) {
            return NetworkException(mContext.getString(R.string.error_service_error), 404)
        }

        if (isHttp400Error(throwable)) {
            return NetworkException(mContext.getString(R.string.error_service_error), 400)
        }

        if (isHttp409Error(throwable)) {
            return NetworkException(mContext.getString(R.string.error_service_error), 409)
        }

        return if (isHttp503Error(throwable)) {
            NetworkException(mContext.getString(R.string.error_service_error), 503)
        } else NetworkException(mContext.getString(R.string.error_unknown_error), 0)

    }

    private class ErrorResponse {
        var code: Int = 0
        var message: String? = null
    }

    companion object {

        fun isAuthenticationError(code: Int): Boolean {
            return code == ExceptionCodes.AUTHENTICATION_ERROR_CODE
        }

        fun isNetworkError(code: Int): Boolean {
            return code == ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE || code == ExceptionCodes.SERVICE_ERROR_CODE
        }

        fun isHttp503Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == 503
        }

        // authorization error
        private fun isHttp403Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == ExceptionCodes.AUTHENTICATION_ERROR_CODE
        }

        fun isHttp403Error(code: Int): Boolean {
            return code == ExceptionCodes.AUTHENTICATION_ERROR_CODE
        }

        // bad request
        fun isHttp400Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == ExceptionCodes.BAD_REQUEST_ERROR_CODE
        }

        fun isHttp409Error(code: Int): Boolean {
            return code == 409
        }

        fun isHttp405Error(code: Int): Boolean {
            return code == 405
        }

        fun isHttp503Error(code: Int): Boolean {
            return code == 503
        }

        fun isHttp404Error(code: Int): Boolean {
            return code == 404
        }

        fun isHttp400Error(code: Int): Boolean {
            return code == 400
        }

        // network error
        private fun isHttp401Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == 401
        }

        private fun isHttp1007Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == 1007
        }

        // server error
        fun isHttp500Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == 500
        }

        fun isHttp5003Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == 503
        }

        private fun isHttp404Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == ExceptionCodes.NETWORK_CONNECTION_ERROR_CODE
        }

        private fun isHttp409Error(throwable: Throwable): Boolean {
            return (throwable as HttpException).code() == 409
        }
    }
}