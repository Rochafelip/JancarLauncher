package com.rocha.jancarlauncher.launch

enum class Projection { AndroidAuto, CarPlay }

data class BondedPhone(val name: String?)

data class ProjectionInput(
    val bondedPhones: List<BondedPhone>,
    val usbPhoneAttached: Boolean,
    val userOverride: Projection? = null,
)

/**
 * Decide Android Auto vs CarPlay ANTES de abrir qualquer coisa (nunca por tentativa e erro).
 * Heurística (minha escolha, a validar na central): override do usuário manda; celular pareado cujo nome
 * indica Apple → CarPlay; qualquer outro caso → Android Auto. [ProjectionInput.usbPhoneAttached] não muda o
 * resultado hoje (só Android Auto como padrão); fica na entrada para afinar a regra no aparelho.
 */
object ProjectionChooser {
    private val appleMarkers = listOf("iphone", "ipad", "apple")

    fun choose(input: ProjectionInput): Projection {
        input.userOverride?.let { return it }
        val hasApple = input.bondedPhones.any { phone ->
            val name = phone.name?.lowercase() ?: return@any false
            appleMarkers.any { it in name }
        }
        return if (hasApple) Projection.CarPlay else Projection.AndroidAuto
    }
}
