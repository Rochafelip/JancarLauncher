package com.rocha.jancarlauncher.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Wi-Fi para a Home (pedido do usuário, 2026-11-13: ícone e nome da rede quando conectado).
 * [ssid] `null` = conectado, mas o Android não entregou o nome (sem permissão de localização: o nome da rede
 * conta como dado de localização; na central concede-se por adb, ver `docs/INSTALL.md`).
 */
data class WifiStatus(val connected: Boolean, val ssid: String?) {
    companion object {
        val Disconnected = WifiStatus(false, null)

        /** O SSID do Android vem entre aspas; "<unknown ssid>" (sem permissão) ou vazio viram `null`. */
        fun cleanSsid(raw: String?): String? {
            val s = raw?.trim()?.removeSurrounding("\"")?.trim() ?: return null
            if (s.isEmpty() || s.equals("<unknown ssid>", ignoreCase = true) || s == "0x") return null
            return s
        }
    }
}

object WifiStatusReader {
    @Suppress("DEPRECATION") // connectionInfo: único jeito de ler o SSID na API 27 da central.
    fun read(context: Context): WifiStatus {
        val app = context.applicationContext
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return WifiStatus.Disconnected
        val onWifi = runCatching {
            cm.allNetworks.any { n -> cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        }.getOrDefault(false)
        if (!onWifi) return WifiStatus.Disconnected
        val wm = app.getSystemService(WifiManager::class.java)
        val ssid = runCatching { WifiStatus.cleanSsid(wm?.connectionInfo?.ssid) }.getOrNull()
        return WifiStatus(true, ssid)
    }
}

/**
 * Estado do Wi-Fi na tela: relê quando o sistema avisa que uma rede Wi-Fi entrou/saiu e a cada [tickKey]
 * (o tick de minuto da Home), para pegar troca de rede e permissão concedida depois.
 */
@Composable
fun rememberWifiStatus(tickKey: Any?): State<WifiStatus> {
    val context = LocalContext.current.applicationContext
    val state = remember { mutableStateOf(WifiStatusReader.read(context)) }
    // Redes Wi-Fi que o sistema avisou que estão de pé. Ao cair, o Android ainda lista a rede por um instante:
    // reler a lista no aviso de queda dava "conectado" com o Wi-Fi desligado; quem manda é o aviso.
    val up = remember { java.util.concurrent.ConcurrentHashMap.newKeySet<Network>() }
    fun update() {
        state.value = if (up.isEmpty()) WifiStatus.Disconnected else WifiStatusReader.read(context).copy(connected = true)
    }
    LaunchedEffect(tickKey) { update() }
    DisposableEffect(context) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { up.add(network); update() }
            override fun onLost(network: Network) { up.remove(network); update() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { up.add(network); update() }
        }
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val ok = runCatching { cm?.registerNetworkCallback(request, callback) }.isSuccess
        onDispose { if (ok) runCatching { cm?.unregisterNetworkCallback(callback) } }
    }
    return state
}
