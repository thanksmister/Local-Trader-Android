package com.thanksmister.bitcoin.localtrader.ui.wallet;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface WalletPresenter
{
    public void onResume();
    
    public void onDestroy();

    void scanQrCode();

    void setAddressOnClipboard();

    void viewBlockChain();

    void shareAddress();

    void newWalletAddress();
}
