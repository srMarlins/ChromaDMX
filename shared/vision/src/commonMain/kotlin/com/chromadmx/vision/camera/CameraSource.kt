package com.chromadmx.vision.camera

/**
 * Platform-specific camera source that provides grayscale frames.
 *
 * Each platform implements this via CameraX (Android) or AVCaptureSession (iOS).
 * The vision pipeline only consumes [GrayscaleFrame] instances, keeping all
 * detection algorithms pure and platform-independent.
 */
expect class PlatformCameraSource {
    /** Capture a single grayscale frame from the camera. */
    suspend fun captureFrame(): GrayscaleFrame

    /** Start the camera preview (activates the sensor). */
    fun startPreview()

    /** Stop the camera preview (releases the sensor). */
    fun stopPreview()
}
