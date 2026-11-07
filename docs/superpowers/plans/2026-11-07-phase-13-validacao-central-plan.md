# Fase 13 — Plano

Spec: `../specs/2026-11-07-phase-13-validacao-central-design.md`. Executar **na central**, com
`adb connect 192.168.3.33:5555` (reconectar quando cair) e `adb logcat` abertos onde indicado.
Não é TDD — é checklist: cada item vira uma linha de evidência no `ledger.md` ao final (T13.15).

## T13.1 Instalação limpa

Seguir `docs/INSTALL.md` do início ao fim: build release, `adb install -r`, `set-home-activity`,
esperar os ~25s antes de reiniciar (achado da Fase 6). Anotar qualquer passo que não bateu com o
documento (versão de Android, mensagens diferentes, etc.) pra corrigir o `INSTALL.md` depois.

## T13.2 Boot como HOME

`adb reboot`. Cronometrar do boot até o launcher aparecer. Conferir: sem tela branca, HOME do
sistema aponta pro nosso app (`adb shell cmd package resolve-activity --brief
android.intent.category.HOME`), Boas-vindas aparece (1ª vez após reinício).

## T13.3 Os 4 blocos com apps reais

Tocar Auto, Música, Rádio, Vídeo um por um. Pra cada: abriu certo, sem diálogo de escolha (sem
`ResolverActivity`)? Se Música ou Vídeo abrirem errado (ou nada), essa é a confirmação final de que
`AppTargets.MUSIC`/`VIDEO` (preenchidos na sessão anterior) estão certos — se não bater, corrigir ali
mesmo com `dumpsys package` de novo.

## T13.4 Auto vs. CarPlay com hardware real

Parear um celular Android via Bluetooth → tocar "Auto" → confirmar que abre Android Auto. Trocar
pelo celular Apple (ou simular) → confirmar que abre CarPlay. Repetir conectando por cabo USB em vez
de Bluetooth. Documentar qualquer caso que a heurística (`ProjectionChooser`) errar.

## T13.5 Câmera de ré manual

Tocar o chip "Câmera de ré" fora da marcha-ré. Abriu, ou deu erro/não fez nada? Se não aceitar
abertura manual, decidir: deixar o chip mesmo assim (falha silenciosa) ou escondê-lo — registrar a
decisão no `app/CLAUDE.md`.

## T13.6 Brilho do sistema

`adb shell appops set com.rocha.jancarlauncher WRITE_SETTINGS allow` (comando já documentado no
`INSTALL.md`). Testar o slider: ele está mudando `Settings.System.SCREEN_BRIGHTNESS` de verdade ou
só a janela do app? Documentar qual dos dois caminhos é o real na central.

## T13.7 Boas-vindas ao ligar o carro

Desligar e ligar a central (ou simular apagando a tela por mais e por menos de 10 min, se não der
pra desligar o carro de verdade agora). Confirmar que a Boas-vindas aparece só quando apropriado.

## T13.8 Memória e CPU reais

`dumpsys meminfo com.rocha.jancarlauncher` em alguns momentos (abertura, depois de 10 min de uso,
depois de abrir/fechar a Gaveta várias vezes). Comparar com a meta (≤70MB) e com os números do
emulador (24-33MB) registrados na Fase 7.

## T13.9 "Tocando agora" com player real

Conceder acesso a notificações manualmente (Configurações → Apps → Acesso a notificações, se
existir na central). Tocar algo num player real (Música, Rádio, VLC) e conferir se o card aparece
com capa/título/artista certos e se os controles de transporte funcionam.

## T13.10 Indicador de ADB

Conferir se o indicador mostra "ADB ativo" / "ADB conectado" corretamente. Se não mostrar o IP,
confirmar que é porque `/proc/net/tcp` não é legível ali (não é bug, é o caminho já previsto).

## T13.11 Modo noturno

Se der pra esperar até perto de 20:00 ou 05:30, observar a transição ao vivo. Se não der, ajustar o
relógio do Android (`adb shell date` ou pelas Configurações) pra forçar passar pela borda, com
cuidado de voltar o relógio certo depois (isso pode mexer com o ADB/outras coisas que dependem de
hora — anotar se der algum efeito colateral).

## T13.12 Desativar ADB pelas Configurações

Procurar, nas Configurações da própria central, alguma opção de depuração sem fio. Documentar se
existe ou não — é a pendência mais antiga do projeto (desde o mapeamento inicial).

## T13.13 Calibração do CAN bus

Com o carro ligado:
1. Abrir `adb logcat -s CanBusClient` numa janela.
2. Abrir o launcher na Home (o `VehicleInfoCard` já está lá, chamado em `HomeScreen.kt`).
3. Conferir `connected` = true (bind no serviço funcionou).
4. Comparar velocidade/RPM mostrados com o painel físico, com o carro em movimento (ou pelo menos
   ligado, parado).
5. Abrir/fechar cada porta, puxar o freio de mão, apertar o cinto, ligar cada seta — um de cada vez,
   olhando se o número certo muda no card e no logcat (`restante=0` confirma que a leitura do Parcel
   terminou sem sobra; `restante!=0` indica campo faltando/sobrando na ordem).
6. Se algum valor não bater: ajustar a ordem dos campos em `readDashboard`/`readCabin`/`readLights`
   (`can/CanBusClient.kt`) e repetir o teste daquele bloco específico até bater.
7. Depois de calibrado: decidir se o `VehicleInfoCard` sai da `HomeScreen.kt` nesta fase ou fica
   para a próxima (ex: virar uma tela de diagnóstico separada, atrás de um gesto).

## T13.14 Smoke test final

Com tudo acima feito, usar o launcher por uns 10-15 minutos contínuos (abrir/fechar Gaveta, trocar
tema, ajustar brilho, tocar nos 4 blocos) com `adb logcat -s AndroidRuntime:E *:F` aberto, igual ao
smoke test das Fases 8-12 — mas agora na central de verdade.

## T13.15 Atualizar ledger.md e DONE.md

Uma entrada "Fase 13" no `ledger.md`, no mesmo formato das anteriores (o que foi feito, evidência
exata, o que ficou não verificado e por quê). Atualizar a tabela de critérios do `DONE.md` trocando
⚠/◐ por ✅ onde a central confirmou.

## T13.16 Atualizar app/CLAUDE.md

Seção "Integração CAN bus": trocar "ainda não validado" pelo resultado real. Pendências
não-bloqueantes: remover as que a central resolveu (câmera de ré, ADB pelas Configurações).

## T13.17 Commit

Um commit por correção de código que a central exigir (ex: `fix: corrige ordem de campos do
CanBusClient após calibração na central`), e um commit final de documentação (`docs: registra
validação na central real (Fase 13)`) cobrindo ledger.md, DONE.md e CLAUDE.md.
