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
