package com.rocha.jancarlauncher.ui.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.rocha.jancarlauncher.obd.ScanGate
import com.rocha.jancarlauncher.obd.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.jancarlauncher.ui.components.JIconButton
import com.rocha.jancarlauncher.ui.components.OnPhotoSurfaces
import com.rocha.jancarlauncher.ui.components.PhotoBackdrop
import com.rocha.jancarlauncher.ui.icons.JIcons
import com.rocha.jancarlauncher.ui.theme.Dimens
import com.rocha.jancarlauncher.ui.theme.JTheme
import com.rocha.jancarlauncher.ui.theme.JancarType

/** Abas da tela OBD. */
enum class ObdTab(val label: String) { Viagens("VIAGENS"), Falhas("FALHAS") }

/**
 * Diagnóstico pelo OBD (aberta pelo botão de chave inglesa do Painel): viagens anteriores e scanner de falhas.
 * Estilo do Painel: foto escurecida, cartões translúcidos, tema claro/escuro.
 */
@Composable
fun ObdScreen(
    onBack: () -> Unit,
    tripsTab: @Composable () -> Unit,
    faultsTab: @Composable () -> Unit,
    initialTab: ObdTab = ObdTab.Falhas,
) {
    val c = JTheme.colors
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        PhotoBackdrop(dim = 0.86f)
        OnPhotoSurfaces {
            Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JIconButton(JIcons.Back, "Voltar para o Painel", onBack, size = 52.dp, iconSize = 22.dp)
                    Spacer(Modifier.width(16.dp))
                    BasicText("OBD", style = JancarType.h1.copy(color = c.fg, fontSize = 26.sp))
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ObdTab.entries.forEach { t -> TabChip(t.label, t == tab) { tab = t } }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (tab) {
                        ObdTab.Viagens -> tripsTab()
                        ObdTab.Falhas -> faultsTab()
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = JTheme.colors
    Box(
        Modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(Dimens.RadiusButton))
            .background(if (selected) c.accent.copy(alpha = 0.25f) else c.surface)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = JancarType.chip.copy(color = if (selected) c.fg else c.muted, letterSpacing = 1.sp))
    }
}

/** Rota da tela OBD: lê do `JancarApp` (OBD, histórico, tabela de códigos). */
@Composable
fun ObdRoute(onBack: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rocha.jancarlauncher.JancarApp
    val live by app.obd.live.collectAsState()
    var scanning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var history by remember { mutableStateOf(emptyList<ScanResult>()) }
    val scope = rememberCoroutineScope()
    val currentTrip by app.obd.currentTrip.collectAsState()
    val gasCents by app.settings.gasPriceCents.collectAsState(initial = null)
    val ethCents by app.settings.ethanolPriceCents.collectAsState(initial = null)
    var trips by remember { mutableStateOf(emptyList<com.rocha.jancarlauncher.obd.TripRecord>()) }
    // Recarrega a lista quando a viagem atual some (acabou de ser fechada e gravada).
    LaunchedEffect(currentTrip == null) { trips = withContext(Dispatchers.IO) { app.tripStore.list() } }
    LaunchedEffect(Unit) { history = withContext(Dispatchers.IO) { app.scanHistory.list() } }
    ObdScreen(
        onBack = onBack,
        tripsTab = { TripsTab(currentTrip, trips, gasCents, ethCents, System.currentTimeMillis()) },
        faultsTab = {
            FaultsTab(
                canScan = ScanGate.allowed(live.speedKmh),
                scanning = scanning,
                result = result,
                history = history,
                catalog = app.dtcCatalog,
                onScan = {
                    scope.launch {
                        scanning = true
                        val r = app.obd.scan()
                        history = withContext(Dispatchers.IO) { app.scanHistory.add(r); app.scanHistory.list() }
                        result = r
                        scanning = false
                    }
                },
            )
        },
    )
}
