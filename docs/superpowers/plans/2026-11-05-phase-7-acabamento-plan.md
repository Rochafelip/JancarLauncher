# Fase 7 — Plano

Spec: `../specs/2026-11-05-phase-7-acabamento-design.md`.

## T7.1 Medição de memória (baseline e soak)
Script `scripts/soak.ps1` (ou `.sh`) que, via ADB, a cada 5 min por ≥ 2 h: registra `adb shell dumpsys meminfo com.rocha.jancarlauncher | grep -E "TOTAL|Native Heap|Java Heap"` em `scratch/` (fora do repo ou ignorado) e dispara ações com `adb shell input tap/keyevent` (Home, abrir gaveta, alternar tema). Critério: tendência plana. Se crescer: investigar com `am dumpheap` + Android Studio Profiler no AVD (suspeitos: receivers, `MediaController.Callback`, bitmaps da capa, `remember` sem chave).

## T7.2 Verificação de vazamentos no AVD
Adicionar LeakCanary **apenas** como `debugImplementation` e remover do APK da central (ou usar um build type `leakcheck`). Percorrer fluxos: Home↔Gaveta, tema, painel de brilho, tocar mídia. Corrigir qualquer vazamento reportado e adicionar teste de regressão quando for lógica pura.

## T7.3 Desempenho
Medir tempo até a Home pintar (`adb shell am start -W`). Se > 1,5 s no aparelho: perfilar (`Trace`), reduzir trabalho no `Application.onCreate`, adiar leitura de DataStore, considerar **Baseline Profile** (gerar no AVD ARM/x86 conforme possível; ARMv7 pode não ser suportado pelo gerador — se não, pular e registrar). Revisar recomposições (Layout Inspector) nas telas com relógio.

## T7.4 Release e R8
`release { isMinifyEnabled = true; isShrinkResources = true }` + `proguard-rules.pro` (manter `BootReceiver`, classes referenciadas por manifesto — o AGP cuida; adicionar regras de DataStore/Compose se o build falhar). `./gradlew assembleRelease`, assinar com a chave de debug localmente só para teste, instalar e repetir smoke test. Registrar tamanho do APK debug vs release.

## T7.5 Pendências finais
Atualizar `AppTargets` com Scrcpy/OBD-II/Vídeo/Música conforme Fase 0; decidir e implementar o comportamento final do chip "Câmera de ré" (se `backcarapp` não aceitar abertura manual: ocultar o chip ou mostrar aviso). Registrar a decisão em `app/CLAUDE.md`.

## T7.6 Textos, acessibilidade e ícone
Mover strings literais para `res/values/strings.xml`; revisar `contentDescription`; ícone adaptativo em `res/mipmap-anydpi` (foreground vetorial prata, background `#0a0b0f`). Percorrer a UI só com D-pad/teclado no AVD para checar foco visível e ordem.

## T7.7 Revisão e limpeza
Rodar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`; remover logs de depuração e a galeria de componentes do release; executar `/code-review` e tratar os achados; `/simplify` se houver duplicação (ex.: ícones).

## T7.8 Documentação final
Criar `README.md` (resumo, build, instalação, link para `docs/INSTALL.md`, links para specs/planos); atualizar `app/CLAUDE.md` (pendências resolvidas, pacotes confirmados, decisões novas). Checklist da **Definição de pronto** marcada com evidências (saídas de comando/print) em `docs/superpowers/DONE.md`.

## T7.9 Commit/tag
`chore: acabamento` e tag `v1.0.0` (apenas se o usuário pedir versionamento git — o projeto hoje não é um repositório git).
