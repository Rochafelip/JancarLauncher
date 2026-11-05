# Fase 3 — Plano

Spec: `../specs/2026-11-05-phase-3-lancamento-design.md`. Pacote: `com.rocha.jancarlauncher.launch`.

## T3.1 Modelo e tabela
`launch/AppTarget.kt`:
```kotlin
enum class Kind { App, System }
data class AppTarget(val id: String, val displayName: String, val kind: Kind,
                     val activity: ComponentName? = null, val packageName: String? = activity?.packageName)
```
`launch/AppTargets.kt` (objeto com os alvos): `ANDROID_AUTO = ComponentName("com.google.android.projection.sink","com.google.android.projection.sink.ui.AndroidAutoActivity")`, `CARPLAY = ComponentName("com.autochips.carplayapp","com.autochips.carplayapp.CarPlayActivity")` (**nunca** `com.autochips.carplay`), `RADIO = ComponentName("com.jancar.radio","com.jancar.radio.activity.RadioMainActivity")`, `MUSIC` e `VIDEO` = `ComponentName` informado pelo usuário (Fase 0); `null` enquanto não informado, `BACK_CAMERA = pacote com.autochips.backcarapp`. Conferir o nome completo da activity do CarPlay no `CLAUDE.md` (`.CarPlayActivity`). Teste: nenhum id/pacote igual a `com.autochips.carplay`.

## T3.2 AppLauncher (teste primeiro)
Interfaces para testar sem Android: `interface PackageGateway { fun isInstalled(pkg: String): Boolean; fun isEnabled(pkg: String): Boolean; fun launchIntentFor(pkg: String): Intent? }` e `interface ActivityStarter { fun start(intent: Intent) }`.
- `AppLauncherTest` com fakes: casos do spec.
- `AppLauncher(gateway, starter)`: `launch(target)`: pkg nulo→`NotInstalled`; `!isInstalled`→`NotInstalled`; `!isEnabled`→`Disabled`; monta `Intent().setComponent(target.activity)` (ou `launchIntentFor`), `addFlags(FLAG_ACTIVITY_NEW_TASK)`; `try { starter.start(i); Started } catch (e: ActivityNotFoundException/SecurityException/RuntimeException) { Failed(e) }`.
- Implementações Android: `AndroidPackageGateway(PackageManager)` (`getApplicationEnabledSetting` + `getPackageInfo` com `NameNotFoundException`→false) e `ContextActivityStarter(Context)`.

## T3.3 ProjectionChooser (teste primeiro)
`launch/ProjectionChooser.kt`: `enum Projection { AndroidAuto, CarPlay }`, `data class BondedPhone(val name: String?)`, `data class ProjectionInput(val bondedPhones: List<BondedPhone>, val usbPhoneAttached: Boolean, val userOverride: Projection? = null)`, `object ProjectionChooser { fun choose(i): Projection }`. Testes dos 5 casos do spec; nomes nulos tratados.

## T3.4 Leitura do ambiente
`launch/Environment.kt`: `fun readBondedPhones(ctx): List<BondedPhone>` (`BluetoothAdapter.getDefaultAdapter()?.bondedDevices` filtrando `bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.PHONE`, em `try/catch SecurityException`); `fun usbPhoneAttached(ctx): Boolean` (`UsbManager.deviceList.isNotEmpty()`). Manifesto: `<uses-permission android:name="android.permission.BLUETOOTH"/>` (normal na API 27).

## T3.5 Fachada para a UI
`launch/LaunchController.kt`: `fun open(target)` e `fun openAuto()` (usa `ProjectionChooser` → `AppTargets`), devolvendo `LaunchResult`; a UI mostra o toast. Instância criada em `JancarApp`.

## T3.6 Verificação
`./gradlew testDebugUnitTest`; instalar e usar um botão temporário (ou a galeria da Fase 2) para: abrir Rádio, abrir Auto, abrir alvo inexistente (só toast). Confirmar `grep -rn "startActivity" app/src/main` só em `ContextActivityStarter`. Commit: `feat: núcleo de lançamento e escolha Auto/CarPlay`.
