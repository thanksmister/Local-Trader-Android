/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */
package com.thanksmister.bitcoin.localtrader.network.api.model;

import java.util.List;

public class AdvertiserData
{
    // advertiser list
    public List<Method> methods;
    public List<Advertisement> advertisements;
    
    // single advertiser
    public Advertisement advertisement;
}
