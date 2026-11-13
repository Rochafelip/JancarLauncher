package com.rocha.jancarlauncher.ui.car3d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView

/**
 * Superfície do carro 3D: arrastar gira 360°, pinça aproxima/afasta, dois toques centraliza.
 *
 * É uma `TextureView`, não `GLSurfaceView`: na central (Mali-450, testado em 2026-11-13) a `GLSurfaceView`
 * deixava **preto tudo fora do 3D** — fundo, botão Voltar e textos sumiam, mesmo com o fundo pintado pelo
 * Compose. A `TextureView` entra na composição normal da janela. O GL roda numa thread própria ([GlThread])
 * e desenha só quando pedido, no ritmo da tela (`Choreographer`); o [CarRenderer] pede mais quadros só
 * enquanto algo se mexe.
 */
@SuppressLint("ViewConstructor")
class CarGlView(context: Context, scene: GlbScene) : TextureView(context), TextureView.SurfaceTextureListener {

    private val renderer = CarRenderer(scene, ::requestRender)

    /** Existe enquanto há superfície; toda chamada ao [renderer] vai pra ela (ou roda aqui, se não há). */
    private var gl: GlThread? = null
    private var paused = false

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
        surfaceTextureListener = this
        isOpaque = true // o renderer limpa com a cor de fundo do tema
    }

    fun update(state: CarRenderState) {
        queueEvent { renderer.setState(state) }
        requestRender()
    }

    fun onPause() {
        paused = true
        gl?.setPaused(true)
    }

    fun onResume() {
        paused = false
        gl?.setPaused(false)
    }

    private fun requestRender() {
        gl?.requestRender()
    }

    /**
     * Sem thread de GL (superfície ainda não existe ou já foi embora) ninguém mais mexe no renderer, então
     * roda aqui mesmo; a thread nasce e morre na thread da tela, então a ordem das chamadas se mantém.
     */
    private fun queueEvent(event: () -> Unit) {
        val t = gl
        if (t != null) t.post(event) else event()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        gl = GlThread(renderer, surface, width, height, paused).also { it.begin() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        gl?.resize(width, height)
    }

    /** Devolve `false`: quem solta a textura é a thread do GL, depois de destruir a superfície EGL dela. */
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        gl?.finish()
        gl = null
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        scale.onTouchEvent(event)
        gestures.onTouchEvent(event)
        return true
    }

    /** Thread do GL: contexto EGL próprio sobre a textura; tudo do [renderer] roda aqui. */
    private class GlThread(
        private val renderer: GLSurfaceView.Renderer,
        private val texture: SurfaceTexture,
        private val width: Int,
        private val height: Int,
        private var paused: Boolean,
    ) : HandlerThread("CarGl") {
        private lateinit var handler: Handler
        private var egl: Egl? = null
        private var frameScheduled = false
        private var finished = false
        private val frame = Choreographer.FrameCallback {
            frameScheduled = false
            draw()
        }

        fun begin() {
            start()
            handler = Handler(looper)
            handler.post(::setup)
        }

        fun post(event: () -> Unit) {
            handler.post(event)
        }

        fun requestRender() {
            handler.post(::schedule)
        }

        fun resize(w: Int, h: Int) = handler.post {
            if (egl != null) renderer.onSurfaceChanged(null, w, h)
            schedule()
        }

        fun setPaused(p: Boolean) = handler.post {
            paused = p
            if (!p) schedule()
        }

        fun finish() = handler.post {
            finished = true
            Choreographer.getInstance().removeFrameCallback(frame)
            egl?.release()
            egl = null
            texture.release()
            quitSafely()
        }

        private fun setup() {
            egl = Egl.create(texture)
            if (egl == null) {
                Log.e(TAG, "sem EGL/OpenGL ES 2.0 para o carro 3D")
                return
            }
            renderer.onSurfaceCreated(null, null)
            renderer.onSurfaceChanged(null, width, height)
            schedule()
        }

        private fun schedule() {
            if (frameScheduled || finished) return
            frameScheduled = true
            Choreographer.getInstance().postFrameCallback(frame)
        }

        private fun draw() {
            if (finished || paused) return
            val e = egl ?: return
            renderer.onDrawFrame(null)
            if (!e.swap()) Log.w(TAG, "eglSwapBuffers falhou: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    private class Egl private constructor(
        private val display: EGLDisplay,
        private val context: EGLContext,
        private val surface: EGLSurface,
    ) {
        fun swap(): Boolean = EGL14.eglSwapBuffers(display, surface)

        /** Sem `eglTerminate`: o display padrão é o mesmo que o resto da tela (HWUI) usa. */
        fun release() {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglReleaseThread()
        }

        companion object {
            /** 4× MSAA quando a GPU oferece (na Mali-450 sai quase de graça); senão, sem. */
            private val WITH_MSAA = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SAMPLE_BUFFERS, 1, EGL14.EGL_SAMPLES, 4, EGL14.EGL_NONE,
            )
            private val PLAIN = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE,
            )

            fun create(texture: SurfaceTexture): Egl? {
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (display == EGL14.EGL_NO_DISPLAY) return null
                val version = IntArray(2)
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
                val config = choose(display, WITH_MSAA) ?: choose(display, PLAIN) ?: return null
                val context = EGL14.eglCreateContext(
                    display, config, EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
                )
                if (context == EGL14.EGL_NO_CONTEXT) return null
                val surface = EGL14.eglCreateWindowSurface(display, config, texture, intArrayOf(EGL14.EGL_NONE), 0)
                if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroyContext(display, context)
                    return null
                }
                if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                    EGL14.eglDestroySurface(display, surface)
                    EGL14.eglDestroyContext(display, context)
                    return null
                }
                return Egl(display, context, surface)
            }

            private fun choose(display: EGLDisplay, attrs: IntArray): EGLConfig? {
                val configs = arrayOfNulls<EGLConfig>(1)
                val count = IntArray(1)
                val ok = EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0)
                return if (ok && count[0] > 0) configs[0] else null
            }
        }
    }

    private companion object {
        const val TAG = "CarGlView"
    }
}
