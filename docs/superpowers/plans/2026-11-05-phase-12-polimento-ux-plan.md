# Fase 12 — Plano

Spec: `../specs/2026-11-05-phase-12-polimento-ux-design.md`.

## T12.1 Transições de tela
Em `AppRoot.kt` (onde a troca Boas-vindas/Home/Gaveta acontece), envolver a troca de `screen` num `AnimatedContent` com `fadeIn()+fadeOut()` (~180 ms). Sem teste de unidade (é visual); checar manualmente.

## T12.2 `Pressable` com toque longo (teste primeiro onde der)
Estender `ui/components/Components.kt`: `Pressable(onClick, onLongClick: (() -> Unit)? = null, ...)` usando `Modifier.combinedClickable`. Mantém toque mínimo 44–48 dp existente. Teste de unidade possível só pra lógica que decide habilitar/desabilitar o long-click (se houver), não pro gesto em si (isso é instrumentado/manual).

## T12.3 Estado de "abrindo" nos blocos principais
`ui/home/HomeScreen.kt`: ao tocar num dos 4 blocos, setar um estado local `opening: Boolean` (feedback visual: opacidade reduzida) até `LaunchController` retornar sucesso/erro, com um teto de ~800 ms (usar `LaunchedEffect` com `delay` como salvaguarda, caso o retorno nunca chegue). Erro → estado volta a `false` e o toast de erro já existente aparece.

## T12.4 Toque longo nos 4 blocos
Plugar `onLongClick` nos 4 blocos principais (sem ação por enquanto, conforme spec — só o suporte técnico). Deixar um comentário `// TODO(issue): atalho por bloco` apontando pra onde adicionar ações específicas depois.

## T12.5 Verificação no emulador
Navegar entre as 3 telas observando a transição; tocar nos blocos observando o feedback de carregamento antes do app abrir (ou do toast de erro); toque longo não quebra o toque curto.

## T12.6 Commit
`feat: transições suaves, feedback de carregamento e suporte a toque longo`.
