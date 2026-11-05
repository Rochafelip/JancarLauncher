# Instalação do JancarLauncher na central Jancar 8227L

Guia para instalar o launcher **sem root**. A central não tem seletor de launcher nas Configurações, então tudo é feito por ADB.

> **Regra de ouro:** guarde e teste o comando de **rollback** (seção 3) *antes* de definir o nosso app como HOME.

## 0. Pré-requisitos

- Computador com `adb` (platform-tools do Android SDK).
- Central e computador na mesma rede Wi-Fi (a central só usa 2,4 GHz). ADB sem fio: `<IP_DA_CENTRAL>:5555`, sem senha.
- O ADB sem fio **cai com frequência**: se um comando falhar com `no devices/emulators found`, rode `adb connect <IP_DA_CENTRAL>:5555` de novo e repita.
- APK de debug (a central não valida assinatura): `app/build/outputs/apk/debug/app-debug.apk`, gerado com `./gradlew assembleDebug`.

## 1. Conectar e anotar o HOME atual

```
adb connect <IP_DA_CENTRAL>:5555
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Deve mostrar o launcher de fábrica (`com.android.launcher3/com.jancar.launcher.act.ActLauncher`). Se mostrar outro, **use o que aparecer** no rollback.

## 2. Instalar

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rocha.jancarlauncher/.MainActivity
```

Abra pelo comando acima e confira as telas **antes** de definir como HOME. Atualizar uma versão nova usa o mesmo `adb install -r` e mantém a definição de HOME e as preferências.

## 3. Rollback (guarde este comando)

```
adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher
```

Teste uma vez: defina o nosso (seção 4), aperte Home, rode o rollback, aperte Home de novo e confirme que voltou o launcher de fábrica.

## 4. Definir o JancarLauncher como HOME

```
adb shell cmd package set-home-activity com.rocha.jancarlauncher/.MainActivity
```

**Espere uns 30 segundos antes de reiniciar ou desligar a central.** O sistema grava essa preferência em disco com atraso: no emulador, reiniciar logo depois do comando perdeu a definição (apareceu o seletor "Usar JancarLauncher como Home"); esperando ~25 s ela sobreviveu ao reboot. Confira:

```
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Deve listar `com.rocha.jancarlauncher/.MainActivity`.

## 5. Permissão opcional: brilho do sistema

Sem ela o slider de brilho muda só a janela do launcher (vale enquanto ele está na frente). Com ela, grava o brilho do sistema:

```
adb shell appops set com.rocha.jancarlauncher WRITE_SETTINGS allow
```

*Não verificado na central.* O launcher nunca mexe no brilho antes de você mexer no slider.

## 6. Desligar a depuração sem fio

A porta 5555 fica aberta **sem senha**. Quando terminar:

```
adb disconnect
```

e desligue a depuração sem fio nas configurações da central, se a tela existir (pendência do projeto).

## 7. Se algo der errado

| Sintoma | O que fazer |
|---|---|
| Aparece "Usar JancarLauncher como Home" | Escolha "Sempre" ou rode a seção 4 de novo e espere 30 s. |
| Tela do launcher quebrada ou travada | Rode o rollback (seção 3). Pelo botão físico de Home dá para voltar ao launcher de fábrica depois. |
| ADB sem fio não conecta | `adb connect <IP_DA_CENTRAL>:5555`; se persistir, use ADB por USB. |
| Quero desinstalar | Rode o rollback **primeiro**, depois `adb uninstall com.rocha.jancarlauncher`. |
| O launcher fecha sozinho | Ele reabre automaticamente até 3 vezes por minuto; acima disso o próprio sistema reabre a Home. Veja `adb logcat -s JancarCrash`. |

## 8. Comportamento esperado

- **Ligar a central (boot):** o sistema abre o launcher como HOME e a Boas-vindas aparece uma vez por reinício.
- **Acordar da suspensão:** a Boas-vindas reaparece se a tela ficou apagada por 10 minutos ou mais.
- **Botão Home:** de qualquer app volta à Home do launcher.
- **Crash:** reabre a Home automaticamente (máx. 3 por minuto).

## 9. Verificado em

Emulador Android 15 (API 35), tela 1024×600 a 160 dpi — **2026-11-05**, build debug:

- [x] Definir HOME e botão Home abre o launcher
- [x] Rollback: botão Home volta ao launcher original
- [x] Reiniciar o aparelho: o sistema abre o launcher e a Boas-vindas aparece
- [x] `force-stop` + botão Home: reabre
- [x] Crash forçado: reabre até 3 vezes; o 4º e o 5º dentro de 1 minuto são negados
- [x] Definição de HOME sobrevive ao reboot (esperando ~25 s depois do comando)
- [ ] **Na central real (firmware `JCAC10003-OC2-V1.1.62R0`, API 27):** nada disto foi testado ainda. Repita a lista acima e anote o resultado aqui.
