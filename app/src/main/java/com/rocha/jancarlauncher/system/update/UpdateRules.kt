package com.rocha.jancarlauncher.system.update

import org.json.JSONObject

/** Última release do GitHub: a tag (ex.: "v1.8.0") e o link do `.apk`. */
data class Release(val tag: String, val apkUrl: String)

/** O que importa de um APK para decidir se pode instalar por cima. */
data class ApkInfo(val pkg: String, val versionCode: Long, val certSha256: Set<String>)

enum class Verdict { Ok, Unreadable, OtherPackage, OtherSignature, NotNewer }

/** Regras puras da atualização sem ADB (GitHub ou pendrive). */
object UpdateRules {
    /** Resposta de `releases/latest`; `null` se não der para ler ou não houver `.apk` na release. */
    fun parseLatest(json: String): Release? = runCatching {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val assets = o.optJSONArray("assets") ?: return null
        (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?.optString("browser_download_url")?.takeIf { it.startsWith("https://") }
            ?.let { Release(tag, it) }
    }.getOrNull()

    /** A tag remota é mais nova que a versão instalada? Tag que não é número = não. */
    fun isNewer(remoteTag: String, installedName: String): Boolean {
        val r = numbers(remoteTag) ?: return false
        val i = numbers(installedName) ?: return true
        for (k in 0 until maxOf(r.size, i.size)) {
            val a = r.getOrElse(k) { 0 }
            val b = i.getOrElse(k) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Só instala o mesmo app, com a mesma assinatura e versão maior (nunca volta versão nem troca de chave). */
    fun verdict(archive: ApkInfo?, installed: ApkInfo): Verdict = when {
        archive == null -> Verdict.Unreadable
        archive.pkg != installed.pkg -> Verdict.OtherPackage
        archive.certSha256.isEmpty() || archive.certSha256 != installed.certSha256 -> Verdict.OtherSignature
        archive.versionCode <= installed.versionCode -> Verdict.NotNewer
        else -> Verdict.Ok
    }

    private fun numbers(v: String): List<Int>? =
        v.trim().removePrefix("v").removePrefix("V").split('.').map { it.toIntOrNull() ?: return null }
}
