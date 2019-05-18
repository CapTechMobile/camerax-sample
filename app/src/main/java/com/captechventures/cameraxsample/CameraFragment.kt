package com.captechventures.cameraxsample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.util.concurrent.TimeUnit

const val TAG = "CameraXSample"

class CameraFragment : Fragment() {

    private var imageCapture: ImageCapture? = null

    private val cameraPermissionGranted
        get() = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private var phrase = ""


    // listener for after an image is captured
    private val imageCaptureListener = object : ImageCapture.OnImageSavedListener {
        override fun onError(error: ImageCapture.UseCaseError, message: String, exc: Throwable?) {
            Log.e(TAG, "Photo capture failed: $message", exc)
        }

        override fun onImageSaved(photoFile: File) {
            Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
            requireFragmentManager().transaction {
                replace(R.id.fragmentContainer, PhotoFragment.newInstance(photoFile.absolutePath))
                addToBackStack(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phrase = requireArguments().getString(PHRASE_ARG, "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (cameraPermissionGranted) {
            surfacePreview.post { startCamera() }
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
        }

        Toast.makeText(requireContext(), "Point camera at text containing the phrase: $phrase", Toast.LENGTH_LONG)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (cameraPermissionGranted) {
                // permission granted start camera
                surfacePreview.post { startCamera() }
            } else {
                Toast.makeText(requireContext(), "Permission denied, closing.", Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CameraX.unbindAll()
    }

    private fun startCamera() {
        // unbind anything that still might be open
        CameraX.unbindAll()

        val metrics = DisplayMetrics().also { surfacePreview.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(CameraX.LensFacing.BACK)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetResolution(screenSize)
            .build()

        // Build the viewfinder use case
        val preview = AutoFitPreviewBuilder.build(previewConfig, surfacePreview)

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            val analyzerThread = HandlerThread("OCR").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            setTargetResolution(Size(1280, 720))
        }.build()

        val captureConfig = ImageCaptureConfig.Builder()
            .setLensFacing(CameraX.LensFacing.BACK)
            .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            .setTargetRotation(surfacePreview.display.rotation)
            .setTargetAspectRatio(screenAspectRatio)
            .build()

        imageCapture = ImageCapture(captureConfig)


        val imageAnalysis = ImageAnalysis(analyzerConfig)
        imageAnalysis.analyzer = TextAnalyzer(phrase) {
            val outputDirectory: File = requireContext().filesDir
            val photoFile = File(outputDirectory, "${System.currentTimeMillis()}.jpg")
            imageCapture?.takePicture(photoFile, imageCaptureListener, ImageCapture.Metadata())

        }

        CameraX.bindToLifecycle(this, preview, imageAnalysis, imageCapture)
    }


    companion object {

        private const val PERMISSION_CODE = 15
        private const val PHRASE_ARG = "phrase_arg"

        @JvmStatic
        fun newInstance(phrase: String): CameraFragment {
            return CameraFragment().apply {
                arguments = Bundle().apply { putString(PHRASE_ARG, phrase) }
            }
        }

    }
}

class TextAnalyzer(
    private val identifier: String,
    private val identifierDetectedCallback: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(0, FirebaseVisionImageMetadata.ROTATION_0)
            ORIENTATIONS.append(90, FirebaseVisionImageMetadata.ROTATION_90)
            ORIENTATIONS.append(180, FirebaseVisionImageMetadata.ROTATION_180)
            ORIENTATIONS.append(270, FirebaseVisionImageMetadata.ROTATION_270)
        }
    }

    private var lastAnalyzedTimestamp = 0L


    private fun getOrientationFromRotation(rotationDegrees: Int): Int {
        return when (rotationDegrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> FirebaseVisionImageMetadata.ROTATION_90
        }
    }

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (image?.image == null || image.image == null) return

        val timestamp = System.currentTimeMillis()
        // only run once per second
        if (timestamp - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
            val visionImage = FirebaseVisionImage.fromMediaImage(
                image.image!!,
                getOrientationFromRotation(rotationDegrees)
            )

            val detector = FirebaseVision.getInstance()
                .onDeviceTextRecognizer

            detector.processImage(visionImage)
                .addOnSuccessListener { result: FirebaseVisionText ->
                    // remove the new lines and join to a single string,
                    // then search for our identifier
                    val textToSearch = result.text.split("\n").joinToString(" ")
                    if (textToSearch.contains(identifier, true)) {
                        identifierDetectedCallback()
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error processing image", it)
                }
            lastAnalyzedTimestamp = timestamp
        }
    }

}