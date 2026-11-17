package com.rocha.jancarlauncher.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rocha.jancarlauncher.BuildConfig
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.system.update.Release
import com.rocha.jancarlauncher.system.update.UpdateRules
import com.rocha.jancarlauncher.system.update.Updater
import com.rocha.jancarlauncher.system.update.Verdict
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType
import kotlinx.coroutines.launch
import java.io.File

/** Ajustes › Atualizar o launcher: pelo GitHub (última release) ou por um APK do pendrive. */
@Composable
fun UpdateSection() {
    val app = LocalContext.current.applicationContext as JancarApp
    val c = JTheme.colors
    val updater = remember { Updater(app) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var release by remember { mutableStateOf<Release?>(null) }
    var busy by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<List<File>>(emptyList()) }

    fun installChecked(apk: File?) {
        val v = UpdateRules.verdict(apk?.let(updater::inspect), updater.installed())
        app.log.add("update", "arquivo ${apk?.length() ?: 0} B: $v")
        status = when (v) {
            Verdict.Ok -> if (updater.canInstall()) null else NO_PERMISSION
            Verdict.Unreadable -> "Não deu para ler esse APK."
            Verdict.OtherPackage -> "APK de outro aplicativo."
            Verdict.OtherSignature -> "APK assinado com outra chave."
            Verdict.NotNewer -> "Esse APK não é mais novo que o instalado."
        }
        if (v == Verdict.Ok && apk != null && updater.canInstall()) {
            runCatching { updater.install(apk) }.onFailure { status = "O Android não abriu a instalação." }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { busy = true; installChecked(updater.copyFromUri(uri)); busy = false }
    }

    SettingsCard("Atualizar o launcher") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsButton("Procurar no GitHub", enabled = !busy, onClick = {
                scope.launch {
                    busy = true; found = emptyList(); status = "Procurando…"
                    val r = updater.checkGitHub()
                    release = r?.takeIf { UpdateRules.isNewer(it.tag, BuildConfig.VERSION_NAME) }
                    status = when {
                        r == null -> "Sem internet ou GitHub fora do ar."
                        release == null -> "Você já tem a mais nova (${BuildConfig.VERSION_NAME})."
                        else -> null
                    }
                    busy = false
                }
            })
            SettingsButton("Instalar de um arquivo", enabled = !busy, onClick = {
                release = null; status = null
                try {
                    picker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
                } catch (_: ActivityNotFoundException) {
                    // Central sem seletor de arquivos: procura no pendrive/memória.
                    found = updater.findOnStorage()
                    if (found.isEmpty()) status = "Nenhum APK encontrado no pendrive ou na memória."
                }
            })
        }
        release?.let { r ->
            SettingsButton("Versão ${r.tag.removePrefix("v")} disponível · Baixar", accent = true, enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    app.log.add("update", "baixando ${r.tag}")
                    val apk = updater.download(r) { pct -> status = "Baixando… $pct%" }
                    if (apk == null) status = "O download falhou. Confira a internet e tente de novo." else installChecked(apk)
                    busy = false
                }
            })
        }
        found.forEach { f ->
            SettingsButton(f.name, enabled = !busy, onClick = {
                scope.launch { busy = true; installChecked(updater.copyFromFile(f)); busy = false }
            })
        }
        status?.let { BasicText(it, style = JancarType.body.copy(color = c.muted)) }
    }
}

private const val NO_PERMISSION =
    "O Android não deixa o launcher instalar apps. Libere uma vez por adb: appops set com.rocha.jancarlauncher REQUEST_INSTALL_PACKAGES allow"
