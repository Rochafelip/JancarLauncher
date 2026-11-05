# Fase 8 — Plano

Spec: `../specs/2026-11-05-phase-8-modo-noturno-design.md`. Pacote `settings/`.

## T8.1 `NightModePolicy` (teste primeiro)
`settings/NightModePolicy.kt`:
```kotlin
object NightModePolicy {
    val NIGHT_START: LocalTime = LocalTime.of(20, 0)
    val NIGHT_END: LocalTime = LocalTime.of(5, 30)
    const val NIGHT_BRIGHTNESS = 35

    fun isNight(now: LocalTime): Boolean =
        now >= NIGHT_START || now < NIGHT_END
}
```
`NightModePolicyTest`: 19:59→false, 20:00→true, 23:59→true, 00:00→true, 05:29→true, 05:30→false, 12:00→false.

## T8.2 Estado de transição em `SettingsStore`
Adicionar `autoNightApplied: Flow<Boolean>` (se a automação está com o estado noturno aplicado agora) e `preNightDark/preNightBrightness` (tema/brilho a restaurar na saída). `SettingsStoreTest`: set/get dos três novos campos.

## T8.3 `NightModeController`
`settings/NightModeController.kt`, orquestra: recebe `NightModePolicy.isNight(now)` + estado salvo, decide se deve aplicar/restaurar, chama `SettingsStore.setDark`/`BrightnessController`. Lógica pura separada da coleta do tick (testável sem Android): `NightModeController.evaluate(now, wasApplied, currentDark, currentBrightness): NightModeAction` (selado: `ApplyNight(previousDark, previousBrightness)` | `RestoreDay(dark, brightness)` | `NoOp`). `NightModeControllerTest`: cobre as 4 combinações (entrando/saindo da janela × já aplicado/não aplicado).

## T8.4 Ligação na Home
Em `HomeScreen`/`AppRoot`, reaproveitar o tick de minuto de `ClockState.kt` (extrair o `BroadcastReceiver` para algo reutilizável se necessário, sem duplicar) para chamar `NightModeController.evaluate` e aplicar a `NightModeAction` resultante via `SettingsStore`/`BrightnessController`. Chamar também uma vez na composição inicial (cobre o caso "app abriu já dentro da janela").

## T8.5 Verificação no emulador
`adb shell date` para ajustar o horário do sistema (ou `Settings > Date & time` manual) cruzando 20:00 e 05:30; conferir tema e brilho mudando; trocar tema manualmente às 21:00 e confirmar que não volta sozinho até 05:30.

## T8.6 Commit
`feat: modo noturno automático (tema + brilho, 20:00-05:30)`.
