package org.lineageos.pctexteditor.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import org.lineageos.pctexteditor.core.KeyChord

/** Names of the keys the shortcut table (core Shortcuts) uses. */
private val KEY_NAMES = mapOf(
    Key.N to "N", Key.T to "T", Key.O to "O", Key.S to "S", Key.W to "W", Key.F to "F", Key.H to "H",
    Key.G to "G", Key.F3 to "F3", Key.Tab to "Tab",
)

fun KeyEvent.toChord(): KeyChord? {
    if (type != KeyEventType.KeyDown) return null
    val name = KEY_NAMES[key] ?: return null
    return KeyChord(name, ctrl = isCtrlPressed, shift = isShiftPressed, alt = isAltPressed)
}
