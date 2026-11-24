package org.lineageos.pctexteditor.core

enum class TokenType {
    KEYWORD, TYPE, STRING, NUMBER, COMMENT, META, TAG, ATTRIBUTE, PROPERTY, HEADING, EMPHASIS, CODE, LINK, VARIABLE,
    CONSTANT,
}

data class Token(val start: Int, val end: Int, val type: TokenType)

/** One kind of token and the pattern that finds it. Patterns use only non-capturing groups. */
class SyntaxRule(val type: TokenType, val pattern: String)

/**
 * A language is an ordered table of rules: at each position the first rule that matches wins, so comments
 * and strings come before keywords. The rules are joined into one regular expression, one group per rule.
 */
class SyntaxLanguage(val name: String, val rules: List<SyntaxRule>) {
    internal val regex = Regex(rules.joinToString("|") { "(${it.pattern})" }, RegexOption.MULTILINE)

    override fun toString() = name
}

object SyntaxHighlighter {
    /** Longer texts are left plain: tokenizing them on every pause in typing would make the editor lag. */
    const val MAX_CHARS = 1_000_000

    /** The highlighted spans of [text], in order and never overlapping; text between them is plain. */
    fun tokenize(text: CharSequence, language: SyntaxLanguage): List<Token> {
        if (text.length > MAX_CHARS) return emptyList()
        return language.regex.findAll(text)
            .filter { it.range.last >= it.range.first }
            .map { match -> Token(match.range.first, match.range.last + 1, typeOf(match, language)) }
            .toList()
    }

    private fun typeOf(match: MatchResult, language: SyntaxLanguage): TokenType {
        val group = (1..language.rules.size).first { match.groups[it] != null }
        return language.rules[group - 1].type
    }
}
