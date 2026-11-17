package com.rocha.jancarlauncher.system.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Atualizar o launcher sem ADB (o ADB sem fio da central cai toda hora): baixa a última release do GitHub ou copia
 * um APK do pendrive para `cache/update`, confere pacote/assinatura/versão ([UpdateRules.verdict]) e entrega à tela
 * de instalação do Android. A requisição ao GitHub não leva nenhum dado do dono.
 */
class Updater(private val context: Context) {
    private val dir get() = File(context.cacheDir, "update").apply { mkdirs() }
    private val target get() = File(dir, "update.apk")

    suspend fun checkGitHub(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val c = open(LATEST_URL)
            c.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (c.responseCode != 200) null else UpdateRules.parseLatest(c.inputStream.bufferedReader().readText())
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }

    /** Baixa o APK (segue o redirecionamento do GitHub para o armazenamento dele). `null` = falhou. */
    suspend fun download(r: Release, onProgress: (Int) -> Unit): File? = withContext(Dispatchers.IO) {
        runCatching {
            var url = r.apkUrl
            var c = open(url)
            repeat(5) {
                if (c.responseCode !in 300..399) return@repeat
                url = c.getHeaderField("Location") ?: return@repeat
                c.disconnect()
                c = open(url)
            }
            try {
                if (c.responseCode != 200) return@runCatching null
                val total = c.contentLengthLong
                val out = target
                c.inputStream.use { input ->
                    out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done * 100 / total).toInt())
                        }
                    }
                }
                out
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }

    /** Copia o APK escolhido no seletor de arquivos (pendrive) para `cache/update`. */
    suspend fun copyFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            target.takeIf { it.length() > 0 }
        }.getOrNull()
    }

    /** Copia um APK achado em [findOnStorage] para `cache/update`. */
    suspend fun copyFromFile(file: File): File? = withContext(Dispatchers.IO) {
        runCatching { file.copyTo(target, overwrite = true) }.getOrNull()
    }

    /**
     * Sem seletor de arquivos na central: procura `*.apk` até 2 níveis nas raízes de `/storage` (pendrive e memória
     * interna), fora da pasta `Android`. Precisa de `READ_EXTERNAL_STORAGE` (concedida por adb).
     */
    fun findOnStorage(): List<File> {
        val roots = buildList {
            File("/storage").listFiles()?.filter { it.isDirectory && it.name != "self" && it.name != "emulated" }?.let(::addAll)
            add(Environment.getExternalStorageDirectory())
        }
        fun scan(d: File, depth: Int): List<File> {
            val children = runCatching { d.listFiles()?.toList() }.getOrNull().orEmpty()
            return children.filter { it.isFile && it.name.endsWith(".apk", true) } +
                if (depth == 0) emptyList() else children.filter { it.isDirectory && it.name != "Android" }.flatMap { scan(it, depth - 1) }
        }
        return roots.flatMap { scan(it, 2) }.distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
    }

    fun inspect(apk: File): ApkInfo? = runCatching {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)?.toApkInfo()
    }.getOrNull()

    fun installed(): ApkInfo {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return context.packageManager.getPackageInfo(context.packageName, flags).toApkInfo()
    }

    /** Abre a tela de instalação do Android (o dono confirma). Só depois de [UpdateRules.verdict] = Ok. */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update", apk)
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        context.startActivity(intent)
    }

    /** O Android deixa este app instalar pacotes? (fontes desconhecidas; na central, liberado por adb). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    @Suppress("DEPRECATION")
    private fun PackageInfo.toApkInfo(): ApkInfo {
        val certs = if (Build.VERSION.SDK_INT >= 28) {
            signingInfo?.let { if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory }
        } else {
            signatures
        }
        val code = if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()
        return ApkInfo(packageName, code, certs.orEmpty().map { sha256(it.toByteArray()) }.toSet())
    }

    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
        instanceFollowRedirects = false
        setRequestProperty("User-Agent", "JancarLauncher-updater")
    }

    companion object {
        const val LATEST_URL = "https://api.github.com/repos/Rochafelip/JancarLauncher/releases/latest"
    }
}
