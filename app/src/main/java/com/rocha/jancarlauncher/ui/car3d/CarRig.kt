package com.rocha.jancarlauncher.ui.car3d

import com.rocha.jancarlauncher.ui.dash.DashModel

/**
 * Contrato entre o modelo 3D e o app: nomes dos nós que se mexem ou acendem, e como (ver
 * `docs/car3d/contrato-do-modelo.md`). Eixos do glTF: +Y para cima, frente do carro em +Z, lado do
 * motorista (esquerdo) em +X. Cada peça móvel tem o pivô na dobradiça.
 */
object CarRig {
    enum class Axis { X, Y }

    /** Peça móvel: gira [openDegrees] em torno de [axis] (local, pelo pivô) quando aberta. */
    enum class Part(val node: String, val axis: Axis, val openDegrees: Float) {
        // Portas abrem para fora pela dobradiça da frente: as da esquerda (+X) giram negativo em Y.
        DoorFrontLeft("door_fl", Axis.Y, -62f),
        DoorFrontRight("door_fr", Axis.Y, 62f),
        DoorRearLeft("door_rl", Axis.Y, -62f),
        DoorRearRight("door_rr", Axis.Y, 62f),
        // Tampa traseira: dobradiça em cima, a borda de baixo sobe para trás.
        Tailgate("tailgate", Axis.X, 78f),
        // Capô: dobradiça perto do para-brisa, a frente sobe.
        Hood("hood", Axis.X, -42f),
    }

    enum class Lamp(val nodes: List<String>) {
        Head(listOf("lamp_head_l", "lamp_head_r")),
        Tail(listOf("lamp_tail_l", "lamp_tail_r")),
        // ml/mr = repetidor no retrovisor (o Fox do dono tem; nó filho da porta, abre junto). Opcional no modelo.
        TurnLeft(listOf("lamp_turn_fl", "lamp_turn_rl", "lamp_turn_ml")),
        TurnRight(listOf("lamp_turn_fr", "lamp_turn_rr", "lamp_turn_mr")),
    }

    /**
     * Lâmpada acendendo/apagando: o filamento leva um instante para esquentar e esfria um pouco mais devagar.
     * Constantes de tempo (ms) do fade das luzes no desenho.
     */
    const val LAMP_RISE_MS = 45f
    const val LAMP_FALL_MS = 110f

    /** Aproxima o brilho [level] (0..1) do alvo em [dtMs]; chega exatamente no alvo quando fica bem perto. */
    fun fadeLamp(level: Float, on: Boolean, dtMs: Float): Float {
        val target = if (on) 1f else 0f
        val tau = if (on) LAMP_RISE_MS else LAMP_FALL_MS
        val next = level + (target - level) * (1f - kotlin.math.exp(-dtMs / tau))
        return if (kotlin.math.abs(next - target) < 0.01f) target else next
    }

    /** Quanto tempo uma porta leva para abrir ou fechar por completo no desenho. */
    const val SWING_MS = 700f

    fun isOpen(part: Part, d: DashModel.Doors): Boolean = when (part) {
        Part.DoorFrontLeft -> d.frontLeft
        Part.DoorFrontRight -> d.frontRight
        Part.DoorRearLeft -> d.rearLeft
        Part.DoorRearRight -> d.rearRight
        Part.Tailgate -> d.trunk
        Part.Hood -> d.hood
    } == true

    /**
     * Ângulo de giro da câmera (graus, 0 = de frente, 90 = lado do motorista) que mostra melhor a peça que
     * acabou de abrir.
     */
    fun focusYaw(part: Part): Float = when (part) {
        Part.DoorFrontLeft, Part.DoorRearLeft -> 70f
        Part.DoorFrontRight, Part.DoorRearRight -> -70f
        Part.Tailgate -> 155f
        Part.Hood -> 25f
    }

    /** Suaviza o início e o fim do movimento (0..1 → 0..1). */
    fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}

/**
 * Estado animado das peças móveis: cada uma vai de 0 (fechada) a 1 (aberta) no seu ritmo, sem pular quando
 * o CAN muda no meio do movimento. Roda na thread de desenho; sem Android, para dar pra testar.
 */
class CarAnimator {
    private val progress = FloatArray(CarRig.Part.entries.size)
    private val target = FloatArray(CarRig.Part.entries.size)

    /** Atualiza os alvos; devolve a peça que acabou de começar a abrir (para a câmera ir até ela), se houver. */
    fun setDoors(d: DashModel.Doors): CarRig.Part? {
        var opened: CarRig.Part? = null
        for (part in CarRig.Part.entries) {
            val t = if (CarRig.isOpen(part, d)) 1f else 0f
            if (t == 1f && target[part.ordinal] == 0f) opened = part
            target[part.ordinal] = t
        }
        return opened
    }

    /** Avança [dtMs]; devolve `true` enquanto alguma peça ainda está se mexendo. */
    fun step(dtMs: Float): Boolean {
        val delta = dtMs / CarRig.SWING_MS
        var moving = false
        for (i in progress.indices) {
            val p = progress[i]
            val t = target[i]
            if (p == t) continue
            progress[i] = if (t > p) minOf(t, p + delta) else maxOf(t, p - delta)
            if (progress[i] != t) moving = true
        }
        return moving
    }

    /** Ângulo atual da peça, em graus. */
    fun angle(part: CarRig.Part): Float = CarRig.ease(progress[part.ordinal]) * part.openDegrees

    fun progress(part: CarRig.Part): Float = progress[part.ordinal]
}
