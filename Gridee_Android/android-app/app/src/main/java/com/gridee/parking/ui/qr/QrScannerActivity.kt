package com.gridee.parking.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gridee.parking.R
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var lastScanTimestamp: Long = 0L

    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_SCAN_TYPE = "scan_type" // CHECK_IN or CHECK_OUT
        const val EXTRA_QR_CODE = "qr_code"
        const val RESULT_QR_SCANNED = 100
        private const val CAMERA_PERMISSION_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.barcode_scanner)

        if (checkCameraPermission()) {
            startScanning()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    val now = System.currentTimeMillis()
                    if (now - lastScanTimestamp < 1500L) return
                    lastScanTimestamp = now
                    handleQrCodeScanned(it.text)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    private fun handleQrCodeScanned(qrCode: String) {
        barcodeView.pause()
        val resultIntent = Intent().apply {
            putExtra(EXTRA_QR_CODE, qrCode)
        }
        setResult(RESULT_QR_SCANNED, resultIntent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) barcodeView.pause()
    }
}
