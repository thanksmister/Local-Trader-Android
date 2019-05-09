/*
 * Copyright (c) 2018 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.modules

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Camera
import com.google.android.gms.vision.*
import com.google.android.gms.vision.CameraSource.CAMERA_FACING_BACK
import com.google.android.gms.vision.CameraSource.CAMERA_FACING_FRONT
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.thanksmister.bitcoin.localtrader.ui.views.CameraSourcePreview
import com.thanksmister.bitcoin.localtrader.utils.CameraUtils
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class CameraReader @Inject
constructor(private val context: Context) {

    private var cameraCallback: CameraCallback? = null
    private var barcodeDetector: BarcodeDetector? = null
    private var multiDetector: MultiDetector? = null
    private var cameraSource: CameraSource? = null
    private var barCodeDetectorProcessor: MultiProcessor<Barcode>? = null
    private var cameraOrientation: Int = 0
    private var cameraPreview: CameraSourcePreview? = null
    private var cameraId: Int = 0;

    interface CameraReaderListener {
        fun onCameraReaderInitialized()
        fun onCameraReaderError()
    }

    fun stopCamera() {
        if (cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
        }
        if (barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
        }
        if (multiDetector != null) {
            multiDetector!!.release()
            multiDetector = null
        }
        if (barCodeDetectorProcessor != null) {
            barCodeDetectorProcessor!!.release()
            barCodeDetectorProcessor = null
        }

        this.cameraCallback = null
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun startCameraPreview(callback: CameraCallback, preview: CameraSourcePreview?) {
        Timber.d("startCameraPreview")
        if (preview != null) {
            this.cameraCallback = callback
            this.cameraPreview = preview
            cameraId = CameraSource.CAMERA_FACING_BACK
            if(cameraId >= 0) {
                buildDetectors()
                if (multiDetector != null) {
                    cameraSource = initCamera(cameraId)
                    cameraPreview?.start(cameraSource, object : CameraSourcePreview.OnCameraPreviewListener {
                        override fun onCameraError() {
                            Timber.e("Camera Preview Error")
                            cameraSource = if (cameraId == CAMERA_FACING_FRONT) {
                                initCamera(CAMERA_FACING_BACK)
                            } else {
                                initCamera(CAMERA_FACING_FRONT)
                            }
                            if (cameraPreview != null) {
                                try {
                                    cameraPreview?.start(cameraSource, object : CameraSourcePreview.OnCameraPreviewListener {
                                        override fun onCameraError() {
                                            Timber.e("Camera Preview Error")
                                            cameraCallback?.onCameraError()
                                        }
                                    })
                                } catch (e: Exception) {
                                    Timber.e(e.message)
                                    cameraPreview?.stop()
                                    cameraSource?.stop()
                                    cameraCallback?.onCameraError()
                                }
                            }
                        }
                    })
                }
            } else {
                cameraCallback?.onCameraError()
            }
        }
    }

    private fun buildDetectors() {
        val info = Camera.CameraInfo()
        try{
            Camera.getCameraInfo(cameraId, info)
        } catch (e: RuntimeException) {
            Timber.e(e.message)
            cameraCallback!!.onCameraError()
            return
        }
        cameraOrientation = info.orientation
        val multiDetectorBuilder = MultiDetector.Builder()
        var detectorAdded = false
        barcodeDetector = BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

        barCodeDetectorProcessor = MultiProcessor.Builder<Barcode>(MultiProcessor.Factory<Barcode> {
            object : Tracker<Barcode>() {
                override fun onUpdate(p0: Detector.Detections<Barcode>?, p1: Barcode?) {
                    super.onUpdate(p0, p1)
                    if (cameraCallback != null) {
                        Timber.d("Barcode: " + p1?.displayValue)
                        cameraCallback!!.onQRCode(p1?.displayValue)
                    }
                }
            }
        }).build()

        barcodeDetector?.setProcessor(barCodeDetectorProcessor);
        multiDetectorBuilder.add(barcodeDetector)
        detectorAdded = true

        if(detectorAdded) {
            multiDetector = multiDetectorBuilder.build()
            multiDetector?.let {
                if(it.isOperational) {
                    cameraCallback?.onDetectorError()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initCamera(camerId: Int): CameraSource {
        Timber.d("initCamera camerId $camerId")
        return CameraSource.Builder(context, multiDetector)
                .setAutoFocusEnabled(true)
                .setRequestedFps(9.0f)
                .setRequestedPreviewSize(640, 480)
                .setFacing(camerId)
                .build()
    }

    companion object {
    }
}