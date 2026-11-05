# Fase 4 — Telas: Boas-vindas, Home e Gaveta (spec)

Mestre: §5, §5.1, §6.3–6.6. Depende das Fases 2 e 3.

## Objetivo
As três telas completas, fiéis ao preview HTML, ligadas ao tema e ao lançador.

## Requisitos
1. **Navegação:** estado `Screen { Welcome, Home, Drawer }` em `rememberSaveable`, sem biblioteca de navegação. `BackHandler`: Gaveta→Home; Home não sai (é a HOME); Boas-vindas→ignora.
2. **Boas-vindas:** conforme spec mestre §5.1 (fundo `boot-background`, degradê, "MULTIMÍDIA", relógio 88 sp, data, "Toque para continuar"). Toque em qualquer ponto → Home. **Quando aparece (decidido pelo usuário):** a central **sai de suspensão** ao ligar o carro, não reinicia por completo. Portanto a Boas-vindas aparece (a) na primeira abertura após reinício (`Settings.Global.BOOT_COUNT` ≠ valor guardado) **e** (b) ao acordar da suspensão depois de ≥ 10 min com a tela apagada (N=10 é padrão meu, ajustável numa constante). Detecção por `ACTION_SCREEN_OFF`/`ACTION_SCREEN_ON` registrados dinamicamente no `Application` (o manifesto não aceita esses broadcasts). **Risco a validar no aparelho:** a central pode "dormir" por um broadcast próprio do MCU (ACC) sem apagar a tela via esses eventos; se for o caso, trocar o gatilho (descobrir com `adb shell dumpsys power` / `logcat` ao desligar o carro).
3. **Relógio e data:** hora local 24 h (`HH:mm`), data em pt-BR ("Quinta-feira, 5 de novembro", capitalizada). Atualização por `ACTION_TIME_TICK` (receiver dinâmico enquanto a tela está visível; sem loops de `delay`), também `ACTION_TIMEZONE_CHANGED`/`TIME_SET`.
4. **Home:** layout do spec §5.1. Botões topo: tema (persiste), brilho (abre/fecha painel), gaveta. 4 blocos: Auto → `openAuto()`, Música, Rádio, Vídeo (Fase 3). Chips: Wi-Fi → `Settings.ACTION_WIFI_SETTINGS`; Bluetooth → `ACTION_BLUETOOTH_SETTINGS`; GPS → `ACTION_LOCATION_SOURCE_SETTINGS`; Câmera de ré → alvo `BACK_CAMERA`; Ajustes → `ACTION_SETTINGS`. Cada um com fallback para `ACTION_SETTINGS` e depois toast. **Todos via `AppLauncher`/helper protegido.**
5. **Painel de brilho:** 260 dp, slider 10–100 %, rótulo "NN%". Comportamento: tentar `Settings.System.SCREEN_BRIGHTNESS` se `Settings.System.canWrite`; senão `window.attributes.screenBrightness` da Activity (vale só enquanto o launcher está na frente — **limitação aceita e sinalizada discretamente**). Valor guardado em DataStore. Fecha ao tocar fora.
6. **Gaveta:** cabeçalho (voltar, "Todos os apps", "N apps instalados", tema) e grid de 6 colunas dos 12 apps do preview (nomes curtos e tags do §5.1). "N apps instalados" = quantos estão realmente instalados e habilitados. App ausente/desativado: tile com 40% de opacidade, sem ação, e toast ao tocar ("Não instalado"). Ícones: os vetores da Fase 2 (não os ícones do app instalado — mantém o visual uniforme e é mais leve).

## Fora de escopo
Alterar a regra de Auto/CarPlay; card de player (removido).

## Decisões minhas
- Limite de 10 min de tela apagada para reexibir a Boas-vindas (o gatilho de suspensão foi decidido pelo usuário).
- Ícones vetoriais próprios na gaveta, não os do app.
- Controle de brilho com fallback de janela.

## Aceite
- Comparação visual com o preview em 1024×600 @160dpi: medidas do §5.1 dentro de ±2 dp.
- Tema alternado reflete nas 3 telas e persiste.
- Relógio muda no minuto sem travar; sem coletores/receivers vazando (conferir `onDispose`).
- Chips e blocos abrem destinos corretos no aparelho; destino ausente nunca crasha.
- Todos os alvos ≥ 48 dp; navegação por D-pad percorre os elementos na ordem visual.
