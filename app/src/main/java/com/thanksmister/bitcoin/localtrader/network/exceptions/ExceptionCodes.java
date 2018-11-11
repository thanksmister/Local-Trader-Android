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

package com.thanksmister.bitcoin.localtrader.network.exceptions;

/**
 * Author: Michael Ritchie
 * Updated: 3/25/16
 */
public class ExceptionCodes
{
    public static final int CODE_MINUS_ONE = -1; // authorization failed
    public static final int CODE_THREE = 3; // authorization failed
    public static final int INVALID_GRANT = 5; // authorization failed
    public static final int NO_ERROR_CODE = -1;
    public static final int NO_REFRESH_NEEDED = 1487;
    public static final int BAD_REQUEST_ERROR_CODE = 400;
    public static final int NETWORK_CONNECTION_ERROR_CODE = 404;
    public static final int SERVICE_ERROR_CODE = 503;
    public static final int AUTHENTICATION_ERROR_CODE = 403; // authorization failed
}
