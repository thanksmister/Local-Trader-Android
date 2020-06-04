/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class CameraReader @Inject
constructor(private val context: Context) {

    private var cameraCallback: CameraCallback? = null
    private var barcodeDetector: BarcodeDetector? = null

    private var cameraSource: CameraSource? = null
    private var barCodeDetectorProcessor: MultiProcessor<Barcode>? = null
    private var cameraOrientation: Int = 0
    private var cameraPreview: CameraSourcePreview? = null
    private var cameraId: Int = 0;

    fun stopCamera() {
        if (cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
        }
        if (barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
        }

        if (barCodeDetectorProcessor != null) {
            barCodeDetectorProcessor!!.release()
            barCodeDetectorProcessor = null
        }

        this.cameraCallback = null
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun startCameraPreview(callback: CameraCallback, preview: CameraSourcePreview) {
        Timber.d("startCameraPreview")
        this.cameraCallback = callback
        this.cameraPreview = preview
        cameraId = CAMERA_FACING_BACK
        if(cameraId >= 0) {
            barcodeDetector = createDetector()
            barcodeDetector?.let {
                cameraSource = initCamera(cameraId, it)
                cameraPreview?.start(cameraSource, object : CameraSourcePreview.OnCameraPreviewListener {
                    override fun onCameraError() {
                        Timber.e("Camera Preview Error")
                        cameraSource = if (cameraId == CAMERA_FACING_FRONT) {
                            initCamera(CAMERA_FACING_BACK, it)
                        } else {
                            initCamera(CAMERA_FACING_FRONT, it)
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
                                cameraCallback?.onCameraError()
                                stopCamera()
                            } finally {
                                cameraCallback?.onCameraError()
                                stopCamera()
                            }
                        }
                    }
                })
            }?:cameraCallback?.onCameraError()
        }
    }

    private fun createDetector() : BarcodeDetector? {
        val info = Camera.CameraInfo()
        try{
            Camera.getCameraInfo(cameraId, info)
        } catch (e: RuntimeException) {
            Timber.e(e.message)
            cameraCallback!!.onCameraError()
            return null
        }
        cameraOrientation = info.orientation
        val barcodeDetector = BarcodeDetector.Builder(context)
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

        barcodeDetector.setProcessor(barCodeDetectorProcessor)
        return if(barcodeDetector.isOperational) {
            barcodeDetector
        } else {
            cameraCallback?.onDetectorError()
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun initCamera(cameraId: Int, barcodeDetector: BarcodeDetector): CameraSource {
        Timber.d("initCamera cameraId $cameraId")
        return CameraSource.Builder(context, barcodeDetector)
                .setAutoFocusEnabled(true)
                .setRequestedFps(9.0f)
                .setRequestedPreviewSize(640, 480)
                .setFacing(cameraId)
                .build()
    }
}