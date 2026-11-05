# Fase 7 — Acabamento (spec)

Mestre: §2, §8, §9. Depende de todas as anteriores.

## Objetivo
Deixar o launcher estável por semanas na central, leve, polido e com as pendências finais resolvidas.

## Requisitos
1. **Memória e estabilidade:** PSS do app ≤ ~70 MB (`dumpsys meminfo com.rocha.jancarlauncher`) logo após abrir e **sem crescimento** após ≥ 2 h de uso simulado (alternar telas, trocar tema, abrir/voltar de apps). Sem vazamento de receivers/callbacks/bitmaps (LeakCanary só no AVD debug, **não** no APK da central).
2. **Desempenho:** abrir a Home em < 1,5 s a partir do toque no botão Home; sem quedas visíveis de quadros nas transições; sem trabalho periódico além do tick de minuto.
3. **Tamanho/build:** release com `isMinifyEnabled` + `isShrinkResources` testado (regras R8 para Compose/DataStore); **debug continua sendo o APK instalado** (central não valida assinatura), mas o release minificado precisa funcionar caso se prefira usá-lo.
4. **Pendências não-bloqueantes** (da Fase 0): preencher pacotes de Scrcpy/OBD-II; decidir o destino do chip "Câmera de ré" (manter, ocultar ou avisar); decidir "Vídeo" definitivo; confirmar desativação de ADB sem fio e registrar em `INSTALL.md`.
5. **Textos e acessibilidade:** strings em `strings.xml` pt-BR; `contentDescription` em todos os botões de ícone; contraste do texto `muted`/`faint` sobre as superfícies (verificar; se `faint #53565c` sobre `#0a0b0f` falhar em 4,5:1, **manter o hex do usuário** e só registrar a observação — a paleta é decisão dele).
6. **Ícone do app:** ícone adaptativo simples em tom prata/azul-aço (ou o fornecido pelo usuário).
7. **Qualidade:** rodar `/code-review` na árvore final; remover logs de depuração e a galeria da Fase 2 do release; `./gradlew lint` sem erros novos.
8. **Documentação:** `README.md` (o que é, como compilar, como instalar, link para `INSTALL.md`), `app/CLAUDE.md` atualizado com as decisões fechadas e os pacotes confirmados.

## Fora de escopo
Novas funcionalidades (widgets, múltiplas páginas, tema por horário — **revertido em 2026-11-05**, ver `2026-11-05-phase-8-modo-noturno-design.md`).

## Aceite = Definição de pronto do plano mestre
- Roda na central real sem crash após reinício e após desativar um app-alvo.
- 4 blocos abrem o destino certo sem diálogo.
- Auto/CarPlay decidido antes do `startActivity`.
- Tema persiste; brilho funciona ou o fallback está documentado.
- Alvos de toque ≥ 44 dp; hex exatos.
- Rollback testado e documentado.
- Memória dentro da meta e estável por horas.
