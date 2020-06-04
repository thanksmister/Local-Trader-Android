/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.modules.CameraCallback
import com.thanksmister.bitcoin.localtrader.modules.CameraReader
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.views.CameraSourcePreview
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import javax.inject.Inject

class ScanQrCodeActivity : BaseActivity() {

    @Inject lateinit var cameraReader: CameraReader

    private var codeRead: Boolean = false

    private val cameraPreview: CameraSourcePreview by lazy {
        findViewById<CameraSourcePreview>(R.id.cameraPreview)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qrcode)
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            startCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraReader.stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraReader.stopCamera()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PERMISSIONS_REQUEST_CAMERA) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCameraSource()
            return
        }
        Toast.makeText(this, getString(R.string.toast_camera_denied), Toast.LENGTH_LONG).show()
    }

    private fun requestCameraPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CAMERA)
            return
        }
        ActivityCompat.requestPermissions(this@ScanQrCodeActivity, permissions, PERMISSIONS_REQUEST_CAMERA)
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        } else {
            cameraReader.startCameraPreview(cameraCallback, cameraPreview)
        }
    }

    private val cameraCallback = object : CameraCallback {
        override fun onDetectorError() {
            dialogUtils.toast(getString(R.string.error_missing_vision_library))
        }
        override fun onCameraError() {
            dialogUtils.toast(getString(R.string.error_camera_start))
        }
        override fun onQRCode(data: String) {
            runOnUiThread {
                startSendActivity(data)
            }
        }
    }

    private fun startSendActivity(data: String) {
        if(!codeRead) {
            codeRead = true;
            val bitcoinAddress = WalletUtils.parseBitcoinAddress(data)
            val bitcoinAmount = WalletUtils.parseBitcoinAmount(data)
            if(WalletUtils.validBitcoinAddress(bitcoinAddress)) {
                intent.putExtra(PinCodeActivity.EXTRA_ADDRESS, bitcoinAddress)
                intent.putExtra(PinCodeActivity.EXTRA_AMOUNT, bitcoinAmount)
                setResult(SCAN_SUCCESS, intent)
                finish()
            } else {
                Toast.makeText(this@ScanQrCodeActivity, getString(R.string.toast_invalid_bitcoin_address), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val RC_HANDLE_GMS = 9001
        const val PERMISSIONS_REQUEST_CAMERA = 201
        const val SCAN_INTENT = 7978
        const val SCAN_SUCCESS = 53
        fun createStartIntent(context: Context): Intent {
            return Intent(context, ScanQrCodeActivity::class.java)
        }
    }
}