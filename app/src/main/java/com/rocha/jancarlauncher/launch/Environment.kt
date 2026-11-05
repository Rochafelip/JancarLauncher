package com.rocha.jancarlauncher.launch

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager

/** Implementações Android finas (sem lógica) das interfaces de [AppLauncher]. */
class AndroidPackageGateway(private val pm: PackageManager) : PackageGateway {
    override fun isInstalled(pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    override fun isEnabled(pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

class ContextActivityStarter(private val context: Context) : ActivityStarter {
    override fun startComponent(pkg: String, activityClass: String) {
        val intent = Intent().setClassName(pkg, activityClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun startLaunchIntentFor(pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}

/**
 * Celulares pareados (classe Bluetooth PHONE). Sem permissão/adaptador → lista vazia, nunca exceção.
 * A central é API 27, onde `BLUETOOTH` (normal) basta. Em Android 12+ faltaria BLUETOOTH_CONNECT (runtime): o
 * SecurityException é capturado abaixo e o resultado cai no padrão Android Auto.
 */
@SuppressLint("MissingPermission")
fun readBondedPhones(context: Context): List<BondedPhone> = try {
    BluetoothAdapter.getDefaultAdapter()?.bondedDevices.orEmpty()
        .filter { it.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.PHONE }
        .map { BondedPhone(it.name) }
} catch (e: SecurityException) {
    emptyList()
} catch (e: RuntimeException) {
    emptyList()
}

/** Há algum dispositivo USB ligado (celular no cabo)? Falha → false. */
fun usbPhoneAttached(context: Context): Boolean = try {
    (context.getSystemService(Context.USB_SERVICE) as? UsbManager)?.deviceList?.isNotEmpty() ?: false
} catch (e: RuntimeException) {
    false
}
