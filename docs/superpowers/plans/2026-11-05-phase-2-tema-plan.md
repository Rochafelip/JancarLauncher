# Fase 2 — Plano

Spec: `../specs/2026-11-05-phase-2-tema-design.md`. Pacote base: `com.rocha.jancarlauncher`. TDD onde indicado.

## T2.1 Tokens de cor (teste primeiro)
- `app/src/test/.../ui/theme/PaletteTest.kt`: assert dos hex (`0xFF0A0B0F` bg escuro, etc., e acento `0xFF6FA3D8`) para `DarkColors` e `LightColors`; assert das bordas com alpha (`0x12FFFFFF` ≈ 0,07 → usar `Color(1f,1f,1f,0.07f)`).
- `ui/theme/Palette.kt`: `data class JancarColors(...)`, `val DarkColors`, `val LightColors`, `val LocalJancarColors = staticCompositionLocalOf { DarkColors }`.

## T2.2 Fonte e tipografia
- Baixar Inter TTF (latin) pesos 200/400/500/600 do Google Fonts (OFL) → `app/src/main/res/font/inter_extralight.ttf` etc. Incluir `docs/licenses/Inter-OFL.txt`.
- `ui/theme/Type.kt`: `val Inter = FontFamily(Font(R.font.inter_extralight, FontWeight.W200), …)` e `object JancarType { val clockLarge = TextStyle(fontFamily = Inter, fontWeight = W200, fontSize = 92.sp, letterSpacing = (-2).sp) … }`.

## T2.3 Tema
`ui/theme/Theme.kt`: `@Composable fun JancarTheme(dark: Boolean, content: @Composable () -> Unit) = CompositionLocalProvider(LocalJancarColors provides if (dark) DarkColors else LightColors, content = content)`; objeto `JTheme { val colors @Composable get() = LocalJancarColors.current }`; `object Dimens { val MinTouch = 48.dp; … }`.

## T2.4 ThemeStore (teste primeiro)
- `settings/ThemeStoreTest.kt` usando `PreferenceDataStoreFactory.create(scope = TestScope(), produceFile = { tmpFile })` + `kotlinx-coroutines-test`: padrão `true`; `setDark(false)` → `dark.first() == false`.
- `settings/SettingsStore.kt`: wrapper do DataStore (construtor recebe `DataStore<Preferences>` para ser testável) com `val dark: Flow<Boolean>`, `suspend fun setDark(Boolean)`. Instância única em `JancarApp : Application` (adicionar `android:name` no manifesto).

## T2.5 Ícones
`ui/icons/Icons.kt`: um `ImageVector` por ícone usando `path { … }` com os `d` do preview (copiar de `app/Jancar launcher — preview visual.html`, pág. Main/Drawer). Usar `ImageVector.Builder(defaultWidth = 24.dp, …, viewportWidth = 24f)` com `stroke = SolidColor(Color.Black)` e `strokeLineWidth = 1.6f`, `strokeLineCap = Round`; tingir com `Icon(tint)`. Escrever um helper `strokeIcon(name) { … }` para reduzir repetição. Pontos da gaveta usam `fill`.

## T2.6 Componentes
`ui/components/`: `JIconButton(icon, desc, onClick)`, `QuickChip(icon, label, onClick)`, `MainTile(icon, label, onClick)`, `AppTile(icon, name, tag, enabled, onClick)`; modifier `pressScale()` com `animateFloatAsState` 0,98; `semantics { contentDescription }` e `focusable()` com borda de acento. Alvos ≥ 48 dp.

## T2.7 Galeria de depuração
`debug/GalleryActivity` no source set `debug` (ou um `if (BuildConfig.DEBUG)` na MainActivity via ação) mostrando todos os componentes em escuro e claro lado a lado. Ligar `MainActivity` ao `ThemeStore` e ao `JancarTheme` (carregamento assíncrono: `val dark by store.dark.collectAsState(initial = null)`; se null, só o fundo).

## T2.8 Verificação
`./gradlew testDebugUnitTest assembleDebug`; instalar no AVD 1024×600; comparar a galeria com o preview lado a lado; alternar e reabrir o app confirmando persistência. Commit: `feat: tema, tipografia, ícones e componentes base`.
