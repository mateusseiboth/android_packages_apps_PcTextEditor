package org.lineageos.pctexteditor.core

import org.lineageos.pctexteditor.core.TokenType.ATTRIBUTE
import org.lineageos.pctexteditor.core.TokenType.CODE
import org.lineageos.pctexteditor.core.TokenType.COMMENT
import org.lineageos.pctexteditor.core.TokenType.CONSTANT
import org.lineageos.pctexteditor.core.TokenType.EMPHASIS
import org.lineageos.pctexteditor.core.TokenType.HEADING
import org.lineageos.pctexteditor.core.TokenType.KEYWORD
import org.lineageos.pctexteditor.core.TokenType.LINK
import org.lineageos.pctexteditor.core.TokenType.META
import org.lineageos.pctexteditor.core.TokenType.NUMBER
import org.lineageos.pctexteditor.core.TokenType.PROPERTY
import org.lineageos.pctexteditor.core.TokenType.STRING
import org.lineageos.pctexteditor.core.TokenType.TAG
import org.lineageos.pctexteditor.core.TokenType.TYPE
import org.lineageos.pctexteditor.core.TokenType.VARIABLE

/** The rule tables of the languages the editor highlights, and which one a file name uses. */
object SyntaxLanguages {
    private const val SLASH_COMMENT = """//[^\n]*"""
    private const val BLOCK_COMMENT = """/\*[\s\S]*?(?:\*/|\z)"""
    private const val HASH_COMMENT = """#[^\n]*"""
    private const val DOUBLE_QUOTED = """"(?:\\.|[^"\\\n])*"?"""
    private const val SINGLE_QUOTED = """'(?:\\.|[^'\\\n])*'?"""
    private const val NUMBER_LITERAL =
        """\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\d[\d_]*(?:\.\d[\d_]*)?(?:[eE][+-]?\d+)?)[fFdDlLuU]*\b"""
    private const val CAPITALIZED = """\b[A-Z]\w*\b"""

    private fun rule(type: TokenType, pattern: String) = SyntaxRule(type, pattern)

    private fun words(type: TokenType, list: String) =
        SyntaxRule(type, list.trim().split(Regex("\\s+")).joinToString("|", "\\b(?:", ")\\b") { Regex.escape(it) })

    private fun cLike(keywords: String, vararg extra: SyntaxRule) = listOf(
        rule(COMMENT, SLASH_COMMENT),
        rule(COMMENT, BLOCK_COMMENT),
    ) + extra + listOf(
        rule(STRING, DOUBLE_QUOTED),
        rule(STRING, SINGLE_QUOTED),
        words(KEYWORD, keywords),
        words(CONSTANT, "true false null"),
        rule(TYPE, CAPITALIZED),
        rule(NUMBER, NUMBER_LITERAL),
    )

    val KOTLIN = SyntaxLanguage(
        "Kotlin",
        cLike(
            """package import class interface object fun val var if else when for while do return break continue
               throw try catch finally in is as by get set this super typealias constructor init companion data
               sealed enum open abstract override private protected public internal inline suspend operator
               infix lateinit const vararg reified out where""",
            rule(STRING, "\"\"\"[\\s\\S]*?(?:\"\"\"|\\z)"),
            rule(META, """@[\w.]+"""),
        ),
    )

    val JAVA = SyntaxLanguage(
        "Java",
        cLike(
            """package import class interface enum record extends implements new return if else for while do
               switch case default break continue throw throws try catch finally this super static final abstract
               public private protected void int long short byte char boolean float double var synchronized
               volatile transient native instanceof sealed permits yield""",
            rule(STRING, "\"\"\"[\\s\\S]*?(?:\"\"\"|\\z)"),
            rule(META, """@[\w.]+"""),
        ),
    )

    private const val C_KEYWORDS = """auto break case char const continue default do double else enum extern float for
        goto if inline int long register restrict return short signed sizeof static struct switch typedef union
        unsigned void volatile while bool"""

    val C = SyntaxLanguage("C", cLike(C_KEYWORDS, rule(META, """^\s*#\s*\w+""")))

    val CPP = SyntaxLanguage(
        "C++",
        cLike(
            "$C_KEYWORDS class namespace template typename using public private protected virtual override new " +
                "delete this operator friend explicit constexpr nullptr noexcept static_cast dynamic_cast " +
                "reinterpret_cast const_cast try catch throw auto decltype final mutable",
            rule(META, """^\s*#\s*\w+"""),
        ),
    )

