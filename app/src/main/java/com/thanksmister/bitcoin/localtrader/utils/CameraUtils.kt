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