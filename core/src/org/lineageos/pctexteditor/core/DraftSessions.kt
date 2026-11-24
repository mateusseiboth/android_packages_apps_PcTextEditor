package org.lineageos.pctexteditor.core

import java.net.URLDecoder
import java.net.URLEncoder

/** A tab as kept across restarts; its current and saved texts are stored next to the session, one file each. */
data class DraftTab(
    val id: Long,
    val name: String,
    val uri: String?,
    val encoding: TextEncoding,
    val lineEnding: LineEnding,
    val savedEncoding: TextEncoding,
    val savedLineEnding: LineEnding,
    val stamp: FileStamp?,
)

data class DraftSession(val tabs: List<DraftTab>, val selectedId: Long?)

/** The session file: a "selected" line, then one line per tab with percent-encoded fields. */
object DraftSessions {
    private const val SELECTED = "selected"
    private const val TAB = "tab"
    private const val FIELDS = 10

    fun format(session: DraftSession): String =
        (listOf("$SELECTED ${session.selectedId ?: ""}") + session.tabs.map(::formatTab)).joinToString("\n")

    fun parse(text: String): DraftSession {
        val lines = text.lines()
        val selected = lines.firstOrNull { it.startsWith("$SELECTED ") }?.substringAfter(' ')?.toLongOrNull()
        val tabs = lines.filter { it.startsWith("$TAB ") }.mapNotNull { runCatching { parseTab(it) }.getOrNull() }
        return DraftSession(tabs, selected)
    }

    private fun formatTab(tab: DraftTab): String = listOf(
        TAB, tab.id.toString(), tab.name, tab.uri.orEmpty(), tab.encoding.name, tab.lineEnding.name,
        tab.savedEncoding.name, tab.savedLineEnding.name,
        tab.stamp?.modified?.toString().orEmpty(), tab.stamp?.size?.toString().orEmpty(),
        tab.stamp?.hash?.toString().orEmpty(),
    ).joinToString(" ") { encode(it) }

    private fun parseTab(line: String): DraftTab {
        val f = line.split(" ").map { decode(it) }.drop(1)
        require(f.size == FIELDS)
        val stamp = FileStamp(f[7].toLongOrNull(), f[8].toLongOrNull(), f[9].toIntOrNull())
        return DraftTab(
            id = f[0].toLong(),
            name = f[1],
            uri = f[2].ifEmpty { null },
            encoding = TextEncoding.valueOf(f[3]),
            lineEnding = LineEnding.valueOf(f[4]),
            savedEncoding = TextEncoding.valueOf(f[5]),
            savedLineEnding = LineEnding.valueOf(f[6]),
            stamp = stamp.takeUnless { it == FileStamp(null, null, null) },
        )
    }

    private fun encode(field: String) = URLEncoder.encode(field, Charsets.UTF_8.name())

    private fun decode(field: String) = URLDecoder.decode(field, Charsets.UTF_8.name())
}

/** Auto-save writes a tab back to its file once typing has paused. */
object AutoSave {
    const val IDLE_MILLIS = 2_000L

    fun isDue(enabled: Boolean, hasFile: Boolean, isDirty: Boolean, lastEditAt: Long, now: Long): Boolean =
        enabled && hasFile && isDirty && now - lastEditAt >= IDLE_MILLIS
}
