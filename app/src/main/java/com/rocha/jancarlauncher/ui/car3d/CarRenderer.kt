package com.rocha.jancarlauncher.ui.car3d

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import com.rocha.jancarlauncher.ui.dash.DashModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** O que a tela manda para o desenho. Luzes já vêm com o pisca resolvido (a tela pisca junto com os ícones). */
data class CarRenderState(
    val doors: DashModel.Doors = DashModel.Doors(),
    val headlights: Boolean = false,
    val turnLeftLit: Boolean = false,
    val turnRightLit: Boolean = false,
    val dark: Boolean = true,
    /** Cor de fundo da tela (ARGB), para o carro não ficar numa caixa de outra cor. */
    val background: Int = 0xFF0A0B0F.toInt(),
)

/**
 * Desenha o carro em OpenGL ES 2.0 (a GPU da central é uma Mali-450: sem ES 3.0, então nada de Filament).
 * Só desenha quando algo muda (toque, porta se mexendo, luz trocando): parado, não gasta GPU.
 * Tudo aqui roda na thread do GL; a tela fala com ele por [CarGlView] (queueEvent).
 */
class CarRenderer(
    private val scene: GlbScene,
    private val requestRender: () -> Unit,
) : GLSurfaceView.Renderer {

    private var state = CarRenderState()
    private val animator = CarAnimator()

    // Câmera orbitando o carro (graus). O dedo mexe direto; "centralizar" e "ir até a porta" deslizam.
    private var yaw = DEFAULT_YAW
    private var pitch = DEFAULT_PITCH
    private var zoom = 1f
    private var yawTarget = yaw
    private var pitchTarget = pitch
    private var zoomTarget = zoom
    private var lastTouch = 0L
    private var lastFrame = 0L

    private var width = 1
    private var height = 1

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val eye = FloatArray(3)
    private val light = FloatArray(3)

    private val partOfNode = arrayOfNulls<CarRig.Part>(scene.nodes.size)
    private val lampOfNode = arrayOfNulls<CarRig.Lamp>(scene.nodes.size)
    private val world = Array(scene.nodes.size) { FloatArray(16) }
    private val restCenter = FloatArray(3)
    private var radius = 2.2f
    /** Centro (local) e meia-diagonal da malha de cada nó: posição e tamanho do brilho da lanterna. */
    private val meshCenter = arrayOfNulls<FloatArray>(scene.nodes.size)
    /** Brilho atual de cada luz (0..1), com fade de lâmpada ([CarRig.fadeLamp]). */
    private val lampLevel = FloatArray(CarRig.Lamp.entries.size)
    /** Verniz por material: só a pintura da lataria (nome com "paint"/"body"/"lataria"). */
    private val coat = FloatArray(scene.materials.size) { i ->
        val n = scene.materials[i].name.lowercase()
        if ("paint" in n || "body" in n || "lataria" in n) 1f else 0f
    }
    private val emissive = FloatArray(3)
    private val glow = FloatArray(4)

    // Objetos do GL (recriados quando o contexto é recriado).
    private var carProgram = 0
    private var fxProgram = 0
    private var buffers = emptyArray<IntArray>()
    private var textures = IntArray(0)

    private val tmp = FloatArray(16)
    private val rot = FloatArray(16)

    init {
        scene.nodes.forEachIndexed { i, n ->
            partOfNode[i] = CarRig.Part.entries.firstOrNull { it.node == n.name }
            lampOfNode[i] = CarRig.Lamp.entries.firstOrNull { n.name in it.nodes }
            if (n.mesh >= 0) meshCenter[i] = meshBoundsCenter(scene.meshes[n.mesh])
        }
        computeBounds()
    }

    // ---------------- entradas (thread do GL, via queueEvent) ----------------

    fun setState(s: CarRenderState) {
        val opened = animator.setDoors(s.doors)
        // Abriu algo e ninguém mexeu na câmera há um tempo: gira até a peça.
        if (opened != null && SystemClock.uptimeMillis() - lastTouch > IDLE_FOCUS_MS) {
            yawTarget = nearestAngle(yaw, CarRig.focusYaw(opened))
        }
        state = s
    }

    fun rotateBy(dxPx: Float, dyPx: Float) {
        lastTouch = SystemClock.uptimeMillis()
        yaw -= dxPx * DEG_PER_PX
        pitch = (pitch + dyPx * DEG_PER_PX * 0.6f).coerceIn(MIN_PITCH, MAX_PITCH)
        yawTarget = yaw; pitchTarget = pitch
    }

    fun zoomBy(factor: Float) {
        lastTouch = SystemClock.uptimeMillis()
        zoom = (zoom / factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        zoomTarget = zoom
    }

    fun resetView() {
        lastTouch = SystemClock.uptimeMillis()
        yawTarget = nearestAngle(yaw, DEFAULT_YAW); pitchTarget = DEFAULT_PITCH; zoomTarget = 1f
    }

    // ---------------- ciclo do GL ----------------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        carProgram = program(CAR_VS, CAR_FS)
        fxProgram = program(FX_VS, FX_FS)
        buffers = scene.meshes.flatMap { it }.map { p ->
            val ids = IntArray(4)
            GLES20.glGenBuffers(4, ids, 0)
            upload(GLES20.GL_ARRAY_BUFFER, ids[0], p.positions, p.vertexCount * 12)
            upload(GLES20.GL_ARRAY_BUFFER, ids[1], p.normals, p.vertexCount * 12)
            p.uvs?.let { upload(GLES20.GL_ARRAY_BUFFER, ids[2], it, p.vertexCount * 8) }
            p.indices?.let { upload(GLES20.GL_ELEMENT_ARRAY_BUFFER, ids[3], it, p.indexCount * 2) }
            ids
        }.toTypedArray()
        textures = IntArray(scene.textureImages.size)
        scene.textureImages.forEachIndexed { i, bytes -> textures[i] = bytes?.let(::loadTexture) ?: 0 }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        lastFrame = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = max(1, w); height = max(1, h)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 16f else (now - lastFrame).coerceIn(0L, 50L).toFloat()
        lastFrame = now
        var busy = animator.step(dt)
        busy = easeCamera(dt) || busy
        for (lamp in CarRig.Lamp.entries) {
            val on = lampOn(lamp)
            val level = CarRig.fadeLamp(lampLevel[lamp.ordinal], on, dt)
            lampLevel[lamp.ordinal] = level
            if (level != (if (on) 1f else 0f)) busy = true
        }

        val bg = state.background
        GLES20.glClearColor(((bg shr 16) and 0xFF) / 255f, ((bg shr 8) and 0xFF) / 255f, (bg and 0xFF) / 255f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        setupCamera()
        updateWorld()

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        drawGround()
        drawCar()
        drawGlows()

        if (busy) requestRender() else lastFrame = 0L
    }

    // ---------------- câmera ----------------

    private fun easeCamera(dt: Float): Boolean {
        val k = 1f - exp(-dt / CAMERA_EASE_MS)
        yaw += (yawTarget - yaw) * k
        pitch += (pitchTarget - pitch) * k
        zoom += (zoomTarget - zoom) * k
        val settled = abs(yawTarget - yaw) < 0.05f && abs(pitchTarget - pitch) < 0.05f && abs(zoomTarget - zoom) < 0.001f
        if (settled) { yaw = yawTarget; pitch = pitchTarget; zoom = zoomTarget }
        return !settled
    }

    private fun setupCamera() {
        val aspect = width.toFloat() / height
        val halfFov = Math.toRadians(FOV_Y / 2.0).toFloat()
        // Enquadra a esfera do carro na altura e na largura. Numa área larga dá para apertar (a esfera sobra em
        // cima e embaixo); entre os mostradores a área é quase quadrada e o carro de lado ocupa a largura toda,
        // com as portas abertas para fora, então aperta menos.
        // Área bem larga (a tela do Carro: o 3D passa por trás dos mostradores) aperta mais e o carro fica grande.
        val fit = max(radius / tan(halfFov), radius / (tan(halfFov) * aspect))
        val tight = when {
            aspect >= 2.2f -> 0.66f
            aspect >= 1.6f -> 0.82f - (aspect - 1.6f) / 0.6f * 0.16f
            else -> 0.82f + (1.6f - aspect).coerceIn(0f, 0.6f) * 0.3f
        }
        val dist = fit * tight * zoom
        val y = Math.toRadians(yaw.toDouble()); val p = Math.toRadians(pitch.toDouble())
        eye[0] = restCenter[0] + (dist * cos(p) * sin(y)).toFloat()
        eye[1] = restCenter[1] + (dist * sin(p)).toFloat()
        eye[2] = restCenter[2] + (dist * cos(p) * cos(y)).toFloat()
        Matrix.perspectiveM(proj, 0, FOV_Y, aspect, dist * 0.2f, dist * 3f)
        Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], restCenter[0], restCenter[1] - 0.1f, restCenter[2], 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
        // Luz principal acompanha a câmera (de cima, um pouco à esquerda): o carro fica bonito de qualquer lado.
        val fx = (eye[0] - restCenter[0]) / dist; val fz = (eye[2] - restCenter[2]) / dist
        light[0] = fx * 0.55f - fz * 0.35f; light[1] = 0.85f; light[2] = fz * 0.55f + fx * 0.35f
        normalize(light)
    }

    // ---------------- cena ----------------

    private fun updateWorld() {
        val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        for (root in scene.roots) walk(root, identity)
    }

    private fun walk(index: Int, parent: FloatArray) {
        val n = scene.nodes[index]
        val local = FloatArray(16)
        if (n.matrix != null) {
            System.arraycopy(n.matrix, 0, local, 0, 16)
        } else {
            Matrix.setIdentityM(local, 0)
            Matrix.translateM(local, 0, n.translation[0], n.translation[1], n.translation[2])
            quatToMatrix(n.rotation, rot)
            Matrix.multiplyMM(tmp, 0, local, 0, rot, 0)
            System.arraycopy(tmp, 0, local, 0, 16)
        }
        partOfNode[index]?.let { part ->
            val angle = animator.angle(part)
            if (angle != 0f) {
                if (part.axis == CarRig.Axis.X) Matrix.rotateM(local, 0, angle, 1f, 0f, 0f)
                else Matrix.rotateM(local, 0, angle, 0f, 1f, 0f)
            }
        }
        if (n.matrix == null) Matrix.scaleM(local, 0, n.scale[0], n.scale[1], n.scale[2])
        Matrix.multiplyMM(world[index], 0, parent, 0, local, 0)
        for (c in n.children) walk(c, world[index])
    }

    private fun lampOn(lamp: CarRig.Lamp?): Boolean = when (lamp) {
        CarRig.Lamp.Head, CarRig.Lamp.Tail -> state.headlights
        CarRig.Lamp.TurnLeft -> state.turnLeftLit
        CarRig.Lamp.TurnRight -> state.turnRightLit
        null -> false
    }

    private fun drawCar() {
        GLES20.glUseProgram(carProgram)
        val uVp = uniform(carProgram, "uVp"); val uModel = uniform(carProgram, "uModel")
        val uBase = uniform(carProgram, "uBase"); val uEmissive = uniform(carProgram, "uEmissive")
        val uMetal = uniform(carProgram, "uMetal"); val uRough = uniform(carProgram, "uRough")
        val uEye = uniform(carProgram, "uEye"); val uLight = uniform(carProgram, "uLight")
        val uSky = uniform(carProgram, "uSky"); val uGround = uniform(carProgram, "uGround")
        val uUseTex = uniform(carProgram, "uUseTex"); val uTex = uniform(carProgram, "uTex")
        val uCoat = uniform(carProgram, "uCoat"); val uStudio = uniform(carProgram, "uStudio")
        val aPos = GLES20.glGetAttribLocation(carProgram, "aPos")
        val aNormal = GLES20.glGetAttribLocation(carProgram, "aNormal")
        val aUv = GLES20.glGetAttribLocation(carProgram, "aUv")

        GLES20.glUniformMatrix4fv(uVp, 1, false, vp, 0)
        GLES20.glUniform3fv(uEye, 1, eye, 0)
        GLES20.glUniform3fv(uLight, 1, light, 0)
        val sky = if (state.dark) SKY_DARK else SKY_LIGHT
        val ground = if (state.dark) GROUND_DARK else GROUND_LIGHT
        GLES20.glUniform3fv(uSky, 1, sky, 0)
        GLES20.glUniform3fv(uGround, 1, ground, 0)
        GLES20.glUniform3fv(uStudio, 1, if (state.dark) STUDIO_DARK else STUDIO_LIGHT, 0)
        GLES20.glUniform1i(uTex, 0)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)

        // Opacos primeiro, transparentes (vidro com alpha) por último sem gravar profundidade.
        for (pass in 0..1) {
            if (pass == 1) {
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                GLES20.glDepthMask(false)
            }
            var bufferIndex = 0
            val firstBuffer = IntArray(scene.meshes.size)
            scene.meshes.forEachIndexed { i, m -> firstBuffer[i] = bufferIndex; bufferIndex += m.size }
            scene.nodes.forEachIndexed { nodeIndex, node ->
                if (node.mesh < 0) return@forEachIndexed
                val lamp = lampOfNode[nodeIndex]
                val level = lamp?.let { lampLevel[it.ordinal] } ?: 0f
                GLES20.glUniformMatrix4fv(uModel, 1, false, world[nodeIndex], 0)
                scene.meshes[node.mesh].forEachIndexed { k, prim ->
                    val mat = scene.materials.getOrNull(prim.material) ?: DEFAULT_MATERIAL
                    if (mat.blend != (pass == 1)) return@forEachIndexed
                    val ids = buffers[firstBuffer[node.mesh] + k]
                    GLES20.glUniform4fv(uBase, 1, mat.baseColor, 0)
                    if (lamp != null && level > 0f) {
                        val on = lampColor(lamp)
                        for (c in 0..2) emissive[c] = mat.emissive[c] + (on[c] - mat.emissive[c]) * level
                        GLES20.glUniform3fv(uEmissive, 1, emissive, 0)
                    } else {
                        GLES20.glUniform3fv(uEmissive, 1, mat.emissive, 0)
                    }
                    GLES20.glUniform1f(uCoat, coat.getOrElse(prim.material) { 0f })
                    GLES20.glUniform1f(uMetal, mat.metallic)
                    GLES20.glUniform1f(uRough, mat.roughness.coerceIn(0.05f, 1f))
                    val tex = textures.getOrNull(mat.baseColorTexture) ?: 0
                    GLES20.glUniform1f(uUseTex, if (tex != 0 && prim.uvs != null) 1f else 0f)
                    if (tex != 0) {
                        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
                    }
                    if (mat.doubleSided) GLES20.glDisable(GLES20.GL_CULL_FACE) else GLES20.glEnable(GLES20.GL_CULL_FACE)
                    attrib(aPos, ids[0], 3)
                    attrib(aNormal, ids[1], 3)
                    if (prim.uvs != null && aUv >= 0) attrib(aUv, ids[2], 2)
                    else if (aUv >= 0) { GLES20.glDisableVertexAttribArray(aUv); GLES20.glVertexAttrib2f(aUv, 0f, 0f) }
                    if (prim.indices != null) {
                        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ids[3])
                        GLES20.glDrawElements(GLES20.GL_TRIANGLES, prim.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                    } else {
                        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, prim.vertexCount)
                    }
                }
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    // ---------------- sombra, facho do farol e brilho das lanternas ----------------

    private val quad: FloatBuffer = ByteBuffer.allocateDirect(4 * 5 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private fun drawGround() {
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // Sombra suave embaixo do carro ("ancora" o carro no fundo liso).
        val shadowAlpha = if (state.dark) 0.75f else 0.32f
        groundQuad(restCenter[0], restCenter[2], radius * 0.5f, radius * 0.95f, floatArrayOf(0f, 0f, 0f, shadowAlpha), 0.25f)
        // Facho no chão só no tema escuro: no claro (dia) a luz somada vira uma mancha branca.
        val head = lampLevel[CarRig.Lamp.Head.ordinal]
        if (head > 0f && state.dark) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
            val front = restCenter[2] + radius * 0.85f
            for (c in 0..2) glow[c] = BEAM_COLOR[c]
            glow[3] = BEAM_COLOR[3] * head
            for (s in floatArrayOf(-1f, 1f)) {
                // Facho curto: cabe no quadro mesmo de lado.
                groundQuad(s * 0.5f, front + 0.7f, 0.6f, 0.95f, glow, 0f)
            }
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
    }

    private fun groundQuad(cx: Float, cz: Float, hx: Float, hz: Float, color: FloatArray, soft: Float) {
        val y = 0.003f
        fx(floatArrayOf(
            cx - hx, y, cz - hz, -1f, -1f,
            cx + hx, y, cz - hz, 1f, -1f,
            cx + hx, y, cz + hz, 1f, 1f,
            cx - hx, y, cz + hz, -1f, 1f,
        ), color, soft)
    }

    private val center = FloatArray(4)
    private val out = FloatArray(4)

    private fun drawGlows() {
        if (lampLevel.all { it == 0f }) return
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        // Eixos da câmera (para o brilho ficar sempre de frente).
        val rx = view[0]; val ry = view[4]; val rz = view[8]
        val ux = view[1]; val uy = view[5]; val uz = view[9]
        scene.nodes.indices.forEach { i ->
            val lamp = lampOfNode[i] ?: return@forEach
            val level = lampLevel[lamp.ordinal]
            if (level <= 0f) return@forEach
            val c = meshCenter[i] ?: return@forEach
            center[0] = c[0]; center[1] = c[1]; center[2] = c[2]; center[3] = 1f
            Matrix.multiplyMV(out, 0, world[i], 0, center, 0)
            // Um pouco para fora do carro, para não sumir dentro da lataria.
            val push = if (out[2] >= restCenter[2]) 0.1f else -0.1f
            val px = out[0]; val py = out[1]; val pz = out[2] + push
            // Brilho do tamanho da peça (o repetidor do retrovisor é bem menor que o farol).
            val size = (c[3] * 1.6f).coerceIn(0.08f, if (lamp == CarRig.Lamp.Head) 0.32f else 0.26f)
            val base = glowColor(lamp)
            glow[0] = base[0]; glow[1] = base[1]; glow[2] = base[2]; glow[3] = base[3] * level
            fx(floatArrayOf(
                px - (rx + ux) * size, py - (ry + uy) * size, pz - (rz + uz) * size, -1f, -1f,
                px + (rx - ux) * size, py + (ry - uy) * size, pz + (rz - uz) * size, 1f, -1f,
                px + (rx + ux) * size, py + (ry + uy) * size, pz + (rz + uz) * size, 1f, 1f,
                px - (rx - ux) * size, py - (ry - uy) * size, pz - (rz - uz) * size, -1f, 1f,
            ), glow, 0f)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
    }

    private fun fx(vertices: FloatArray, color: FloatArray, soft: Float) {
        GLES20.glUseProgram(fxProgram)
        GLES20.glUniformMatrix4fv(uniform(fxProgram, "uVp"), 1, false, vp, 0)
        GLES20.glUniform4fv(uniform(fxProgram, "uColor"), 1, color, 0)
        GLES20.glUniform1f(uniform(fxProgram, "uSoft"), soft)
        quad.position(0); quad.put(vertices); quad.position(0)
        val aPos = GLES20.glGetAttribLocation(fxProgram, "aPos")
        val aUv = GLES20.glGetAttribLocation(fxProgram, "aUv")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 20, quad)
        quad.position(3)
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 20, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(aUv)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    // ---------------- auxiliares ----------------

    private fun computeBounds() {
        val min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val max = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        updateWorld()
        val v = FloatArray(4); val w = FloatArray(4)
        scene.nodes.forEachIndexed { i, n ->
            if (n.mesh < 0) return@forEachIndexed
            for (p in scene.meshes[n.mesh]) {
                val pos = p.positions
                // Amostra (até ~2000 pontos por primitiva): suficiente para enquadrar.
                val step = max(1, p.vertexCount / 2000)
                var k = 0
                while (k < p.vertexCount) {
                    v[0] = pos.get(k * 3); v[1] = pos.get(k * 3 + 1); v[2] = pos.get(k * 3 + 2); v[3] = 1f
                    Matrix.multiplyMV(w, 0, world[i], 0, v, 0)
                    for (a in 0..2) { min[a] = minOf(min[a], w[a]); max[a] = maxOf(max[a], w[a]) }
                    k += step
                }
            }
        }
        if (min[0] > max[0]) return
        for (a in 0..2) restCenter[a] = (min[a] + max[a]) / 2f
        val dx = max[0] - min[0]; val dy = max[1] - min[1]; val dz = max[2] - min[2]
        radius = sqrt(dx * dx + dy * dy + dz * dz) / 2f
    }

    private fun meshBoundsCenter(prims: List<GlbPrimitive>): FloatArray {
        val min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val max = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (p in prims) for (k in 0 until p.vertexCount) for (a in 0..2) {
            val x = p.positions.get(k * 3 + a)
            min[a] = minOf(min[a], x); max[a] = maxOf(max[a], x)
        }
        if (min[0] > max[0]) return FloatArray(4)
        val dx = max[0] - min[0]; val dy = max[1] - min[1]; val dz = max[2] - min[2]
        return floatArrayOf((min[0] + max[0]) / 2f, (min[1] + max[1]) / 2f, (min[2] + max[2]) / 2f, sqrt(dx * dx + dy * dy + dz * dz) / 2f)
    }

    private fun upload(target: Int, id: Int, data: java.nio.Buffer, bytes: Int) {
        GLES20.glBindBuffer(target, id)
        data.position(0)
        GLES20.glBufferData(target, bytes, data, GLES20.GL_STATIC_DRAW)
    }

    private fun attrib(location: Int, buffer: Int, size: Int) {
        if (location < 0) return
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer)
        GLES20.glEnableVertexAttribArray(location)
        GLES20.glVertexAttribPointer(location, size, GLES20.GL_FLOAT, false, 0, 0)
    }

    /** Textura com no máximo 1024 px de lado (memória da central); mipmap só em potência de 2 (limite do ES 2.0). */
    private fun loadTexture(bytes: ByteArray): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_TEXTURE) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return 0
        val id = IntArray(1)
        GLES20.glGenTextures(1, id, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0])
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        val pot = bitmap.width.isPowerOfTwo() && bitmap.height.isPowerOfTwo()
        if (pot) {
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        } else {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        bitmap.recycle()
        return id[0]
    }

    private fun Int.isPowerOfTwo() = this > 0 && (this and (this - 1)) == 0

    private fun program(vs: String, fs: String): Int {
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) Log.e(TAG, "link: ${GLES20.glGetProgramInfoLog(p)}")
        return p
    }

    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) Log.e(TAG, "shader: ${GLES20.glGetShaderInfoLog(s)}")
        return s
    }

    private fun uniform(program: Int, name: String) = GLES20.glGetUniformLocation(program, name)

    companion object {
        private const val TAG = "CarRenderer"

        // 3/4 pela frente do lado do motorista, câmera baixa (como as fotos do dono à noite).
        const val DEFAULT_YAW = 35f
        const val DEFAULT_PITCH = 9f
        private const val MIN_PITCH = 2f
        // Até 30° de cima (pedido do dono, 2026-11-13): sem vista de topo; o modelo pode economizar no que só
        // apareceria de cima ou de baixo (o fundo do carro nem é gerado).
        private const val MAX_PITCH = 30f
        private const val MIN_ZOOM = 0.6f
        private const val MAX_ZOOM = 1.6f
        private const val FOV_Y = 30f
        private const val DEG_PER_PX = 0.35f
        private const val CAMERA_EASE_MS = 160f
        /** Sem toque há este tempo, a câmera vai sozinha até a porta que abriu. */
        private const val IDLE_FOCUS_MS = 8_000L
        private const val MAX_TEXTURE = 1024

        private val SKY_DARK = floatArrayOf(0.62f, 0.67f, 0.75f)
        private val GROUND_DARK = floatArrayOf(0.09f, 0.095f, 0.11f)
        private val SKY_LIGHT = floatArrayOf(0.95f, 0.96f, 0.98f)
        private val GROUND_LIGHT = floatArrayOf(0.5f, 0.51f, 0.54f)
        /** Luzes de estúdio (softbox no teto e faixas em volta) que correm na lataria quando o carro gira. */
        private val STUDIO_DARK = floatArrayOf(0.55f, 0.6f, 0.68f)
        private val STUDIO_LIGHT = floatArrayOf(0.35f, 0.36f, 0.38f)

        // Luz branca-azulada do LED (o azul-aço do projeto), lanterna vermelha e seta âmbar.
        private val HEAD_ON = floatArrayOf(0.95f, 1f, 1.05f)
        private val TAIL_ON = floatArrayOf(0.95f, 0.08f, 0.06f)
        private val TURN_ON = floatArrayOf(1f, 0.55f, 0.08f)
        private val HEAD_GLOW = floatArrayOf(0.66f, 0.82f, 1f, 0.8f)
        private val TAIL_GLOW = floatArrayOf(1f, 0.12f, 0.08f, 0.6f)
        private val TURN_GLOW = floatArrayOf(1f, 0.58f, 0.1f, 0.85f)
        private val BEAM_COLOR = floatArrayOf(0.55f, 0.72f, 0.95f, 0.18f)

        private val DEFAULT_MATERIAL = GlbMaterial("", floatArrayOf(0.7f, 0.7f, 0.7f, 1f), 0f, 0.6f, floatArrayOf(0f, 0f, 0f), -1, false, false)

        private fun lampColor(lamp: CarRig.Lamp?) = when (lamp) {
            CarRig.Lamp.Tail -> TAIL_ON
            CarRig.Lamp.TurnLeft, CarRig.Lamp.TurnRight -> TURN_ON
            else -> HEAD_ON
        }

        private fun glowColor(lamp: CarRig.Lamp) = when (lamp) {
            CarRig.Lamp.Head -> HEAD_GLOW
            CarRig.Lamp.Tail -> TAIL_GLOW
            CarRig.Lamp.TurnLeft, CarRig.Lamp.TurnRight -> TURN_GLOW
        }

        /** [target] trocado pelo equivalente (± 360°) mais perto de [from]: a câmera gira pelo caminho curto. */
        fun nearestAngle(from: Float, target: Float): Float {
            var t = target
            while (t - from > 180f) t -= 360f
            while (t - from < -180f) t += 360f
            return t
        }

        private fun normalize(v: FloatArray) {
            val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            if (l > 0f) { v[0] /= l; v[1] /= l; v[2] /= l }
        }

        /** Quatérnio (x, y, z, w) → matriz 4×4 em colunas. */
        private fun quatToMatrix(q: FloatArray, m: FloatArray) {
            val x = q[0]; val y = q[1]; val z = q[2]; val w = q[3]
            m[0] = 1 - 2 * (y * y + z * z); m[1] = 2 * (x * y + z * w); m[2] = 2 * (x * z - y * w); m[3] = 0f
            m[4] = 2 * (x * y - z * w); m[5] = 1 - 2 * (x * x + z * z); m[6] = 2 * (y * z + x * w); m[7] = 0f
            m[8] = 2 * (x * z + y * w); m[9] = 2 * (y * z - x * w); m[10] = 1 - 2 * (x * x + y * y); m[11] = 0f
            m[12] = 0f; m[13] = 0f; m[14] = 0f; m[15] = 1f
        }

        private const val CAR_VS = """
uniform mat4 uVp;
uniform mat4 uModel;
attribute vec3 aPos;
attribute vec3 aNormal;
attribute vec2 aUv;
varying vec3 vNormal;
varying vec3 vWorld;
varying vec2 vUv;
void main() {
  vec4 w = uModel * vec4(aPos, 1.0);
  vWorld = w.xyz;
  vNormal = (uModel * vec4(aNormal, 0.0)).xyz;
  vUv = aUv;
  gl_Position = uVp * w;
}
"""

        // Pintura com reflexo falso de estúdio (sem cubemap): céu/chão + softbox no teto + faixas de luz em volta,
        // que correm na lataria quando o carro gira. Na pintura ([uCoat] = 1) vai uma camada de verniz por cima:
        // reflexo nítido e brilho fino sobre o metálico borrado. Barato para a Mali-450, que só tem mediump.
        private const val CAR_FS = """
precision mediump float;
uniform vec4 uBase;
uniform vec3 uEmissive;
uniform float uMetal;
uniform float uRough;
uniform float uCoat;
uniform vec3 uEye;
uniform vec3 uLight;
uniform vec3 uSky;
uniform vec3 uGround;
uniform vec3 uStudio;
uniform sampler2D uTex;
uniform float uUseTex;
varying vec3 vNormal;
varying vec3 vWorld;
varying vec2 vUv;
vec3 envAt(vec3 r, float soft) {
  // Lateral (reflexo quase horizontal) já pega boa parte do "céu": é o que deixa o prata claro dos lados.
  vec3 e = mix(uGround, uSky, smoothstep(-0.5, 0.2, r.y));
  float top = smoothstep(0.78 - soft * 0.3, 0.92, r.y);
  float strip = smoothstep(0.84 - soft * 0.6, 0.97, sin(atan(r.z, r.x) * 3.0))
      * smoothstep(0.02, 0.18, r.y) * (1.0 - smoothstep(0.42, 0.6, r.y));
  float horizon = (1.0 - smoothstep(0.0, 0.1 + soft * 0.2, abs(r.y - 0.05))) * 0.45;
  return e + uStudio * (top * 1.2 + strip + horizon);
}
void main() {
  vec3 n = normalize(vNormal);
  if (!gl_FrontFacing) n = -n;
  vec3 v = normalize(uEye - vWorld);
  vec3 base = uBase.rgb;
  if (uUseTex > 0.5) base *= texture2D(uTex, vUv).rgb;
  float ndl = max(dot(n, uLight), 0.0);
  float ndv = max(dot(n, v), 0.0);
  vec3 amb = mix(uGround, uSky, 0.5 + 0.5 * n.y);
  float gloss = 1.0 - uRough;
  vec3 h = normalize(uLight + v);
  float ndh = max(dot(n, h), 0.0);
  float spec = pow(ndh, 4.0 + 120.0 * gloss * gloss) * gloss;
  vec3 r = reflect(-v, n);
  float fres = pow(1.0 - ndv, 4.0);
  vec3 f0 = mix(vec3(0.04), base, uMetal);
  vec3 refl = envAt(r, uRough) * (f0 + (1.0 - f0) * fres) * gloss;
  vec3 diff = base * (1.0 - uMetal * 0.7) * (amb * 0.6 + ndl * 0.8);
  float cc = uCoat * (0.04 + 0.96 * pow(1.0 - ndv, 5.0));
  vec3 coat = envAt(r, 0.0) * cc + uCoat * pow(ndh, 220.0) * 1.1;
  gl_FragColor = vec4((diff + refl + spec * 0.7) * (1.0 - cc) + coat + uEmissive, uBase.a);
}
"""

        private const val FX_VS = """
uniform mat4 uVp;
attribute vec3 aPos;
attribute vec2 aUv;
varying vec2 vUv;
void main() {
  vUv = aUv;
  gl_Position = uVp * vec4(aPos, 1.0);
}
"""

        private const val FX_FS = """
precision mediump float;
uniform vec4 uColor;
uniform float uSoft;
varying vec2 vUv;
void main() {
  float a = 1.0 - smoothstep(uSoft, 1.0, length(vUv));
  gl_FragColor = vec4(uColor.rgb, uColor.a * a * a);
}
"""
    }
}
