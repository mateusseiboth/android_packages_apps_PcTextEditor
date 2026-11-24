package org.lineageos.pctexteditor.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TextLinesTest {
    @Test
    fun emptyTextHasOneLine() {
        assertArrayEquals(intArrayOf(0), TextLines.lineStarts(""))
    }

    @Test
    fun lineStartsFollowEachBreak() {
        assertArrayEquals(intArrayOf(0, 4, 5, 9), TextLines.lineStarts("abc\n\ndef\n"))
    }

    @Test
    fun positionIsOneBased() {
        val text = "abc\ndef"
        assertEquals(LineColumn(1, 1), TextLines.positionOf(text, 0))
        assertEquals(LineColumn(1, 4), TextLines.positionOf(text, 3))
        assertEquals(LineColumn(2, 1), TextLines.positionOf(text, 4))
        assertEquals(LineColumn(2, 4), TextLines.positionOf(text, 7))
    }

    @Test
    fun positionClampsOutOfRangeOffsets() {
        assertEquals(LineColumn(2, 4), TextLines.positionOf("abc\ndef", 99))
        assertEquals(LineColumn(1, 1), TextLines.positionOf("abc\ndef", -5))
    }

    @Test
    fun gutterWidthInDigits() {
        assertEquals(1, TextLines.digits(9))
        assertEquals(2, TextLines.digits(10))
        assertEquals(4, TextLines.digits(1234))
    }
}
