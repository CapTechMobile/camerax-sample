package com.captechventures.cameraxsample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Rational
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private val cameraPermissionGranted
        get() = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_main)

        if (cameraPermissionGranted) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (cameraPermissionGranted) {
                // permission granted start camera
                surfacePreview.post { startCamera() }
            } else {
                Toast.makeText(this, "Permission denied, closing.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val config = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(1, 1))
            setTargetResolution(Size(640, 640))
        }.build()

        // Build the viewfinder use case
        val previewConfig = Preview(config)

        // Every time the viewfinder is updated, recompute layout
        previewConfig.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = surfacePreview.parent as ViewGroup
            parent.removeView(surfacePreview)
            parent.addView(surfacePreview, 0)

            surfacePreview.surfaceTexture = it.surfaceTexture
//            updateTransform()
        }

        CameraX.bindToLifecycle(this, previewConfig)
    }

    private fun stopCamera() {

    }

    companion object {
        const val PERMISSION_CODE = 15
    }

}
