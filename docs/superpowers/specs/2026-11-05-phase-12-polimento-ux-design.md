# Fase 12 — Polimento de UX (spec)

## Objetivo
Transições suaves, feedback de carregamento ao abrir apps, e toque longo nos 4 blocos principais.

## Requisitos
1. **Transições**: `AnimatedContent`/`AnimatedVisibility` entre Boas-vindas→Home e Home↔Gaveta, fade ou slide curto (~150–200 ms). Testar em emulador com perfil de CPU reduzido (a central é ARMv7 2 GB); se engasgar, cair para fade simples.
2. **Loading ao abrir app**: entre o toque num bloco principal e o `startActivity` de fato acontecer (ou falhar), o bloco tocado mostra feedback visual (ex. leve pulsação ou opacidade reduzida) por até ~800 ms ou até a Activity ir para segundo plano, o que vier primeiro. Erro (`ActivityNotFoundException`) interrompe a animação e mostra o toast já existente.
3. **Toque longo** nos 4 blocos principais via `combinedClickable` no `Pressable` existente, mantendo toque mínimo 44–48 dp. Ação por bloco é opcional: onde não há atalho conhecido, toque longo não faz nada diferente do toque curto.

## Decisões minhas
- Toque longo nesta primeira versão não abre nenhum atalho específico (nenhum app-alvo confirmado aceita extras de Intent) — só fica o suporte técnico pronto (`onLongClick` disponível em `Pressable`), para plugar ações depois sem precisar mexer na UI de novo.
- Duração das transições e do feedback de loading são constantes nomeadas, fáceis de ajustar.

## Fora de escopo
Reordenar blocos por drag-and-drop.

## Aceite
1. Navegar Boas-vindas→Home→Gaveta→Home visualmente sem corte seco, sem queda perceptível de quadros no emulador.
2. Tocar num bloco mostra feedback antes do app abrir (ou do toast de erro aparecer).
3. Toque longo registra o evento (log ou teste de unidade do `onLongClick`) sem quebrar o toque curto.
