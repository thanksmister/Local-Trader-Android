package com.thanksmister.bitcoin.localtrader.data.api.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Author: Michael Ritchie
 * Date: 1/7/15
 * Copyright 2013, ThanksMister LLC
 */
public class Profile implements Parcelable
{
    public String name;
    public String trade_count;
    public String feedback_score;
    public String username;
    public String last_online;

    public Profile()
    {
    }

    // Parcelling part
    public Profile(Parcel parcel)
    {
        this();
        name = parcel.readString();
        trade_count = parcel.readString();
        feedback_score = parcel.readString();
        username = parcel.readString();
        last_online = parcel.readString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(name);
        parcel.writeString(trade_count);
        parcel.writeString(feedback_score);
        parcel.writeString(username);
        parcel.writeString(last_online);
    }

    public static Creator<Profile> CREATOR = new Creator<Profile>()
    {
        public Profile createFromParcel(Parcel parcel) {
            return new Profile(parcel);
        }

        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };
}
