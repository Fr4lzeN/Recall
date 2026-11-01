package com.recall.app.core.common.diagnostics

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class SearchTraceStore(
    private val directory: File,
    private val maxFiles: Int = 20,
    private val maxTotalBytes: Long = 30L * 1024 * 1024,
    private val maxFileBytes: Int = 4 * 1024 * 1024,
) {
    @Synchronized
    fun write(record: SearchDiagnosticRecord): String? {
        val data = SearchTraceJson.encode(record).toByteArray(Charsets.UTF_8)
        if (data.size > maxFileBytes || data.size > maxTotalBytes) return null
        check(directory.exists() || directory.mkdirs()) { "Cannot create diagnostics directory" }
        val id = record.traceId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(100)
        val name = "${record.timestampMillis}_$id.json"
        val destination = File(directory, name)
        val temporary = File(directory, "$name.tmp")
        try {
            temporary.outputStream().use { it.write(data) }
            check(temporary.renameTo(destination)) { "Cannot save search diagnostic trace" }
        } finally {
            temporary.delete()
        }
        prune()
        return destination.name
    }

    @Synchronized
    fun exportTo(output: OutputStream) {
        val traces = traceFiles()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"schemaVersion":1,"traceCount":${traces.size},"containsImagePixels":false,"maxRetainedTraces":$maxFiles}""".toByteArray())
            zip.closeEntry()
            for (file in traces) {
                zip.putNextEntry(ZipEntry("traces/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    @Synchronized
    fun clear() {
        directory.listFiles()?.filter { it.isFile && (it.extension == "json" || it.name.endsWith(".tmp")) }
            ?.forEach { check(it.delete()) { "Cannot delete diagnostic trace" } }
    }

    private fun traceFiles() = directory.listFiles()?.filter { it.isFile && it.extension == "json" }
        ?.sortedBy { it.name }.orEmpty()

    private fun prune() {
        val files = traceFiles().toMutableList()
        var bytes = files.sumOf { it.length() }
        while (files.size > maxFiles || bytes > maxTotalBytes) {
            val oldest = files.removeAt(0)
            val size = oldest.length()
            check(oldest.delete()) { "Cannot rotate search diagnostic traces" }
            bytes -= size
        }
    }
}
