package com.rocha.jancarlauncher.obd

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Uma leitura de falhas: luz de injeção, códigos confirmados (03), pendentes (07) e permanentes (0A; `null` = o carro
 * não suporta). [error] = a leitura parou no meio (o que já tinha lido fica).
 */
data class ScanResult(
    val wallMs: Long,
    val mil: MilStatus?,
    val confirmed: List<String>,
    val pending: List<String>,
    val permanent: List<String>?,
    val error: String? = null,
)

/** Últimas [keep] leituras em `dir/scans.json`, a mais nova primeiro. Arquivo estragado = lista vazia. */
class ScanHistory(private val dir: File, private val keep: Int = 10) {
    private val file get() = File(dir, "scans.json")

    @Synchronized
    fun add(r: ScanResult) {
        val all = (listOf(r) + list()).take(keep)
        dir.mkdirs()
        file.writeText(JSONArray(all.map(::toJson)).toString())
    }

    @Synchronized
    fun list(): List<ScanResult> = runCatching {
        val a = JSONArray(file.readText())
        List(a.length()) { fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun toJson(r: ScanResult) = JSONObject().apply {
        put("t", r.wallMs)
        r.mil?.let { put("mil", it.milOn); put("n", it.count) }
        put("c", JSONArray(r.confirmed))
        put("p", JSONArray(r.pending))
        r.permanent?.let { put("x", JSONArray(it)) }
        r.error?.let { put("e", it) }
    }

    private fun fromJson(o: JSONObject) = ScanResult(
        wallMs = o.getLong("t"),
        mil = if (o.has("mil")) MilStatus(o.getBoolean("mil"), o.getInt("n")) else null,
        confirmed = strings(o.getJSONArray("c")),
        pending = strings(o.getJSONArray("p")),
        permanent = o.optJSONArray("x")?.let(::strings),
        error = if (o.has("e")) o.getString("e") else null,
    )

    private fun strings(a: JSONArray) = List(a.length()) { a.getString(it) }
}
