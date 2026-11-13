package com.rocha.jancarlauncher.ui.car3d

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Leitor mínimo de glTF binário (.glb) para o carro 3D. Lê só o que o renderizador usa:
 * nós (TRS ou matriz, filhos), malhas de triângulos (posição, normal, UV 0, índices), materiais
 * PBR básicos (cor, metal, aspereza, emissão, textura de cor) e imagens embutidas.
 *
 * Fora do escopo (o modelo é preparado no Blender para não precisar): Draco/meshopt, morph, skin,
 * animações do arquivo, texturas externas por URI, accessors esparsos.
 *
 * A GPU da central (Mali-450, OpenGL ES 2.0) pode não ter índice de 32 bits: índices grandes viram 16 bits
 * quando cabem; se não couberem, a primitiva é desindexada.
 */
class GlbScene(
    val nodes: List<GlbNode>,
    val roots: IntArray,
    val meshes: List<List<GlbPrimitive>>,
    val materials: List<GlbMaterial>,
    /** Bytes da imagem (PNG/JPEG) de cada textura glTF; `null` se não estiver embutida. */
    val textureImages: List<ByteArray?>,
    /** `asset.extras.placeholder`: o modelo provisório gerado por script (a tela avisa). */
    val placeholder: Boolean,
) {
    fun nodeIndex(name: String): Int = nodes.indexOfFirst { it.name == name }
}

class GlbNode(
    val name: String,
    val children: IntArray,
    val mesh: Int,
    val translation: FloatArray,
    /** Quatérnio x, y, z, w. */
    val rotation: FloatArray,
    val scale: FloatArray,
    /** Matriz 4×4 em colunas, quando o nó usa `matrix` em vez de TRS. */
    val matrix: FloatArray?,
)

class GlbPrimitive(
    val positions: FloatBuffer,
    val normals: FloatBuffer,
    val uvs: FloatBuffer?,
    /** `null` = desenhar sem índice (glDrawArrays). */
    val indices: ShortBuffer?,
    val vertexCount: Int,
    val indexCount: Int,
    val material: Int,
)

class GlbMaterial(
    val name: String,
    val baseColor: FloatArray,
    val metallic: Float,
    val roughness: Float,
    val emissive: FloatArray,
    val baseColorTexture: Int,
    val doubleSided: Boolean,
    val blend: Boolean,
)

class GlbFormatException(message: String) : Exception(message)

object Glb {
    private const val MAGIC = 0x46546C67
    private const val CHUNK_JSON = 0x4E4F534A
    private const val CHUNK_BIN = 0x004E4942

    private const val FLOAT = 5126
    private const val UBYTE = 5121
    private const val USHORT = 5123
    private const val UINT = 5125
    private const val TRIANGLES = 4

