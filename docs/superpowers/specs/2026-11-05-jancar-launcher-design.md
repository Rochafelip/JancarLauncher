# JancarLauncher — Design (spec)

Data: 2026-11-05 · Fonte: `app/CLAUDE.md` (decisões do usuário) + estado atual do projeto.
Status: **rascunho para aprovação** — nada de código de produto foi escrito.

## 1. Intenção

Launcher Android minimalista que substitui o launcher de fábrica da central automotiva **Jancar 8227L** do VW Fox do usuário. A central não tem root nem seletor de launcher; o usuário quer abrir direto nas funções do dia a dia (Android Auto/CarPlay, Rádio, Música, Vídeo) com visual prata/azul-aço inspirado na lataria e nos faróis de LED do carro.

## 2. Objetivos / Não-objetivos / Anti-objetivos

**Objetivos**
- Abrir sozinho no boot, em ≤ 1 toque chegar a Auto, Música, Rádio ou Vídeo.
- Tela Home com relógio, chips de sistema e 4 blocos principais.
- Gaveta com apps secundários; tema claro/escuro persistido.
- Leve (2 GB RAM, ARMv7, 1024×600 @160dpi).

**Não-objetivos**
- Não há suporte a root, widgets de terceiros, múltiplas telas/páginas, loja, nem outras resoluções além de ajustes razoáveis.
- Não instalar/gerenciar apps (só lançar).

**Anti-objetivos (falha mesmo que "funcione")**
- Travar/crashar o launcher porque um pacote-alvo sumiu (é a tela HOME: crash = central inutilizável).
- Abrir diálogo de escolha (`ResolverActivity`) ao tocar num bloco.
- Escolher Android Auto/CarPlay por tentativa-e-erro após `startActivity`.
- Alvos de toque < 44 dp; consumo de RAM/CPU perceptível (animações pesadas, blur em tempo real).

## 3. Restrições (do usuário, não reabrir)

- Kotlin + Jetpack Compose (não XML/Views). `minSdk 27`; tudo deve funcionar na API 27. Build debug basta.
- Sem root (`ro.secure=1`). Launcher definido só via `adb shell cmd package set-home-activity`; ADB Wi-Fi (`<IP_DA_CENTRAL>:5555`) é instável.
- Launcher-substituto (não atalho): `BOOT_COMPLETED` + categoria `HOME`/`DEFAULT`.
- Detecção Auto vs CarPlay por Bluetooth (dispositivos pareados) + estado USB, decidida **antes** do `startActivity`.
- **Sem card "Tocando agora" e sem acesso a notificações** (decisão do usuário: o mais limpo possível, sem permissões especiais).
- Todo `startActivity` protegido por `try/catch ActivityNotFoundException`.
- `com.autochips.carplayapp` / `.CarPlayActivity` para CarPlay — **nunca** `com.autochips.carplay`.
- Paleta exata: escuro `#0a0b0f/#eef0f2/#888d94/#53565c/#15161b/#1c1e24/#c9cdd3`; claro `#f1f2f4/#16181c/#696d73/#9a9da3/#fbfbfc/#e9eaed/#3b3e44`; acento `#6fa3d8`. Tema escuro é o padrão.
- Referências visuais: `app/Jancar launcher — preview visual.html` (3 telas, lido e transcrito no §5.1) e `app/Central Jancar 8227L — ficha técnica e pesquisa de root.docx` (lido; achados no §3.1). Os links de artifact do `CLAUDE.md` são os mesmos conteúdos.

### 3.1 Achados da ficha técnica da central (.docx)
- Firmware `JCAC10003-OC2-V1.1.62R0-250423_0757`; patch de segurança 05/10/2021; kernel 3.18; fingerprints inconsistentes (system 6.0 / vendor 12 / API real 27). **Não confiar em `Build.VERSION.RELEASE`** (diz "12"); usar `SDK_INT` (27) e testar comportamento no aparelho.
- **Memória é o limite duro:** só ~320–360 MB de RAM livres no sistema inteiro (+~400 MB de zram), carga média ~9,7 em 4 núcleos, central ligada 78 dias sem reiniciar, ~59 °C. O launcher precisa ser muito frugal e **resistir a ficar semanas em memória** (sem vazamentos; atualizações de relógio/player baratas).
- `/system` 89% cheio: o app vai em `/data` normalmente (APK de usuário); nada de instalá-lo como app de sistema.
- Já desativados com `pm disable-user` (reversível): **Play Store, YouTube, Maps, Chrome, OTA**, gravador, visualizador de HTML, calendário, seletores de papel de parede, logs MTK, etc. Consequências: (a) nenhum atalho/fallback do launcher pode depender desses pacotes; (b) papel de parede nativo não existe — o launcher usa o próprio fundo; (c) o launcher deve tratar "pacote desativado" como caso normal, não exceção (`PackageManager.getApplicationEnabledSetting`).
- ADB na 5555 sem autenticação e `ro.debuggable=1`: sem impacto no app, mas o `INSTALL.md` deve lembrar de desligar a depuração sem fio após o uso.
- Sem root e sem método de root confirmado → reforça o anti-objetivo "nenhuma operação pode depender de root".

