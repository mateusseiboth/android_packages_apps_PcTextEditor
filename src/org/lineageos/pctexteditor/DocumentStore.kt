package org.lineageos.pctexteditor

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

data class DocumentMeta(val name: String, val size: Long)

/** Reads and writes documents through the system file picker's URIs. Every call blocks: run it off the main thread. */
class DocumentStore(private val resolver: ContentResolver) {
    fun metaOf(uri: Uri): DocumentMeta {
        val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val fallback = DocumentMeta(uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString(), -1)
        val cursor = resolver.query(uri, columns, null, null, null) ?: return fallback
        return cursor.use {
            if (!it.moveToFirst()) return fallback
            val name = it.getString(0) ?: fallback.name
            val size = it.takeUnless { c -> c.isNull(1) }?.getLong(1) ?: -1
            DocumentMeta(name, size)
        }
    }

    fun readHead(uri: Uri, count: Int): ByteArray = open(uri).use { it.readNBytes(count) }

    fun readAll(uri: Uri): ByteArray = open(uri).use { it.readBytes() }

    /** "wt" truncates: a shorter text must not leave the old file's tail behind. */
    fun write(uri: Uri, bytes: ByteArray) {
        val output = resolver.openOutputStream(uri, "wt") ?: error("no output stream for $uri")
        output.use { it.write(bytes) }
    }

    /** Keeps access after a restart when the picker allows it; files shared for one use only stay one-use. */
    fun keepAccess(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { resolver.takePersistableUriPermission(uri, flags) }
            .recoverCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    private fun open(uri: Uri) = resolver.openInputStream(uri) ?: error("no input stream for $uri")
}
