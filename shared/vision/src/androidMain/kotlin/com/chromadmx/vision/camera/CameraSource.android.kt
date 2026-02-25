package com.chromadmx.vision.camera

/**
 * Android actual implementation of [PlatformCameraSource].
 *
 * TODO: Implement using CameraX ImageAnalysis with YUV→grayscale conversion.
 */
actual class PlatformCameraSource {

    actual suspend fun captureFrame(): GrayscaleFrame {
        TODO("Android CameraX integration not yet implemented")
    }

    actual fun startPreview() {
        TODO("Android CameraX preview not yet implemented")
    }

    actual fun stopPreview() {
        TODO("Android CameraX preview stop not yet implemented")
    }
}