    val PYTHON = SyntaxLanguage(
        "Python",
        listOf(
            rule(COMMENT, HASH_COMMENT),
            rule(STRING, "[rRbBuUfF]{0,2}(?:\"\"\"[\\s\\S]*?(?:\"\"\"|\\z)|'''[\\s\\S]*?(?:'''|\\z))"),
            rule(STRING, "[rRbBuUfF]{0,2}(?:$DOUBLE_QUOTED|$SINGLE_QUOTED)"),
            rule(META, """@[\w.]+"""),
            words(
                KEYWORD,
                """and as assert async await break class continue def del elif else except finally for from global
                   if import in is lambda nonlocal not or pass raise return try while with yield match case""",
            ),
            words(CONSTANT, "None True False self"),
            rule(TYPE, CAPITALIZED),
            rule(NUMBER, NUMBER_LITERAL),
        ),
    )

    val SHELL = SyntaxLanguage(
        "Shell",
        listOf(
            rule(COMMENT, """(?<![^\s;])#[^\n]*"""),
            rule(STRING, """"(?:\\.|[^"\\])*"?"""),
            rule(STRING, """'[^']*'?"""),
            rule(VARIABLE, """\$\{[^}\n]*\}|\$[A-Za-z_]\w*|\$[0-9@#?$!*-]"""),
            words(
                KEYWORD,
                """if then else elif fi for while until do done case esac in function return local export
                   readonly declare unset shift break continue exit source alias select""",
            ),
            rule(NUMBER, """\b\d+\b"""),
        ),
    )

    val JSON = SyntaxLanguage(
        "JSON",
        listOf(
            rule(PROPERTY, """"(?:\\.|[^"\\\n])*"(?=\s*:)"""),
            rule(STRING, DOUBLE_QUOTED),
            rule(NUMBER, """-?\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b"""),
            words(CONSTANT, "true false null"),
        ),
    )

    private val MARKUP = listOf(
        rule(COMMENT, """<!--[\s\S]*?(?:-->|\z)"""),
        rule(STRING, """<!\[CDATA\[[\s\S]*?(?:\]\]>|\z)"""),
        rule(META, """<\?[\s\S]*?(?:\?>|\z)|<![A-Za-z][^>]*>"""),
        rule(TAG, """</?[\w:.-]+|/?>"""),
        rule(ATTRIBUTE, """[\w:.-]+(?=\s*=\s*["'])"""),
        rule(STRING, """(?<==\s{0,3})(?:"[^"]*"|'[^']*')"""),
        rule(CONSTANT, """&#?\w+;"""),
    )

    val XML = SyntaxLanguage("XML", MARKUP)

    val HTML = SyntaxLanguage("HTML", MARKUP)

    val CSS = SyntaxLanguage(
        "CSS",
        listOf(
            rule(COMMENT, BLOCK_COMMENT),
            rule(STRING, DOUBLE_QUOTED),
            rule(STRING, SINGLE_QUOTED),
            rule(KEYWORD, """@[\w-]+|!important"""),
            rule(NUMBER, """#[0-9a-fA-F]{3,8}\b"""),
            rule(PROPERTY, """[\w-]+(?=\s*:[^:])"""),
            rule(NUMBER, """-?\b\d+(?:\.\d+)?(?:%|[a-zA-Z]+)?"""),
        ),
    )

    private const val JS_KEYWORDS = """const let var function return if else for while do switch case default break
        continue new this class extends super import export from as async await try catch finally throw typeof
        instanceof in of yield delete void static get set"""

    private val JS_RULES = listOf(
        rule(COMMENT, SLASH_COMMENT),
        rule(COMMENT, BLOCK_COMMENT),
        rule(STRING, """`(?:\\.|[^`\\])*`?"""),
        rule(STRING, DOUBLE_QUOTED),
        rule(STRING, SINGLE_QUOTED),
        rule(META, """@[\w.]+"""),
    )

    val JAVASCRIPT = SyntaxLanguage(
        "JavaScript",
        JS_RULES + listOf(
            words(KEYWORD, JS_KEYWORDS),
            words(CONSTANT, "true false null undefined NaN Infinity"),
            rule(NUMBER, NUMBER_LITERAL),
        ),
    )

    val TYPESCRIPT = SyntaxLanguage(
        "TypeScript",
        JS_RULES + listOf(
            words(
                KEYWORD,
                "$JS_KEYWORDS interface type enum implements namespace declare readonly public private protected " +
                    "abstract keyof is any unknown never number string boolean",
            ),
            words(CONSTANT, "true false null undefined NaN Infinity"),
            rule(TYPE, CAPITALIZED),
            rule(NUMBER, NUMBER_LITERAL),
        ),
    )

