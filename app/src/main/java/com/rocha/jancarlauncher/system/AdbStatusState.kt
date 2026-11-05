package com.rocha.jancarlauncher.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Reavalia [AdbStatus] a cada mudança de [tickKey] (reaproveita o tick de minuto já existente na Home). */
@Composable
fun rememberAdbStatus(tickKey: Any?): State<AdbStatus> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<AdbStatus>(AdbStatus.Disabled) }
    LaunchedEffect(tickKey) { state.value = AdbStatusReader.read(context) }
    return state
}
