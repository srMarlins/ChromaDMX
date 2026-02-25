package com.chromadmx.android.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * CameraX-based frame capture that extracts grayscale luminance data from the
 * camera feed. The Y plane of YUV_420_888 images is used directly as the
 * grayscale source, avoiding expensive color-space conversions.
 *
 * Usage:
 * 1. Call [start] with a [LifecycleOwner] and a callback to receive frames.
 * 2. Each frame is delivered as a [FloatArray] of normalized luminance values
 *    (0.0 = black, 1.0 = white) in row-major order.
 * 3. Call [stop] to release camera resources.
 *
 * This infrastructure is the Android-side implementation that feeds into the
 * shared VisionProcessor module for fixture-mapping via phone-flash detection.
 */
class CameraFrameCapture(private val context: Context) {

    companion object {
        private const val TAG = "CameraFrameCapture"
        private const val ANALYSIS_WIDTH = 320
        private const val ANALYSIS_HEIGHT = 240
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Bind CameraX to the given lifecycle and start delivering grayscale frames.
     *
     * @param lifecycleOwner Activity or Fragment lifecycle to bind the camera to.
     * @param onFrame Callback receiving (width, height, grayscale FloatArray) for each frame.
     */
    fun start(
        lifecycleOwner: LifecycleOwner,
        onFrame: (width: Int, height: Int, luminance: FloatArray) -> Unit,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    processFrame(imageProxy, onFrame)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalysis,
                    )
                    Log.d(TAG, "CameraX bound successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "CameraX binding failed", e)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /**
     * Release camera resources.
     */
    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    /**
     * Extract the Y (luminance) plane from a YUV_420_888 image and convert to
     * a normalized FloatArray. The Y plane is a direct grayscale representation
     * so no color conversion is needed.
     */
    private fun processFrame(
        imageProxy: ImageProxy,
        onFrame: (Int, Int, FloatArray) -> Unit,
    ) {
        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer: ByteBuffer = yPlane.buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = yPlane.rowStride

            val luminance = FloatArray(width * height)
            for (row in 0 until height) {
                for (col in 0 until width) {
                    val yValue = yBuffer.get(row * rowStride + col).toInt() and 0xFF
                    luminance[row * width + col] = yValue / 255f
                }
            }

            onFrame(width, height, luminance)
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        } finally {
            imageProxy.close()
        }
    }
}
