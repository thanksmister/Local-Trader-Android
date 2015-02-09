package com.thanksmister.bitcoin.localtrader.ui.traderequest;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface TradeRequestPresenter
{
    public void onDestroy();
    public void onResume();
    void sendTradeRequest(String ad_id, String amount, String message);
}
