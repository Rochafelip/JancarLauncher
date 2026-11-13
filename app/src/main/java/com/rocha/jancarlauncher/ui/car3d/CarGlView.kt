package com.rocha.jancarlauncher.ui.car3d

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * Superfície do carro 3D: arrastar gira 360°, pinça aproxima/afasta, dois toques centraliza.
 * Desenha só quando pedido (RENDERMODE_WHEN_DIRTY); o [CarRenderer] pede mais quadros só enquanto algo se mexe.
 */
@SuppressLint("ViewConstructor")
class CarGlView(context: Context, scene: GlbScene) : GLSurfaceView(context) {

    private val renderer = CarRenderer(scene, ::requestRender)

    private val scale = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val f = detector.scaleFactor
            queueEvent { renderer.zoomBy(f) }
            requestRender()
            return true
        }
    })

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (scale.isInProgress || e2.pointerCount > 1) return false
            queueEvent { renderer.rotateBy(dx, dy) }
            requestRender()
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            queueEvent { renderer.resetView() }
            requestRender()
            return true
        }
    })

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(MultisampleChooser())
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun update(state: CarRenderState) {
        queueEvent { renderer.setState(state) }
        requestRender()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        scale.onTouchEvent(event)
        gestures.onTouchEvent(event)
        return true
    }

    /** 4× MSAA quando a GPU oferece (na Mali-450 sai quase de graça); senão, sem. */
    private class MultisampleChooser : EGLConfigChooser {
        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val withMsaa = intArrayOf(
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, 4, EGL10.EGL_NONE,
            )
            val plain = intArrayOf(
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE,
            )
            return first(egl, display, withMsaa) ?: first(egl, display, plain)
                ?: throw IllegalStateException("sem configuração EGL para OpenGL ES 2.0")
        }

        private fun first(egl: EGL10, display: EGLDisplay, attrs: IntArray): EGLConfig? {
            val count = IntArray(1)
            val configs = arrayOfNulls<EGLConfig>(1)
            return if (egl.eglChooseConfig(display, attrs, configs, 1, count) && count[0] > 0) configs[0] else null
        }

        private companion object {
            const val EGL_RENDERABLE_TYPE = 0x3040
            const val EGL_OPENGL_ES2_BIT = 4
        }
    }
}
