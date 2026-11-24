package org.lineageos.pctexteditor.core

data class FindOptions(val matchCase: Boolean = false, val wholeWord: Boolean = false)

/** The matches of a literal query and which one is selected; [position] is 1-based, 0 when there is none. */
data class FindState(val query: String, val matches: List<IntRange>, val current: Int) {
    val count: Int get() = matches.size
    val currentMatch: IntRange? get() = matches.getOrNull(current)
    val position: Int get() = currentMatch?.let { current + 1 } ?: 0

    fun next(): FindState = moveBy(1)

    fun previous(): FindState = moveBy(-1)

    private fun moveBy(step: Int): FindState {
        if (matches.isEmpty()) return this
        return copy(current = Math.floorMod(current + step, matches.size))
    }

    companion object {
        /** Selects the first match starting at or after [cursor], wrapping to the first one. */
        fun of(text: String, query: String, options: FindOptions, cursor: Int): FindState {
            val matches = TextMatcher.findAll(text, query, options)
            val atCursor = matches.indexOfFirst { it.first >= cursor }.takeIf { it >= 0 } ?: 0
            return FindState(query, matches, atCursor)
        }
    }
}

internal object TextMatcher {
    // Spelled out instead of \b, whose idea of a letter differs between the JDK and Android's ICU.
    private const val NOT_AFTER_WORD = "(?<![\\p{L}\\p{N}_])"
    private const val NOT_BEFORE_WORD = "(?![\\p{L}\\p{N}_])"

    fun regexFor(query: String, options: FindOptions): Regex {
        val literal = Regex.escape(query)
        val pattern = "$NOT_AFTER_WORD$literal$NOT_BEFORE_WORD".takeIf { options.wholeWord } ?: literal
        val flags = setOf(RegexOption.IGNORE_CASE).takeUnless { options.matchCase } ?: emptySet()
        return Regex(pattern, flags)
    }

    fun findAll(text: String, query: String, options: FindOptions): List<IntRange> {
        if (query.isEmpty()) return emptyList()
        return regexFor(query, options).findAll(text).map { it.range }.toList()
    }
}
