/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.utils

import android.hardware.Camera

class CameraUtils {
    companion object {
        @Throws(RuntimeException::class)
        fun getCamera(): Int {
            var cameraId = -1;
            for (i in 0 until Camera.getNumberOfCameras()) {
                val c = Camera.open(i)
                val info = Camera.CameraInfo()
                Camera.getCameraInfo(i, info)
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                }
                c.stopPreview()
                c.release()
            }
            return cameraId
        }
    }
}