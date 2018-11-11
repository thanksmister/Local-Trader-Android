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

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.modules.CameraCallback
import com.thanksmister.bitcoin.localtrader.modules.CameraReader
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.modules.BarcodeGraphic
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils
import kotlinx.android.synthetic.main.activity_scan_qrcode.*

import javax.inject.Inject
import com.thanksmister.bitcoin.localtrader.ui.views.GraphicOverlay



class ScanQrCodeActivity : BaseActivity() {

    @Inject lateinit var cameraReader: CameraReader

    private var codeRead: Boolean = false

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
            Toast.makeText(this@ScanQrCodeActivity, "Missing libraries to scan code...", Toast.LENGTH_LONG).show()
        }
        override fun onCameraError() {
            Toast.makeText(this@ScanQrCodeActivity, "Could not start camera...", Toast.LENGTH_LONG).show()
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
                startActivity(SendActivity.createStartIntent(this@ScanQrCodeActivity, bitcoinAddress, bitcoinAmount))
            } else {
                Toast.makeText(this@ScanQrCodeActivity, "Invalid Bitcoin address...", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val RC_HANDLE_GMS = 9001
        const val PERMISSIONS_REQUEST_CAMERA = 201
        fun createStartIntent(context: Context): Intent {
            return Intent(context, ScanQrCodeActivity::class.java)
        }
    }
}