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
 *
 */

package com.thanksmister.bitcoin.localtrader.network.api.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class WalletData implements Parcelable {
    private String address;
    private String balance;
    private String rate;
    private Bitmap image;

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getRate() {
        return rate;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public WalletData() {
    }

    public WalletData(Parcel parcel) {
        this();
        address = parcel.readString();
        balance = parcel.readString();
        rate = parcel.readString();
        image = parcel.readParcelable(Bitmap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeString(balance);
        parcel.writeString(rate);
        parcel.writeValue(image);
    }

    public static Creator<WalletData> CREATOR = new Creator<WalletData>() {
        public WalletData createFromParcel(Parcel parcel) {
            return new WalletData(parcel);
        }

        public WalletData[] newArray(int size) {
            return new WalletData[size];
        }
    };
}
