package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindStateTest {
    private val text = "Gato gato GATO gatoeiro"

    @Test
    fun emptyQueryFindsNothing() {
        val state = FindState.of(text, "", FindOptions(), cursor = 0)
        assertEquals(0, state.count)
        assertNull(state.currentMatch)
    }

    @Test
    fun ignoresCaseByDefault() {
        val state = FindState.of(text, "gato", FindOptions(), cursor = 0)
        assertEquals(listOf(0..3, 5..8, 10..13, 15..18), state.matches)
    }

    @Test
    fun matchCase() {
        val state = FindState.of(text, "gato", FindOptions(matchCase = true), cursor = 0)
        assertEquals(listOf(5..8, 15..18), state.matches)
    }

    @Test
    fun wholeWord() {
        val state = FindState.of(text, "gato", FindOptions(wholeWord = true), cursor = 0)
        assertEquals(listOf(0..3, 5..8, 10..13), state.matches)
    }

    @Test
    fun wholeWordKnowsAccentedLetters() {
        val state = FindState.of("ação a época", "a", FindOptions(wholeWord = true), cursor = 0)
        assertEquals(listOf(5..5), state.matches)
    }

    @Test
    fun ignoringCaseCoversAccentedLetters() {
        assertEquals(listOf(0..3), FindState.of("AÇÃO", "ação", FindOptions(), cursor = 0).matches)
    }

    @Test
    fun queryIsLiteralNotARegex() {
        val state = FindState.of("a.b axb", "a.b", FindOptions(), cursor = 0)
        assertEquals(listOf(0..2), state.matches)
    }

    @Test
    fun startsAtTheFirstMatchFromTheCursor() {
        val state = FindState.of(text, "gato", FindOptions(), cursor = 6)
        assertEquals(10..13, state.currentMatch)
        assertEquals(3, state.position)
    }

    @Test
    fun cursorAfterTheLastMatchWrapsToTheFirst() {
        val state = FindState.of(text, "gato", FindOptions(), cursor = 20)
        assertEquals(0..3, state.currentMatch)
    }

    @Test
    fun nextAndPreviousWrapAround() {
        val last = FindState.of(text, "gato", FindOptions(), cursor = 15)
        assertEquals(0..3, last.next().currentMatch)
        assertEquals(15..18, last.next().previous().currentMatch)
    }

    @Test
    fun nextWithoutMatchesStaysEmpty() {
        assertNull(FindState.of(text, "cão", FindOptions(), cursor = 0).next().currentMatch)
    }
}
