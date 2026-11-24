package org.lineageos.pctexteditor.core

enum class LineEnding(val separator: String, val label: String) {
    LF("\n", "LF"),
    CRLF("\r\n", "CRLF"),
    CR("\r", "CR"),
}

/** The editor keeps text with "\n" only; the file's own line break is restored when saving. */
object LineEndings {
    private val ANY_BREAK = Regex("\r\n|\r|\n")

    /** The most common break in [text]; LF when there is none or on a tie. */
    fun detect(text: String): LineEnding {
        val counts = ANY_BREAK.findAll(text).groupingBy { it.value }.eachCount()
        val byKind = LineEnding.entries.associateWith { counts[it.separator] ?: 0 }
        val most = byKind.values.max()
        return byKind.entries.first { it.value == most }.key
    }

    fun normalize(text: String): String = ANY_BREAK.replace(text, "\n")

    fun apply(text: String, ending: LineEnding): String = text.replace("\n", ending.separator)
}
