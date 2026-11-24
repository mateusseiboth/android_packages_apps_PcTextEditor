package org.lineageos.pctexteditor.core

enum class EditorCommand {
    NEW_TAB, OPEN, SAVE, SAVE_AS, CLOSE_TAB, FIND, REPLACE, FIND_NEXT, FIND_PREVIOUS, NEXT_TAB, PREVIOUS_TAB,
}

/** A key with its modifiers; [key] is the key's name ("S", "Tab", "F3"). */
data class KeyChord(val key: String, val ctrl: Boolean = false, val shift: Boolean = false, val alt: Boolean = false)

object Shortcuts {
    private val COMMANDS = mapOf(
        KeyChord("N", ctrl = true) to EditorCommand.NEW_TAB,
        KeyChord("T", ctrl = true) to EditorCommand.NEW_TAB,
        KeyChord("O", ctrl = true) to EditorCommand.OPEN,
        KeyChord("S", ctrl = true) to EditorCommand.SAVE,
        KeyChord("S", ctrl = true, shift = true) to EditorCommand.SAVE_AS,
        KeyChord("W", ctrl = true) to EditorCommand.CLOSE_TAB,
        KeyChord("F", ctrl = true) to EditorCommand.FIND,
        KeyChord("H", ctrl = true) to EditorCommand.REPLACE,
        KeyChord("F3") to EditorCommand.FIND_NEXT,
        KeyChord("G", ctrl = true) to EditorCommand.FIND_NEXT,
        KeyChord("F3", shift = true) to EditorCommand.FIND_PREVIOUS,
        KeyChord("G", ctrl = true, shift = true) to EditorCommand.FIND_PREVIOUS,
        KeyChord("Tab", ctrl = true) to EditorCommand.NEXT_TAB,
        KeyChord("Tab", ctrl = true, shift = true) to EditorCommand.PREVIOUS_TAB,
    )

    fun commandFor(chord: KeyChord): EditorCommand? = COMMANDS[chord]
}