## 4. Estado atual do projeto

Esqueleto do Android Studio: AGP 9.2.1, `compileSdk 36`, `minSdk 27`, namespace/appId `com.rocha.jancarlauncher`. Dependências são só appcompat/material/core-ktx; **não há Compose, nem Activity, nem manifesto com `<activity>`**, nem Kotlin de produção. `AndroidManifest.xml` só tem `<application>` vazio.

## 5. Telas

1. **Boas-vindas** — foto do carro (tratada: desfoque + escurecimento + duotone azul-acinzentado) de fundo, relógio centralizado, "toque para continuar" → Home. Aparece ao ligar o carro.
2. **Home** — relógio grande + data (topo esq.); ícones topo dir.: tema, brilho (painel com slider), gaveta; chips: Wi-Fi, Bluetooth, GPS, Câmera de ré (manual), Ajustes; 4 blocos grandes: Auto, Música, Rádio, Vídeo.
3. **Gaveta de apps** — grid: VLC, NewPipe, F-Droid, Lightning Browser, Galeria, Arquivos, Bluetooth, Música BT, EQ, Informações do veículo, Scrcpy for Android, Android OBD-II Reader; cada um com ícone, nome e tag "App"/"Sistema".

### 5.1 Medidas e detalhes extraídos do preview HTML
O preview é 1024×600 a 160 dpi, então **1 px do preview = 1 dp**. Fonte **Inter** (pesos 200, 400, 500, 600). Tokens adicionais que o `CLAUDE.md` não tinha:

| Token | Escuro | Claro |
|---|---|---|
| surface-border | `rgba(255,255,255,0.07)` | `rgba(16,18,22,0.08)` |
| surface-hover-border | `rgba(255,255,255,0.13)` | `rgba(16,18,22,0.14)` |

**Atenção — inconsistência do preview:** o `renderVals` da Home usa `#e3a458` (âmbar) como fallback de acento, mas o `default` declarado e o `CLAUDE.md` dizem `#6fa3d8`. **Vale `#6fa3d8`** (o fallback é um descuido do protótipo).

**Boas-vindas:** fundo `boot-background.jpg` (1024×600, 27 KB, já desfocado/escurecido/duotone — **já extraído para `docs/reference/boot-background.jpg`**, resolvendo a pendência da foto) + degradê vertical `rgba(10,11,15,.3) → .6 (68%) → .9`; sobre ele, legenda "MULTIMÍDIA" (13 sp, peso 500, espaçamento 3, 55% opacidade), relógio 88 sp peso 200, data 16 sp, e no rodapé (44 dp da base) seta para cima + "Toque para continuar".

**Home** (padding 52/64/48 dp; coluna): topo — relógio 92 sp peso 200 (letter-spacing −2) + data 18 sp muted à esquerda; à direita legenda "MULTIMÍDIA" (13 sp, espaçamento 2,5, faint) acima de 3 botões 48×48, raio 14 (tema, brilho, gaveta). Sem card de player: o espaço entre o topo e os chips fica livre. **Chips** (pílula, padding 15/22, ícone 18 + rótulo 13 sp/500, gap 12) e embaixo **4 blocos** em grid de 4 colunas (gap 18, raio 22, padding 36/16, ícone 40, rótulo 15 sp/500). Estado de toque: escala 0,98; ícone vira acento ao pressionar/foco. **Painel de brilho**: 260 dp de largura, raio 16, ancorado a 146 dp do topo e 64 dp da direita, título "Brilho" + "NN%", slider de 10 a 100.

**Gaveta** (padding 44/64/40): cabeçalho com botão voltar 48×48, título "Todos os apps" (26 sp/500) e subtítulo "N apps instalados", botão de tema à direita; grid de **6 colunas** (gap 16), tile raio 18, ícone 30, nome 14 sp/500 e tag em caixa alta 10,5 sp faint. Apps e tags no preview: VLC, NewPipe, F-Droid, Lightning, Scrcpy, OBD-II = **App**; Galeria, Arquivos, Bluetooth, Música BT, EQ, Veículo = **Sistema** (12 itens; nomes curtos, ex. "Veículo", "Lightning", "Scrcpy", "OBD-II").

**Fora do protótipo (decisão minha):** os botões anterior/próxima e os chips não têm comportamento no preview; seguem o §6. A tag do rótulo "MULTIMÍDIA" é só decorativa. Ícones são traçados SVG de 24×24 — **converter para `ImageVector`** (os caminhos estão no HTML). O texto de exemplo da data ("Quarta-feira, 5 de novembro") e a hora (18:37) são fictícios; usar o relógio real em pt-BR.

## 6. Comportamento