    fun parse(bytes: ByteArray): GlbScene {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.size < 20 || buf.getInt(0) != MAGIC) throw GlbFormatException("não é um arquivo .glb")
        if (buf.getInt(4) != 2) throw GlbFormatException("só glTF 2.0")
        var json: JSONObject? = null
        var bin: ByteBuffer? = null
        var pos = 12
        while (pos + 8 <= bytes.size) {
            val length = buf.getInt(pos)
            val type = buf.getInt(pos + 4)
            val start = pos + 8
            if (start + length > bytes.size) throw GlbFormatException("pedaço cortado")
            when (type) {
                CHUNK_JSON -> json = JSONObject(String(bytes, start, length, Charsets.UTF_8))
                CHUNK_BIN -> bin = ByteBuffer.wrap(bytes, start, length).slice().order(ByteOrder.LITTLE_ENDIAN)
            }
            pos = start + length
        }
        val root = json ?: throw GlbFormatException("sem JSON")
        return Reader(root, bin).read()
    }

    private class Reader(val json: JSONObject, val bin: ByteBuffer?) {
        val accessors = json.optJSONArray("accessors") ?: JSONArray()
        val views = json.optJSONArray("bufferViews") ?: JSONArray()

        fun read(): GlbScene {
            val nodesJson = json.optJSONArray("nodes") ?: JSONArray()
            val nodes = (0 until nodesJson.length()).map { node(nodesJson.getJSONObject(it)) }
            val sceneIndex = json.optInt("scene", 0)
            val scenes = json.optJSONArray("scenes")
            val roots = scenes?.optJSONObject(sceneIndex)?.optJSONArray("nodes")?.toIntArray()
                ?: rootsOf(nodes)
            val meshesJson = json.optJSONArray("meshes") ?: JSONArray()
            val meshes = (0 until meshesJson.length()).map { mesh(meshesJson.getJSONObject(it)) }
            val materialsJson = json.optJSONArray("materials") ?: JSONArray()
            val materials = (0 until materialsJson.length()).map { material(materialsJson.getJSONObject(it)) }
            val placeholder = json.optJSONObject("asset")?.optJSONObject("extras")?.optBoolean("placeholder") == true
            return GlbScene(nodes, roots, meshes, materials, textureImages(), placeholder)
        }

        private fun rootsOf(nodes: List<GlbNode>): IntArray {
            val child = BooleanArray(nodes.size)
            nodes.forEach { n -> n.children.forEach { child[it] = true } }
            return nodes.indices.filter { !child[it] }.toIntArray()
        }

        private fun node(o: JSONObject) = GlbNode(
            name = o.optString("name", ""),
            children = o.optJSONArray("children")?.toIntArray() ?: IntArray(0),
            mesh = o.optInt("mesh", -1),
            translation = o.optJSONArray("translation")?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f),
            rotation = o.optJSONArray("rotation")?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f, 1f),
            scale = o.optJSONArray("scale")?.toFloatArray() ?: floatArrayOf(1f, 1f, 1f),
            matrix = o.optJSONArray("matrix")?.toFloatArray(),
        )

        private fun material(o: JSONObject): GlbMaterial {
            val pbr = o.optJSONObject("pbrMetallicRoughness") ?: JSONObject()
            return GlbMaterial(
                name = o.optString("name", ""),
                baseColor = pbr.optJSONArray("baseColorFactor")?.toFloatArray() ?: floatArrayOf(1f, 1f, 1f, 1f),
                metallic = pbr.optDouble("metallicFactor", 1.0).toFloat(),
                roughness = pbr.optDouble("roughnessFactor", 1.0).toFloat(),
                emissive = o.optJSONArray("emissiveFactor")?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f),
                baseColorTexture = pbr.optJSONObject("baseColorTexture")?.optInt("index", -1) ?: -1,
                doubleSided = o.optBoolean("doubleSided", false),
                blend = o.optString("alphaMode", "OPAQUE") == "BLEND",
            )
        }

        private fun textureImages(): List<ByteArray?> {
            val textures = json.optJSONArray("textures") ?: return emptyList()
            val images = json.optJSONArray("images") ?: JSONArray()
            return (0 until textures.length()).map { t ->
                val source = textures.getJSONObject(t).optInt("source", -1)
                val image = images.optJSONObject(source) ?: return@map null
                if (!image.has("bufferView")) return@map null
                val (data, _) = view(image.getInt("bufferView"))
                ByteArray(data.remaining()).also { data.get(it) }
            }
        }

        private fun mesh(o: JSONObject): List<GlbPrimitive> {
            val prims = o.optJSONArray("primitives") ?: return emptyList()
            return (0 until prims.length()).mapNotNull { primitive(prims.getJSONObject(it)) }
        }

        private fun primitive(o: JSONObject): GlbPrimitive? {
            if (o.optInt("mode", TRIANGLES) != TRIANGLES) return null
            val attrs = o.getJSONObject("attributes")
            if (!attrs.has("POSITION")) return null
            var positions = floats(attrs.getInt("POSITION"), 3)
            var normals = if (attrs.has("NORMAL")) floats(attrs.getInt("NORMAL"), 3) else null
            var uvs = if (attrs.has("TEXCOORD_0")) floats(attrs.getInt("TEXCOORD_0"), 2) else null
            var indices = if (o.has("indices")) ints(o.getInt("indices")) else null
            if (indices != null && (indices.maxOrNull() ?: 0) > 0xFFFF) {
                positions = expand(positions, indices, 3)
                normals = normals?.let { expand(it, indices, 3) }
                uvs = uvs?.let { expand(it, indices, 2) }
                indices = null
            }
            val vertexCount = positions.size / 3
            val n = normals ?: computeNormals(positions, indices)
            return GlbPrimitive(
                positions = floatBuffer(positions),
                normals = floatBuffer(n),
                uvs = uvs?.let(::floatBuffer),
                indices = indices?.let(::shortBuffer),
                vertexCount = vertexCount,
                indexCount = indices?.size ?: vertexCount,
                material = o.optInt("material", -1),
            )
        }

        /** Dados do bufferView [index] e o stride declarado (0 = compacto). */
        private fun view(index: Int): Pair<ByteBuffer, Int> {
            val v = views.getJSONObject(index)
            val data = bin ?: throw GlbFormatException("sem dados binários")
            val offset = v.optInt("byteOffset", 0)
            val length = v.getInt("byteLength")
            val dup = data.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            dup.position(offset)
            dup.limit(offset + length)
            return dup.slice().order(ByteOrder.LITTLE_ENDIAN) to v.optInt("byteStride", 0)
        }

        private fun floats(accessor: Int, components: Int): FloatArray {
            val a = accessors.getJSONObject(accessor)
            if (a.optInt("componentType") != FLOAT) throw GlbFormatException("atributo não-float (quantizado) não suportado")
            val count = a.getInt("count")
            val (data, declaredStride) = view(a.getInt("bufferView"))
            val base = a.optInt("byteOffset", 0)
            val stride = if (declaredStride > 0) declaredStride else components * 4
            val out = FloatArray(count * components)
            for (i in 0 until count) for (c in 0 until components) {
                out[i * components + c] = data.getFloat(base + i * stride + c * 4)
            }
            return out
        }

        private fun ints(accessor: Int): IntArray {
            val a = accessors.getJSONObject(accessor)
            val count = a.getInt("count")
            val (data, _) = view(a.getInt("bufferView"))
            val base = a.optInt("byteOffset", 0)
            return when (a.getInt("componentType")) {
                UBYTE -> IntArray(count) { data.get(base + it).toInt() and 0xFF }
                USHORT -> IntArray(count) { data.getShort(base + it * 2).toInt() and 0xFFFF }
                UINT -> IntArray(count) { data.getInt(base + it * 4) }
                else -> throw GlbFormatException("tipo de índice desconhecido")
            }
        }
    }

    private fun expand(src: FloatArray, indices: IntArray, components: Int): FloatArray {
        val out = FloatArray(indices.size * components)
        indices.forEachIndexed { i, v -> System.arraycopy(src, v * components, out, i * components, components) }
        return out
    }

    /** Normais suaves (média das faces) para modelos exportados sem normal. */
    internal fun computeNormals(p: FloatArray, indices: IntArray?): FloatArray {
        val n = FloatArray(p.size)
        val count = indices?.size ?: (p.size / 3)
        var t = 0
        while (t + 2 < count) {
            val a = indices?.get(t) ?: t
            val b = indices?.get(t + 1) ?: (t + 1)
            val c = indices?.get(t + 2) ?: (t + 2)
            val ux = p[b * 3] - p[a * 3]; val uy = p[b * 3 + 1] - p[a * 3 + 1]; val uz = p[b * 3 + 2] - p[a * 3 + 2]
            val vx = p[c * 3] - p[a * 3]; val vy = p[c * 3 + 1] - p[a * 3 + 1]; val vz = p[c * 3 + 2] - p[a * 3 + 2]
            val nx = uy * vz - uz * vy; val ny = uz * vx - ux * vz; val nz = ux * vy - uy * vx
            for (v in intArrayOf(a, b, c)) { n[v * 3] += nx; n[v * 3 + 1] += ny; n[v * 3 + 2] += nz }
            t += 3
        }
        for (v in 0 until n.size / 3) {
            val l = kotlin.math.sqrt(n[v * 3] * n[v * 3] + n[v * 3 + 1] * n[v * 3 + 1] + n[v * 3 + 2] * n[v * 3 + 2])
            if (l > 0f) { n[v * 3] /= l; n[v * 3 + 1] /= l; n[v * 3 + 2] /= l } else n[v * 3 + 1] = 1f
        }
        return n
    }

    private fun floatBuffer(a: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(a); position(0) }

    private fun shortBuffer(a: IntArray): ShortBuffer =
        ByteBuffer.allocateDirect(a.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
            a.forEach { put(it.toShort()) }
            position(0)
        }

    private fun JSONArray.toIntArray() = IntArray(length()) { getInt(it) }
    private fun JSONArray.toFloatArray() = FloatArray(length()) { getDouble(it).toFloat() }
}
