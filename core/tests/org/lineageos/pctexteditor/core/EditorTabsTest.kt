package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorTabsTest {
    private fun tab(id: Long, name: String = "f$id.txt", uri: String? = "content://f$id") = TabInfo(id, name, uri)

    private val three = EditorTabs.EMPTY.open(tab(1)).open(tab(2)).open(tab(3))

    @Test
    fun openAddsAtTheEndAndSelects() {
        assertEquals(listOf(1L, 2L, 3L), three.tabs.map { it.id })
        assertEquals(3L, three.selectedId)
    }

    @Test
    fun openingAFileAlreadyOpenSelectsItsTab() {
        val tabs = three.open(tab(9, uri = "content://f1"))
        assertEquals(3, tabs.tabs.size)
        assertEquals(1L, tabs.selectedId)
    }

    @Test
    fun untitledTabsNeverMatchEachOther() {
        val tabs = EditorTabs.EMPTY.open(tab(1, uri = null)).open(tab(2, uri = null))
        assertEquals(2, tabs.tabs.size)
    }

    @Test
    fun closingTheSelectedTabSelectsTheOneToItsRight() {
        val tabs = three.select(2).close(2)
        assertEquals(listOf(1L, 3L), tabs.tabs.map { it.id })
        assertEquals(3L, tabs.selectedId)
    }

    @Test
    fun closingTheLastSelectedTabSelectsTheOneToItsLeft() {
        assertEquals(2L, three.close(3).selectedId)
    }

    @Test
    fun closingAnotherTabKeepsTheSelection() {
        assertEquals(3L, three.close(1).selectedId)
    }

    @Test
    fun closingEveryTabLeavesNoSelection() {
        assertNull(three.close(1).close(2).close(3).selectedId)
    }

    @Test
    fun selectIgnoresUnknownIds() {
        assertEquals(3L, three.select(42).selectedId)
    }

    @Test
    fun updateChangesOnlyThatTab() {
        val tabs = three.update(2) { it.copy(name = "novo.txt") }
        assertEquals(listOf("f1.txt", "novo.txt", "f3.txt"), tabs.tabs.map { it.name })
    }

    @Test
    fun neighbourSelectionMovesWithWrapAround() {
        assertEquals(1L, three.selectNext().selectedId)
        assertEquals(2L, three.selectPrevious().selectedId)
    }

    @Test
    fun untitledNamesAreNumbered() {
        assertEquals("Untitled", UntitledNames.next(emptyList(), "Untitled"))
        assertEquals("Untitled 2", UntitledNames.next(listOf("Untitled"), "Untitled"))
        assertEquals("Untitled 3", UntitledNames.next(listOf("Untitled", "Untitled 2", "a.txt"), "Untitled"))
        assertEquals("Untitled 2", UntitledNames.next(listOf("Untitled", "Untitled 3"), "Untitled"))
    }
}
