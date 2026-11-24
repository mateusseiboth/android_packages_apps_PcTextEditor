package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextReplaceTest {
    @Test
    fun replaceCurrentReplacesOnlyTheSelectedMatchAndMovesOn() {
        val text = "um dois um dois"
        val state = FindState.of(text, "um", FindOptions(), cursor = 0)
        val result = TextReplace.replaceCurrent(text, state, "três")
        assertEquals("três dois um dois", result.text)
        assertEquals(4, result.cursor)
    }

    @Test
    fun replaceCurrentWithoutMatchKeepsTheText() {
        val text = "nada aqui"
        val state = FindState.of(text, "xyz", FindOptions(), cursor = 3)
        val result = TextReplace.replaceCurrent(text, state, "abc")
        assertEquals(text, result.text)
        assertEquals(0, result.count)
    }

    @Test
    fun replaceAllCountsTheReplacements() {
        val result = TextReplace.replaceAll("Gato gato rato", "gato", "cão", FindOptions())
        assertEquals("cão cão rato", result.text)
        assertEquals(2, result.count)
    }

    @Test
    fun replaceAllTreatsDollarSignsLiterally() {
        val result = TextReplace.replaceAll("preço", "preço", "$1", FindOptions())
        assertEquals("$1", result.text)
    }

    @Test
    fun replaceAllHonoursMatchCase() {
        val result = TextReplace.replaceAll("Gato gato", "gato", "x", FindOptions(matchCase = true))
        assertEquals("Gato x", result.text)
    }
}
