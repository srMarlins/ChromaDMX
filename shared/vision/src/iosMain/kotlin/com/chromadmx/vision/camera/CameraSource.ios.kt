package com.chromadmx.vision.camera

/**
 * iOS actual implementation of [PlatformCameraSource].
 *
 * TODO: Implement using AVCaptureSession with grayscale pixel buffer extraction.
 */
actual class PlatformCameraSource {

    actual suspend fun captureFrame(): GrayscaleFrame {
        TODO("iOS AVCaptureSession integration not yet implemented")
    }

    actual fun startPreview() {
        TODO("iOS AVCaptureSession preview not yet implemented")
    }

    actual fun stopPreview() {
        TODO("iOS AVCaptureSession preview stop not yet implemented")
    }
}
