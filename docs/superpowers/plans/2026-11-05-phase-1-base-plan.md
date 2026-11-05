# Fase 1 — Plano

Spec: `../specs/2026-11-05-phase-1-base-design.md`. Base: `C:\Users\rocha\AndroidStudioProjects\JancarLauncher`. Ao final de cada tarefa, `./gradlew assembleDebug`.

## T1.1 Catálogo de versões
Editar `gradle/libs.versions.toml`:
- `[versions]`: `composeBom`, `activityCompose`, `lifecycle`, `datastore`, `kotlin`/plugin compose (usar versões estáveis compatíveis com AGP 9.2.1; **consultar a documentação/IDE para as versões atuais — não chutar**).
- `[libraries]`: `androidx-compose-bom`, `androidx-compose-ui`, `androidx-compose-foundation`, `androidx-activity-compose`, `androidx-lifecycle-runtime-compose`, `androidx-lifecycle-viewmodel-compose`, `androidx-datastore-preferences`, `kotlinx-coroutines-test` (test).
- `[plugins]`: `kotlin-compose` (`org.jetbrains.kotlin.plugin.compose`).
Também `build.gradle.kts` raiz: `alias(libs.plugins.kotlin.compose) apply false`.

## T1.2 `app/build.gradle.kts`
- `plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.compose) }` (se o AGP 9.x exigir o plugin Kotlin explícito, adicioná-lo também).
- `buildFeatures { compose = true }`; `kotlinOptions`/`jvmTarget 11`.
- dependencies: `implementation(platform(libs.androidx.compose.bom))` + as libs acima; remover `appcompat`/`material`; manter `core-ktx`, `junit`.
- `release { isMinifyEnabled = true; isShrinkResources = true }` pode ficar para a fase 7; **debug continua o alvo de instalação**.

## T1.3 Manifesto (`app/src/main/AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<application ... android:theme="@style/Theme.JancarLauncher">
  <activity android:name=".MainActivity" android:exported="true"
      android:launchMode="singleTask" android:stateNotNeeded="true"
      android:excludeFromRecents="true" android:screenOrientation="landscape"
      android:configChanges="orientation|screenSize|keyboardHidden|uiMode|density">
    <intent-filter>
      <action android:name="android.intent.action.MAIN"/>
      <category android:name="android.intent.category.HOME"/>
      <category android:name="android.intent.category.DEFAULT"/>
      <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>
  </activity>
</application>
```

## T1.4 Tema XML mínimo
`res/values/themes.xml` e `res/values-night/themes.xml`: `Theme.JancarLauncher` com parent `android:Theme.Material.NoActionBar`, `android:windowBackground=#0a0b0f`, `android:statusBarColor`/`navigationBarColor` transparentes. Atualizar `res/values/colors.xml` (remover cores do template não usadas).

## T1.5 `MainActivity.kt`
`app/src/main/java/com/rocha/jancarlauncher/MainActivity.kt`: `ComponentActivity` → `setContent { Box(Modifier.fillMaxSize().background(Color(0xFF0A0B0F))) { BasicText("ok", style = TextStyle(color = Color.White)) } }`. Sem `enableEdgeToEdge` por enquanto.

## T1.6 Limpeza
Apagar `ExampleUnitTest`/`ExampleInstrumentedTest` ou trocá-los por um teste de sanidade real (ex.: constante de pacote). Rodar `./gradlew clean assembleDebug testDebugUnitTest`.

## T1.7 Verificação (sem alegar sucesso antes de rodar)
1. Criar AVD API 27, 1024×600, 160 dpi (hardware profile custom).
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`, abrir; checar ausência de faixa branca e de recriação ao girar.
3. `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` — MainActivity presente.
4. Commit: `feat: base compose + home activity`.