### 6.1 Lançamento dos blocos
| Bloco | Destino | Estado |
|---|---|---|
| Auto | `com.google.android.projection.sink/.ui.AndroidAutoActivity` ou `com.autochips.carplayapp/.CarPlayActivity` | Confirmado |
| Rádio | `com.jancar.radio/.activity.RadioMainActivity` | Confirmado |
| Música | `com.jancar.music` — activity desconhecida | **Bloqueado** |
| Vídeo | desconhecido | **Bloqueado** |

Os destinos ficam **numa tabela de configuração única** (`AppTargets`), para que resolver os bloqueios seja trocar uma linha. Música e Vídeo: **activity fixa na tabela, escolhida pelo usuário** (sem descoberta automática nem fallback por pacote). Enquanto o usuário não informar os nomes (Fase 0), a entrada fica nula e o bloco mostra o toast "não configurado".

### 6.2 Auto vs CarPlay
Função pura `chooseProjection(bondedDevices, usbState, userOverride?) -> AndroidAuto | CarPlay`. Regra exata (como o usuário definiu só "por Bluetooth + cabo"): **[minha escolha, a validar na central]** pareado marcado como iPhone/Apple (nome ou classe) → CarPlay; pareado Android → Android Auto; ambíguo ou nenhum → Android Auto. Heurística isolada e testável; trocar a regra não toca a UI.

### 6.3 Tocando agora — removido
Decisão do usuário: sem card de player e **sem `NotificationListenerService`** (o app não pede nenhuma permissão especial). Consequência aceita: a Home não mostra o que está tocando; os controles de mídia ficam nos apps nativos.

### 6.4 Chips
Wi-Fi, Bluetooth, GPS, Ajustes abrem as telas de Settings correspondentes (`Settings.ACTION_*`) com fallback para Ajustes gerais. Câmera de ré abre `com.autochips.backcarapp` (não confirmado que aceita abertura manual; falha silenciosa com toast).

### 6.5 Tema e brilho
Toggle sol/lua persiste em DataStore (ou SharedPreferences — **minha escolha: DataStore**). Brilho: painel com slider que usa `Settings.System.SCREEN_BRIGHTNESS`, exigindo `WRITE_SETTINGS`; **[minha escolha]** se a permissão não puder ser concedida, usar `WindowManager.LayoutParams.screenBrightness` só da janela do launcher e informar isso discretamente.

### 6.6 Boot
`BroadcastReceiver` em `BOOT_COMPLETED` inicia a Home. Na API 27 o launcher definido como HOME já é iniciado pelo sistema; o receiver é cinto-e-suspensório. Boas-vindas é mostrada uma vez por boot (flag em memória/`SystemClock`), não a cada volta à Home.

### 6.7 Segurança de HOME
Nenhum caminho de código pode lançar exceção não tratada na Home. Botão Home do sistema retorna à Home. Comando de rollback guardado em `docs/` e no README.

## 7. Arquitetura (decisão minha, simples por ser app pequeno)

Módulo único `app`. Pacotes: `ui/` (telas + tema Compose), `launch/` (`AppTargets`, `AppLauncher`, `chooseProjection`), `settings/` (tema/brilho/DataStore), `boot/` (receiver). Um `ViewModel` por tela com `StateFlow`; sem Hilt/Room/Retrofit (YAGNI). Compose sem Material3 pesado onde evitável; tokens de cor em objeto próprio com as duas paletas.

## 8. Desempenho

Sem blur em tempo real: o fundo da Boas-vindas é o **asset pré-tratado já existente** (`boot-background.jpg`, 27 KB). Animações curtas e baratas. Ícones de apps carregados via `PackageManager` com cache em memória pequeno (12 itens). **Orçamento de memória [minha meta]:** PSS do app ≤ ~70 MB, dado que o sistema inteiro tem só ~350 MB livres; estabilidade ao longo de semanas (sem crescer). Sem Material3/bibliotecas pesadas: Compose `foundation` + `ui`. Fonte Inter embutida só nos 4 pesos usados (TTF, licença OFL; incluir o texto da licença).

## 9. Testes e validação

- Unit: `chooseProjection`, tabela `AppTargets`, mapeamento de estado do player.
- Instrumentado/manual: instalação no aparelho real via `adb install`, `set-home-activity`, verificação de rollback. Emulador API 27 1024×600 @160dpi para a UI.
- Critério de aceite: ver plano, seção "Definição de pronto".

## 10. Pendências

Bloqueantes: activity de `com.jancar.music`; pacote/activity de Vídeo.
Não-bloqueantes: pacotes de Scrcpy e OBD-II; `backcarapp` manual; desativar ADB sem fio.
Resolvido: a foto do carro (extraída do preview). Falta do usuário: o ícone final do app (usar o padrão do Studio até lá).

## 11. Escolhas minhas (marcadas para revisão)

DataStore; heurística Auto/CarPlay (§6.2); fallback de brilho (§6.5); arquitetura de módulo único sem DI; Música/Vídeo com activity fixa informada pelo usuário (nula = "não configurado"); asset pré-tratado em vez de blur em runtime.
