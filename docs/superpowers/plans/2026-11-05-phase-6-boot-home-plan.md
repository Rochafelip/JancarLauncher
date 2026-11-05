# Fase 6 — Plano

Spec: `../specs/2026-11-05-phase-6-boot-home-design.md`. Pacote `boot/`.

## T6.1 Política de reinício (teste primeiro)
`boot/RestartPolicy.kt`: `class RestartPolicy(private val max: Int = 3, private val windowMs: Long = 60_000)` com `fun allow(nowMs: Long): Boolean` guardando timestamps em lista. `RestartPolicyTest`: 3 permitidos, 4º negado dentro da janela; depois da janela volta a permitir.

## T6.2 BootReceiver
`boot/BootReceiver.kt`:
```kotlin
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    runCatching { ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
      .onFailure { Log.w("JancarBoot", "falha ao abrir", it) }
  }
}
```
Manifesto: `<receiver android:name=".boot.BootReceiver" android:exported="true"><intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/></intent-filter></receiver>`.

## T6.3 Resiliência
`JancarApp.onCreate`: instalar `Thread.setDefaultUncaughtExceptionHandler { t, e -> Log.e(...); if (policy.allow(now)) startActivity(MainActivity, NEW_TASK|CLEAR_TASK); previous?.uncaughtException(t, e) }` (manter o handler anterior para o processo encerrar). Garantir que `onCreate` só instancia objetos leves (nada de I/O no main).

## T6.4 Home sempre volta à Home
`MainActivity.onNewIntent(intent)`: se `Intent.CATEGORY_HOME` estiver em `intent.categories`, setar um evento (`SharedFlow`/estado em `AppRoot`) para `screen = Home`. Teste instrumentado opcional com `ActivityScenario`.

## T6.5 `docs/INSTALL.md`
Escrever em pt-BR as seções da spec (§3). Incluir tabela de comandos prontos:
```
adb connect <IP_DA_CENTRAL>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity com.rocha.jancarlauncher/.MainActivity
adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher   # ROLLBACK
adb shell pm grant com.rocha.jancarlauncher android.permission.WRITE_SETTINGS   # só se aceito (pode ser negado)
adb disconnect   # e desligar a depuração sem fio nas configurações
```
Passo a passo de verificação com checkboxes.

## T6.6 Ensaio no aparelho (ordem importa)
1. Confirmar o rollback **antes** de tudo: definir o launcher de fábrica explicitamente e voltar.
2. `adb install -r`; abrir manualmente; validar telas.
3. `set-home-activity` do nosso app; apertar o botão Home.
4. Reiniciar a central; cronometrar o boot; conferir Boas-vindas→Home.
5. `force-stop` e verificar reabertura; abrir Rádio/Música/Android Auto e voltar pelo botão Home.
6. Reinstalar por cima.
7. Teste de crash (debug): confirmar limite de reinícios.
Registrar o resultado de cada passo em `docs/INSTALL.md` (seção “Verificado em”) com data e versão do firmware `JCAC10003-OC2-V1.1.62R0`.

## T6.7 Commit
`feat: boot receiver, resiliência de HOME e guia de instalação`.
