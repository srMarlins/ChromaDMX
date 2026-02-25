package com.chromadmx.android.rendering

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 3.0 surface for rendering a 3D venue visualization.
 *
 * This scaffold clears to black and will later render:
 * - Fixture positions as colored dots reflecting live DMX output
 * - A simple wireframe or bounding box representing the venue
 * - Camera-mapped fixture positions from the VisionProcessor
 *
 * Usage in Compose: wrap in an AndroidView composable.
 */
class VenueGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    val renderer: VenueRenderer

    init {
        // Request OpenGL ES 3.0 context
        setEGLContextClientVersion(3)

        renderer = VenueRenderer()
        setRenderer(renderer)

        // Only render when explicitly requested (saves battery during idle)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}

/**
 * Minimal renderer that clears to black. Serves as the scaffold for the
 * venue visualization pipeline.
 *
 * Future additions:
 * - Fixture point-sprite shader (position + color per fixture)
 * - Camera feed background texture
 * - Venue bounding geometry
 */
class VenueRenderer : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "VenueRenderer"
    }

    /** Current viewport dimensions. */
    var viewportWidth: Int = 0
        private set
    var viewportHeight: Int = 0
        private set

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "OpenGL ES 3.0 surface created")
        // Dark background — venue viz will render on top
        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        Log.d(TAG, "Viewport: ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Future: draw fixture dots, venue wireframe, etc.
    }
}
