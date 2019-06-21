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

/**
 * Author: Michael Ritchie
 * Updated: 3/25/16
 */
object ExceptionCodes {
    val CODE_MINUS_ONE = -1 // authorization failed
    val CODE_THREE = 3 // authorization failed
    val INVALID_GRANT = 5 // authorization failed
    val INSUFFICIENT_BALANCE = 19 // authorization failed
    val NO_ERROR_CODE = -1
    val NO_REFRESH_NEEDED = 1487
    val BAD_REQUEST_ERROR_CODE = 400
    val NETWORK_CONNECTION_ERROR_CODE = 404
    val SOCKET_ERROR_CODE = 405
    val SERVICE_ERROR_CODE = 503
    val AUTHENTICATION_ERROR_CODE = 403 // authorization failed
    val MESSAGE_ERROR_CODE = 38 // can't send message to contact
}
