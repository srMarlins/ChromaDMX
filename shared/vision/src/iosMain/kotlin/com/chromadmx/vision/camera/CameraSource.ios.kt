package com.chromadmx.vision.camera

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset352x288
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddressOfPlane
import platform.CoreVideo.CVPixelBufferGetBytesPerRowOfPlane
import platform.CoreVideo.CVPixelBufferGetHeightOfPlane
import platform.CoreVideo.CVPixelBufferGetWidthOfPlane
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

/**
 * iOS actual implementation of [PlatformCameraSource] using AVFoundation.
 *
 * Captures camera frames via [AVCaptureSession] with a [AVCaptureVideoDataOutput]
 * configured for YUV bi-planar format. The Y (luminance) plane is extracted from
 * the CVPixelBuffer and normalized to a [GrayscaleFrame] with pixel values in
 * [0.0, 1.0].
 *
 * Frame delivery is asynchronous via [AVCaptureVideoDataOutputSampleBufferDelegateProtocol].
 * The [captureFrame] function bridges this to Kotlin coroutines using
 * [suspendCancellableCoroutine].
 *
 * Session lifecycle:
 * - [startPreview] activates the camera sensor and begins frame delivery
 * - [stopPreview] stops the session and releases the sensor
 * - [captureFrame] auto-starts the session if not already running
 *
 * Uses the smallest available preset ([AVCaptureSessionPreset352x288]) to minimize
 * memory and CPU overhead for the vision analysis pipeline, which only needs
 * approximate blob positions rather than high-resolution imagery.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformCameraSource {

    /** The AVFoundation capture session managing the camera pipeline. */
    private val captureSession = AVCaptureSession()

    /**
     * Dedicated serial dispatch queue for sample buffer callbacks.
     *
     * AVFoundation requires that delegate callbacks run on a serial queue
     * to prevent concurrent access to the pixel buffer data. Using a
     * dedicated queue avoids blocking the main thread during frame processing.
     */
    private val sampleBufferQueue = dispatch_queue_create(
        "com.chromadmx.vision.cameraSource",
        null
    )

    /** Delegate handling frame delivery from AVFoundation. */
    private val sampleBufferDelegate = SampleBufferDelegate()

    /** Whether the capture session has been configured with input/output. */
    private var isConfigured = false

    /**
     * Continuation slot for the pending [captureFrame] call.
     *
     * Only one frame capture can be in flight at a time. When a new sample
     * buffer arrives and this continuation is non-null, the Y plane is
     * extracted and the continuation is resumed with the resulting
     * [GrayscaleFrame]. If null, incoming frames are silently dropped
     * (the session remains running for preview purposes).
     */
    private val frameContinuation = atomic<CancellableContinuation<GrayscaleFrame>?>(null)

    /**
     * Capture a single grayscale frame from the camera.
     *
     * If the capture session is not running, it is started automatically.
     * The function suspends until the next sample buffer arrives from
     * AVFoundation, then extracts the Y (luminance) plane and returns
     * a normalized [GrayscaleFrame].
     *
     * @return A [GrayscaleFrame] with pixel values in [0.0, 1.0]
     * @throws IllegalStateException if the camera cannot be configured
     */
    actual suspend fun captureFrame(): GrayscaleFrame {
        if (!captureSession.isRunning()) {
            startPreview()
        }

        return suspendCancellableCoroutine { cont ->
            frameContinuation.value = cont
            cont.invokeOnCancellation {
                frameContinuation.value = null
            }
        }
    }

    /**
     * Start the camera preview (activates the sensor).
     *
     * Configures the capture session on first call: selects the default
     * back-facing video camera, creates a device input, adds a video data
     * output configured for YUV bi-planar format, and sets the session
     * preset to 352x288 for minimal resource usage.
     *
     * Subsequent calls resume the session without reconfiguring.
     *
     * @throws IllegalStateException if no video capture device is available
     */
    actual fun startPreview() {
        if (captureSession.isRunning()) return

        if (!isConfigured) {
            configureCaptureSession()
        }

        captureSession.startRunning()
    }

    /**
     * Stop the camera preview (releases the sensor).
     *
     * Stops the capture session. Any pending [captureFrame] continuation
     * is not cancelled -- it will be resumed when the session is restarted
     * and the next frame arrives. Call this to conserve battery and release
     * the camera for other applications.
     */
    actual fun stopPreview() {
        if (captureSession.isRunning()) {
            captureSession.stopRunning()
            frameContinuation.getAndSet(null)?.cancel()
        }
    }

    // ------------------------------------------------------------------
    // Internal: session configuration
    // ------------------------------------------------------------------

    /**
     * Configure the AVCaptureSession with camera input and video data output.
     *
     * Selects the default back-facing video camera, creates a device input,
     * and adds a [AVCaptureVideoDataOutput] configured for
     * [kCVPixelFormatType_420YpCbCr8BiPlanarFullRange] (YUV 4:2:0 bi-planar).
     * Only the Y plane (luminance) is used for grayscale analysis.
     */
    private fun configureCaptureSession() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            ?: error("No video capture device available")

        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
            ?: error("Failed to create capture device input")

        captureSession.beginConfiguration()

        if (captureSession.canAddInput(input)) {
            captureSession.addInput(input)
        } else {
            captureSession.commitConfiguration()
            error("Cannot add camera input to capture session")
        }

        val output = AVCaptureVideoDataOutput().apply {
            // Request YUV bi-planar format for efficient luminance extraction.
            // kCVPixelBufferPixelFormatTypeKey is used as the dictionary key,
            // and the pixel format constant is auto-boxed to NSNumber.
            videoSettings = mapOf(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
            )

            // Drop late frames rather than queuing them -- we only need the latest
            alwaysDiscardsLateVideoFrames = true

            // Set the delegate on our dedicated serial queue
            setSampleBufferDelegate(sampleBufferDelegate, queue = sampleBufferQueue)
        }

        if (captureSession.canAddOutput(output)) {
            captureSession.addOutput(output)
        } else {
            captureSession.commitConfiguration()
            error("Cannot add video data output to capture session")
        }

        // Use smallest preset to minimize memory/CPU for vision analysis
        if (captureSession.canSetSessionPreset(AVCaptureSessionPreset352x288)) {
            captureSession.sessionPreset = AVCaptureSessionPreset352x288
        }

        captureSession.commitConfiguration()
        isConfigured = true
    }

    // ------------------------------------------------------------------
    // Internal: pixel buffer processing
    // ------------------------------------------------------------------

    /**
     * Extract the Y (luminance) plane from a CVPixelBuffer and normalize
     * to a [GrayscaleFrame] with pixel values in [0.0, 1.0].
     *
     * The pixel buffer is expected to be in YUV 4:2:0 bi-planar format
     * ([kCVPixelFormatType_420YpCbCr8BiPlanarFullRange]). Plane 0 contains
     * the full-resolution Y (luma) data with one byte per pixel (0-255).
     *
     * The buffer is locked for read access before extraction and unlocked
     * afterward. Row stride (bytes per row) may differ from width due to
     * memory alignment, so each row is read using the stride offset.
     *
     * @param sampleBuffer The CMSampleBuffer delivered by AVFoundation
     * @return A [GrayscaleFrame], or null if the pixel buffer is unavailable
     */
    private fun extractGrayscaleFrame(sampleBuffer: CMSampleBufferRef): GrayscaleFrame? {
        val pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return null

        // Lock the pixel buffer base address for CPU read access.
        // Flag 0x00000001 = kCVPixelBufferLock_ReadOnly
        CVPixelBufferLockBaseAddress(pixelBuffer, 1u)

        try {
            // Plane 0 = Y (luminance), full resolution
            val baseAddress = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 0u)
                ?: return null
            val width = CVPixelBufferGetWidthOfPlane(pixelBuffer, 0u).toInt()
            val height = CVPixelBufferGetHeightOfPlane(pixelBuffer, 0u).toInt()
            val bytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 0u).toInt()

            if (width <= 0 || height <= 0) return null

            // Reinterpret the raw pointer as a UByte pointer for byte-level access
            val bytePtr = baseAddress.reinterpret<UByteVar>()

            // Convert Y plane bytes (0-255) to normalized floats (0.0-1.0)
            val pixels = FloatArray(width * height)
            for (y in 0 until height) {
                val rowOffset = y * bytesPerRow
                for (x in 0 until width) {
                    val luminance = bytePtr[rowOffset + x].toInt()
                    pixels[y * width + x] = luminance / 255f
                }
            }

            return GrayscaleFrame(
                pixels = pixels,
                width = width,
                height = height
            )
        } finally {
            // Unlock with the same flags used for locking
            CVPixelBufferUnlockBaseAddress(pixelBuffer, 1u)
        }
    }

    // ------------------------------------------------------------------
    // AVCaptureVideoDataOutputSampleBufferDelegate
    // ------------------------------------------------------------------

    /**
     * Delegate receiving sample buffer callbacks from AVFoundation.
     *
     * When a frame continuation is pending (i.e., [captureFrame] has been
     * called and is waiting), the Y plane is extracted from the sample buffer
     * and the continuation is resumed with the resulting [GrayscaleFrame].
     *
     * If no continuation is pending, frames are silently dropped.
     *
     * **Must** extend [NSObject] for Kotlin/Native ObjC protocol conformance.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private inner class SampleBufferDelegate :
        NSObject(),
        AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

        /**
         * Called by AVFoundation each time a new video frame is available.
         *
         * This runs on [sampleBufferQueue] (a dedicated serial dispatch queue).
         * The sample buffer is only valid for the duration of this callback,
         * so pixel data must be fully extracted before returning.
         */
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputSampleBuffer: CMSampleBufferRef?,
            fromConnection: AVCaptureConnection
        ) {
            // Only process if someone is waiting for a frame
            val continuation = frameContinuation.getAndSet(null) ?: return
            val buffer = didOutputSampleBuffer ?: return

            val frame = extractGrayscaleFrame(buffer) ?: return

            continuation.resume(frame)
        }
    }
}
