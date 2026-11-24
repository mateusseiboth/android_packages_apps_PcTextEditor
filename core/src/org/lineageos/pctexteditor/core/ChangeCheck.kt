package org.lineageos.pctexteditor.core

/** What the file's provider says about it; each field is null when the provider does not report it. */
data class FileStamp(val modified: Long?, val size: Long?, val hash: Int?)

enum class ExternalChange { NONE, RELOAD, ASK, GONE }

/** Whether another app changed an open file, and what to do: reload a clean tab, ask about one with changes. */
object ChangeCheck {
    private val FIELDS: List<(FileStamp) -> Any?> = listOf({ it.modified }, { it.size }, { it.hash })

    fun decide(known: FileStamp?, current: FileStamp?, isDirty: Boolean): ExternalChange {
        known ?: return ExternalChange.NONE
        current ?: return ExternalChange.GONE
        if (!differ(known, current)) return ExternalChange.NONE
        return ExternalChange.ASK.takeIf { isDirty } ?: ExternalChange.RELOAD
    }

    /** Only fields both stamps know are compared. */
    private fun differ(a: FileStamp, b: FileStamp): Boolean =
        FIELDS.any { field -> field(a) != null && field(b) != null && field(a) != field(b) }
}
