# Fase 8 — Modo noturno automático (spec)

Supersede a linha "tema por horário" marcada fora de escopo na Fase 7 (decisão revertida pelo usuário em 2026-11-05).

## Objetivo
Tema escuro e brilho reduzido aplicados automaticamente das 20:00 às 05:30, revertendo fora dessa janela, sem sensor de luz (hardware não expõe).

## Requisitos
1. **`NightModePolicy`** (`settings/`), função pura: dado um `LocalTime`, devolve se está na janela noturna (20:00–05:30, cruza a meia-noite). Testável sem Android.
2. **Checagem**: na abertura do app (pode já nascer dentro da janela, ex. boot às 22h) e a cada tick de minuto (reaproveitar o `BroadcastReceiver` de `ACTION_TIME_TICK` já usado em `rememberClock`, não criar um novo).
3. **Tema**: ao entrar na janela, aplica escuro; ao sair, volta pro tema de antes da última transição automática. Toggle manual do usuário dentro da janela **não é revertido** pela automação até a próxima transição (20:00 ou 05:30) — evita brigar com o usuário.
4. **Brilho**: ao entrar na janela, aplica um nível noturno fixo (`NIGHT_BRIGHTNESS = 35`, constante ajustável) via `BrightnessController`/`BrightnessMapper` existentes, guardando o brilho anterior; ao sair, restaura. Se o usuário nunca escolheu brilho (`userBrightness == null`), a automação não mexe no brilho do sistema, só na janela do app (mesmo comportamento já existente para brilho).
5. **Persistência**: `SettingsStore` ganha os campos necessários para saber se a última transição aplicada foi "entrada" ou "saída" da janela, e o tema/brilho anteriores à última entrada — para não perder o estado se o processo morrer e reabrir dentro da janela.

## Decisões minhas
- Janela fixa 20:00–05:30 (pedido do usuário), sem configuração nesta versão.
- `NIGHT_BRIGHTNESS = 35%` como ponto de partida; é só uma constante, fácil de ajustar depois.
- A automação roda só com o app em primeiro plano (sem `WorkManager`/serviço em segundo plano) — o launcher fica sempre visível por trás de qualquer app, então o tick de minuto já cobre o caso.

## Fora de escopo
Horário configurável pelo usuário; sensor de luz ambiente; sincronizar com pôr do sol.

## Aceite
1. `NightModePolicyTest`: dentro/fora da janela em vários horários, incluindo a borda 20:00/05:30 e a virada da meia-noite.
2. No emulador: ajustar o relógio do sistema para 19:59 → 20:00 e ver o tema escurecer e o brilho cair; ajustar para 05:29 → 05:30 e ver reverter.
3. Reabrir o app com o relógio já dentro da janela (sem transição observada) aplica o estado noturno imediatamente.
4. Trocar o tema manualmente às 21:00 não é revertido até 05:30.
