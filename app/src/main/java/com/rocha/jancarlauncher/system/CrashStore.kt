package com.rocha.jancarlauncher.system

import java.io.File

data class CrashEntry(val file: File, val wallMs: Long, val firstLine: String)

/** Crashes gravados no aparelho (`filesDir/crash`), os [keep] mais novos, para ler em Ajustes sem ADB. */
class CrashStore(private val dir: File, private val keep: Int = 10) {

    fun write(wallMs: Long, version: String, error: Throwable, log: List<String>): File {
        dir.mkdirs()
        val file = File(dir, "$PREFIX$wallMs$SUFFIX")
        file.writeText(buildString {
            append("versão ").append(version).append('\n')
            append(error.stackTraceToString()).append('\n')
            append("--- registro ---\n")
            log.forEach { append(it).append('\n') }
        })
        files().drop(keep).forEach { it.delete() }
        return file
    }

    fun list(): List<CrashEntry> = files().map { f ->
        val first = runCatching { f.useLines { l -> l.drop(1).firstOrNull() } }.getOrNull().orEmpty()
        CrashEntry(f, stamp(f) ?: 0L, first)
    }

    fun read(e: CrashEntry): String = runCatching { e.file.readText() }.getOrDefault("")

    fun clear() { files().forEach { it.delete() } }

    private fun files(): List<File> =
        (dir.listFiles() ?: emptyArray()).filter { stamp(it) != null }.sortedByDescending { stamp(it) }

    private fun stamp(f: File): Long? =
        f.name.takeIf { it.startsWith(PREFIX) && it.endsWith(SUFFIX) }?.removePrefix(PREFIX)?.removeSuffix(SUFFIX)?.toLongOrNull()

    private companion object {
        const val PREFIX = "crash-"
        const val SUFFIX = ".txt"
    }
}
