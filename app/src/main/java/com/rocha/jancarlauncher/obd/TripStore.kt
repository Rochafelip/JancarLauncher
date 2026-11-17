package com.rocha.jancarlauncher.obd

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Últimas [keep] viagens em `dir/trips.json`, a mais nova primeiro. Arquivo estragado = lista vazia. */
class TripStore(private val dir: File, private val keep: Int = 20) {
    private val file get() = File(dir, "trips.json")

    @Synchronized
    fun add(r: TripRecord) {
        val all = (listOf(r) + list()).take(keep)
        dir.mkdirs()
        file.writeText(JSONArray(all.map(::toJson)).toString())
    }

    @Synchronized
    fun list(): List<TripRecord> = runCatching {
        val a = JSONArray(file.readText())
        List(a.length()) { fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun toJson(r: TripRecord) = JSONObject().apply {
        put("s", r.startWallMs); put("e", r.endWallMs); put("m", r.movingS); put("km", r.km)
        put("l", r.liters); put("el", r.ethanolLiters); put("max", r.maxKmh.toDouble())
    }

    private fun fromJson(o: JSONObject) = TripRecord(
        startWallMs = o.getLong("s"), endWallMs = o.getLong("e"), movingS = o.getDouble("m"), km = o.getDouble("km"),
        liters = o.getDouble("l"), ethanolLiters = o.getDouble("el"), maxKmh = o.getDouble("max").toFloat(),
    )
}
