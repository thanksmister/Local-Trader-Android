/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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