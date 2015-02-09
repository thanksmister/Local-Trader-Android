package com.thanksmister.bitcoin.localtrader.data.api.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Author: Michael Ritchie
 * Date: 1/7/15
 * Copyright 2013, ThanksMister LLC
 */
public class Actions implements Parcelable
{
    public String public_view;

    public Actions()
    {
    }

    // Parcelling part
    public Actions(Parcel parcel)
    {
        this();
        public_view = parcel.readString();
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(public_view);
    }

    public static Creator<Actions> CREATOR = new Creator<Actions>()
    {
        public Actions createFromParcel(Parcel parcel) {
            return new Actions(parcel);
        }

        public Actions[] newArray(int size) {
            return new Actions[size];
        }
    };
}
