package com.chromadmx.platform

/**
 * iOS camera frame source stub for spatial fixture mapping.
 *
 * When implemented, this will use AVFoundation via Kotlin/Native cinterop
 * to capture camera frames for the vision/spatial mapping pipeline.
 *
 * The fixture mapping process requires:
 * 1. Live camera preview (displayed via native SwiftUI/UIKit view)
 * 2. Individual frame capture (grayscale FloatArray for blob detection)
 * 3. Ambient baseline capture (for brightness subtraction)
 *
 * The camera preview itself will be a native SwiftUI view embedded in Compose
 * via UIKitView interop, while the frame data flows back to shared Kotlin
 * code for processing.
 *
 * Key AVFoundation APIs to wrap:
 * - AVCaptureSession for camera session management
 * - AVCaptureVideoDataOutput for frame data access
 * - AVCaptureDevice for camera selection and configuration
 * - CVPixelBuffer for raw frame data extraction
 *
 * Frame data flow:
 * ```
 * AVCaptureVideoDataOutput
 *   -> CVPixelBuffer (native)
 *   -> grayscale FloatArray (converted in Kotlin/Native)
 *   -> shared/vision blob detection (pure Kotlin math)
 * ```
 */
class IosCameraSource {
    // TODO: Implement when shared/vision module defines the expect CameraSource interface
    //
    // Expected interface:
    //   actual class IosCameraSource : CameraSource {
    //       override suspend fun startPreview()
    //       override suspend fun captureFrame(): GrayscaleFrame
    //       override suspend fun stopPreview()
    //       override val isAvailable: Boolean
    //   }
}
