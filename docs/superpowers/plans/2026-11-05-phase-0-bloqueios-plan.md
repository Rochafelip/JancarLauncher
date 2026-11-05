# Fase 0 — Plano (executado pelo usuário)

Spec: `../specs/2026-11-05-phase-0-bloqueios-design.md`. Antes: `adb connect <IP_DA_CENTRAL>:5555` (reconectar quando cair).

- [ ] **0.1 Rollback primeiro.** Confirmar o launcher atual: `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` (deve citar `com.android.launcher3/com.jancar.launcher.act.ActLauncher`). Guardar o comando: `adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher`.
- [ ] **0.2 Música.** `adb shell dumpsys package com.jancar.music` → no bloco de *Activity Resolver Table* / `android.intent.action.MAIN`, listar todas as activities com `LAUNCHER`. Testar cada uma: `adb shell am start -n com.jancar.music/<activity>`. Escolher a que abre a tela principal do player.
- [ ] **0.3 Vídeo.** Abrir o app Vídeo nativo na central e rodar `adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp"`. Alternativa: `adb shell pm list packages | grep -i -E "video|movie|player"`. Validar com `am start -n`.
- [ ] **0.4 Scrcpy / OBD-II.** `adb shell pm list packages | grep -i -E "scrcpy|obd|elm"`; para cada um: `adb shell cmd package resolve-activity --brief <pacote>`.
- [ ] **0.5 Câmera de ré.** `adb shell am start -n com.autochips.backcarapp/<activity>` com o carro em P. Anotar se abre, se fecha sozinha, se trava a central. (Descobrir a activity com `dumpsys package com.autochips.backcarapp`.)
- [ ] **0.7 Registrar** os resultados no spec e commitar.

**Saída:** valores para `AppTargets` (fase 3), lista da gaveta (fase 4).
