package org.lineageos.pctexteditor

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import org.lineageos.pctexteditor.core.FileStamp

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

    /**
     * The file's date and size as its provider reports them, or null when it can no longer be reached. A
     * provider that reports neither gets a hash of the content, so changes are still noticed.
     */
    fun stampOf(uri: Uri): FileStamp? {
        val cursor = runCatching { resolver.query(uri, null, null, null, null) }.getOrNull() ?: return null
        val known = cursor.use { row ->
            if (!row.moveToFirst()) return null
            val modified = row.longAt(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                ?: row.longAt(MEDIA_DATE_MODIFIED)?.times(MILLIS_PER_SECOND)
            FileStamp(modified, row.longAt(OpenableColumns.SIZE), hash = null)
        }
        val isBlind = known.modified == null && known.size == null
        return known.takeUnless { isBlind } ?: runCatching { known.copy(hash = readAll(uri).contentHashCode()) }.getOrNull()
    }

    private fun Cursor.longAt(column: String): Long? =
        getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong)

    private fun open(uri: Uri) = resolver.openInputStream(uri) ?: error("no input stream for $uri")

    private companion object {
        const val MEDIA_DATE_MODIFIED = "date_modified"
        const val MILLIS_PER_SECOND = 1000L
    }
}