    val MARKDOWN = SyntaxLanguage(
        "Markdown",
        listOf(
            rule(CODE, """^```[^\n]*\n[\s\S]*?(?:^```[^\n]*$|\z)"""),
            rule(HEADING, """^#{1,6}\s[^\n]*"""),
            rule(COMMENT, """^>[^\n]*"""),
            rule(CODE, """`[^`\n]+`"""),
            rule(LINK, """!?\[[^\]\n]*\]\([^)\n]*\)"""),
            rule(EMPHASIS, """\*\*[^*\n]+\*\*|__[^_\n]+__|\*[^*\s][^*\n]*\*|\b_[^_\n]+_\b"""),
            rule(KEYWORD, """^\s*(?:[-*+]|\d+\.)\s"""),
        ),
    )

    val YAML = SyntaxLanguage(
        "YAML",
        listOf(
            rule(COMMENT, """(?<!\S)#[^\n]*"""),
            rule(META, """^(?:---|\.\.\.)\s*$"""),
            rule(STRING, DOUBLE_QUOTED),
            rule(STRING, SINGLE_QUOTED),
            rule(PROPERTY, """[\w.-]+(?=\s*:(?:\s|$))"""),
            rule(VARIABLE, """[&*][\w-]+"""),
            words(CONSTANT, "true false null yes no on off True False Null"),
            rule(NUMBER, """(?<![\w.-])-?\d+(?:\.\d+)?(?![\w.-])"""),
        ),
    )

    val INI = SyntaxLanguage(
        "INI",
        listOf(
            rule(COMMENT, """^\s*[;#][^\n]*"""),
            rule(HEADING, """^\s*\[[^\]\n]*\]"""),
            rule(PROPERTY, """^\s*[^=:\n;#\[\s][^=:\n]*?(?=\s*[=:])"""),
            rule(STRING, """"[^"\n]*""""),
        ),
    )

    val MAKEFILE = SyntaxLanguage(
        "Makefile",
        listOf(
            rule(COMMENT, """(?<!\$)#[^\n]*"""),
            rule(
                KEYWORD,
                """^\s*-?(?:include|sinclude|ifeq|ifneq|ifdef|ifndef|else|endif|define|endef|export|unexport|override|vpath)\b""",
            ),
            rule(VARIABLE, """\$\([^)\n]*\)|\$\{[^}\n]*\}|\$[@<^*?%+|]"""),
            rule(PROPERTY, """^[\w.-]+(?=\s*(?:[:+?!]?=|::=))"""),
            rule(TYPE, """^[^\s:#=][^:#=\n]*?(?=\s*::?(?!=))"""),
        ),
    )

    private val BY_EXTENSION = mapOf(
        "kt" to KOTLIN, "kts" to KOTLIN, "java" to JAVA, "c" to C, "h" to C,
        "cc" to CPP, "cpp" to CPP, "cxx" to CPP, "hpp" to CPP, "hh" to CPP, "hxx" to CPP,
        "py" to PYTHON, "pyw" to PYTHON, "sh" to SHELL, "bash" to SHELL, "zsh" to SHELL,
        "json" to JSON, "xml" to XML, "svg" to XML, "xsd" to XML, "xsl" to XML, "plist" to XML,
        "html" to HTML, "htm" to HTML, "xhtml" to HTML, "css" to CSS,
        "js" to JAVASCRIPT, "mjs" to JAVASCRIPT, "cjs" to JAVASCRIPT, "jsx" to JAVASCRIPT,
        "ts" to TYPESCRIPT, "tsx" to TYPESCRIPT, "mts" to TYPESCRIPT, "cts" to TYPESCRIPT,
        "md" to MARKDOWN, "markdown" to MARKDOWN, "yaml" to YAML, "yml" to YAML,
        "ini" to INI, "conf" to INI, "cfg" to INI, "properties" to INI, "mk" to MAKEFILE,
    )

    private val BY_NAME = mapOf(
        "makefile" to MAKEFILE, "gnumakefile" to MAKEFILE,
        ".bashrc" to SHELL, ".bash_profile" to SHELL, ".profile" to SHELL, ".zshrc" to SHELL,
        ".gitconfig" to INI, ".editorconfig" to INI,
    )

    val ALL = listOf(KOTLIN, JAVA, C, CPP, PYTHON, SHELL, JSON, XML, HTML, CSS, JAVASCRIPT, TYPESCRIPT, MARKDOWN, YAML, INI, MAKEFILE)

    fun forFileName(name: String): SyntaxLanguage? {
        val lower = name.lowercase()
        val extension = lower.substringAfterLast('.', "").takeIf { lower.lastIndexOf('.') > 0 }
        return BY_NAME[lower] ?: extension?.let { BY_EXTENSION[it] }
    }
}
