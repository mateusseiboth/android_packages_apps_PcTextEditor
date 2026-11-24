package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyntaxHighlighterTest {
    /** The token type covering the [occurrence]-th appearance of [part] in [text], or null when it is plain. */
    private fun typeOf(language: SyntaxLanguage, text: String, part: String, occurrence: Int = 1): TokenType? {
        val at = generateSequence(text.indexOf(part)) { text.indexOf(part, it + 1).takeIf { i -> i >= 0 } }
            .elementAt(occurrence - 1)
        return SyntaxHighlighter.tokenize(text, language).firstOrNull { at >= it.start && at < it.end }?.type
    }

    @Test
    fun kotlin() {
        val code = "@Test\nfun main() { val s = \"oi // não\" // fim\n  /* bloco */ return 42 }\nclass Foo"
        val kotlin = SyntaxLanguages.KOTLIN
        assertEquals(TokenType.META, typeOf(kotlin, code, "@Test"))
        assertEquals(TokenType.KEYWORD, typeOf(kotlin, code, "fun"))
        assertEquals(TokenType.STRING, typeOf(kotlin, code, "// não"))
        assertEquals(TokenType.COMMENT, typeOf(kotlin, code, "// fim"))
        assertEquals(TokenType.COMMENT, typeOf(kotlin, code, "bloco"))
        assertEquals(TokenType.NUMBER, typeOf(kotlin, code, "42"))
        assertEquals(TokenType.TYPE, typeOf(kotlin, code, "Foo"))
        assertNull(typeOf(kotlin, code, "main"))
    }

    @Test
    fun kotlinRawStringSpansLines() {
        val code = "val x = \"\"\"a\nfun b\"\"\""
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.KOTLIN, code, "fun"))
    }

    @Test
    fun unterminatedBlockCommentRunsToTheEnd() {
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.JAVA, "int a; /* aberto\nint b;", "int", 2))
    }

    @Test
    fun java() {
        val code = "public class A { private int n = 0x1F; String s = \"x\"; }"
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.JAVA, code, "public"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.JAVA, code, "int"))
        assertEquals(TokenType.NUMBER, typeOf(SyntaxLanguages.JAVA, code, "0x1F"))
        assertEquals(TokenType.TYPE, typeOf(SyntaxLanguages.JAVA, code, "String"))
    }

    @Test
    fun cAndCpp() {
        val code = "#include <stdio.h>\nint main(void) { char c = 'a'; return 0; } // ok"
        assertEquals(TokenType.META, typeOf(SyntaxLanguages.C, code, "#include"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.C, code, "return"))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.C, code, "'a'"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.CPP, "namespace x { template<class T> }", "template"))
    }

    @Test
    fun python() {
        val code = "@dataclass\ndef f(x):\n    \"\"\"doc\n    def\"\"\"\n    return None  # fim"
        assertEquals(TokenType.META, typeOf(SyntaxLanguages.PYTHON, code, "@dataclass"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.PYTHON, code, "def"))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.PYTHON, code, "def", 2))
        assertEquals(TokenType.CONSTANT, typeOf(SyntaxLanguages.PYTHON, code, "None"))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.PYTHON, code, "# fim"))
    }

    @Test
    fun shell() {
        val code = "if [ -n \"\$HOME\" ]; then echo \${USER} # oi\nfi\nurl=a#b"
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.SHELL, code, "if"))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.SHELL, code, "\"\$HOME\""))
        assertEquals(TokenType.VARIABLE, typeOf(SyntaxLanguages.SHELL, code, "\${USER}"))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.SHELL, code, "# oi"))
        assertNull(typeOf(SyntaxLanguages.SHELL, code, "#b"))
    }

    @Test
    fun json() {
        val code = "{\"nome\": \"valor\", \"n\": -1.5e3, \"ok\": true, \"x\": null}"
        assertEquals(TokenType.PROPERTY, typeOf(SyntaxLanguages.JSON, code, "\"nome\""))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.JSON, code, "\"valor\""))
        assertEquals(TokenType.NUMBER, typeOf(SyntaxLanguages.JSON, code, "-1.5e3"))
        assertEquals(TokenType.CONSTANT, typeOf(SyntaxLanguages.JSON, code, "true"))
    }

    @Test
    fun xmlAndHtml() {
        val code = "<!-- nota -->\n<a href=\"x\">texto &amp; mais</a>"
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.XML, code, "nota"))
        assertEquals(TokenType.TAG, typeOf(SyntaxLanguages.XML, code, "<a"))
        assertEquals(TokenType.ATTRIBUTE, typeOf(SyntaxLanguages.XML, code, "href"))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.XML, code, "\"x\""))
        assertEquals(TokenType.CONSTANT, typeOf(SyntaxLanguages.XML, code, "&amp;"))
        assertNull(typeOf(SyntaxLanguages.XML, code, "texto"))
        assertEquals(TokenType.TAG, typeOf(SyntaxLanguages.HTML, "<div class=\"a\">", "<div"))
    }

    @Test
    fun css() {
        val code = "@media screen { .a { color: #fff; margin: 10px; } } /* fim */"
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.CSS, code, "@media"))
        assertEquals(TokenType.PROPERTY, typeOf(SyntaxLanguages.CSS, code, "color"))
        assertEquals(TokenType.NUMBER, typeOf(SyntaxLanguages.CSS, code, "#fff"))
        assertEquals(TokenType.NUMBER, typeOf(SyntaxLanguages.CSS, code, "10px"))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.CSS, code, "fim"))
    }

    @Test
    fun javascriptAndTypescript() {
        val code = "const s = `a\${b}c`; function f(): Promise<void> { return null } // x"
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.JAVASCRIPT, code, "const"))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.JAVASCRIPT, code, "`a"))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.JAVASCRIPT, code, "// x"))
        assertEquals(TokenType.CONSTANT, typeOf(SyntaxLanguages.JAVASCRIPT, code, "null"))
        assertEquals(TokenType.TYPE, typeOf(SyntaxLanguages.TYPESCRIPT, code, "Promise"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.TYPESCRIPT, "interface A {}", "interface"))
    }

    @Test
    fun markdown() {
        val code = "# Título\nTexto **forte** e `código`.\n```\n# não é título\n```\n- item [link](http://x)"
        assertEquals(TokenType.HEADING, typeOf(SyntaxLanguages.MARKDOWN, code, "Título"))
        assertEquals(TokenType.EMPHASIS, typeOf(SyntaxLanguages.MARKDOWN, code, "forte"))
        assertEquals(TokenType.CODE, typeOf(SyntaxLanguages.MARKDOWN, code, "código"))
        assertEquals(TokenType.CODE, typeOf(SyntaxLanguages.MARKDOWN, code, "não é título"))
        assertEquals(TokenType.LINK, typeOf(SyntaxLanguages.MARKDOWN, code, "[link]"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.MARKDOWN, code, "- "))
        assertNull(typeOf(SyntaxLanguages.MARKDOWN, code, "Texto"))
    }

    @Test
    fun yaml() {
        val code = "---\nnome: \"x\" # comentário\nlista:\n  - ativo: true\n    n: 3"
        assertEquals(TokenType.PROPERTY, typeOf(SyntaxLanguages.YAML, code, "nome"))
        assertEquals(TokenType.PROPERTY, typeOf(SyntaxLanguages.YAML, code, "ativo"))
        assertEquals(TokenType.STRING, typeOf(SyntaxLanguages.YAML, code, "\"x\""))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.YAML, code, "comentário"))
        assertEquals(TokenType.CONSTANT, typeOf(SyntaxLanguages.YAML, code, "true"))
        assertEquals(TokenType.NUMBER, typeOf(SyntaxLanguages.YAML, code, "3"))
        assertEquals(TokenType.META, typeOf(SyntaxLanguages.YAML, code, "---"))
    }

    @Test
    fun ini() {
        val code = "; nota\n[seção]\nchave = valor\n# outra"
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.INI, code, "nota"))
        assertEquals(TokenType.HEADING, typeOf(SyntaxLanguages.INI, code, "seção"))
        assertEquals(TokenType.PROPERTY, typeOf(SyntaxLanguages.INI, code, "chave"))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.INI, code, "outra"))
        assertNull(typeOf(SyntaxLanguages.INI, code, "valor"))
    }

    @Test
    fun makefile() {
        val code = "CC ?= gcc\nall: main.o\n\t\$(CC) -o \$@ \$^ # liga\nifeq (\$(X),1)\nendif"
        assertEquals(TokenType.PROPERTY, typeOf(SyntaxLanguages.MAKEFILE, code, "CC"))
        assertEquals(TokenType.TYPE, typeOf(SyntaxLanguages.MAKEFILE, code, "all"))
        assertEquals(TokenType.VARIABLE, typeOf(SyntaxLanguages.MAKEFILE, code, "\$(CC)"))
        assertEquals(TokenType.VARIABLE, typeOf(SyntaxLanguages.MAKEFILE, code, "\$@"))
        assertEquals(TokenType.COMMENT, typeOf(SyntaxLanguages.MAKEFILE, code, "liga"))
        assertEquals(TokenType.KEYWORD, typeOf(SyntaxLanguages.MAKEFILE, code, "ifeq"))
    }

    @Test
    fun tokensAreOrderedAndDoNotOverlap() {
        val tokens = SyntaxHighlighter.tokenize("val a = \"b\" // c\nfun d() = 1", SyntaxLanguages.KOTLIN)
        tokens.zipWithNext().forEach { (a, b) -> assertEquals(true, a.end <= b.start) }
    }

    @Test
    fun languageFromFileName() {
        val cases = mapOf(
            "Main.kt" to SyntaxLanguages.KOTLIN, "build.gradle.kts" to SyntaxLanguages.KOTLIN,
            "A.java" to SyntaxLanguages.JAVA, "x.c" to SyntaxLanguages.C, "x.h" to SyntaxLanguages.C,
            "x.cpp" to SyntaxLanguages.CPP, "x.hpp" to SyntaxLanguages.CPP, "a.py" to SyntaxLanguages.PYTHON,
            "run.sh" to SyntaxLanguages.SHELL, ".bashrc" to SyntaxLanguages.SHELL, "a.json" to SyntaxLanguages.JSON,
            "a.xml" to SyntaxLanguages.XML, "index.html" to SyntaxLanguages.HTML, "a.css" to SyntaxLanguages.CSS,
            "a.js" to SyntaxLanguages.JAVASCRIPT, "a.mjs" to SyntaxLanguages.JAVASCRIPT,
            "a.ts" to SyntaxLanguages.TYPESCRIPT, "a.tsx" to SyntaxLanguages.TYPESCRIPT,
            "README.md" to SyntaxLanguages.MARKDOWN, "a.yaml" to SyntaxLanguages.YAML, "a.yml" to SyntaxLanguages.YAML,
            "a.ini" to SyntaxLanguages.INI, "a.conf" to SyntaxLanguages.INI, "Makefile" to SyntaxLanguages.MAKEFILE,
            "rules.mk" to SyntaxLanguages.MAKEFILE, "GNUmakefile" to SyntaxLanguages.MAKEFILE,
        )
        cases.forEach { (name, language) -> assertEquals(name, language, SyntaxLanguages.forFileName(name)) }
        assertNull(SyntaxLanguages.forFileName("notas.txt"))
        assertNull(SyntaxLanguages.forFileName("Untitled"))
    }
}
