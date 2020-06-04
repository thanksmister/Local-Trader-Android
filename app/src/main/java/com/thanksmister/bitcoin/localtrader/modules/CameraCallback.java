/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.modules;

public interface CameraCallback {
    void onQRCode(String data);
    void onCameraError();
    void onDetectorError();
}
