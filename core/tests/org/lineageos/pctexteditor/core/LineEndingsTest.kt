package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LineEndingsTest {
    @Test
    fun textWithoutBreaksIsLf() {
        assertEquals(LineEnding.LF, LineEndings.detect("one line"))
    }

    @Test
    fun detectsEachKind() {
        assertEquals(LineEnding.LF, LineEndings.detect("a\nb\n"))
        assertEquals(LineEnding.CRLF, LineEndings.detect("a\r\nb\r\n"))
        assertEquals(LineEnding.CR, LineEndings.detect("a\rb\r"))
    }

    @Test
    fun mixedTextTakesTheMostCommon() {
        assertEquals(LineEnding.CRLF, LineEndings.detect("a\r\nb\r\nc\nd"))
        assertEquals(LineEnding.LF, LineEndings.detect("a\nb\nc\r\nd"))
    }

    @Test
    fun tieGoesToLf() {
        assertEquals(LineEnding.LF, LineEndings.detect("a\r\nb\nc"))
    }

    @Test
    fun normalizeTurnsEveryBreakIntoLf() {
        assertEquals("a\nb\nc\nd", LineEndings.normalize("a\r\nb\rc\nd"))
    }

    @Test
    fun applyWritesTheChosenBreak() {
        assertEquals("a\r\nb\r\n", LineEndings.apply("a\nb\n", LineEnding.CRLF))
        assertEquals("a\rb", LineEndings.apply("a\nb", LineEnding.CR))
        assertEquals("a\nb", LineEndings.apply("a\nb", LineEnding.LF))
    }
}
