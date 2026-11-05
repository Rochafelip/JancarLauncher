# Fase 2 — Tema, tokens, tipografia, ícones e componentes base (spec)

Mestre: §3, §5.1, §6.5, §8. Depende da Fase 1.

## Objetivo
Toda a camada visual reutilizável pelas telas: cores exatas das duas paletas, fonte Inter, ícones como `ImageVector`, componentes base (botão de ícone, chip, tile) e persistência do tema.

## Requisitos
1. **Tokens** (`JancarColors`): bg, fg, muted, faint, surface, surfaceHover, surfaceBorder, surfaceHoverBorder, icon, accent. Valores exatos de spec §3 e §5.1; acento `#6fa3d8` em ambos os temas. Exposto via `CompositionLocal` (`LocalJancarColors`), não via Material.
2. **Tipografia:** Inter 200/400/500/600 (TTF em `res/font/`). Estilos nomeados: `clockLarge` (92 sp/200, ls −2), `clockBoot` (88 sp/200), `date` (18), `caption` (13/500, ls 2,5), `title` (16/600), `body` (13), `tileLabel` (15/500), `chip` (13/500), `appName` (14/500), `appTag` (10,5/500, caixa alta), `h1` (26/500).
3. **Tamanhos:** toque mínimo 48 dp (`MinTouch`); raios (botão 14, chip 9999, tile 22, app-tile 18, card 20, painel 16); espaçamentos do spec §5.1.
4. **Ícones** (`ui/icons`): converter os SVG 24×24 do preview em `ImageVector`: sol, lua, brilho (sliders), gaveta (9 pontos), voltar, anterior, play, pause, próxima, nota musical, Wi-Fi, Bluetooth, GPS, câmera, ajustes, Auto (monitor), rádio, vídeo, seta para cima, e os ícones dos 12 apps da gaveta. Traço 1,6–1,8, cantos arredondados, `currentColor` → `tint` do tema.
5. **Componentes:** `JIconButton` (48×48), `QuickChip`, `MainTile`, `AppTile`. Estado pressionado: escala 0,98 e ícone em acento. Foco (D-pad/rotary): borda de acento visível — a central pode ter botões físicos.
6. **Persistência:** `ThemeStore` (DataStore Preferences, chave `dark_theme`, padrão `true`). Mesmo mecanismo guardará brilho (fase 4) e o contador de boot (fase 4).
7. `JancarTheme(dark: Boolean, content)` provê cores + tipografia.

## Decisões minhas
- Sem Material3 (memória). Sem ripple padrão (`indication = null` + escala própria) — mais barato.
- Tema carregado assincronamente; até carregar, renderiza fundo escuro vazio (evita piscar errado; ver plano).
- Ícones sem biblioteca (`material-icons-extended` é pesada).

## Aceite
- Teste unitário: round-trip do `ThemeStore` (padrão escuro; grava claro; lê claro).
- Teste unitário: paletas têm exatamente os hex do spec (teste de “golden values”).
- Tela de galeria de depuração (só debug) mostra todos os componentes nos 2 temas e se compara bem com o preview.
- Todos os alvos de toque ≥ 44 dp (48 dp nos botões/ chips/ tiles).
