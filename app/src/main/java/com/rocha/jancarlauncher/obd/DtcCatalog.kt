package com.rocha.jancarlauncher.obd

import org.json.JSONObject

enum class Severity { Baixa, Media, Alta }

/** O que mostrar de um código: título, gravidade, possíveis causas e o que conferir. [known] = está na tabela. */
data class DtcInfo(
    val code: String,
    val title: String,
    val severity: Severity,
    val causes: List<String>,
    val checks: List<String>,
    val known: Boolean,
)

/**
 * Tabela própria de códigos em português (`assets/obd/dtc_pt.json`, ~130 códigos genéricos P0 mais comuns). Fora da
 * tabela: P0/P2 = genérico sem descrição; P1/P3 = do fabricante (VW); B/C/U = de outros módulos, sem descrição.
 */
class DtcCatalog(json: String) {
    private val table: Map<String, DtcInfo> = runCatching {
        val o = JSONObject(json)
        o.keys().asSequence().associateWith { code ->
            val e = o.getJSONObject(code)
            DtcInfo(
                code = code,
                title = e.getString("t"),
                severity = when (e.optString("s")) { "alta" -> Severity.Alta; "media" -> Severity.Media; else -> Severity.Baixa },
                causes = e.getJSONArray("c").let { a -> List(a.length()) { a.getString(it) } },
                checks = e.getJSONArray("v").let { a -> List(a.length()) { a.getString(it) } },
                known = true,
            )
        }
    }.getOrDefault(emptyMap())

    fun describe(code: String): DtcInfo = table[code] ?: fallback(code)

    private fun fallback(code: String): DtcInfo {
        val manufacturer = code.length >= 2 && code[0] == 'P' && (code[1] == '1' || code[1] == '3')
        return DtcInfo(
            code = code,
            title = if (manufacturer) "Código do fabricante (VW): confirme o significado com a documentação da marca"
            else "Código genérico sem descrição nesta versão",
            severity = Severity.Media,
            causes = listOf("O significado exato depende do fabricante e do modelo"),
            checks = listOf("Pesquisar o código com a marca e o modelo do carro", "Levar a uma oficina com equipamento da marca"),
            known = false,
        )
    }
}
