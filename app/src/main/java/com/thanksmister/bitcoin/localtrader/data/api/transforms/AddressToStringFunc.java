/*
 * Copyright 2007 ZXing authors
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

package com.thanksmister.bitcoin.localtrader.data.api.transforms;

import android.location.Address;

import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import rx.functions.Func1;
import timber.log.Timber;

public class AddressToStringFunc implements Func1<Address, String> {
    @Override
    public String call(Address address) {
        if (address == null) return "";

            
        String addressLines = "";
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            addressLines += address.getAddressLine(i) + '\n';
        }
        return addressLines;
    }
}