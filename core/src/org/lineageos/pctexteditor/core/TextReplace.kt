package org.lineageos.pctexteditor.core

/** [cursor] is where the next search starts: just after the last replacement. */
data class ReplaceResult(val text: String, val count: Int, val cursor: Int)

object TextReplace {
    fun replaceCurrent(text: String, state: FindState, replacement: String): ReplaceResult {
        val match = state.currentMatch ?: return ReplaceResult(text, 0, 0)
        val replaced = text.replaceRange(match, replacement)
        return ReplaceResult(replaced, 1, match.first + replacement.length)
    }

    fun replaceAll(text: String, query: String, replacement: String, options: FindOptions): ReplaceResult {
        if (query.isEmpty()) return ReplaceResult(text, 0, 0)
        val regex = TextMatcher.regexFor(query, options)
        val count = regex.findAll(text).count()
        val replaced = regex.replace(text) { replacement }
        return ReplaceResult(replaced, count, 0)
    }
}
