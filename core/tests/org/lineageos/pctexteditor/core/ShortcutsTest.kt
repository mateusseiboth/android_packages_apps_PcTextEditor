package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutsTest {
    @Test
    fun fileCommands() {
        assertEquals(EditorCommand.NEW_TAB, Shortcuts.commandFor(KeyChord("N", ctrl = true)))
        assertEquals(EditorCommand.NEW_TAB, Shortcuts.commandFor(KeyChord("T", ctrl = true)))
        assertEquals(EditorCommand.OPEN, Shortcuts.commandFor(KeyChord("O", ctrl = true)))
        assertEquals(EditorCommand.SAVE, Shortcuts.commandFor(KeyChord("S", ctrl = true)))
        assertEquals(EditorCommand.SAVE_AS, Shortcuts.commandFor(KeyChord("S", ctrl = true, shift = true)))
        assertEquals(EditorCommand.CLOSE_TAB, Shortcuts.commandFor(KeyChord("W", ctrl = true)))
    }

    @Test
    fun findCommands() {
        assertEquals(EditorCommand.FIND, Shortcuts.commandFor(KeyChord("F", ctrl = true)))
        assertEquals(EditorCommand.REPLACE, Shortcuts.commandFor(KeyChord("H", ctrl = true)))
        assertEquals(EditorCommand.FIND_NEXT, Shortcuts.commandFor(KeyChord("F3")))
        assertEquals(EditorCommand.FIND_NEXT, Shortcuts.commandFor(KeyChord("G", ctrl = true)))
        assertEquals(EditorCommand.FIND_PREVIOUS, Shortcuts.commandFor(KeyChord("F3", shift = true)))
        assertEquals(EditorCommand.FIND_PREVIOUS, Shortcuts.commandFor(KeyChord("G", ctrl = true, shift = true)))
    }

    @Test
    fun tabNavigation() {
        assertEquals(EditorCommand.NEXT_TAB, Shortcuts.commandFor(KeyChord("Tab", ctrl = true)))
        assertEquals(EditorCommand.PREVIOUS_TAB, Shortcuts.commandFor(KeyChord("Tab", ctrl = true, shift = true)))
    }

    @Test
    fun plainTypingIsNotACommand() {
        assertNull(Shortcuts.commandFor(KeyChord("S")))
        assertNull(Shortcuts.commandFor(KeyChord("Tab")))
        assertNull(Shortcuts.commandFor(KeyChord("S", ctrl = true, alt = true)))
    }
}
